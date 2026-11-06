package com.highlands.service;

import com.highlands.entity.ChiTietDonHang;
import com.highlands.entity.ChiTietDonHangTopping;
import com.highlands.entity.DonHang;
import com.highlands.entity.KhuyenMai;
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
import org.springframework.transaction.TransactionDefinition;
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
    public static final String DANG_XU_LY = "Đang xử lý";
    public static final String DANG_GIAO = "Đang giao";
    public static final String HOAN_THANH = "Hoàn thành";
    public static final String DA_HUY = "Đã hủy";
    public static final List<String> TRANG_THAI = List.of(DANG_XU_LY, DANG_GIAO, HOAN_THANH, DA_HUY);
    private static final int SO_DON_TOI_DA = 500;
    private static final BigDecimal TIEN_KHACH_TOI_DA = new BigDecimal("100000000000"); // 100 tỷ, chặn nhập nhầm

    public record Dong(int soLuong, String ten, BigDecimal donGia, List<String> topping,
                       BigDecimal giaTopping, BigDecimal thanhTien) {
    }

    public record HoaDon(Long maDon, LocalDateTime ngay, String nhanVien, List<Dong> dong,
                         BigDecimal tienHang, String khuyenMai, BigDecimal giamGia,
                         BigDecimal tongThanhToan, String phuongThuc, BigDecimal tienKhach, BigDecimal tienThua,
                         String trangThai, String ghiChu) {
    }

    public record DonLichSu(Long thanhToanId, Long maDon, String nhanVien, LocalDateTime ngay,
                            BigDecimal tong, String phuongThuc, String trangThai) {
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
    private final TransactionTemplate ghiThanhToan;

    public record BaoGia(BigDecimal tienHang, BigDecimal giam, String dauVanTay) {
        public BigDecimal tong() { return tienHang.subtract(giam); }
    }

    public BaoGia baoGia(List<DongGio> gio, String maKhuyenMai) {
        return doc.execute(st -> tinhBaoGia(gio, maKhuyenMai));
    }

    public HoaDon timHoaDonDaLuu(String maGiaoDich) {
        return doc.execute(st -> thanhToanRepo.findByMaGiaoDich(maGiaoDich).map(this::dungHoaDon).orElse(null));
    }

    private BaoGia tinhBaoGia(List<DongGio> gio, String maKhuyenMai) {
        if (gio == null || gio.isEmpty()) throw new LoiNghiepVu("Giỏ hàng đang trống");
        BigDecimal tien = BigDecimal.ZERO;
        List<String> dau = new ArrayList<>();
        for (DongGio d : gio) {
            if (d.getSoLuong() < 1 || d.getSoLuong() > 99) throw new LoiNghiepVu("Số lượng phải từ 1 đến 99");
            SanPham sp = sanPhamRepo.findById(d.getSanPham().getId())
                    .orElseThrow(() -> new LoiNghiepVu("Món đã bị xóa. Hãy làm mới thực đơn và kiểm tra giỏ hàng."));
            if (sp.getGia() <= 0) throw new LoiNghiepVu("Giá sản phẩm không hợp lệ");
            BigDecimal gia = BigDecimal.valueOf(sp.getGia());
            List<String> tp = new ArrayList<>();
            for (Long id : d.getTopping().stream().map(Topping::getId).distinct().sorted().toList()) {
                Topping t = toppingRepo.findById(id)
                        .orElseThrow(() -> new LoiNghiepVu("Topping đã bị xóa. Hãy kiểm tra lại giỏ hàng."));
                if (t.getGia() < 0) throw new LoiNghiepVu("Giá topping không hợp lệ");
                gia = gia.add(BigDecimal.valueOf(t.getGia()));
                tp.add(id + ":" + t.getGia());
            }
            tien = tien.add(gia.multiply(BigDecimal.valueOf(d.getSoLuong())));
            dau.add(sp.getId() + ":" + sp.getGia() + ":" + d.getSoLuong() + ":" + tp);
        }
        BigDecimal giam = BigDecimal.ZERO;
        if (maKhuyenMai != null && !maKhuyenMai.isBlank()) {
            BanHangService.ApDung km = banHang.apDung(maKhuyenMai, tien);
            if (!km.hopLe()) throw new LoiNghiepVu(km.thongBao());
            giam = km.giam();
            KhuyenMai k = km.khuyenMai();
            dau.add("KM:" + k.getId() + ":" + k.getLoai() + ":" + k.getPhanTramGiam()
                    + ":" + k.getSoTienGiam() + ":" + k.getDonToiThieu());
        }
        return new BaoGia(tien, giam, dau.toString());
    }

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
        this.ghiThanhToan = new TransactionTemplate(tm);
        // Khóa phạm vi tra mã giao dịch tới lúc commit, kể cả khi chưa có bản ghi.
        // Không cần thêm cột hoặc ràng buộc vào database.
        this.ghiThanhToan.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
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
        return thanhToan(nhanVienId, gio, maKhuyenMai, tienKhach, maGiaoDich, phuongThuc, null);
    }

    public HoaDon thanhToan(Long nhanVienId, List<DongGio> gio, String maKhuyenMai,
                            BigDecimal tienKhach, String maGiaoDich, String phuongThuc, BaoGia daXacNhan) {
        if (!TIEN_MAT.equals(phuongThuc) && !CHUYEN_KHOAN.equals(phuongThuc)) {
            throw new LoiNghiepVu("Phương thức thanh toán không hợp lệ");
        }
        final boolean chuyenKhoan = CHUYEN_KHOAN.equals(phuongThuc);
        if (nhanVienId == null) throw new LoiNghiepVu("Chưa xác định nhân viên bán hàng");
        if (gio == null || gio.isEmpty()) throw new LoiNghiepVu("Giỏ hàng đang trống");
        String khoa = maGiaoDich == null ? "" : maGiaoDich.trim();
        if (khoa.isEmpty() || khoa.length() > 64) throw new LoiNghiepVu("Mã giao dịch không hợp lệ");
        if (!chuyenKhoan) {
            if (tienKhach == null || tienKhach.signum() < 0) throw new LoiNghiepVu("Vui lòng nhập số tiền khách đưa hợp lệ");
            if (tienKhach.compareTo(TIEN_KHACH_TOI_DA) > 0 || tienKhach.stripTrailingZeros().scale() > 0) {
                throw new LoiNghiepVu("Số tiền khách đưa không hợp lệ");
            }
        }
        List<DongGio> banSao = new ArrayList<>(gio);
        try {
            return ghiThanhToan.execute(st -> luuDon(nhanVienId, banSao, maKhuyenMai, tienKhach, khoa, phuongThuc, daXacNhan));
        } catch (DataIntegrityViolationException e) {
            // Hai lần bấm chạy song song: lần kia đã lưu xong với cùng mã giao dịch
            HoaDon daLuu = doc.execute(st -> thanhToanRepo.findByMaGiaoDich(khoa).map(this::dungHoaDon).orElse(null));
            if (daLuu != null) return daLuu;
            throw e;
        }
    }

    private HoaDon luuDon(Long nhanVienId, List<DongGio> gio, String maKhuyenMai, BigDecimal tienKhach, String khoa,
                          String phuongThuc, BaoGia daXacNhan) {
        // Đã lưu rồi (gửi lại) -> trả đúng hóa đơn đó
        var daCo = thanhToanRepo.findByMaGiaoDich(khoa);
        if (daCo.isPresent()) return dungHoaDon(daCo.get());

        BaoGia hienTai = tinhBaoGia(gio, maKhuyenMai);
        if (daXacNhan != null && !hienTai.equals(daXacNhan)) {
            throw new LoiNghiepVu("Giá hoặc khuyến mãi vừa thay đổi. Chưa lưu đơn; hãy kiểm tra lại và xác nhận số tiền mới.");
        }

        NhanVien nv = nhanVienRepo.findById(nhanVienId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy nhân viên"));
        if (!nv.isDangLam()) throw new LoiNghiepVu("Nhân viên không còn làm việc");

        // 1. Dựng lại từng dòng từ CSDL (không tin giá từ giao diện)
        record DongDaXacNhan(SanPham sp, int soLuong, List<Topping> topping) {
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
            BigDecimal giaTp = tps.stream().map(t -> BigDecimal.valueOf(t.getGia()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dong.add(new DongDaXacNhan(sp, sl, tps));
            tienHang = tienHang.add(BigDecimal.valueOf(sp.getGia()).add(giaTp)
                    .multiply(BigDecimal.valueOf(sl)));
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
        tt.setTrangThai(HOAN_THANH);
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
                        x.getNgayThanhToan(), x.getTongThanhToan(), x.getPhuongThuc(), x.getTrangThai()));
            }
            return kq;
        });
    }

    // ===== Chỉnh sửa đơn hàng =====

    public record ChiTietSua(Long id, String ten, BigDecimal donGia, BigDecimal giaTopping,
                             List<String> topping, int soLuong) {
    }

    public record DonSua(Long thanhToanId, Long maDon, String trangThai, List<ChiTietSua> dong, String khuyenMai,
                         BigDecimal tienHang, BigDecimal giamGia, BigDecimal tong,
                         String phuongThuc, BigDecimal tienKhach, String ghiChu) {
    }

    public record KetQuaSua(HoaDon hoaDon, String canhBao) {
    }

    // Dữ liệu đưa lên màn hình chỉnh sửa
    public DonSua layDeSua(Long thanhToanId) {
        return doc.execute(st -> {
            ThanhToanDonHang tt = thanhToanRepo.findById(thanhToanId)
                    .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy đơn hàng"));
            List<ChiTietDonHang> cts = chiTietRepo.findByDonHang_IdOrderByIdAsc(tt.getDonHang().getId());
            Map<Long, List<ChiTietDonHangTopping>> tp = toppingTheoDong(cts);
            List<ChiTietSua> dong = new ArrayList<>();
            for (ChiTietDonHang ct : cts) {
                List<ChiTietDonHangTopping> tps = tp.getOrDefault(ct.getId(), List.of());
                BigDecimal giaTp = tps.stream().map(ChiTietDonHangTopping::getGiaTopping)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                dong.add(new ChiTietSua(ct.getId(), ct.getSanPham().getTen(), BigDecimal.valueOf(ct.getDonGia()), giaTp,
                        tps.stream().map(ChiTietDonHangTopping::getTenTopping).toList(), ct.getSoLuong()));
            }
            return new DonSua(tt.getId(), tt.getDonHang().getId(), tt.getTrangThai(), dong, tt.getMaKhuyenMai(),
                    tt.getTienHang(), tt.getGiamGia(), tt.getTongThanhToan(),
                    tt.getPhuongThuc(), tt.getTienKhachDua(), tt.getGhiChu());
        });
    }

    // Cập nhật một đơn đã thanh toán: trạng thái, số lượng từng món (0 = bỏ món), thông tin giao dịch.
    // - Chọn "Đã hủy": chỉ đổi trạng thái (đơn bị loại khỏi thống kê, không sửa lại được nữa).
    // - Số lượng đổi: tính lại tiền hàng, khuyến mãi (giữ mã cũ nếu còn đủ đơn tối thiểu), tổng tiền
    //   và cập nhật don_hang.tong_tien để trang Thống kê đúng.
    public KetQuaSua capNhatDon(Long thanhToanId, String trangThai, Map<Long, Integer> soLuongMoi,
                                String phuongThuc, BigDecimal tienKhach, String ghiChu) {
        if (thanhToanId == null) throw new LoiNghiepVu("Không xác định được đơn hàng");
        if (trangThai == null || !TRANG_THAI.contains(trangThai)) throw new LoiNghiepVu("Trạng thái đơn không hợp lệ");
        if (!TIEN_MAT.equals(phuongThuc) && !CHUYEN_KHOAN.equals(phuongThuc)) {
            throw new LoiNghiepVu("Phương thức thanh toán không hợp lệ");
        }
        String note = ghiChu == null ? "" : ghiChu.trim();
        if (note.length() > 200) throw new LoiNghiepVu("Ghi chú tối đa 200 ký tự");
        final String ghiChuLuu = note.isEmpty() ? null : note;
        final Map<Long, Integer> sl = soLuongMoi == null ? Map.of() : soLuongMoi;

        return ghi.execute(st -> {
            ThanhToanDonHang tt = thanhToanRepo.findById(thanhToanId)
                    .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy đơn hàng"));
            if (DA_HUY.equals(tt.getTrangThai())) throw new LoiNghiepVu("Đơn đã hủy, không thể chỉnh sửa");

            // Hủy đơn: chỉ đổi trạng thái, giữ nguyên số liệu để còn xem lại
            if (DA_HUY.equals(trangThai)) {
                tt.setTrangThai(DA_HUY);
                tt.setGhiChu(ghiChuLuu);
                thanhToanRepo.save(tt);
                return new KetQuaSua(dungHoaDon(tt), null);
            }

            List<ChiTietDonHang> cts = chiTietRepo.findByDonHang_IdOrderByIdAsc(tt.getDonHang().getId());
            java.util.Set<Long> idHopLe = new java.util.HashSet<>();
            for (ChiTietDonHang ct : cts) idHopLe.add(ct.getId());
            for (Long id : sl.keySet()) {
                if (!idHopLe.contains(id)) throw new LoiNghiepVu("Món cần sửa không thuộc đơn hàng này");
            }
            Map<Long, List<ChiTietDonHangTopping>> tp = toppingTheoDong(cts);

            boolean doiSoLuong = false;
            int conLai = 0;
            BigDecimal tienHang = BigDecimal.ZERO;
            for (ChiTietDonHang ct : cts) {
                int moi = sl.getOrDefault(ct.getId(), ct.getSoLuong());
                if (moi < 0 || moi > 99) throw new LoiNghiepVu("Số lượng mỗi món phải từ 0 đến 99");
                if (moi != ct.getSoLuong()) doiSoLuong = true;
                if (moi > 0) {
                    conLai++;
                    BigDecimal giaTp = tp.getOrDefault(ct.getId(), List.of()).stream()
                            .map(ChiTietDonHangTopping::getGiaTopping).reduce(BigDecimal.ZERO, BigDecimal::add);
                    tienHang = tienHang.add(BigDecimal.valueOf(ct.getDonGia()).add(giaTp).multiply(BigDecimal.valueOf(moi)));
                }
            }
            if (conLai == 0) {
                throw new LoiNghiepVu("Đơn phải còn ít nhất một món. Muốn bỏ cả đơn, hãy chọn trạng thái \"Đã hủy\".");
            }

            String canhBao = null;
            BigDecimal giam;
            BigDecimal tong;
            if (doiSoLuong) {
                for (ChiTietDonHang ct : cts) {
                    int moi = sl.getOrDefault(ct.getId(), ct.getSoLuong());
                    if (moi == ct.getSoLuong()) continue;
                    if (moi == 0) {
                        chiTietToppingRepo.deleteAll(tp.getOrDefault(ct.getId(), List.of()));
                        chiTietToppingRepo.flush();
                        chiTietRepo.delete(ct);
                    } else {
                        ct.setSoLuong(moi);
                        chiTietRepo.save(ct);
                    }
                }
                giam = BigDecimal.ZERO;
                KhuyenMai km = tt.getKhuyenMai();
                if (km != null) {
                    if (km.getDonToiThieu() != null && tienHang.compareTo(BigDecimal.valueOf(km.getDonToiThieu())) < 0) {
                        canhBao = "Đơn không còn đạt mức tối thiểu của mã " + km.getMa() + " nên đã bỏ khuyến mãi.";
                        tt.setKhuyenMai(null);
                        tt.setMaKhuyenMai(null);
                    } else {
                        giam = giamKhuyenMai(km, tienHang);
                    }
                }
                tong = tienHang.subtract(giam);
            } else {
                tienHang = tt.getTienHang();
                giam = tt.getGiamGia();
                tong = tt.getTongThanhToan();
            }

            BigDecimal khach;
            BigDecimal thua;
            if (CHUYEN_KHOAN.equals(phuongThuc)) {
                khach = tong;
                thua = BigDecimal.ZERO;
            } else {
                if (tienKhach == null || tienKhach.signum() <= 0) throw new LoiNghiepVu("Vui lòng nhập số tiền khách đưa");
                if (tienKhach.compareTo(TIEN_KHACH_TOI_DA) > 0 || tienKhach.stripTrailingZeros().scale() > 0) {
                    throw new LoiNghiepVu("Số tiền khách đưa không hợp lệ");
                }
                if (tienKhach.compareTo(tong) < 0) {
                    throw new LoiNghiepVu("Tổng tiền của đơn là " + String.format("%,d", tong.longValue()).replace(',', '.')
                            + "đ, tiền khách đưa phải từ mức này trở lên");
                }
                khach = tienKhach;
                thua = khach.subtract(tong);
            }

            tt.setTienHang(tienHang);
            tt.setGiamGia(giam);
            tt.setTongThanhToan(tong);
            tt.setPhuongThuc(phuongThuc);
            tt.setTienKhachDua(khach);
            tt.setTienThua(thua);
            tt.setTrangThai(trangThai);
            tt.setGhiChu(ghiChuLuu);
            thanhToanRepo.save(tt);

            DonHang dh = tt.getDonHang();
            dh.setTongTien(tong.longValueExact());
            donHangRepo.save(dh);
            return new KetQuaSua(dungHoaDon(tt), canhBao);
        });
    }

    // Cùng cách tính với BanHangService.apDung nhưng không kiểm tra ngày (mã đã được dùng hợp lệ lúc bán)
    private static BigDecimal giamKhuyenMai(KhuyenMai k, BigDecimal tienHang) {
        BigDecimal giam;
        if ("GIAM_TIEN".equals(k.getLoai())) {
            giam = BigDecimal.valueOf(k.getSoTienGiam() == null ? 0 : k.getSoTienGiam());
        } else {
            giam = tienHang.multiply(BigDecimal.valueOf(k.getPhanTramGiam()))
                    .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        }
        return giam.min(tienHang).max(BigDecimal.ZERO);
    }

    private Map<Long, List<ChiTietDonHangTopping>> toppingTheoDong(List<ChiTietDonHang> danhSachChiTiet) {
        Map<Long, List<ChiTietDonHangTopping>> ketQua = new HashMap<>();
        if (danhSachChiTiet.isEmpty()) {
            return ketQua;
        }

        List<Long> danhSachId = new ArrayList<>();
        for (ChiTietDonHang chiTiet : danhSachChiTiet) {
            danhSachId.add(chiTiet.getId());
        }
        for (ChiTietDonHangTopping topping : chiTietToppingRepo.findByChiTietDonHang_IdIn(danhSachId)) {
            Long chiTietId = topping.getChiTietDonHang().getId();
            List<ChiTietDonHangTopping> danhSachTopping = ketQua.get(chiTietId);
            if (danhSachTopping == null) {
                danhSachTopping = new ArrayList<>();
                ketQua.put(chiTietId, danhSachTopping);
            }
            danhSachTopping.add(topping);
        }
        return ketQua;
    }

    // Dựng hóa đơn từ dữ liệu đã lưu (dùng cho cả lúc vừa thanh toán và xem lại)
    private HoaDon dungHoaDon(ThanhToanDonHang tt) {
        List<ChiTietDonHang> cts = chiTietRepo.findByDonHang_IdOrderByIdAsc(tt.getDonHang().getId());
        Map<Long, List<ChiTietDonHangTopping>> tpTheoDong = toppingTheoDong(cts);
        List<Dong> dong = new ArrayList<>();
        for (ChiTietDonHang ct : cts) {
            List<ChiTietDonHangTopping> tps = tpTheoDong.getOrDefault(ct.getId(), List.of());
            BigDecimal giaTopping = BigDecimal.ZERO;
            List<String> tenTopping = new ArrayList<>();
            for (ChiTietDonHangTopping topping : tps) {
                giaTopping = giaTopping.add(topping.getGiaTopping());
                tenTopping.add(topping.getTenTopping());
            }
            BigDecimal donGia = BigDecimal.valueOf(ct.getDonGia());
            BigDecimal thanhTien = donGia.add(giaTopping).multiply(BigDecimal.valueOf(ct.getSoLuong()));
            dong.add(new Dong(ct.getSoLuong(), ct.getSanPham().getTen(), donGia,
                    java.util.Collections.unmodifiableList(tenTopping), giaTopping, thanhTien));
        }
        return new HoaDon(tt.getDonHang().getId(), tt.getNgayThanhToan(), tt.getNhanVien().getHoTen(), dong,
                tt.getTienHang(), tt.getMaKhuyenMai(), tt.getGiamGia(), tt.getTongThanhToan(),
                tt.getPhuongThuc(), tt.getTienKhachDua(), tt.getTienThua(), tt.getTrangThai(), tt.getGhiChu());
    }
}
