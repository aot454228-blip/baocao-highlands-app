package com.highlands.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

// Băm và kiểm tra mật khẩu bằng PBKDF2-HMAC-SHA256 (không bao giờ lưu mật khẩu thuần).
// Chuỗi lưu trong DB: pbkdf2_sha256$<số vòng lặp>$<salt base64>$<hash base64>
//
// Tạo hash cho tài khoản mới: chạy hàm main của lớp này, nhập mật khẩu, chép chuỗi hash
// vào cột tai_khoan.mat_khau_hash (xem hướng dẫn cuối sql/migration_nhan_vien.sql).
public final class MatKhau {

    private static final String TIEN_TO = "pbkdf2_sha256";
    private static final int SO_VONG = 600_000;      // khuyến nghị OWASP cho PBKDF2-HMAC-SHA256
    private static final int VONG_TOI_THIEU = 10_000; // chặn chuỗi hash bị sửa thành giá trị quá nhỏ / quá lớn
    private static final int VONG_TOI_DA = 5_000_000;
    private static final int DO_DAI_MUOI = 16;
    private static final int DO_DAI_HASH = 32;
    private static final SecureRandom NGAU_NHIEN = new SecureRandom();

    private MatKhau() {
    }

    public static String bam(char[] matKhau) {
        byte[] muoi = new byte[DO_DAI_MUOI];
        NGAU_NHIEN.nextBytes(muoi);
        byte[] hash = pbkdf2(matKhau, muoi, SO_VONG, DO_DAI_HASH);
        Base64.Encoder b = Base64.getEncoder();
        return TIEN_TO + "$" + SO_VONG + "$" + b.encodeToString(muoi) + "$" + b.encodeToString(hash);
    }

    // true nếu mật khẩu khớp chuỗi đã lưu. Chuỗi lưu sai định dạng -> false (không ném lỗi)
    public static boolean kiemTra(char[] matKhau, String daLuu) {
        if (daLuu == null) return false;
        String[] p = daLuu.split("\\$");
        if (p.length != 4 || !TIEN_TO.equals(p[0])) return false;
        try {
            int vong = Integer.parseInt(p[1]);
            if (vong < VONG_TOI_THIEU || vong > VONG_TOI_DA) return false;
            byte[] muoi = Base64.getDecoder().decode(p[2]);
            byte[] mong = Base64.getDecoder().decode(p[3]);
            byte[] hash = pbkdf2(matKhau, muoi, vong, mong.length);
            return MessageDigest.isEqual(hash, mong); // so sánh thời gian không đổi
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Chạy một lần băm "giả" để thời gian trả lời không tiết lộ tên đăng nhập có tồn tại hay không
    public static void baoGia(char[] matKhau) {
        pbkdf2(matKhau, new byte[DO_DAI_MUOI], SO_VONG, DO_DAI_HASH);
    }

    private static byte[] pbkdf2(char[] matKhau, byte[] muoi, int vong, int doDaiByte) {
        PBEKeySpec spec = new PBEKeySpec(matKhau, muoi, vong, doDaiByte * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Máy không hỗ trợ PBKDF2WithHmacSHA256", e);
        } finally {
            spec.clearPassword();
        }
    }

    // ===== Công cụ dòng lệnh: sinh hash để dán vào CSDL =====
    public static void main(String[] args) {
        char[] mk;
        java.io.Console cs = System.console();
        if (cs != null) {
            mk = cs.readPassword("Nhập mật khẩu: ");
            char[] lai = cs.readPassword("Nhập lại mật khẩu: ");
            if (mk == null || lai == null || !Arrays.equals(mk, lai)) {
                System.err.println("Hai lần nhập không khớp.");
                return;
            }
            Arrays.fill(lai, '\0');
        } else {
            // Chạy từ IDE thường không có Console: đành đọc từ System.in (mật khẩu sẽ hiện trên màn hình)
            System.out.println("(Không có console: mật khẩu nhập vào sẽ hiện rõ trên màn hình)");
            System.out.print("Nhập mật khẩu: ");
            mk = new Scanner(System.in).nextLine().toCharArray();
        }
        if (mk.length < 8) {
            System.err.println("Mật khẩu quá ngắn: cần ít nhất 8 ký tự.");
            return;
        }
        System.out.println("\nChuỗi hash (dán vào cột mat_khau_hash):\n" + bam(mk));
        Arrays.fill(mk, '\0');
    }
}
