package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.Callable;

// Khung hộp thoại thêm/sửa dùng chung: tiêu đề, lưới ô nhập 1 hoặc 2 cột, dòng báo lỗi, nút Hủy / Lưu.
// Lưu bị lỗi thì hộp thoại vẫn mở, giữ nguyên dữ liệu đã nhập.
public abstract class HopThoai extends JDialog {

    protected final JPanel luoi = new JPanel(new GridBagLayout());
    private final JLabel loi = new JLabel();
    private final Nut nutLuu;
    private final int soCot;
    private int viTri = 0;
    private boolean daLuu = false;

    protected HopThoai(Window cha, String tieuDe, String chuNutLuu, int soCot) {
        super(cha, tieuDe, ModalityType.APPLICATION_MODAL);
        this.soCot = soCot;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel goc = new JPanel(new BorderLayout(0, 14));
        goc.setBackground(Color.WHITE);
        goc.setBorder(new EmptyBorder(20, 22, 18, 22));
        goc.add(Theme.tieuDe(tieuDe, 17), BorderLayout.NORTH);

        luoi.setOpaque(false);
        goc.add(luoi, BorderLayout.CENTER);

        loi.setOpaque(true);
        loi.setBackground(new Color(0xFE, 0xE2, 0xE2));
        loi.setForeground(Theme.ERR);
        loi.setFont(Theme.font(Font.PLAIN, 13));
        loi.setBorder(new EmptyBorder(8, 12, 8, 12));
        loi.setVisible(false);

        Nut huy = new Nut("Hủy", Nut.Kieu.THUONG);
        huy.addActionListener(e -> dispose());
        nutLuu = new Nut(chuNutLuu, Nut.Kieu.CHINH);
        nutLuu.addActionListener(e -> luu());
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        nut.setOpaque(false);
        nut.add(huy);
        nut.add(nutLuu);

        JPanel duoi = new JPanel(new BorderLayout(0, 14));
        duoi.setOpaque(false);
        duoi.add(loi, BorderLayout.NORTH);
        duoi.add(nut, BorderLayout.SOUTH);
        goc.add(duoi, BorderLayout.SOUTH);

        setContentPane(goc);
        getRootPane().setDefaultButton(nutLuu);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // Thêm một ô: nhãn ở trên, ô nhập ở dưới; caHang = chiếm cả hàng
    protected void themO(String nhan, JComponent o, boolean caHang) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        if (!nhan.isEmpty()) p.add(Theme.nhan(nhan), BorderLayout.NORTH);
        p.add(o, BorderLayout.CENTER);
        if (caHang && viTri % soCot != 0) viTri += soCot - viTri % soCot;
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = viTri % soCot;
        c.gridy = viTri / soCot;
        c.gridwidth = caHang ? soCot : 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, c.gridx == 0 ? 0 : 7, 12, c.gridx + c.gridwidth >= soCot ? 0 : 7);
        luoi.add(p, c);
        viTri += caHang ? soCot : 1;
    }

    protected void themO(String nhan, JComponent o) {
        themO(nhan, o, false);
    }

    // Hiện hộp thoại (chờ tới khi đóng); trả về true nếu đã lưu thành công
    public boolean moVaCho() {
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return daLuu;
    }

    protected void baoLoi(String chu) {
        loi.setText("<html>" + chu.replace("&", "&amp;").replace("<", "&lt;") + "</html>");
        loi.setVisible(true);
        pack();
    }

    // Lớp con: đọc ô nhập, kiểm tra sơ bộ, trả về việc cần làm với DB (chạy nền).
    // Ném LoiNhap nếu dữ liệu nhập sai định dạng.
    protected abstract Callable<Object> taoViecLuu();

    private void luu() {
        loi.setVisible(false);
        Callable<Object> viec;
        try {
            viec = taoViecLuu();
        } catch (LoiNhap e) {
            baoLoi(e.getMessage());
            return;
        }
        nutLuu.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Nen.chay(viec, kq -> {
            daLuu = true;
            dispose();
        }, thongBao -> {
            nutLuu.setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
            baoLoi(thongBao);
        });
    }

    // ===== Đọc dữ liệu từ ô nhập =====
    public static class LoiNhap extends RuntimeException {
        public LoiNhap(String m) {
            super(m);
        }
    }

    protected static ONhap oChu(String goiY, int soKyTuToiDa) {
        ONhap t = new ONhap(goiY);
        if (soKyTuToiDa > 0) {
            ((javax.swing.text.AbstractDocument) t.getDocument()).setDocumentFilter(new javax.swing.text.DocumentFilter() {
                @Override
                public void replace(FilterBypass fb, int off, int len, String text, javax.swing.text.AttributeSet a)
                        throws javax.swing.text.BadLocationException {
                    int moi = fb.getDocument().getLength() - len + (text == null ? 0 : text.length());
                    if (moi <= soKyTuToiDa) super.replace(fb, off, len, text, a);
                    else Toolkit.getDefaultToolkit().beep();
                }

                @Override
                public void insertString(FilterBypass fb, int off, String text, javax.swing.text.AttributeSet a)
                        throws javax.swing.text.BadLocationException {
                    replace(fb, off, 0, text, a);
                }
            });
        }
        return t;
    }

    // Số tiền: chấp nhận "25000", "25.000", "25,000"; trống -> null
    protected static Long docTien(JTextField t, String tenO) {
        String s = t.getText().trim().replace(".", "").replace(",", "").replace(" ", "").replace("đ", "");
        if (s.isEmpty()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new LoiNhap(tenO + " phải là số nguyên (VD: 25000)");
        }
    }

    // Số thực: chấp nhận dấu phẩy hoặc chấm thập phân; trống -> null
    protected static Double docSoThuc(JTextField t, String tenO) {
        String s = t.getText().trim().replace(",", ".");
        if (s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new LoiNhap(tenO + " phải là số (VD: 20)");
        }
    }

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("d/M/yyyy");

    // Ngày: "2026-11-30" hoặc "30/11/2026"; trống -> null
    protected static LocalDate docNgay(JTextField t, String tenO) {
        String s = t.getText().trim();
        if (s.isEmpty()) return null;
        try {
            return s.contains("/") ? LocalDate.parse(s, DMY) : LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new LoiNhap(tenO + " không đúng dạng ngày (VD: 30/11/2026)");
        }
    }

    protected static String chuNgay(LocalDate d) {
        return d == null ? "" : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
