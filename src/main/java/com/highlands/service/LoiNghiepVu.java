package com.highlands.service;

// Lỗi do người dùng nhập sai hoặc vi phạm quy tắc (trống tên, trùng mã, ...).
// Giao diện hiện nguyên câu thông báo này cho người dùng.
public class LoiNghiepVu extends RuntimeException {
    public LoiNghiepVu(String thongBao) {
        super(thongBao);
    }
}
