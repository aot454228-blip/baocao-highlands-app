package com.highlands.ui;

import javax.swing.*;
import java.awt.*;

// Logo tròn chữ H: nền trắng, viền và chữ đỏ (nhìn rõ cả trên nền đỏ lẫn nền trắng)
public class LogoTron extends JComponent {

    private final int co;

    public LogoTron(int co) {
        this.co = co;
        Dimension d = new Dimension(co, co);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.khuDangCua(g);
        float vien = Math.max(2f, co / 14f);
        g.setColor(Color.WHITE);
        g.fillOval(0, 0, co - 1, co - 1);
        g.setColor(KieuNV.DO);
        g.setStroke(new BasicStroke(vien));
        int lui = Math.round(vien / 2);
        g.drawOval(lui, lui, co - 1 - 2 * lui, co - 1 - 2 * lui);
        g.setFont(Theme.font(Font.BOLD, co * 0.5f));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("H", (co - fm.stringWidth("H")) / 2, (co - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }
}
