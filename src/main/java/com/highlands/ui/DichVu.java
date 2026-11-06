package com.highlands.ui;

import com.highlands.service.DanhMucService;
import com.highlands.service.KhuyenMaiService;
import com.highlands.service.SanPhamService;
import com.highlands.service.ThongKeService;
import com.highlands.service.ToppingService;

// Gom các Service (lấy từ Spring) để truyền cho giao diện
public record DichVu(KhuyenMaiService khuyenMai,
                     SanPhamService sanPham,
                     DanhMucService danhMuc,
                     ToppingService topping,
                     ThongKeService thongKe) {
}
