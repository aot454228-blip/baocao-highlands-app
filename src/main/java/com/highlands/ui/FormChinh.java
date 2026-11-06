package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

// Cửa sổ chính: thanh đỏ trên cùng + menu bên trái + vùng nội dung (giống layout bản web)
public class FormChinh extends JFrame {

    // Mỗi trang (Thống kê, Sản phẩm, Khuyến mãi) cài interface này để được nạp lại dữ liệu khi mở
    public interface Trang {
        void khiMo();
    }

    private final JLabel tieuDe = new JLabel();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel noiDung = new JPanel(cardLayout);
    private final Map<String, Nut> menu = new LinkedHashMap<>();
    private final Map<String, JComponent> trang = new LinkedHashMap<>();
    private final Map<String, String> tenTrang = new LinkedHashMap<>();

    public FormChinh(DichVu dv, Runnable khiDong) {
        super("Highlands - Quản lý cửa hàng");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                khiDong.run();
            }
        });
        setIconImage(veIcon());

        JPanel goc = new JPanel(new BorderLayout());
        goc.setBackground(Theme.BG);
        goc.add(thanhDo(), BorderLayout.NORTH);
        goc.add(menuTrai(), BorderLayout.WEST);
        noiDung.setBackground(Theme.BG);
        goc.add(noiDung, BorderLayout.CENTER);
        setContentPane(goc);

        them("thongKe", "Thống kê", "Highlands - Thống kê", new TrangThongKe(dv));
        them("sanPham", "Sản phẩm & Menu", "Highlands - Quản lý sản phẩm", new TrangSanPham(dv));
        them("khuyenMai", "Khuyến mãi", "Highlands - Quản lý khuyến mãi", new TrangKhuyenMai(dv));

        // Vừa màn hình laptop 1366x768; màn hình lớn thì 1360x820
        Rectangle man = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1360, man.width), Math.min(820, man.height));
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);

        // Bản web mở trang Khuyến mãi đầu tiên
        moTrang("khuyenMai");
    }

    private JComponent thanhDo() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.TOPBAR);
        p.setPreferredSize(new Dimension(0, 60));
        p.setBorder(new EmptyBorder(0, 24, 0, 24));
        tieuDe.setForeground(Color.WHITE);
        tieuDe.setFont(Theme.font(Font.BOLD, 19));
        p.add(tieuDe, BorderLayout.WEST);
        return p;
    }

    private final JPanel khungMenu = new JPanel();

    private JComponent menuTrai() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(230, 0));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.LINE), new EmptyBorder(20, 14, 12, 14)));

        // Logo: chấm đỏ chữ H + "Highlands / ADMIN PANEL"
        JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logo.setOpaque(false);
        logo.add(new HinhTron("H", Theme.BRAND, Color.WHITE, 34));
        JPanel chu = new JPanel(new GridLayout(2, 1));
        chu.setOpaque(false);
        JLabel ten = Theme.tieuDe("Highlands", 15);
        ten.setForeground(Theme.BRAND);
        JLabel phu = Theme.nhan("ADMIN PANEL");
        phu.setFont(Theme.font(Font.PLAIN, 9));
        chu.add(ten);
        chu.add(phu);
        logo.add(chu);
        logo.setBorder(new EmptyBorder(0, 0, 22, 0));
        p.add(logo, BorderLayout.NORTH);

        khungMenu.setOpaque(false);
        khungMenu.setLayout(new BoxLayout(khungMenu, BoxLayout.Y_AXIS));
        p.add(khungMenu, BorderLayout.CENTER);

        // Người dùng ở dưới cùng
        JPanel me = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        me.setOpaque(false);
        me.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE), new EmptyBorder(12, 0, 0, 0)));
        me.add(new HinhTron("Q", new Color(0xFD, 0xE8, 0xE8), Theme.BRAND, 30));
        JPanel meChu = new JPanel(new GridLayout(2, 1));
        meChu.setOpaque(false);
        meChu.add(Theme.tieuDe("Quản lý", 12));
        JLabel vt = Theme.nhan("Quản lý cửa hàng");
        vt.setFont(Theme.font(Font.PLAIN, 10));
        meChu.add(vt);
        me.add(meChu);
        p.add(me, BorderLayout.SOUTH);
        return p;
    }

    private void them(String ma, String chuMenu, String tieuDeTrang, JComponent noiDungTrang) {
        Nut n = new Nut(chuMenu, Nut.Kieu.MENU);
        n.setAlignmentX(Component.LEFT_ALIGNMENT);
        n.addActionListener(e -> moTrang(ma));
        khungMenu.add(n);
        khungMenu.add(Box.createVerticalStrut(6));
        menu.put(ma, n);
        trang.put(ma, noiDungTrang);
        tenTrang.put(ma, tieuDeTrang);
        noiDung.add(noiDungTrang, ma);
    }

    public void moTrang(String ma) {
        cardLayout.show(noiDung, ma);
        tieuDe.setText(tenTrang.get(ma));
        menu.forEach((k, n) -> n.setKieu(k.equals(ma) ? Nut.Kieu.MENU_CHON : Nut.Kieu.MENU));
        if (trang.get(ma) instanceof Trang t) t.khiMo();
    }

    private static Image veIcon() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        new HinhTron("H", Theme.BRAND, Color.WHITE, 64).ve(g, 0, 0);
        g.dispose();
        return img;
    }

    // Hình tròn có chữ ở giữa (logo, avatar)
    static final class HinhTron extends JComponent {
        private final String chu;
        private final Color nen, mau;
        private final int co;

        HinhTron(String chu, Color nen, Color mau, int co) {
            this.chu = chu;
            this.nen = nen;
            this.mau = mau;
            this.co = co;
            setPreferredSize(new Dimension(co, co));
        }

        void ve(Graphics2D g, int x, int y) {
            Theme.khuDangCua(g);
            g.setColor(nen);
            g.fillOval(x, y, co, co);
            g.setColor(mau);
            g.setFont(Theme.font(Font.BOLD, co * 0.42f));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(chu, x + (co - fm.stringWidth(chu)) / 2, y + (co - fm.getHeight()) / 2 + fm.getAscent());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            ve(g, 0, 0);
            g.dispose();
        }
    }
}
