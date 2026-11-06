package com.highlands.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Tài khoản đăng nhập của nhân viên. Mật khẩu chỉ lưu dạng băm PBKDF2 (xem service/MatKhau)
@Entity
@Table(name = "tai_khoan")
public class TaiKhoan {

    public static final String QUAN_LY = "QUAN_LY";
    public static final String NHAN_VIEN = "NHAN_VIEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_dang_nhap", columnDefinition = "NVARCHAR(50) NOT NULL")
    private String tenDangNhap;

    @Column(name = "mat_khau_hash", columnDefinition = "NVARCHAR(255) NOT NULL")
    private String matKhauHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "nhan_vien_id", nullable = false)
    private NhanVien nhanVien;

    // Vai trò đăng nhập: QUAN_LY hoặc NHAN_VIEN
    @Column(name = "vai_tro", columnDefinition = "NVARCHAR(20) NOT NULL DEFAULT 'NHAN_VIEN'")
    private String vaiTro = NHAN_VIEN;

    @Column(name = "hoat_dong", nullable = false)
    private boolean hoatDong = true;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;

    public TaiKhoan() {
    }

    @PrePersist
    void truocKhiLuu() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }
    public String getMatKhauHash() { return matKhauHash; }
    public void setMatKhauHash(String matKhauHash) { this.matKhauHash = matKhauHash; }
    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }
    public String getVaiTro() { return vaiTro; }
    public void setVaiTro(String vaiTro) { this.vaiTro = vaiTro; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
}
