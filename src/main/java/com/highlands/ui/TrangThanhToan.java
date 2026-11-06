package com.highlands.ui;

import com.highlands.service.DongGio;
import com.highlands.service.ThanhToanService;
import com.highlands.service.ThanhToanService.HoaDon;
import com.highlands.service.ThanhToanService.BaoGia;
import com.highlands.service.TienIch;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/** Hộp thoại thanh toán (tiền mặt hoặc chuyển khoản). Chỉ đóng với kết quả khác null khi đã lưu đơn thành công. */
class TrangThanhToan extends JDialog {
    private final ThanhToanService dich;
    private final Long nhanVienId;
    private final List<DongGio> gio;
    private final String maKhuyenMai;
    private BigDecimal tong;
    private BigDecimal tienHangHienThi;
    private BigDecimal giamHienThi;
    private final JLabel lTienHang = new JLabel();
    private final JLabel lGiam = new JLabel();
    private final JLabel lTong = new JLabel();
    private final JLabel huongDan = new JLabel();
    private final String maGiaoDich;

    private final JTextField oKhach = new JTextField();
    private final JLabel nhanThua = new JLabel(" ");
    private final JButton nutXacNhan = KieuNV.nut("Xác nhận thanh toán");
    private final JButton nutHuy = KieuNV.nutPhu("Hủy");
    private final JComboBox<String> phuongThuc = new JComboBox<>(
            new String[]{ThanhToanService.TIEN_MAT, ThanhToanService.CHUYEN_KHOAN});
    private final CardLayout theKhuVuc = new CardLayout();
    private final JPanel khuVuc = new JPanel(theKhuVuc);
    private final JCheckBox daNhanTien = new JCheckBox("Đã nhận được tiền chuyển khoản của khách");
    private final JPanel luoiNhanh = new JPanel(new GridLayout(1, 0, 6, 0));
    private HoaDon ketQua;
    private boolean dangXuLy;

    TrangThanhToan(Window cha, ThanhToanService dich, Long nhanVienId, List<DongGio> gio, String maKhuyenMai,
                   BigDecimal tienHang, BigDecimal giam, String maGiaoDich) {
        super(cha, "Thanh toán", ModalityType.APPLICATION_MODAL);
        this.dich = dich;
        this.nhanVienId = nhanVienId;
        this.gio = gio;
        this.maKhuyenMai = maKhuyenMai;
        this.maGiaoDich = maGiaoDich;
        this.tong = tienHang.subtract(giam);
        this.tienHangHienThi = tienHang;
        this.giamHienThi = giam;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { if (!dangXuLy) dispose(); }
        });

        JPanel goc = new JPanel();
        goc.setLayout(new BoxLayout(goc, BoxLayout.Y_AXIS));
        goc.setBackground(Color.WHITE);
        goc.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel tieuDe = new JLabel("Thanh toán đơn hàng");
        tieuDe.setFont(KieuNV.TIEU_DE);
        tieuDe.setForeground(KieuNV.DO);
        tieuDe.setAlignmentX(LEFT_ALIGNMENT);
        goc.add(tieuDe);
        goc.add(Box.createVerticalStrut(12));
        lTienHang.setText(KieuNV.tien(tienHang));
        lGiam.setText(KieuNV.tien(giam));
        lTong.setText(KieuNV.tien(tong));
        goc.add(hang("Tổng tiền hàng", lTienHang, false));
        goc.add(hang("Tiền giảm giá" + (maKhuyenMai == null ? "" : " (" + maKhuyenMai + ")"),
                lGiam, false));
        goc.add(hang("TỔNG THANH TOÁN", lTong, true));
        goc.add(Box.createVerticalStrut(12));

        JLabel pt = new JLabel("Phương thức thanh toán");
        pt.setFont(KieuNV.DAM);
        pt.setAlignmentX(LEFT_ALIGNMENT);
        goc.add(pt);
        goc.add(Box.createVerticalStrut(4));
        phuongThuc.setFont(KieuNV.FONT);
        phuongThuc.setAlignmentX(LEFT_ALIGNMENT);
        phuongThuc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        goc.add(phuongThuc);
        goc.add(Box.createVerticalStrut(12));

        JPanel tienMat = new JPanel();
        tienMat.setLayout(new BoxLayout(tienMat, BoxLayout.Y_AXIS));
        tienMat.setOpaque(false);

        JLabel kh = new JLabel("Tiền khách đưa (VNĐ)");
        kh.setFont(KieuNV.DAM);
        kh.setAlignmentX(LEFT_ALIGNMENT);
        tienMat.add(kh);
        tienMat.add(Box.createVerticalStrut(4));
        oKhach.setFont(new Font("Segoe UI", Font.BOLD, 20));
        oKhach.setHorizontalAlignment(SwingConstants.RIGHT);
        oKhach.setAlignmentX(LEFT_ALIGNMENT);
        oKhach.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        oKhach.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(2, 10, 2, 10)));
        oKhach.getDocument().addDocumentListener(new TrangBanHang.SuKienGoPhim(this::capNhat));
        tienMat.add(oKhach);
        tienMat.add(Box.createVerticalStrut(8));

        luoiNhanh.setOpaque(false);
        luoiNhanh.setAlignmentX(LEFT_ALIGNMENT);
        luoiNhanh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JButton vua = KieuNV.nutPhu("Vừa đủ");
        vua.addActionListener(e -> datTienKhach(tong));
        luoiNhanh.add(vua);
        for (long menhGia : new long[]{50_000, 100_000, 200_000, 500_000}) {
            JButton b = KieuNV.nutPhu(String.format("%,d", menhGia).replace(',', '.'));
            b.addActionListener(e -> datTienKhach(BigDecimal.valueOf(menhGia)));
            luoiNhanh.add(b);
        }
        tienMat.add(luoiNhanh);
        tienMat.add(Box.createVerticalStrut(10));
        nhanThua.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nhanThua.setAlignmentX(LEFT_ALIGNMENT);
        tienMat.add(nhanThua);

        // Chuyển khoản: khách chuyển đúng số tiền, nhân viên xác nhận đã nhận được tiền
        JPanel chuyenKhoan = new JPanel();
        chuyenKhoan.setLayout(new BoxLayout(chuyenKhoan, BoxLayout.Y_AXIS));
        chuyenKhoan.setOpaque(false);
        capNhatHuongDan();
        huongDan.setFont(KieuNV.FONT);
        huongDan.setAlignmentX(LEFT_ALIGNMENT);
        chuyenKhoan.add(huongDan);
        chuyenKhoan.add(Box.createVerticalStrut(14));
        daNhanTien.setOpaque(false);
        daNhanTien.setFont(KieuNV.DAM);
        daNhanTien.setAlignmentX(LEFT_ALIGNMENT);
        daNhanTien.addActionListener(e -> capNhat());
        chuyenKhoan.add(daNhanTien);

        khuVuc.setOpaque(false);
        khuVuc.setAlignmentX(LEFT_ALIGNMENT);
        khuVuc.add(tienMat, ThanhToanService.TIEN_MAT);
        khuVuc.add(chuyenKhoan, ThanhToanService.CHUYEN_KHOAN);
        phuongThuc.addActionListener(e -> {
            theKhuVuc.show(khuVuc, (String) phuongThuc.getSelectedItem());
            capNhat();
        });
        goc.add(khuVuc);
        goc.add(Box.createVerticalStrut(14));

        JPanel nut = new JPanel(new GridLayout(1, 2, 10, 0));
        nut.setOpaque(false);
        nut.setAlignmentX(LEFT_ALIGNMENT);
        nut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        nutHuy.addActionListener(e -> { if (!dangXuLy) dispose(); });
        nutXacNhan.addActionListener(e -> xacNhan());
        nut.add(nutHuy);
        nut.add(nutXacNhan);
        goc.add(nut);

        setContentPane(goc);
        getRootPane().setDefaultButton(nutXacNhan);
        pack();
        setSize(new Dimension(470, Math.max(getHeight(), 560)));
        setLocationRelativeTo(cha);
        capNhat();
        SwingUtilities.invokeLater(oKhach::requestFocusInWindow);
    }

    private JComponent hang(String ten, JLabel b, boolean dam) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JLabel a = new JLabel(ten);
        a.setFont(dam ? new Font("Segoe UI", Font.BOLD, 16) : KieuNV.FONT);
        b.setFont(dam ? new Font("Segoe UI", Font.BOLD, 22) : KieuNV.DAM);
        if (dam) b.setForeground(KieuNV.DO);
        h.add(a, BorderLayout.WEST);
        h.add(b, BorderLayout.EAST);
        h.setAlignmentX(LEFT_ALIGNMENT);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, dam ? 38 : 28));
        return h;
    }

    private BigDecimal tienKhach() {
        return TienIch.docTienKhach(oKhach.getText());
    }

    private void capNhatHuongDan() {
        huongDan.setText("<html>Khách chuyển khoản đúng số tiền <b>" + KieuNV.tien(tong)
                + "</b>.<br>Kiểm tra tin nhắn / ứng dụng ngân hàng rồi tick xác nhận bên dưới.</html>");
    }

    private void datTienKhach(BigDecimal v) {
        oKhach.setText(v.toPlainString());
    }

    private boolean laChuyenKhoan() {
        return ThanhToanService.CHUYEN_KHOAN.equals(phuongThuc.getSelectedItem());
    }

    private void capNhat() {
        if (laChuyenKhoan()) {
            nutXacNhan.setEnabled(!dangXuLy && daNhanTien.isSelected());
            return;
        }
        BigDecimal kh;
        try {
            kh = tienKhach();
        } catch (com.highlands.service.LoiNghiepVu ex) {
            nhanThua.setForeground(KieuNV.DO_SANG);
            nhanThua.setText(oKhach.getText().isBlank() ? "Nhập số tiền khách đưa" : "Số tiền không hợp lệ");
            nutXacNhan.setEnabled(false);
            return;
        }
        BigDecimal chenh = kh.subtract(tong);
        if (kh.signum() == 0 && tong.signum() > 0) {
            nhanThua.setForeground(KieuNV.CHU_PHU);
            nhanThua.setText("Nhập số tiền khách đưa");
        } else if (chenh.signum() < 0) {
            nhanThua.setForeground(KieuNV.DO_SANG);
            nhanThua.setText("Còn thiếu: " + KieuNV.tien(chenh.negate()));
        } else {
            nhanThua.setForeground(KieuNV.XANH);
            nhanThua.setText("Tiền thừa: " + KieuNV.tien(chenh));
        }
        nutXacNhan.setEnabled(!dangXuLy && kh.compareTo(tong) >= 0);
    }

    private void xacNhan() {
        if (dangXuLy) return;
        datDangXuLy(true);
        // Khi lần trước mất phản hồi sau commit, lấy lại hóa đơn trước khi kiểm tra giá mới.
        ChayNen.chay(() -> dich.timHoaDonDaLuu(maGiaoDich), hd -> {
            if (hd != null) {
                ketQua = hd;
                dispose();
            } else {
                kiemTraGia();
            }
        }, ex -> {
            datDangXuLy(false);
            KieuNV.loi(this, ChayNen.thongDiep(ex) + "\nChưa xác định kết quả. Hãy thử lại với giỏ hàng hiện tại.");
        });
    }

    private void kiemTraGia() {
        ChayNen.chay(() -> dich.baoGia(gio, maKhuyenMai), gia -> {
            boolean doi = gia.tienHang().compareTo(tienHangHienThi) != 0 || gia.giam().compareTo(giamHienThi) != 0;
            tienHangHienThi = gia.tienHang();
            giamHienThi = gia.giam();
            tong = gia.tong();
            lTienHang.setText(KieuNV.tien(gia.tienHang()));
            lGiam.setText(KieuNV.tien(gia.giam()));
            lTong.setText(KieuNV.tien(tong));
            capNhatHuongDan();
            if (doi) {
                daNhanTien.setSelected(false);
                oKhach.setText("");
                datDangXuLy(false);
                KieuNV.thongTin(this, "Giá hoặc khuyến mãi đã thay đổi. Hãy kiểm tra tổng mới và xác nhận lại tiền khách trả.");
                return;
            }
            luuThanhToan(gia);
        }, ex -> {
            datDangXuLy(false);
            KieuNV.loi(this, ChayNen.thongDiep(ex) + "\nChưa thanh toán. Có thể quay lại giỏ để kiểm tra món hoặc mã khuyến mãi.");
        });
    }

    private void datDangXuLy(boolean value) {
        dangXuLy = value;
        nutHuy.setEnabled(!value);
        oKhach.setEnabled(!value);
        phuongThuc.setEnabled(!value);
        daNhanTien.setEnabled(!value);
        for (Component c : luoiNhanh.getComponents()) c.setEnabled(!value);
        capNhat();
    }

    private void luuThanhToan(BaoGia baoGiaDaXacNhan) {
        final boolean chuyenKhoan = laChuyenKhoan();
        final String phuongThucThanhToan;
        if (chuyenKhoan) {
            phuongThucThanhToan = ThanhToanService.CHUYEN_KHOAN;
        } else {
            phuongThucThanhToan = ThanhToanService.TIEN_MAT;
        }

        final BigDecimal tienKhachDua;
        try {
            if (chuyenKhoan) {
                tienKhachDua = tong;
            } else {
                tienKhachDua = tienKhach();
            }
        } catch (com.highlands.service.LoiNghiepVu loi) {
            datDangXuLy(false);
            KieuNV.loi(this, loi.getMessage());
            return;
        }

        if (chuyenKhoan && !daNhanTien.isSelected()) {
            datDangXuLy(false);
            KieuNV.loi(this, "Hãy xác nhận đã nhận được tiền chuyển khoản.");
            return;
        }
        if (!chuyenKhoan && tienKhachDua.compareTo(tong) < 0) {
            datDangXuLy(false);
            KieuNV.loi(this, "Tiền khách đưa chưa đủ.");
            return;
        }

        String noiDungXacNhan;
        if (chuyenKhoan) {
            noiDungXacNhan = "Xác nhận đã nhận " + KieuNV.tien(tong) + " bằng chuyển khoản?";
        } else {
            BigDecimal tienThua = tienKhachDua.subtract(tong);
            noiDungXacNhan = "Xác nhận thanh toán " + KieuNV.tien(tong)
                    + " bằng tiền mặt?\nTiền thừa trả khách: " + KieuNV.tien(tienThua);
        }
        int luaChon = JOptionPane.showConfirmDialog(
                this, noiDungXacNhan, "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (luaChon != JOptionPane.YES_OPTION) {
            datDangXuLy(false);
            return;
        }

        datDangXuLy(true);
        ChayNen.chay(() -> dich.thanhToan(
                nhanVienId, gio, maKhuyenMai, tienKhachDua, maGiaoDich,
                phuongThucThanhToan, baoGiaDaXacNhan), hoaDon -> {
            ketQua = hoaDon;
            dispose();
        }, loi -> {
            datDangXuLy(false);
            String thongBaoLoi = ChayNen.thongDiep(loi) + "\nGiỏ hàng vẫn được giữ nguyên.";
            KieuNV.loi(this, thongBaoLoi);
        });
    }

    HoaDon getKetQua() { return ketQua; }
}
