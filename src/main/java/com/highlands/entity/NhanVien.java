package com.highlands.entity;

import jakarta.persistence.*;

// Nhân viên bán hàng (bảng nhan_vien, tạo bởi sql/migration_nhan_vien.sql)
@Entity
@Table(name = "nhan_vien")
public class NhanVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_nhan_vien", columnDefinition = "NVARCHAR(20) NOT NULL")
    private String maNhanVien;

    @Column(name = "ho_ten", columnDefinition = "NVARCHAR(100) NOT NULL")
    private String hoTen;

    @Column(name = "dang_lam", nullable = false)
    private boolean dangLam = true;

    public NhanVien() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(String maNhanVien) { this.maNhanVien = maNhanVien; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public boolean isDangLam() { return dangLam; }
    public void setDangLam(boolean dangLam) { this.dangLam = dangLam; }
}
