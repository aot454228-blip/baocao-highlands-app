package com.highlands.ui;

import javax.swing.*;
import java.awt.*;

// Nút tự vẽ: bo góc, đổi màu khi rê chuột (giống .btn / .menu a / tab trên web)
public class Nut extends JButton {

    public enum Kieu { CHINH, THUONG, MENU, MENU_CHON, TAB, TAB_CHON }

    private Kieu kieu;

    public Nut(String chu, Kieu kieu) {
        super(chu);
        this.kieu = kieu;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        capNhatKieu();
    }

    public void setKieu(Kieu k) {
        kieu = k;
        capNhatKieu();
        repaint();
    }

    private void capNhatKieu() {
        boolean menu = kieu == Kieu.MENU || kieu == Kieu.MENU_CHON;
        setFont(Theme.font(kieu == Kieu.CHINH || kieu == Kieu.TAB_CHON ? Font.BOLD : Font.PLAIN, 13));
        setHorizontalAlignment(menu ? LEFT : CENTER);
        setMargin(menu ? new Insets(10, 14, 10, 14) : new Insets(8, 16, 8, 16));
        setForeground(kieu == Kieu.CHINH || kieu == Kieu.MENU_CHON || kieu == Kieu.TAB_CHON ? Color.WHITE
                : menu ? new Color(0x37, 0x41, 0x51) : Theme.TEXT);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        Insets m = getMargin();
        return new Dimension(fm.stringWidth(getText()) + m.left + m.right, Math.max(36, fm.getHeight() + m.top + m.bottom));
    }

    @Override
    public Dimension getMaximumSize() {
        boolean menu = kieu == Kieu.MENU || kieu == Kieu.MENU_CHON;
        return menu ? new Dimension(Integer.MAX_VALUE, getPreferredSize().height) : super.getMaximumSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.khuDangCua(g);
        boolean re = getModel().isRollover() && isEnabled();
        int w = getWidth(), h = getHeight();
        int bo = kieu == Kieu.TAB || kieu == Kieu.TAB_CHON ? h : 10;
        Color nen = null, vien = null;
        switch (kieu) {
            case CHINH -> nen = !isEnabled() ? new Color(0xD8, 0x8A, 0x8A) : re ? Theme.BRAND_DARK : Theme.BRAND;
            case THUONG, TAB -> {
                nen = re ? Theme.HOVER : Color.WHITE;
                vien = Theme.VIEN_O;
            }
            case MENU -> nen = re ? Theme.HOVER : null;
            case MENU_CHON, TAB_CHON -> nen = Theme.BRAND;
        }
        if (nen != null) {
            g.setColor(nen);
            g.fillRoundRect(0, 0, w - 1, h - 1, bo, bo);
        }
        if (vien != null) {
            g.setColor(vien);
            g.drawRoundRect(0, 0, w - 1, h - 1, bo, bo);
        }
        g.setFont(getFont());
        g.setColor(isEnabled() ? getForeground() : new Color(0x9C, 0xA3, 0xAF));
        if (kieu == Kieu.CHINH && !isEnabled()) g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int x = getHorizontalAlignment() == LEFT ? getMargin().left : (w - fm.stringWidth(getText())) / 2;
        g.drawString(getText(), x, (h - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }
}
