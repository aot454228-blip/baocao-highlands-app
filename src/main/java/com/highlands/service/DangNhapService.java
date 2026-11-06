package com.highlands.service;

import com.highlands.entity.TaiKhoan;
import com.highlands.repository.TaiKhoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Đăng nhập nhân viên. Mọi lỗi sai tên / sai mật khẩu đều trả CÙNG một thông báo
// (không cho biết tên đăng nhập có tồn tại hay không) và có khóa tạm khi sai liên tiếp.
@Service
public class DangNhapService {

    private static final String SAI = "Tên đăng nhập hoặc mật khẩu không đúng";
    private static final int SO_LAN_SAI_TOI_DA = 5;
    private static final Duration THOI_GIAN_KHOA = Duration.ofSeconds(60);

    private record LanSai(int soLan, Instant khoaDen) {
    }

    private final TaiKhoanRepository repo;
    private final Map<String, LanSai> saiLienTiep = new ConcurrentHashMap<>();

    public DangNhapService(TaiKhoanRepository repo) {
        this.repo = repo;
    }

    // Đăng nhập nhân viên (giữ nguyên cách gọi cũ)
    public TaiKhoan dangNhap(String tenDangNhap, char[] matKhau) {
        return dangNhap(tenDangNhap, matKhau, TaiKhoan.NHAN_VIEN);
    }

    // vaiTro: TaiKhoan.QUAN_LY hoặc TaiKhoan.NHAN_VIEN - tài khoản phải đúng vai trò đang đăng nhập
    @Transactional(readOnly = true)
    public TaiKhoan dangNhap(String tenDangNhap, char[] matKhau, String vaiTro) {
        String ten = tenDangNhap == null ? "" : tenDangNhap.trim();
        if (ten.isEmpty() || matKhau == null || matKhau.length == 0) {
            throw new LoiNghiepVu("Vui lòng nhập tên đăng nhập và mật khẩu");
        }
        if (ten.length() > 50 || matKhau.length > 200) throw new LoiNghiepVu(SAI);

        String khoa = ten.toLowerCase(Locale.ROOT);
        kiemTraKhoa(khoa);

        TaiKhoan tk = repo.findByTenDangNhapIgnoreCase(ten).orElse(null);
        boolean dung;
        if (tk == null) {
            MatKhau.baoGia(matKhau);
            dung = false;
        } else {
            dung = MatKhau.kiemTra(matKhau, tk.getMatKhauHash());
        }
        if (!dung) {
            ghiNhanSai(khoa);
            throw new LoiNghiepVu(SAI);
        }
        saiLienTiep.remove(khoa);

        if (!tk.isHoatDong()) throw new LoiNghiepVu("Tài khoản đã bị khóa. Vui lòng liên hệ quản lý");
        String vtTaiKhoan = tk.getVaiTro() == null ? TaiKhoan.NHAN_VIEN : tk.getVaiTro();
        if (!vtTaiKhoan.equalsIgnoreCase(vaiTro)) {
            throw new LoiNghiepVu(TaiKhoan.QUAN_LY.equals(vaiTro)
                    ? "Tài khoản này không có quyền Quản lý"
                    : "Đây là tài khoản Quản lý. Hãy quay lại và chọn Quản lý để đăng nhập");
        }
        if (!tk.getNhanVien().isDangLam()) throw new LoiNghiepVu("Nhân viên không còn làm việc. Vui lòng liên hệ quản lý");
        return tk;
    }

    private void kiemTraKhoa(String khoa) {
        LanSai l = saiLienTiep.get(khoa);
        if (l != null && l.khoaDen() != null) {
            long con = Duration.between(Instant.now(), l.khoaDen()).toSeconds() + 1;
            if (con > 0) throw new LoiNghiepVu("Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + con + " giây");
            saiLienTiep.remove(khoa);
        }
    }

    private void ghiNhanSai(String khoa) {
        saiLienTiep.merge(khoa, new LanSai(1, null), (cu, moi) -> {
            int n = cu.soLan() + 1;
            return n >= SO_LAN_SAI_TOI_DA ? new LanSai(0, Instant.now().plus(THOI_GIAN_KHOA)) : new LanSai(n, null);
        });
    }
}
