package com.highlands.service;

import com.highlands.entity.KhuyenMai;
import com.highlands.entity.SanPham;
import com.highlands.entity.Topping;
import com.highlands.repository.KhuyenMaiRepository;
import com.highlands.repository.SanPhamRepository;
import com.highlands.repository.ToppingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

// Dữ liệu cho màn hình bán hàng: thực đơn, topping, kiểm tra mã khuyến mãi
@Service
public class BanHangService {

    // Một món trên thực đơn kèm danh mục (khoaDanhMuc = id danh mục, dùng để lọc theo chip)
    public record MonThucDon(SanPham sanPham, Object khoaDanhMuc, String tenDanhMuc) {
    }

    // Kết quả kiểm tra mã: hợp lệ thì có ten + giam, không hợp lệ thì có thongBao
    public record KetQuaKhuyenMai(boolean hopLe, String ten, BigDecimal giam, String thongBao) {
    }

    // Bản đầy đủ (kèm entity) cho ThanhToanService dùng khi lưu đơn
    record ApDung(KhuyenMai khuyenMai, BigDecimal giam, String thongBao) {
        boolean hopLe() {
            return khuyenMai != null;
        }
    }

    private final SanPhamRepository sanPhamRepo;
    private final ToppingRepository toppingRepo;
    private final KhuyenMaiRepository khuyenMaiRepo;

    public BanHangService(SanPhamRepository sanPhamRepo, ToppingRepository toppingRepo, KhuyenMaiRepository khuyenMaiRepo) {
        this.sanPhamRepo = sanPhamRepo;
        this.toppingRepo = toppingRepo;
        this.khuyenMaiRepo = khuyenMaiRepo;
    }

    // Sắp theo danh mục rồi theo tên món
    public List<MonThucDon> layThucDon() {
        return sanPhamRepo.findAll(Sort.by("id")).stream()
                .map(s -> new MonThucDon(s, s.getDanhMuc().getId(), s.getDanhMuc().getTen()))
                .sorted(Comparator.comparing((MonThucDon m) -> TienIch.boDau(m.tenDanhMuc()))
                        .thenComparing(m -> TienIch.boDau(m.sanPham().getTen())))
                .toList();
    }

    public List<Topping> layTopping() {
        return toppingRepo.findAll(Sort.by("ten"));
    }

    public KetQuaKhuyenMai kiemTraKhuyenMai(String ma, BigDecimal tienHang) {
        ApDung a = apDung(ma, tienHang);
        if (!a.hopLe()) return new KetQuaKhuyenMai(false, null, BigDecimal.ZERO, a.thongBao());
        return new KetQuaKhuyenMai(true, a.khuyenMai().getTen(), a.giam(), null);
    }

    // Quy tắc áp mã (dùng chung cho màn hình bán hàng và lúc thanh toán)
    ApDung apDung(String ma, BigDecimal tienHang) {
        String m = ma == null ? "" : ma.trim();
        if (m.isEmpty()) return loi("Vui lòng nhập mã khuyến mãi.");
        if (tienHang == null || tienHang.signum() <= 0) return loi("Giỏ hàng đang trống.");

        KhuyenMai k = khuyenMaiRepo.findFirstByMaIgnoreCase(m).orElse(null);
        if (k == null) return loi("Mã khuyến mãi không tồn tại.");

        LocalDate hom = LocalDate.now();
        if (k.getNgayBatDau() != null && hom.isBefore(k.getNgayBatDau())) return loi("Khuyến mãi chưa bắt đầu.");
        if (k.getNgayKetThuc() != null && hom.isAfter(k.getNgayKetThuc())) return loi("Khuyến mãi đã hết hạn.");
        if (k.getDonToiThieu() != null && tienHang.compareTo(BigDecimal.valueOf(k.getDonToiThieu())) < 0) {
            return loi("Đơn hàng cần tối thiểu " + String.format("%,d", k.getDonToiThieu()).replace(',', '.')
                    + "đ để dùng mã này.");
        }

        BigDecimal giam;
        if ("GIAM_TIEN".equals(k.getLoai())) {
            giam = BigDecimal.valueOf(k.getSoTienGiam() == null ? 0 : k.getSoTienGiam());
        } else { // PHAN_TRAM (hoặc để trống)
            giam = tienHang.multiply(BigDecimal.valueOf(k.getPhanTramGiam()))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }
        giam = giam.min(tienHang).max(BigDecimal.ZERO);
        return new ApDung(k, giam, null);
    }

    private static ApDung loi(String thongBao) {
        return new ApDung(null, BigDecimal.ZERO, thongBao);
    }
}
