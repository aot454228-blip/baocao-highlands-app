package com.highlands.ui;

import com.highlands.entity.TaiKhoan;
import com.highlands.service.DangNhapService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/** Màn hình đăng nhập cho Nhân viên và Quản lý (chọn bằng vaiTro). */
public class DangNhapNhanVien extends JFrame {
    private final ConfigurableApplicationContext ctx;
    private final String vaiTro;
    private final java.util.function.Consumer<TaiKhoan> moQuanLy;
    private final DangNhapService dangNhapService;
    private final JTextField oTen = new JTextField();
    private final JPasswordField oMatKhau = new JPasswordField();
    private final JLabel nhanLoi = new JLabel(" ");
    private final JButton nutDangNhap = KieuNV.nut("Đăng nhập");

    public DangNhapNhanVien(ConfigurableApplicationContext ctx) {
        this(ctx, TaiKhoan.NHAN_VIEN, null);
    }

    /** moQuanLy: việc chạy sau khi Quản lý đăng nhập đúng (mở cửa sổ quản trị), nhận tài khoản vừa đăng nhập. */
    public DangNhapNhanVien(ConfigurableApplicationContext ctx, String vaiTro, java.util.function.Consumer<TaiKhoan> moQuanLy) {
        super(TaiKhoan.QUAN_LY.equals(vaiTro) ? "Highlands Coffee – Đăng nhập quản lý" : "Highlands Coffee – Đăng nhập nhân viên");
        this.ctx = ctx;
        this.vaiTro = vaiTro;
        this.moQuanLy = moQuanLy;
        this.dangNhapService = ctx.getBean(DangNhapService.class);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { thoat(); }
        });
        setSize(840, 500);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel goc = new JPanel(new BorderLayout());

        JPanel trai = new JPanel();
        trai.setBackground(KieuNV.DO);
        trai.setPreferredSize(new Dimension(340, 0));
        trai.setLayout(new BoxLayout(trai, BoxLayout.Y_AXIS));
        trai.add(Box.createVerticalGlue());
        LogoTron logo = new LogoTron(120);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        trai.add(logo);
        trai.add(Box.createVerticalStrut(18));
        trai.add(canGiua(new JLabel("HIGHLANDS COFFEE"), new Font("Segoe UI", Font.BOLD, 24), Color.WHITE));
        trai.add(Box.createVerticalStrut(6));
        trai.add(canGiua(new JLabel(TaiKhoan.QUAN_LY.equals(vaiTro) ? "Phân hệ QUẢN LÝ" : "Phân hệ NHÂN VIÊN bán hàng"), KieuNV.FONT, new Color(0xF5D0D5)));
        trai.add(Box.createVerticalGlue());

        JPanel phai = new JPanel();
        phai.setBackground(Color.WHITE);
        phai.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        phai.setLayout(new BoxLayout(phai, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("Đăng nhập");
        tieuDe.setFont(new Font("Segoe UI", Font.BOLD, 26));
        tieuDe.setForeground(KieuNV.DO);
        tieuDe.setAlignmentX(LEFT_ALIGNMENT);
        phai.add(tieuDe);
        phai.add(Box.createVerticalStrut(24));
        phai.add(nhan("Tên đăng nhập"));
        phai.add(Box.createVerticalStrut(4));
        phai.add(truong(oTen));
        phai.add(Box.createVerticalStrut(14));
        phai.add(nhan("Mật khẩu"));
        phai.add(Box.createVerticalStrut(4));
        phai.add(truong(oMatKhau));
        phai.add(Box.createVerticalStrut(8));
        nhanLoi.setForeground(KieuNV.DO_SANG);
        nhanLoi.setFont(KieuNV.FONT);
        nhanLoi.setAlignmentX(LEFT_ALIGNMENT);
        phai.add(nhanLoi);
        phai.add(Box.createVerticalStrut(14));
        JPanel hang = new JPanel(new GridLayout(1, 2, 12, 0));
        hang.setOpaque(false);
        hang.setAlignmentX(LEFT_ALIGNMENT);
        hang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JButton nutThoat = KieuNV.nutPhu("Thoát");
        nutThoat.addActionListener(e -> thoat());
        nutDangNhap.addActionListener(e -> dangNhap());
        hang.add(nutDangNhap);
        hang.add(nutThoat);
        phai.add(hang);
        phai.add(Box.createVerticalGlue());

        goc.add(trai, BorderLayout.WEST);
        goc.add(phai, BorderLayout.CENTER);
        setContentPane(goc);
        getRootPane().setDefaultButton(nutDangNhap);
    }

    private static JLabel canGiua(JLabel l, Font f, Color c) {
        l.setFont(f); l.setForeground(c); l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    private static JLabel nhan(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(KieuNV.DAM);
        l.setForeground(KieuNV.CHU);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JComponent truong(JTextField t) {
        t.setFont(KieuNV.FONT);
        t.setAlignmentX(LEFT_ALIGNMENT);
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        t.setPreferredSize(new Dimension(300, 38));
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return t;
    }

    private void dangNhap() {
        String ten = oTen.getText();
        char[] mk = oMatKhau.getPassword();
        nhanLoi.setText(" ");
        nutDangNhap.setEnabled(false);
        ChayNen.chay(() -> dangNhapService.dangNhap(ten, mk, vaiTro), tk -> {
            Arrays.fill(mk, '\0');
            dispose();
            if (TaiKhoan.QUAN_LY.equals(vaiTro) && moQuanLy != null) {
                moQuanLy.accept(tk);
            } else {
                new FormNhanVien(ctx, tk).setVisible(true);
            }
        }, ex -> {
            Arrays.fill(mk, '\0');
            oMatKhau.setText("");
            nhanLoi.setText("<html>" + KieuNV.escape(ChayNen.thongDiep(ex)) + "</html>");
            nutDangNhap.setEnabled(true);
            oMatKhau.requestFocusInWindow();
        });
    }

    private void thoat() {
        ctx.close();
        System.exit(0);
    }
}
