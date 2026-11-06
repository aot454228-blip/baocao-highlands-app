package com.highlands.ui;

import com.highlands.service.ThanhToanService;
import com.highlands.service.ThanhToanService.ChiTietSua;
import com.highlands.service.ThanhToanService.DonSua;
import com.highlands.service.TienIch;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Chỉnh sửa đơn hàng đã thanh toán: trạng thái, số lượng từng món (0 = bỏ món), thông tin giao dịch. */
class HopThoaiSuaDon extends JDialog {
    private final ThanhToanService dich;
    private final DonSua don;
    private final int[] soLuong;
    private final Model model = new Model();
    private final JTable bang = new JTable(model);
    private final JComboBox<String> trangThai = new JComboBox<>(ThanhToanService.TRANG_THAI.toArray(new String[0]));
    private final JComboBox<String> phuongThuc =
            new JComboBox<>(new String[]{ThanhToanService.TIEN_MAT, ThanhToanService.CHUYEN_KHOAN});
    private final JTextField oKhach = new JTextField();
    private final JTextField oGhiChu = new JTextField();
    private final JLabel nhanTong = new JLabel(" ");
    private final JLabel nhanHuy = new JLabel(" ");
    private final JButton nutLuu = KieuNV.nut("Lưu thay đổi");
    private final JButton nutDong = KieuNV.nutPhu("Đóng");
    private boolean daLuu;
    private boolean dangXuLy;

    HopThoaiSuaDon(Window cha, ThanhToanService dich, DonSua don) {
        super(cha, "Chỉnh sửa đơn " + TienIch.maHienThi(don.maDon()), ModalityType.APPLICATION_MODAL);
        this.dich = dich;
        this.don = don;
        this.soLuong = new int[don.dong().size()];
        for (int i = 0; i < soLuong.length; i++) soLuong[i] = don.dong().get(i).soLuong();

        JPanel goc = new JPanel();
        goc.setLayout(new BoxLayout(goc, BoxLayout.Y_AXIS));
        goc.setBackground(Color.WHITE);
        goc.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel tieuDe = new JLabel("Chỉnh sửa đơn " + TienIch.maHienThi(don.maDon()));
        tieuDe.setFont(KieuNV.TIEU_DE);
        tieuDe.setForeground(KieuNV.DO);
        tieuDe.setAlignmentX(LEFT_ALIGNMENT);
        goc.add(tieuDe);
        goc.add(Box.createVerticalStrut(12));

        // --- Trạng thái ---
        goc.add(nhan("Trạng thái đơn"));
        goc.add(Box.createVerticalStrut(4));
        trangThai.setSelectedItem(don.trangThai());
        daTuyChinh(trangThai);
        goc.add(trangThai);
        nhanHuy.setFont(KieuNV.NHO);
        nhanHuy.setForeground(KieuNV.DO_SANG);
        nhanHuy.setAlignmentX(LEFT_ALIGNMENT);
        goc.add(nhanHuy);
        trangThai.addActionListener(e -> capNhat());
        goc.add(Box.createVerticalStrut(10));

        // --- Sản phẩm trong đơn ---
        goc.add(nhan("Sản phẩm trong đơn (sửa cột Số lượng, nhập 0 để bỏ món)"));
        goc.add(Box.createVerticalStrut(4));
        KieuNV.kieuBang(bang);
        bang.getColumnModel().getColumn(0).setPreferredWidth(170);
        bang.getColumnModel().getColumn(1).setPreferredWidth(150);
        bang.getColumnModel().getColumn(2).setPreferredWidth(100);
        bang.getColumnModel().getColumn(3).setPreferredWidth(70);
        bang.getColumnModel().getColumn(4).setPreferredWidth(110);
        bang.getColumnModel().getColumn(2).setCellRenderer(KieuNV.canPhai());
        bang.getColumnModel().getColumn(4).setCellRenderer(KieuNV.canPhai());
        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(KieuNV.VIEN));
        cuon.getViewport().setBackground(Color.WHITE);
        cuon.setAlignmentX(LEFT_ALIGNMENT);
        cuon.setPreferredSize(new Dimension(600, 170));
        goc.add(cuon);
        goc.add(Box.createVerticalStrut(6));
        nhanTong.setFont(KieuNV.DAM);
        nhanTong.setAlignmentX(LEFT_ALIGNMENT);
        goc.add(nhanTong);
        goc.add(Box.createVerticalStrut(10));

        // --- Thông tin giao dịch ---
        goc.add(nhan("Phương thức thanh toán"));
        goc.add(Box.createVerticalStrut(4));
        phuongThuc.setSelectedItem(don.phuongThuc());
        daTuyChinh(phuongThuc);
        phuongThuc.addActionListener(e -> capNhat());
        goc.add(phuongThuc);
        goc.add(Box.createVerticalStrut(8));

        goc.add(nhan("Tiền khách đưa (VNĐ) - chỉ dùng cho tiền mặt"));
        goc.add(Box.createVerticalStrut(4));
        oKhach.setText(don.tienKhach().setScale(0, RoundingMode.HALF_UP).toPlainString());
        oKhach.setFont(KieuNV.FONT);
        oKhach.setAlignmentX(LEFT_ALIGNMENT);
        oKhach.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        oKhach.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        goc.add(oKhach);
        goc.add(Box.createVerticalStrut(8));

        goc.add(nhan("Ghi chú giao dịch (VD: mã giao dịch ngân hàng)"));
        goc.add(Box.createVerticalStrut(4));
        oGhiChu.setText(don.ghiChu() == null ? "" : don.ghiChu());
        oGhiChu.setFont(KieuNV.FONT);
        oGhiChu.setAlignmentX(LEFT_ALIGNMENT);
        oGhiChu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        oGhiChu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        goc.add(oGhiChu);
        goc.add(Box.createVerticalStrut(14));

        JPanel nut = new JPanel(new GridLayout(1, 2, 10, 0));
        nut.setOpaque(false);
        nut.setAlignmentX(LEFT_ALIGNMENT);
        nut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        nutDong.addActionListener(e -> { if (!dangXuLy) dispose(); });
        nutLuu.addActionListener(e -> luu());
        nut.add(nutDong);
        nut.add(nutLuu);
        goc.add(nut);

        setContentPane(new JScrollPane(goc, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
        setSize(680, Math.min(760, Toolkit.getDefaultToolkit().getScreenSize().height - 80));
        setLocationRelativeTo(cha);
        capNhat();
    }

    private static JLabel nhan(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(KieuNV.DAM);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static void daTuyChinh(JComboBox<String> c) {
        c.setFont(KieuNV.FONT);
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
    }

    boolean daLuu() {
        return daLuu;
    }

    private BigDecimal tienHangMoi() {
        BigDecimal t = BigDecimal.ZERO;
        for (int i = 0; i < soLuong.length; i++) {
            ChiTietSua d = don.dong().get(i);
            t = t.add(d.donGia().add(d.giaTopping()).multiply(BigDecimal.valueOf(soLuong[i])));
        }
        return t;
    }

    private void capNhat() {
        boolean huy = ThanhToanService.DA_HUY.equals(trangThai.getSelectedItem());
        nhanHuy.setText(huy ? "Hủy đơn: đơn sẽ không tính vào doanh thu và không thể sửa lại." : " ");
        boolean tienMat = ThanhToanService.TIEN_MAT.equals(phuongThuc.getSelectedItem());
        oKhach.setEnabled(tienMat && !huy);
        bang.setEnabled(!huy);
        phuongThuc.setEnabled(!huy);
        String hau = don.khuyenMai() == null ? "" : "  (khuyến mãi " + don.khuyenMai() + " sẽ được tính lại khi lưu)";
        nhanTong.setText(huy ? "Tổng tiền đơn: " + KieuNV.tien(don.tong())
                : "Tiền hàng sau chỉnh sửa: " + KieuNV.tien(tienHangMoi()) + hau);
        nhanTong.setForeground(huy ? KieuNV.CHU_PHU : KieuNV.CHU);
    }

    private void luu() {
        String tt = (String) trangThai.getSelectedItem();
        boolean huy = ThanhToanService.DA_HUY.equals(tt);
        String pt = (String) phuongThuc.getSelectedItem();

        BigDecimal kh = null;
        if (!huy) {
            boolean conMon = false;
            for (int s : soLuong) if (s > 0) conMon = true;
            if (!conMon) {
                KieuNV.loi(this, "Đơn phải còn ít nhất một món. Muốn bỏ cả đơn, hãy chọn trạng thái \"Đã hủy\".");
                return;
            }
            if (ThanhToanService.TIEN_MAT.equals(pt)) {
                String so = oKhach.getText().replaceAll("\\D", "");
                if (so.isEmpty() || so.length() > 12) {
                    KieuNV.loi(this, "Vui lòng nhập số tiền khách đưa hợp lệ.");
                    return;
                }
                kh = new BigDecimal(so);
            }
        }
        String hoi = huy
                ? "Hủy đơn " + TienIch.maHienThi(don.maDon()) + "?\nĐơn sẽ không tính vào doanh thu và không thể sửa lại."
                : "Lưu các thay đổi của đơn " + TienIch.maHienThi(don.maDon()) + "?";
        if (JOptionPane.showConfirmDialog(this, hoi, "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        Map<Long, Integer> sl = new LinkedHashMap<>();
        for (int i = 0; i < soLuong.length; i++) sl.put(don.dong().get(i).id(), soLuong[i]);
        final BigDecimal khachDua = kh;
        final String ghiChu = oGhiChu.getText();

        dangXuLy = true;
        nutLuu.setEnabled(false);
        nutDong.setEnabled(false);
        ChayNen.chay(() -> dich.capNhatDon(don.thanhToanId(), tt, sl, pt, khachDua, ghiChu), kq -> {
            daLuu = true;
            if (kq.canhBao() != null) KieuNV.thongTin(this, kq.canhBao());
            dispose();
        }, ex -> {
            dangXuLy = false;
            nutLuu.setEnabled(true);
            nutDong.setEnabled(true);
            KieuNV.loi(this, ChayNen.thongDiep(ex));
        });
    }

    private final class Model extends AbstractTableModel {
        private final String[] cot = {"Món", "Topping", "Đơn giá", "Số lượng", "Thành tiền"};

        @Override public int getRowCount() { return soLuong.length; }
        @Override public int getColumnCount() { return cot.length; }
        @Override public String getColumnName(int c) { return cot[c]; }
        @Override public Class<?> getColumnClass(int c) { return c == 3 ? Integer.class : String.class; }
        @Override public boolean isCellEditable(int r, int c) { return c == 3 && !dangXuLy; }

        @Override public Object getValueAt(int r, int c) {
            ChiTietSua d = don.dong().get(r);
            BigDecimal donGia = d.donGia().add(d.giaTopping());
            return switch (c) {
                case 0 -> d.ten();
                case 1 -> d.topping().isEmpty() ? "—" : String.join(", ", d.topping());
                case 2 -> KieuNV.tien(donGia);
                case 3 -> soLuong[r];
                default -> KieuNV.tien(donGia.multiply(BigDecimal.valueOf(soLuong[r])));
            };
        }

        @Override public void setValueAt(Object v, int r, int c) {
            if (c != 3 || !(v instanceof Integer n)) return;
            soLuong[r] = Math.max(0, Math.min(99, n));
            fireTableRowsUpdated(r, r);
            capNhat();
        }
    }
}
