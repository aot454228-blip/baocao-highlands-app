package com.highlands.service;

import com.highlands.entity.SanPham;
import com.highlands.entity.Topping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Một dòng trong giỏ hàng: món + các topping + số lượng.
// Giá ở đây chỉ để hiển thị; khi thanh toán ThanhToanService tính lại giá từ CSDL.
public class DongGio {

    private final SanPham sanPham;
    private final List<Topping> topping;
    private int soLuong;

    public DongGio(SanPham sanPham, List<Topping> topping, int soLuong) {
        this.sanPham = sanPham;
        this.topping = topping == null ? List.of() : new ArrayList<>(topping);
        this.soLuong = soLuong;
    }

    public SanPham getSanPham() { return sanPham; }
    public List<Topping> getTopping() { return topping; }
    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    // "Trân châu, Thạch"
    public String moTaTopping() {
        return topping.stream().map(Topping::getTen).collect(Collectors.joining(", "));
    }

    // Cùng món và cùng bộ topping (không phân biệt thứ tự) thì gộp chung một dòng
    public boolean cungLoai(SanPham sp, List<Topping> tp) {
        if (!sanPham.getId().equals(sp.getId())) return false;
        return idTopping(topping).equals(idTopping(tp));
    }

    private static Set<Long> idTopping(List<Topping> ds) {
        Set<Long> s = new HashSet<>();
        for (Topping t : ds) s.add(t.getId());
        return s;
    }

    // Giá 1 ly = giá món + giá các topping
    public BigDecimal donGia() {
        long g = sanPham.getGia();
        for (Topping t : topping) g += t.getGia();
        return BigDecimal.valueOf(g);
    }

    public BigDecimal thanhTien() {
        return donGia().multiply(BigDecimal.valueOf(soLuong));
    }
}
