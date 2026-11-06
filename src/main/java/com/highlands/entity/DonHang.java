package com.highlands.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "don_hang")
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ngay_dat", nullable = false)
    private LocalDate ngayDat;

    // Tổng tiền của đơn (đã trừ khuyến mãi nếu có)
    @Column(name = "tong_tien", nullable = false)
    private long tongTien;

    public DonHang() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDate ngayDat) { this.ngayDat = ngayDat; }
    public long getTongTien() { return tongTien; }
    public void setTongTien(long tongTien) { this.tongTien = tongTien; }
}
