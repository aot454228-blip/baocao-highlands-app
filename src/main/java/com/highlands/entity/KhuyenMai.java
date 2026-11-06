package com.highlands.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "khuyen_mai")
public class KhuyenMai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten", columnDefinition = "NVARCHAR(100) NOT NULL")
    private String ten;

    @Column(name = "phan_tram_giam", nullable = false)
    private double phanTramGiam;

    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDate ngayKetThuc;

    // Đường dẫn ảnh (VD: /images/phin-sua-da.jpg hoặc link https://...). Để trống = hiện khung ảnh trống
    @Column(name = "hinh_anh", length = 500)
    private String hinhAnh;

    // Mã nhập khi thanh toán (VD: GIAM10K). Có thể để trống
    @Column(name = "ma", length = 50)
    private String ma;

    // PHAN_TRAM = giảm theo %, GIAM_TIEN = giảm số tiền cố định. Để trống coi như PHAN_TRAM
    @Column(name = "loai", length = 20)
    private String loai;

    // Số tiền giảm (VND), dùng khi loai = GIAM_TIEN
    @Column(name = "so_tien_giam")
    private Long soTienGiam;

    // Đơn hàng tối thiểu (VND) mới được áp. Để trống = không yêu cầu
    @Column(name = "don_toi_thieu")
    private Long donToiThieu;

    public KhuyenMai() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }

    public double getPhanTramGiam() { return phanTramGiam; }
    public void setPhanTramGiam(double phanTramGiam) { this.phanTramGiam = phanTramGiam; }

    public LocalDate getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDate ngayBatDau) { this.ngayBatDau = ngayBatDau; }

    public LocalDate getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDate ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }
    public String getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }

    public String getMa() { return ma; }
    public void setMa(String ma) { this.ma = ma; }

    public String getLoai() { return loai; }
    public void setLoai(String loai) { this.loai = loai; }

    public Long getSoTienGiam() { return soTienGiam; }
    public void setSoTienGiam(Long soTienGiam) { this.soTienGiam = soTienGiam; }

    public Long getDonToiThieu() { return donToiThieu; }
    public void setDonToiThieu(Long donToiThieu) { this.donToiThieu = donToiThieu; }
}
