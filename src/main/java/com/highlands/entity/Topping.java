package com.highlands.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "topping")
public class Topping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten", columnDefinition = "NVARCHAR(100) NOT NULL")
    private String ten;

    @Column(name = "gia", nullable = false)
    private long gia;

    // Đường dẫn ảnh (VD: /images/phin-sua-da.jpg hoặc link https://...). Để trống = hiện khung ảnh trống
    @Column(name = "hinh_anh", length = 500)
    private String hinhAnh;

    public Topping() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }
    public long getGia() { return gia; }
    public void setGia(long gia) { this.gia = gia; }
    public String getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }
}
