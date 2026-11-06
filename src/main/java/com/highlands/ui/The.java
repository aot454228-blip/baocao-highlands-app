package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Thẻ trắng bo góc có viền mảnh (giống .card trên web)
public class The extends JPanel {

    public The(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(14, 18, 14, 18));
    }

    public The(LayoutManager layout, int tren, int trai, int duoi, int phai) {
        this(layout);
        setBorder(new EmptyBorder(tren, trai, duoi, phai));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.khuDangCua(g);
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g.setColor(Theme.LINE);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g.dispose();
        super.paintComponent(g0);
    }
}
