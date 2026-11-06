package com.highlands.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

// Topping của một dòng trong đơn. Lưu bản chụp tên/giá lúc bán nên đổi giá topping sau này không ảnh hưởng hóa đơn cũ
@Entity
@Table(name = "chi_tiet_don_hang_topping")
public class ChiTietDonHangTopping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chi_tiet_don_hang_id", nullable = false)
    private ChiTietDonHang chiTietDonHang;

    @ManyToOne
    @JoinColumn(name = "topping_id")
    private Topping topping;

    @Column(name = "ten_topping", columnDefinition = "NVARCHAR(100) NOT NULL")
    private String tenTopping;

    @Column(name = "gia_topping", nullable = false, precision = 18, scale = 2)
    private BigDecimal giaTopping;

    public ChiTietDonHangTopping() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ChiTietDonHang getChiTietDonHang() { return chiTietDonHang; }
    public void setChiTietDonHang(ChiTietDonHang chiTietDonHang) { this.chiTietDonHang = chiTietDonHang; }
    public Topping getTopping() { return topping; }
    public void setTopping(Topping topping) { this.topping = topping; }
    public String getTenTopping() { return tenTopping; }
    public void setTenTopping(String tenTopping) { this.tenTopping = tenTopping; }
    public BigDecimal getGiaTopping() { return giaTopping; }
    public void setGiaTopping(BigDecimal giaTopping) { this.giaTopping = giaTopping; }
}
