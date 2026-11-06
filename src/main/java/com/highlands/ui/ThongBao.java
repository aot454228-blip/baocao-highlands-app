package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Thanh thông báo xanh (thành công) / đỏ (lỗi), thành công thì tự ẩn sau 3 giây (giống #msg trên web)
public class ThongBao extends JLabel {

    private final Timer hen = new Timer(3000, e -> an());
    private boolean ok;

    public ThongBao() {
        setFont(Theme.font(Font.PLAIN, 13));
        setBorder(new EmptyBorder(10, 14, 10, 14));
        setOpaque(false);
        hen.setRepeats(false);
        setVisible(false);
    }

    public void hien(String chu, boolean thanhCong) {
        ok = thanhCong;
        setText(chu);
        setForeground(thanhCong ? new Color(0x15, 0x80, 0x3D) : Theme.ERR);
        setVisible(true);
        hen.stop();
        if (thanhCong) hen.start();
        revalidate();
        repaint();
    }

    public void an() {
        hen.stop();
        setVisible(false);
        revalidate();
    }

    // Cao theo nội dung, rộng hết cỡ (để BoxLayout không cắt chữ)
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.khuDangCua(g);
        g.setColor(ok ? new Color(0xDC, 0xFC, 0xE7) : new Color(0xFE, 0xE2, 0xE2));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g.dispose();
        super.paintComponent(g0);
    }
}
