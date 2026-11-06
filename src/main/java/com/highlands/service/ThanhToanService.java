package com.highlands.service;

import com.highlands.entity.ChiTietDonHang;
import com.highlands.entity.ChiTietDonHangTopping;
import com.highlands.entity.DonHang;
import com.highlands.entity.NhanVien;
import com.highlands.entity.SanPham;
import com.highlands.entity.ThanhToanDonHang;
import com.highlands.entity.Topping;
import com.highlands.repository.ChiTietDonHangRepository;
import com.highlands.repository.ChiTietDonHangToppingRepository;
import com.highlands.repository.DonHangRepository;
import com.highlands.repository.NhanVienRepository;
import com.highlands.repository.SanPhamRepository;
import com.highlands.repository.ThanhToanDonHangRepository;
import com.highlands.repository.ToppingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Thanh toán đơn hàng của nhân viên (tiền mặt), xem lại hóa đơn và lịch sử.
//
// Ghi vào các bảng của phân hệ quản lý theo đúng quy ước cũ để trang Thống kê vẫn đúng:
//   don_hang.tong_tien           = tổng thanh toán (đã trừ khuyến mãi)
//   chi_tiet_don_hang.don_gia    = giá của riêng món (không gồm topping)
// Phần riêng của nhân viên (ai bán, mã giảm, tiền khách đưa, topping...) nằm ở các bảng mới.
@Service
public class ThanhToanService {

    public static final String TIEN_MAT = "Tiền mặt";
    public static final String CHUYEN_KHOAN = "Chuyển khoản";
    private static final int SO_DON_TOI_DA = 500;
    private static final BigDecimal TIEN_KHACH_TOI_DA = new BigDecimal("100000000000"); // 100 tỷ, chặn nhập nhầm

    public record Dong(int soLuong, String ten, BigDecimal donGia, List<String> topping,
                       BigDecimal giaTopping, BigDecimal thanhTien) {
    }

    public record HoaDon(Long maDon, LocalDateTime ngay, String nhanVien, List<Dong> dong,
                         BigDecimal tienHang, String khuyenMai, BigDecimal giamGia,
                         BigDecimal tongThanhToan, String phuongThuc, BigDecimal tienKhach, BigDecimal tienThua) {
    }

    public record DonLichSu(Long thanhToanId, Long maDon, String nhanVien, LocalDateTime ngay,
                            BigDecimal tong, String phuongThuc) {
    }

    private final DonHangRepository donHangRepo;
    private final ChiTietDonHangRepository chiTietRepo;
    private final ChiTietDonHangToppingRepository chiTietToppingRepo;
    private final ThanhToanDonHangRepository thanhToanRepo;
    private final NhanVienRepository nhanVienRepo;
    private final SanPhamRepository sanPhamRepo;
    private final ToppingRepository toppingRepo;
    private final BanHangService banHang;
    private final TransactionTemplate ghi;
    private final TransactionTemplate doc;

    public ThanhToanService(DonHangRepository donHangRepo, ChiTietDonHangRepository chiTietRepo,
                            ChiTietDonHangToppingRepository chiTietToppingRepo,
                            ThanhToanDonHangRepository thanhToanRepo, NhanVienRepository nhanVienRepo,
                            SanPhamRepository sanPhamRepo, ToppingRepository toppingRepo,
                            BanHangService banHang, PlatformTransactionManager tm) {
        this.donHangRepo = donHangRepo;
        this.chiTietRepo = chiTietRepo;
        this.chiTietToppingRepo = chiTietToppingRepo;
        this.thanhToanRepo = thanhToanRepo;
        this.nhanVienRepo = nhanVienRepo;
        this.sanPhamRepo = sanPhamRepo;
        this.toppingRepo = toppingRepo;
        this.banHang = banHang;
        this.ghi = new TransactionTemplate(tm);
        this.doc = new TransactionTemplate(tm);
        this.doc.setReadOnly(true);
    }

    // ===== Thanh toán =====

    // Giá, topping, khuyến mãi đều được tính lại từ CSDL; số tiền từ giao diện chỉ là tiền khách đưa.
    // Gọi lại với cùng maGiaoDich (bấm 2 lần, lỗi mạng...) thì trả lại hóa đơn đã lưu, không tạo đơn thứ hai.
    public HoaDon thanhToan(Long nhanVienId, List<DongGio> gio, String maKhuyenMai,
                            BigDecimal tienKhach, String maGiaoDich) {
        return thanhToan(nhanVienId, gio, maKhuyenMai, tienKhach, maGiaoDich, TIEN_MAT);
    }

    // phuongThuc: TIEN_MAT (cần tiền khách đưa) hoặc CHUYEN_KHOAN (khách chuyển đúng số tiền, tienKhach bỏ qua)
    public HoaDon thanhToan(Long nhanVienId, List<DongGio> gio, String maKhuyenMai,
                            BigDecimal tienKhach, String maGiaoDich, String phuongThuc) {
        if (!TIEN_MAT.equals(phuongThuc) && !CHUYEN_KHOAN.equals(phuongThuc)) {
            throw new LoiNghiepVu("Phương thức thanh toán không hợp lệ");
        }
        final boolean chuyenKhoan = CHUYEN_KHOAN.equals(phuongThuc);
        if (nhanVienId == null) throw new LoiNghiepVu("Chưa xác định nhân viên bán hàng");
        if (gio == null || gio.isEmpty()) throw new LoiNghiepVu("Giỏ hàng đang trống");
        String khoa = maGiaoDich == null ? "" : maGiaoDich.trim();
        if (khoa.isEmpty() || khoa.length() > 64) throw new LoiNghiepVu("Mã giao dịch không hợp lệ");
        if (!chuyenKhoan) {
            if (tienKhach == null || tienKhach.signum() <= 0) throw new LoiNghiepVu("Vui lòng nhập số tiền khách đưa");
            if (tienKhach.compareTo(TIEN_KHACH_TOI_DA) > 0 || tienKhach.stripTrailingZeros().scale() > 0) {
                throw new LoiNghiepVu("Số tiền khách đưa không hợp lệ");
            }
        }
        List<DongGio> banSao = new ArrayList<>(gio);
        try {
            return ghi.execute(st -> luuDon(nhanVienId, banSao, maKhuyenMai, tienKhach, khoa, phuongThuc));
        } catch (DataIntegrityViolationException e) {
            // Hai lần bấm chạy song song: lần kia đã lưu xong với cùng mã giao dịch
            HoaDon daLuu = doc.execute(st -> thanhToanRepo.findByMaGiaoDich(khoa).map(this::dungHoaDon).orElse(null));
            if (daLuu != null) return daLuu;
            throw e;
        }
    }

    private HoaDon luuDon(Long nhanVienId, List<DongGio> gio, String maKhuyenMai, BigDecimal tienKhach, String khoa,
                          String phuongThuc) {
        // Đã lưu rồi (gửi lại) -> trả đúng hóa đơn đó
        var daCo = thanhToanRepo.findByMaGiaoDich(khoa);
        if (daCo.isPresent()) return dungHoaDon(daCo.get());

        NhanVien nv = nhanVienRepo.findById(nhanVienId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy nhân viên"));
        if (!nv.isDangLam()) throw new LoiNghiepVu("Nhân viên không còn làm việc");

        // 1. Dựng lại từng dòng từ CSDL (không tin giá từ giao diện)
        record DongDaXacNhan(SanPham sp, int soLuong, List<Topping> topping, long giaTopping) {
        }
        List<DongDaXacNhan> dong = new ArrayList<>();
        BigDecimal tienHang = BigDecimal.ZERO;
        for (DongGio g : gio) {
            int sl = g.getSoLuong();
            if (sl < 1 || sl > 99) throw new LoiNghiepVu("Số lượng mỗi món phải từ 1 đến 99");
            SanPham sp = sanPhamRepo.findById(g.getSanPham().getId()).orElseThrow(() ->
                    new LoiNghiepVu("Món \"" + g.getSanPham().getTen() + "\" không còn trong thực đơn"));
            Map<Long, Topping> tpDuyNhat = new LinkedHashMap<>();
            for (Topping t : g.getTopping()) {
                Topping tp = toppingRepo.findById(t.getId()).orElseThrow(() ->
                        new LoiNghiepVu("Topping \"" + t.getTen() + "\" không còn tồn tại"));
                tpDuyNhat.put(tp.getId(), tp);
            }
            List<Topping> tps = new ArrayList<>(tpDuyNhat.values());
            long giaTp = tps.stream().mapToLong(Topping::getGia).sum();
            dong.add(new DongDaXacNhan(sp, sl, tps, giaTp));
            tienHang = tienHang.add(BigDecimal.valueOf((sp.getGia() + giaTp) * sl));
        }

        // 2. Khuyến mãi
        BigDecimal giam = BigDecimal.ZERO;
        BanHangService.ApDung km = null;
        if (maKhuyenMai != null && !maKhuyenMai.isBlank()) {
            km = banHang.apDung(maKhuyenMai, tienHang);
            if (!km.hopLe()) throw new LoiNghiepVu(km.thongBao());
            giam = km.giam();
        }
        BigDecimal tong = tienHang.subtract(giam);

        // 3. Tiền khách đưa (chuyển khoản: khách chuyển đúng số tiền, không có tiền thừa)
        if (CHUYEN_KHOAN.equals(phuongThuc)) tienKhach = tong;
        if (tienKhach.compareTo(tong) < 0) throw new LoiNghiepVu("Tiền khách đưa chưa đủ");
        BigDecimal tienThua = tienKhach.subtract(tong);

        // 4. Ghi đơn hàng (bảng của phân hệ quản lý)
        DonHang dh = new DonHang();
        dh.setNgayDat(LocalDate.now());
        dh.setTongTien(tong.longValueExact());
        dh = donHangRepo.save(dh);

        for (DongDaXacNhan d : dong) {
            ChiTietDonHang ct = new ChiTietDonHang();
            ct.setDonHang(dh);
            ct.setSanPham(d.sp());
            ct.setSoLuong(d.soLuong());
            ct.setDonGia(d.sp().getGia());
            ct = chiTietRepo.save(ct);
            for (Topping t : d.topping()) {
                ChiTietDonHangTopping ctt = new ChiTietDonHangTopping();
                ctt.setChiTietDonHang(ct);
                ctt.setTopping(t);
                ctt.setTenTopping(t.getTen());
                ctt.setGiaTopping(BigDecimal.valueOf(t.getGia()));
                chiTietToppingRepo.save(ctt);
            }
        }

        // 5. Ghi thông tin bán hàng
        ThanhToanDonHang tt = new ThanhToanDonHang();
        tt.setDonHang(dh);
        tt.setNhanVien(nv);
        if (km != null) {
            tt.setKhuyenMai(km.khuyenMai());
            tt.setMaKhuyenMai(km.khuyenMai().getMa());
        }
        tt.setTienHang(tienHang);
        tt.setGiamGia(giam);
        tt.setTongThanhToan(tong);
        tt.setPhuongThuc(phuongThuc);
        tt.setTienKhachDua(tienKhach);
        tt.setTienThua(tienThua);
        tt.setNgayThanhToan(LocalDateTime.now());
        tt.setMaGiaoDich(khoa);
        tt = thanhToanRepo.save(tt);
        return dungHoaDon(tt);
    }

    // ===== Xem lại hóa đơn / lịch sử =====

    public HoaDon layHoaDon(Long thanhToanId) {
        return doc.execute(st -> dungHoaDon(thanhToanRepo.findById(thanhToanId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy hóa đơn"))));
    }

    // maDon: mã người dùng gõ ("HD000012", "12"), trống = tất cả. tu / den: lọc theo ngày thanh toán (có thể null)
    public List<DonLichSu> timDonHang(String maDon, LocalDate tu, LocalDate den) {
        if (tu != null && den != null && tu.isAfter(den)) {
            throw new LoiNghiepVu("Ngày bắt đầu không được sau ngày kết thúc");
        }
        boolean coMa = maDon != null && !maDon.isBlank();
        Long id = coMa ? TienIch.docMaDon(maDon) : null;
        if (coMa && id == null) throw new LoiNghiepVu("Mã đơn không hợp lệ (VD: HD000012)");

        LocalDateTime t = (tu == null ? LocalDate.of(1970, 1, 1) : tu).atStartOfDay();
        LocalDateTime d = (den == null ? LocalDate.of(2999, 12, 31) : den).plusDays(1).atStartOfDay();

        return doc.execute(st -> {
            List<ThanhToanDonHang> ds;
            if (id != null) {
                ds = thanhToanRepo.timTheoDonHang(id).stream()
                        .filter(x -> !x.getNgayThanhToan().isBefore(t) && x.getNgayThanhToan().isBefore(d))
                        .toList();
            } else {
                ds = thanhToanRepo.timTheoKhoang(t, d, PageRequest.of(0, SO_DON_TOI_DA));
            }
            List<DonLichSu> kq = new ArrayList<>();
            for (ThanhToanDonHang x : ds) {
                kq.add(new DonLichSu(x.getId(), x.getDonHang().getId(), x.getNhanVien().getHoTen(),
                        x.getNgayThanhToan(), x.getTongThanhToan(), x.getPhuongThuc()));
            }
            return kq;
        });
    }

    // Dựng hóa đơn từ dữ liệu đã lưu (dùng cho cả lúc vừa thanh toán và xem lại)
    private HoaDon dungHoaDon(ThanhToanDonHang tt) {
        List<ChiTietDonHang> cts = chiTietRepo.findByDonHang_IdOrderByIdAsc(tt.getDonHang().getId());
        Map<Long, List<ChiTietDonHangTopping>> tpTheoDong = new HashMap<>();
        if (!cts.isEmpty()) {
            List<Long> ids = cts.stream().map(ChiTietDonHang::getId).toList();
            for (ChiTietDonHangTopping t : chiTietToppingRepo.findByChiTietDonHang_IdIn(ids)) {
                tpTheoDong.computeIfAbsent(t.getChiTietDonHang().getId(), k -> new ArrayList<>()).add(t);
            }
        }
        List<Dong> dong = new ArrayList<>();
        for (ChiTietDonHang ct : cts) {
            List<ChiTietDonHangTopping> tps = tpTheoDong.getOrDefault(ct.getId(), List.of());
            BigDecimal giaTp = tps.stream().map(ChiTietDonHangTopping::getGiaTopping)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal donGia = BigDecimal.valueOf(ct.getDonGia());
            dong.add(new Dong(ct.getSoLuong(), ct.getSanPham().getTen(), donGia,
                    tps.stream().map(ChiTietDonHangTopping::getTenTopping).toList(), giaTp,
                    donGia.add(giaTp).multiply(BigDecimal.valueOf(ct.getSoLuong()))));
        }
        return new HoaDon(tt.getDonHang().getId(), tt.getNgayThanhToan(), tt.getNhanVien().getHoTen(), dong,
                tt.getTienHang(), tt.getMaKhuyenMai(), tt.getGiamGia(), tt.getTongThanhToan(),
                tt.getPhuongThuc(), tt.getTienKhachDua(), tt.getTienThua());
    }
}
