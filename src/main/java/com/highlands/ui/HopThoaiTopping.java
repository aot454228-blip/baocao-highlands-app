package com.highlands.ui;

import com.highlands.entity.SanPham;
import com.highlands.entity.Topping;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Hộp thoại chọn topping + số lượng khi bấm "Thêm vào giỏ"
class HopThoaiTopping extends JDialog {

    private final SanPham sanPham;
    private final List<Topping> dsTopping;
    private final List<JCheckBox> oChon = new ArrayList<>();
    private final JLabel nhanSl = new JLabel("1", SwingConstants.CENTER);
    private final JLabel nhanTong = new JLabel();
    private int soLuong = 1;
    private boolean daChon = false;

    HopThoaiTopping(Window cha, SanPham sanPham, List<Topping> dsTopping) {
        super(cha, "Chọn topping", ModalityType.APPLICATION_MODAL);
        this.sanPham = sanPham;
        this.dsTopping = dsTopping;

        JPanel goc = new JPanel(new BorderLayout(0, 12));
        goc.setBackground(Color.WHITE);
        goc.setBorder(new EmptyBorder(18, 22, 18, 22));

        // Đầu: tên món + giá
        JPanel dau = new JPanel(new GridLayout(2, 1, 0, 2));
        dau.setOpaque(false);
        JLabel ten = new JLabel(sanPham.getTen());
        ten.setFont(KieuNV.TIEU_DE);
        ten.setForeground(KieuNV.DO);
        JLabel gia = new JLabel(KieuNV.tien(java.math.BigDecimal.valueOf(sanPham.getGia())));
        gia.setFont(KieuNV.DAM);
        dau.add(ten);
        dau.add(gia);
        goc.add(dau, BorderLayout.NORTH);

        // Giữa: danh sách topping (cuộn được nếu nhiều)
        JPanel ds = new JPanel();
        ds.setOpaque(false);
        ds.setLayout(new BoxLayout(ds, BoxLayout.Y_AXIS));
        JLabel tieuDeTp = new JLabel("Topping (không bắt buộc)");
        tieuDeTp.setFont(KieuNV.DAM);
        tieuDeTp.setAlignmentX(LEFT_ALIGNMENT);
        ds.add(tieuDeTp);
        ds.add(Box.createVerticalStrut(6));
        for (Topping t : dsTopping) {
            JCheckBox c = new JCheckBox(t.getTen() + "  (+" + KieuNV.tien(java.math.BigDecimal.valueOf(t.getGia())) + ")");
            c.setOpaque(false);
            c.setFont(KieuNV.FONT);
            c.setAlignmentX(LEFT_ALIGNMENT);
            c.setFocusPainted(false);
            c.addActionListener(e -> capNhatTong());
            oChon.add(c);
            ds.add(c);
        }
        JScrollPane cuon = Theme.kieuCuon(new JScrollPane(ds));
        cuon.setBorder(BorderFactory.createLineBorder(KieuNV.VIEN));
        cuon.getViewport().setBackground(Color.WHITE);
        ds.setBorder(new EmptyBorder(10, 12, 10, 12));
        cuon.setPreferredSize(new Dimension(380, Math.min(260, 70 + dsTopping.size() * 28)));
        goc.add(cuon, BorderLayout.CENTER);

        // Dưới: số lượng, thành tiền, nút
        JPanel duoi = new JPanel(new BorderLayout(0, 12));
        duoi.setOpaque(false);

        JPanel hangSl = new JPanel(new BorderLayout());
        hangSl.setOpaque(false);
        JLabel chuSl = new JLabel("Số lượng");
        chuSl.setFont(KieuNV.DAM);
        JButton giam = KieuNV.nutPhu("−");
        JButton tang = KieuNV.nutPhu("+");
        giam.addActionListener(e -> doiSl(-1));
        tang.addActionListener(e -> doiSl(+1));
        nhanSl.setFont(KieuNV.DAM);
        nhanSl.setPreferredSize(new Dimension(44, 30));
        JPanel boSl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        boSl.setOpaque(false);
        boSl.add(giam);
        boSl.add(nhanSl);
        boSl.add(tang);
        hangSl.add(chuSl, BorderLayout.WEST);
        hangSl.add(boSl, BorderLayout.EAST);

        JPanel hangTong = new JPanel(new BorderLayout());
        hangTong.setOpaque(false);
        JLabel chuTong = new JLabel("Thành tiền");
        chuTong.setFont(KieuNV.DAM);
        nhanTong.setFont(new Font(KieuNV.TIEU_DE.getName(), Font.BOLD, 20));
        nhanTong.setForeground(KieuNV.DO);
        hangTong.add(chuTong, BorderLayout.WEST);
        hangTong.add(nhanTong, BorderLayout.EAST);

        JButton huy = KieuNV.nutPhu("Hủy");
        JButton them = KieuNV.nut("Thêm vào giỏ");
        huy.addActionListener(e -> dispose());
        them.addActionListener(e -> {
            daChon = true;
            dispose();
        });
        JPanel nut = new JPanel(new GridLayout(1, 2, 10, 0));
        nut.setOpaque(false);
        nut.add(huy);
        nut.add(them);

        JPanel cot = new JPanel();
        cot.setOpaque(false);
        cot.setLayout(new BoxLayout(cot, BoxLayout.Y_AXIS));
        hangSl.setAlignmentX(LEFT_ALIGNMENT);
        hangTong.setAlignmentX(LEFT_ALIGNMENT);
        cot.add(hangSl);
        cot.add(Box.createVerticalStrut(8));
        cot.add(hangTong);
        duoi.add(cot, BorderLayout.CENTER);
        duoi.add(nut, BorderLayout.SOUTH);
        goc.add(duoi, BorderLayout.SOUTH);

        setContentPane(goc);
        getRootPane().setDefaultButton(them);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        capNhatTong();
        pack();
        setLocationRelativeTo(cha);
    }

    private void doiSl(int delta) {
        soLuong = Math.max(1, Math.min(99, soLuong + delta));
        nhanSl.setText(String.valueOf(soLuong));
        capNhatTong();
    }

    private void capNhatTong() {
        long gia = sanPham.getGia();
        for (Topping t : getTopping()) gia += t.getGia();
        nhanTong.setText(KieuNV.tien(java.math.BigDecimal.valueOf(gia * soLuong)));
    }

    boolean daChon() {
        return daChon;
    }

    List<Topping> getTopping() {
        List<Topping> kq = new ArrayList<>();
        for (int i = 0; i < oChon.size(); i++) {
            if (oChon.get(i).isSelected()) kq.add(dsTopping.get(i));
        }
        return kq;
    }

    int getSoLuong() {
        return soLuong;
    }
}
