package com.highlands.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Thông tin bán hàng của một đơn (mở rộng 1-1 với don_hang, không sửa bảng don_hang của phân hệ quản lý).
// Ràng buộc UNIQUE của don_hang_id và ma_giao_dich nằm ở CSDL (sql/migration_nhan_vien.sql).
@Entity
@Table(name = "thanh_toan_don_hang")
public class ThanhToanDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "don_hang_id", nullable = false)
    private DonHang donHang;

    @ManyToOne(optional = false)
    @JoinColumn(name = "nhan_vien_id", nullable = false)
    private NhanVien nhanVien;

    @ManyToOne
    @JoinColumn(name = "khuyen_mai_id")
    private KhuyenMai khuyenMai;

    @Column(name = "ma_khuyen_mai", columnDefinition = "NVARCHAR(50)")
    private String maKhuyenMai;

    @Column(name = "tien_hang", nullable = false, precision = 18, scale = 2)
    private BigDecimal tienHang;

    @Column(name = "giam_gia", nullable = false, precision = 18, scale = 2)
    private BigDecimal giamGia = BigDecimal.ZERO;

    @Column(name = "tong_thanh_toan", nullable = false, precision = 18, scale = 2)
    private BigDecimal tongThanhToan;

    @Column(name = "phuong_thuc", columnDefinition = "NVARCHAR(30) NOT NULL")
    private String phuongThuc;

    @Column(name = "tien_khach_dua", nullable = false, precision = 18, scale = 2)
    private BigDecimal tienKhachDua;

    @Column(name = "tien_thua", nullable = false, precision = 18, scale = 2)
    private BigDecimal tienThua;

    @Column(name = "ngay_thanh_toan", nullable = false)
    private LocalDateTime ngayThanhToan;

    // Khóa chống tạo trùng đơn khi bấm thanh toán nhiều lần
    @Column(name = "ma_giao_dich", columnDefinition = "NVARCHAR(64) NOT NULL")
    private String maGiaoDich;

    // Trạng thái đơn: Đang xử lý / Đang giao / Hoàn thành / Đã hủy (đơn Đã hủy không tính vào thống kê)
    @Column(name = "trang_thai", columnDefinition = "NVARCHAR(30) NOT NULL DEFAULT N'Hoàn thành'")
    private String trangThai = "Hoàn thành";

    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(200)")
    private String ghiChu;

    public ThanhToanDonHang() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DonHang getDonHang() { return donHang; }
    public void setDonHang(DonHang donHang) { this.donHang = donHang; }
    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }
    public KhuyenMai getKhuyenMai() { return khuyenMai; }
    public void setKhuyenMai(KhuyenMai khuyenMai) { this.khuyenMai = khuyenMai; }
    public String getMaKhuyenMai() { return maKhuyenMai; }
    public void setMaKhuyenMai(String maKhuyenMai) { this.maKhuyenMai = maKhuyenMai; }
    public BigDecimal getTienHang() { return tienHang; }
    public void setTienHang(BigDecimal tienHang) { this.tienHang = tienHang; }
    public BigDecimal getGiamGia() { return giamGia; }
    public void setGiamGia(BigDecimal giamGia) { this.giamGia = giamGia; }
    public BigDecimal getTongThanhToan() { return tongThanhToan; }
    public void setTongThanhToan(BigDecimal tongThanhToan) { this.tongThanhToan = tongThanhToan; }
    public String getPhuongThuc() { return phuongThuc; }
    public void setPhuongThuc(String phuongThuc) { this.phuongThuc = phuongThuc; }
    public BigDecimal getTienKhachDua() { return tienKhachDua; }
    public void setTienKhachDua(BigDecimal tienKhachDua) { this.tienKhachDua = tienKhachDua; }
    public BigDecimal getTienThua() { return tienThua; }
    public void setTienThua(BigDecimal tienThua) { this.tienThua = tienThua; }
    public LocalDateTime getNgayThanhToan() { return ngayThanhToan; }
    public void setNgayThanhToan(LocalDateTime ngayThanhToan) { this.ngayThanhToan = ngayThanhToan; }
    public String getMaGiaoDich() { return maGiaoDich; }
    public void setMaGiaoDich(String maGiaoDich) { this.maGiaoDich = maGiaoDich; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
