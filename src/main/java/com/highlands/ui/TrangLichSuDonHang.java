package com.highlands.ui;

import com.highlands.service.ThanhToanService;
import com.highlands.service.ThanhToanService.DonLichSu;
import com.highlands.service.TienIch;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Lịch sử đơn hàng đã thanh toán: xem chi tiết và chỉnh sửa (trạng thái, số lượng món, thông tin giao dịch). */
public class TrangLichSuDonHang extends JPanel {
    private static final DateTimeFormatter DINH_DANG = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int COT_XEM = 6;
    private static final int COT_SUA = 7;

    private final ThanhToanService thanhToan;
    private final List<DonLichSu> ds = new ArrayList<>();
    private final Model model = new Model();
    private final JTable bang = new JTable(model);
    private final JTextField oMa = new JTextField();
    private final JCheckBox locNgay = new JCheckBox("Lọc theo ngày");
    private final JSpinner tuNgay = taoNgay();
    private final JSpinner denNgay = taoNgay();
    private final JLabel nhanTrangThai = new JLabel(" ");

    public TrangLichSuDonHang(ThanhToanService thanhToan) {
        this.thanhToan = thanhToan;
        setLayout(new BorderLayout(0, 10));
        setBackground(KieuNV.NEN);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel tren = new JPanel(new BorderLayout(0, 8));
        tren.setBackground(Color.WHITE);
        tren.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel tieuDe = new JLabel("Lịch sử đơn hàng");
        tieuDe.setFont(KieuNV.TIEU_DE);
        tieuDe.setForeground(KieuNV.DO);
        tren.add(tieuDe, BorderLayout.NORTH);

        JPanel loc = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        loc.setOpaque(false);
        oMa.setFont(KieuNV.FONT);
        oMa.setPreferredSize(new Dimension(200, 34));
        oMa.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        oMa.putClientProperty("JTextField.placeholderText", "Mã đơn hàng (VD: HD000012)");
        oMa.addActionListener(e -> lamMoi());
        locNgay.setOpaque(false);
        locNgay.setFont(KieuNV.FONT);
        tuNgay.setEnabled(false);
        denNgay.setEnabled(false);
        locNgay.addActionListener(e -> { tuNgay.setEnabled(locNgay.isSelected()); denNgay.setEnabled(locNgay.isSelected()); });
        JButton tim = KieuNV.nut("Tìm kiếm");
        JButton datLai = KieuNV.nutPhu("Làm mới");
        tim.addActionListener(e -> lamMoi());
        datLai.addActionListener(e -> {
            oMa.setText("");
            locNgay.setSelected(false);
            tuNgay.setEnabled(false);
            denNgay.setEnabled(false);
            lamMoi();
        });
        loc.add(new JLabel("Mã đơn:"));
        loc.add(oMa);
        loc.add(locNgay);
        loc.add(new JLabel("Từ"));
        loc.add(tuNgay);
        loc.add(new JLabel("đến"));
        loc.add(denNgay);
        loc.add(tim);
        loc.add(datLai);
        tren.add(loc, BorderLayout.CENTER);
        add(tren, BorderLayout.NORTH);

        KieuNV.kieuBang(bang);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.getColumnModel().getColumn(0).setPreferredWidth(110);
        bang.getColumnModel().getColumn(1).setPreferredWidth(150);
        bang.getColumnModel().getColumn(2).setPreferredWidth(180);
        bang.getColumnModel().getColumn(3).setPreferredWidth(130);
        bang.getColumnModel().getColumn(4).setPreferredWidth(120);
        bang.getColumnModel().getColumn(5).setPreferredWidth(110);
        bang.getColumnModel().getColumn(3).setCellRenderer(KieuNV.canPhai());
        DefaultTableCellRenderer lienKet = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, "<html><u>Xem chi tiết</u></html>", s, f, r, c);
                setForeground(KieuNV.DO_SANG);
                setFont(KieuNV.DAM);
                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        };
        bang.getColumnModel().getColumn(COT_XEM).setCellRenderer(lienKet);
        DefaultTableCellRenderer lienKetSua = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                boolean khoa = ThanhToanService.DA_HUY.equals(ds.get(r).trangThai());
                super.getTableCellRendererComponent(t, khoa ? "—" : "<html><u>Chỉnh sửa</u></html>", s, f, r, c);
                setForeground(khoa ? KieuNV.CHU_PHU : KieuNV.DO_SANG);
                setFont(KieuNV.DAM);
                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        };
        bang.getColumnModel().getColumn(COT_SUA).setCellRenderer(lienKetSua);
        bang.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int r = bang.rowAtPoint(e.getPoint());
                int c = bang.columnAtPoint(e.getPoint());
                if (r < 0) return;
                if (c == COT_SUA) suaDon(r);
                else if (c == COT_XEM || e.getClickCount() == 2) xemChiTiet(r);
            }
        });
        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(KieuNV.VIEN));
        cuon.getViewport().setBackground(Color.WHITE);
        add(cuon, BorderLayout.CENTER);

        nhanTrangThai.setFont(KieuNV.NHO);
        nhanTrangThai.setForeground(KieuNV.CHU_PHU);
        add(nhanTrangThai, BorderLayout.SOUTH);
    }

    private static JSpinner taoNgay() {
        JSpinner s = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        s.setPreferredSize(new Dimension(110, 34));
        s.setFont(KieuNV.FONT);
        return s;
    }

    private static LocalDate sangNgay(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Tải lại danh sách theo bộ lọc hiện tại (gọi mỗi khi mở trang này). */
    public void lamMoi() {
        String ma = oMa.getText();
        LocalDate tu = locNgay.isSelected() ? sangNgay(tuNgay) : null;
        LocalDate den = locNgay.isSelected() ? sangNgay(denNgay) : null;
        if (tu != null && den != null && tu.isAfter(den)) {
            KieuNV.loi(this, "Ngày bắt đầu không được sau ngày kết thúc.");
            return;
        }
        nhanTrangThai.setText("Đang tải...");
        ChayNen.chay(() -> thanhToan.timDonHang(ma, tu, den), kq -> {
            ds.clear();
            ds.addAll(kq);
            model.fireTableDataChanged();
            nhanTrangThai.setText(kq.size() + " đơn hàng");
        }, ex -> {
            nhanTrangThai.setText(" ");
            KieuNV.loi(this, ChayNen.thongDiep(ex));
        });
    }

    private void xemChiTiet(int dong) {
        Long id = ds.get(dong).thanhToanId();
        ChayNen.chay(() -> thanhToan.layHoaDon(id), hd ->
                new HopThoaiHoaDon(SwingUtilities.getWindowAncestor(this), hd).setVisible(true),
                ex -> KieuNV.loi(this, ChayNen.thongDiep(ex)));
    }

    private void suaDon(int dong) {
        DonLichSu d = ds.get(dong);
        if (ThanhToanService.DA_HUY.equals(d.trangThai())) {
            KieuNV.thongTin(this, "Đơn đã hủy, không thể chỉnh sửa.");
            return;
        }
        ChayNen.chay(() -> thanhToan.layDeSua(d.thanhToanId()), don -> {
            HopThoaiSuaDon h = new HopThoaiSuaDon(SwingUtilities.getWindowAncestor(this), thanhToan, don);
            h.setVisible(true);
            if (h.daLuu()) lamMoi();
        }, ex -> KieuNV.loi(this, ChayNen.thongDiep(ex)));
    }

    private final class Model extends AbstractTableModel {
        private final String[] cot = {"Mã đơn hàng", "Ngày đặt", "Nhân viên thực hiện", "Tổng tiền", "Phương thức", "Trạng thái", "Chi tiết", "Chỉnh sửa"};
        @Override public int getRowCount() { return ds.size(); }
        @Override public int getColumnCount() { return cot.length; }
        @Override public String getColumnName(int c) { return cot[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }   // chỉ xem
        @Override public Object getValueAt(int r, int c) {
            DonLichSu d = ds.get(r);
            return switch (c) {
                case 0 -> TienIch.maHienThi(d.maDon());
                case 1 -> d.ngay().format(DINH_DANG);
                case 2 -> d.nhanVien();
                case 3 -> KieuNV.tien(d.tong());
                case 4 -> d.phuongThuc();
                case 5 -> d.trangThai();
                default -> "";
            };
        }
    }
}
