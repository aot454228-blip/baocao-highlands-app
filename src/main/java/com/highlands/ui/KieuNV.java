package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;

// Màu, chữ, nút và hàm hiển thị dùng chung cho giao diện nhân viên.
// Mượn lại Theme / Nut của phân hệ quản lý nên hai phân hệ trông giống nhau.
public final class KieuNV {

    public static final Color DO = Theme.TOPBAR;          // thanh tiêu đề, chữ tiêu đề
    public static final Color DO_SANG = Theme.BRAND;      // mục đang chọn, viền, lỗi
    public static final Color NEN = Theme.BG;
    public static final Color VIEN = Theme.VIEN_O;
    public static final Color CHU = Theme.TEXT;
    public static final Color CHU_PHU = Theme.MUTED;
    public static final Color XANH = new Color(0x15, 0x80, 0x3D); // thông báo thành công / tiền thừa

    // Chữ lấy từ Theme (đã chọn font có sẵn trên máy); KieuNV chỉ được nạp sau Theme.caiDat()
    public static final Font FONT = Theme.font(Font.PLAIN, 14);
    public static final Font DAM = Theme.font(Font.BOLD, 14);
    public static final Font NHO = Theme.font(Font.PLAIN, 12);
    public static final Font TIEU_DE = Theme.font(Font.BOLD, 20);

    private KieuNV() {
    }

    // Nút chính (nền đỏ) và nút phụ (nền trắng viền xám)
    public static JButton nut(String chu) {
        return new Nut(chu, Nut.Kieu.CHINH);
    }

    public static JButton nutPhu(String chu) {
        return new Nut(chu, Nut.Kieu.THUONG);
    }

    // 25000 -> "25.000đ"
    public static String tien(BigDecimal v) {
        return v == null ? "-" : Theme.so(v) + "đ";
    }

    // Chặn chèn HTML khi đưa chữ vào JLabel dạng <html>
    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static void loi(Component cha, String noiDung) {
        JOptionPane.showMessageDialog(cha, noiDung, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void thongTin(Component cha, String noiDung) {
        JOptionPane.showMessageDialog(cha, noiDung, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // Kiểu bảng giống phân hệ quản lý (dòng cao, kẻ ngang mảnh, tiêu đề chữ nhỏ in hoa)
    public static void kieuBang(JTable t) {
        Theme.kieuBang(t, 44);
    }

    // Ô căn phải cho cột tiền
    public static TableCellRenderer canPhai() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, row, col);
                l.setBorder(new EmptyBorder(0, 4, 0, 10));
                l.setFont(Theme.font(Font.PLAIN, 13));
                l.setForeground(Theme.TEXT);
                return l;
            }
        };
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        return r;
    }

    // Ảnh món đã co về đúng w x h (cắt vừa khung). Gọi từ luồng nền.
    // Không có ảnh thì vẽ khung xám có chữ cái đầu của món (không bao giờ trả về null).
    public static BufferedImage taiAnh(String duongDan, int w, int h, String ten) {
        BufferedImage goc = Anh.taiDongBo(duongDan);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            Theme.khuDangCua(g);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            Shape khung = new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 12, 12);
            g.setColor(Theme.HOVER);
            g.fill(khung);
            if (goc != null) {
                g.clip(khung);
                double tl = Math.max(w / (double) goc.getWidth(), h / (double) goc.getHeight());
                int rw = (int) Math.round(goc.getWidth() * tl), rh = (int) Math.round(goc.getHeight() * tl);
                g.drawImage(goc, (w - rw) / 2, (h - rh) / 2, rw, rh, null);
            } else {
                String chu = ten == null || ten.isBlank() ? "?" : ten.trim().substring(0, 1).toUpperCase();
                g.setColor(new Color(0xD1, 0xD5, 0xDB));
                g.setFont(Theme.font(Font.BOLD, h * 0.45f));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(chu, (w - fm.stringWidth(chu)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            }
        } finally {
            g.dispose();
        }
        return out;
    }
}
