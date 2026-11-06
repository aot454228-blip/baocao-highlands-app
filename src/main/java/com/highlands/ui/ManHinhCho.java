package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Cửa sổ nhỏ "Đang kết nối..." trong lúc Spring khởi động (mất vài giây)
public class ManHinhCho {

    private JWindow w;

    public static ManHinhCho hien(String chu) {
        ManHinhCho m = new ManHinhCho();
        try {
            SwingUtilities.invokeAndWait(() -> {
                m.w = new JWindow();
                JPanel p = new JPanel(new BorderLayout(0, 10));
                p.setBackground(Color.WHITE);
                p.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(4, 1, 1, 1, Theme.TOPBAR), new EmptyBorder(22, 28, 22, 28)));
                JLabel ten = Theme.tieuDe("Highlands", 20);
                ten.setForeground(Theme.BRAND);
                JLabel l = Theme.nhan(chu);
                JProgressBar thanh = new JProgressBar();
                thanh.setIndeterminate(true);
                thanh.setForeground(Theme.BRAND);
                thanh.setBorderPainted(false);
                thanh.setPreferredSize(new Dimension(260, 6));
                p.add(ten, BorderLayout.NORTH);
                p.add(l, BorderLayout.CENTER);
                p.add(thanh, BorderLayout.SOUTH);
                m.w.setContentPane(p);
                m.w.pack();
                m.w.setLocationRelativeTo(null);
                m.w.setVisible(true);
            });
        } catch (Exception ignored) {
        }
        return m;
    }

    public void dong() {
        SwingUtilities.invokeLater(() -> {
            if (w != null) w.dispose();
        });
    }
}
