package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Function;
import java.util.function.IntConsumer;

// Các kiểu ô (cell renderer) dùng trong bảng
public final class O {

    private O() {
    }

    // Ô chữ thường; bienDoi đổi giá trị thành chữ hiển thị; dam = in đậm; mau = màu chữ (null = mặc định)
    public static DefaultTableCellRenderer chu(Function<Object, String> bienDoi, int canhLe, boolean dam, Color mau) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, bienDoi == null ? v : bienDoi.apply(v), sel, false, row, col);
                l.setBorder(new EmptyBorder(0, 14, 0, 14));
                l.setFont(Theme.font(dam ? Font.BOLD : Font.PLAIN, 13));
                l.setForeground(mau != null ? mau : Theme.TEXT);
                return l;
            }
        };
        r.setHorizontalAlignment(canhLe);
        return r;
    }

    public static DefaultTableCellRenderer chu() {
        return chu(null, SwingConstants.LEFT, false, null);
    }

    // Lớp cha cho các ô tự vẽ
    public abstract static class TuVe extends JComponent implements TableCellRenderer, Theme.CoCanhLe {
        protected Object giaTri;
        protected boolean chon;
        protected JTable bang;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            bang = t;
            giaTri = v;
            chon = sel;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(chon ? Theme.CHON : Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            ve(g, new Rectangle(0, 0, getWidth(), getHeight()));
            g.dispose();
        }

        protected abstract void ve(Graphics2D g, Rectangle o);

        @Override
        public int canhLe() {
            return SwingConstants.CENTER;
        }
    }

    // Ô ảnh: giá trị là đường dẫn ảnh
    public static TuVe anh(int co) {
        return new TuVe() {
            @Override
            protected void ve(Graphics2D g, Rectangle o) {
                Anh.veKhung(g, new Rectangle(o.x + 14, o.y + (o.height - co) / 2, co, co), (String) giaTri, bang);
            }

            @Override
            public int canhLe() {
                return SwingConstants.LEFT;
            }
        };
    }

    // Ô badge: hamBadge trả về {chữ, màu nền, màu chữ} hoặc null (hiện "-")
    public static TuVe badge(Function<Object, Object[]> hamBadge, boolean giua) {
        return new TuVe() {
            @Override
            protected void ve(Graphics2D g, Rectangle o) {
                Object[] b = hamBadge.apply(giaTri);
                if (b == null) {
                    Theme.khuDangCua(g);
                    g.setColor(Theme.MUTED);
                    g.setFont(Theme.font(Font.PLAIN, 13));
                    FontMetrics fm = g.getFontMetrics();
                    int x = giua ? (o.width - fm.stringWidth("-")) / 2 : 14;
                    g.drawString("-", x, (o.height - fm.getHeight()) / 2 + fm.getAscent());
                    return;
                }
                Theme.veBadge(g, o, (String) b[0], (Color) b[1], (Color) b[2], giua);
            }

            @Override
            public int canhLe() {
                return giua ? SwingConstants.CENTER : SwingConstants.LEFT;
            }
        };
    }

    // Ô thanh đỏ thể hiện tỉ lệ (0..1)
    public static TuVe thanh() {
        return new TuVe() {
            @Override
            protected void ve(Graphics2D g, Rectangle o) {
                double tl = giaTri == null ? 0 : ((Number) giaTri).doubleValue();
                int rong = Math.max(2, (int) ((o.width - 32) * tl));
                Theme.khuDangCua(g);
                g.setColor(Theme.BRAND);
                g.fillRoundRect(16, (o.height - 8) / 2, rong, 8, 8, 8);
            }

            @Override
            public int canhLe() {
                return SwingConstants.LEFT;
            }
        };
    }

    // ===== Cột "Thao tác": chữ Sửa (xanh) và Xóa (đỏ) =====
    private static final String SUA = "Sửa", XOA = "Xóa";

    public static TuVe thaoTac() {
        return new TuVe() {
            @Override
            protected void ve(Graphics2D g, Rectangle o) {
                Theme.khuDangCua(g);
                g.setFont(Theme.font(Font.BOLD, 13));
                FontMetrics fm = g.getFontMetrics();
                Rectangle[] r = viTri(o.width, o.height, fm);
                int y = (o.height - fm.getHeight()) / 2 + fm.getAscent();
                g.setColor(Theme.XANH);
                g.drawString(SUA, r[0].x, y);
                g.setColor(Theme.ERR);
                g.drawString(XOA, r[1].x, y);
            }
        };
    }

    private static Rectangle[] viTri(int w, int h, FontMetrics fm) {
        int a = fm.stringWidth(SUA), b = fm.stringWidth(XOA), khoang = 18;
        int x = (w - a - b - khoang) / 2;
        return new Rectangle[]{new Rectangle(x - 4, 0, a + 8, h), new Rectangle(x + a + khoang - 4, 0, b + 8, h)};
    }

    // Gắn xử lý bấm Sửa / Xóa cho cột thao tác (cot = chỉ số cột trong bảng), và nhấp đúp dòng = Sửa
    public static void ganThaoTac(JTable t, IntConsumer sua, IntConsumer xoa) {
        MouseAdapter m = new MouseAdapter() {
            private int nutTai(MouseEvent e) {
                int row = t.rowAtPoint(e.getPoint()), col = t.columnAtPoint(e.getPoint());
                if (row < 0 || col < 0) return 0;
                if (!(t.getColumnModel().getColumn(col).getCellRenderer() instanceof TuVe tv) || !laThaoTac(t, col)) return 0;
                Rectangle o = t.getCellRect(row, col, false);
                FontMetrics fm = t.getFontMetrics(Theme.font(Font.BOLD, 13));
                Rectangle[] r = viTri(o.width, o.height, fm);
                int x = e.getX() - o.x;
                if (x >= r[0].x && x <= r[0].x + r[0].width) return 1;
                if (x >= r[1].x && x <= r[1].x + r[1].width) return 2;
                return 0;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int row = t.rowAtPoint(e.getPoint());
                if (row < 0 || !SwingUtilities.isLeftMouseButton(e)) return;
                int n = nutTai(e);
                if (n == 1) sua.accept(row);
                else if (n == 2) xoa.accept(row);
                else if (e.getClickCount() == 2) sua.accept(row);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                t.setCursor(Cursor.getPredefinedCursor(nutTai(e) > 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        t.addMouseListener(m);
        t.addMouseMotionListener(m);
    }

    private static boolean laThaoTac(JTable t, int col) {
        return "thaoTac".equals(t.getColumnModel().getColumn(col).getIdentifier());
    }
}
