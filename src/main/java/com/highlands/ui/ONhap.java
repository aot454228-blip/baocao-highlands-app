package com.highlands.ui;

import javax.swing.*;
import java.awt.*;

// Ô nhập có chữ gợi ý mờ khi trống (giống placeholder trên web)
public class ONhap extends JTextField {

    private String goiY;

    public ONhap(String goiY) {
        super(18);
        this.goiY = goiY;
        Theme.kieuO(this);
    }

    public void setGoiY(String g) {
        goiY = g;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (goiY != null && getText().isEmpty()) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.khuDangCua(g);
            g.setColor(new Color(0x9C, 0xA3, 0xAF));
            g.setFont(getFont());
            Insets in = getInsets();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(goiY, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
