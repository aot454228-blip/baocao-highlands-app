package com.highlands.ui;

import com.highlands.entity.NhanVien;
import com.highlands.entity.TaiKhoan;
import com.highlands.service.BanHangService;
import com.highlands.service.ThanhToanService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;

/** Giao diện chính của Nhân viên: header đỏ + sidebar + CardLayout (Bán hàng / Lịch sử đơn hàng). */
public class FormNhanVien extends JFrame {
    private static final String CARD_BAN_HANG = "banhang";
    private static final String CARD_LICH_SU = "lichsu";

    private final ConfigurableApplicationContext ctx;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel noiDung = new JPanel(cardLayout);
    private final MucMenu mucBanHang = new MucMenu("Bán hàng");
    private final MucMenu mucLichSu = new MucMenu("Lịch sử đơn hàng");
    private final TrangLichSuDonHang trangLichSu;

    public FormNhanVien(ConfigurableApplicationContext ctx, TaiKhoan taiKhoan) {
        super("Highlands Coffee – Nhân viên");
        this.ctx = ctx;
        NhanVien nv = taiKhoan.getNhanVien();
        BanHangService banHang = ctx.getBean(BanHangService.class);
        ThanhToanService thanhToan = ctx.getBean(ThanhToanService.class);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { thoatUngDung(); }
        });
        setSize(1360, 800);
        setMinimumSize(new Dimension(1180, 680));
        setLocationRelativeTo(null);

        JPanel goc = new JPanel(new BorderLayout());
        goc.add(taoTieuDe(nv), BorderLayout.NORTH);
        goc.add(taoSidebar(nv), BorderLayout.WEST);

        noiDung.setBackground(KieuNV.NEN);
        trangLichSu = new TrangLichSuDonHang(thanhToan);
        noiDung.add(new TrangBanHang(banHang, thanhToan, nv), CARD_BAN_HANG);
        noiDung.add(trangLichSu, CARD_LICH_SU);
        goc.add(noiDung, BorderLayout.CENTER);
        setContentPane(goc);

        mucBanHang.addActionListener(e -> chuyen(CARD_BAN_HANG));
        mucLichSu.addActionListener(e -> chuyen(CARD_LICH_SU));
        chuyen(CARD_BAN_HANG);
    }

    private JComponent taoTieuDe(NhanVien nv) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(KieuNV.DO);
        p.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        JPanel trai = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        trai.setOpaque(false);
        trai.add(new LogoTron(38));
        JLabel ten = new JLabel("HIGHLANDS COFFEE");
        ten.setFont(new Font("Segoe UI", Font.BOLD, 20));
        ten.setForeground(Color.WHITE);
        trai.add(ten);
        JLabel vaiTro = new JLabel("NHÂN VIÊN  |  " + nv.getHoTen());
        vaiTro.setFont(KieuNV.DAM);
        vaiTro.setForeground(Color.WHITE);
        p.add(trai, BorderLayout.WEST);
        p.add(vaiTro, BorderLayout.EAST);
        return p;
    }

    private JComponent taoSidebar(NhanVien nv) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(240, 0));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, KieuNV.VIEN), BorderFactory.createEmptyBorder(20, 14, 16, 14)));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        LogoTron logo = new LogoTron(76);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        p.add(logo);
        p.add(Box.createVerticalStrut(8));
        p.add(dong("Highlands", new Font("Segoe UI", Font.BOLD, 18), KieuNV.DO, CENTER_ALIGNMENT));
        p.add(dong("NHÂN VIÊN", KieuNV.NHO, KieuNV.CHU_PHU, CENTER_ALIGNMENT));
        p.add(Box.createVerticalStrut(24));
        p.add(mucBanHang);
        p.add(Box.createVerticalStrut(6));
        p.add(mucLichSu);
        p.add(Box.createVerticalGlue());

        p.add(dong("Đang đăng nhập", KieuNV.NHO, KieuNV.CHU_PHU, LEFT_ALIGNMENT));
        p.add(dong(nv.getHoTen(), KieuNV.DAM, KieuNV.CHU, LEFT_ALIGNMENT));
        p.add(dong("Mã NV: " + nv.getMaNhanVien(), KieuNV.NHO, KieuNV.CHU_PHU, LEFT_ALIGNMENT));
        p.add(Box.createVerticalStrut(12));
        JButton dangXuat = KieuNV.nutPhu("Đăng xuất / Đổi vai trò");
        dangXuat.setAlignmentX(LEFT_ALIGNMENT);
        dangXuat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        dangXuat.addActionListener(e -> dangXuat());
        p.add(dangXuat);
        return p;
    }

    private static JLabel dong(String chu, Font f, Color c, float canh) {
        JLabel l = new JLabel(chu);
        l.setFont(f); l.setForeground(c); l.setAlignmentX(canh);
        return l;
    }

    private void chuyen(String the) {
        cardLayout.show(noiDung, the);
        mucBanHang.setChon(the.equals(CARD_BAN_HANG));
        mucLichSu.setChon(the.equals(CARD_LICH_SU));
        if (the.equals(CARD_LICH_SU)) trangLichSu.lamMoi();
    }

    private void dangXuat() {
        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn đăng xuất?\nGiỏ hàng chưa thanh toán sẽ bị hủy.",
                "Đăng xuất", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        dispose();
        com.highlands.HighlandsApplication.moDangNhap();
    }

    private void thoatUngDung() {
        if (JOptionPane.showConfirmDialog(this, "Thoát ứng dụng?\nGiỏ hàng chưa thanh toán sẽ bị hủy.",
                "Thoát", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        ctx.close();
        System.exit(0);
    }

    /** Nút menu bên trái: đang chọn thì nền đỏ chữ trắng. */
    private static final class MucMenu extends JButton {
        private boolean chon;

        MucMenu(String chu) {
            super(chu);
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 12));
            setAlignmentX(CENTER_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        }

        void setChon(boolean chon) { this.chon = chon; repaint(); }

        @Override public Color getForeground() { return chon ? Color.WHITE : KieuNV.CHU; }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (chon) g.setColor(KieuNV.DO_SANG);
            else if (getModel().isRollover()) g.setColor(KieuNV.NEN);
            else g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
