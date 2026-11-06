package com.highlands.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

// Hàm tiện ích dùng chung cho phân hệ nhân viên
public final class TienIch {

    private TienIch() {
    }

    // Mã đơn hiển thị: 12 -> "HD000012"
    public static String maHienThi(Long maDon) {
        return maDon == null ? "-" : String.format("HD%06d", maDon);
    }

    // Đọc lại mã người dùng gõ ("HD000012", "hd12", "12") thành id đơn; không đọc được -> null
    public static Long docMaDon(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase(Locale.ROOT);
        if (t.startsWith("HD")) t = t.substring(2);
        if (t.isEmpty() || t.length() > 15 || !t.chars().allMatch(c -> c >= '0' && c <= '9')) return null;
        return Long.parseLong(t);
    }

    public static String str(String s) {
        return s == null ? "" : s;
    }

    public static BigDecimal bd(long v) {
        return BigDecimal.valueOf(v);
    }

    // "Phin Sữa Đá" -> "phin sua da" (để tìm không phân biệt hoa thường và dấu)
    public static String boDau(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return t.toLowerCase(Locale.ROOT);
    }
}
