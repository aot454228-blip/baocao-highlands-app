package com.highlands.ui;

import com.highlands.service.ThanhToanService.Dong;
import com.highlands.service.ThanhToanService.HoaDon;
import com.highlands.service.TienIch;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.format.DateTimeFormatter;

/** Hóa đơn bán hàng (xem + in). Dùng cho cả lúc vừa thanh toán và xem lại từ lịch sử. */
class HopThoaiHoaDon extends JDialog {
    private static final DateTimeFormatter DINH_DANG = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Font F = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font F_DAM = new Font("SansSerif", Font.BOLD, 12);
    private static final Font F_NHO = new Font("SansSerif", Font.PLAIN, 11);
    private final JPanel giay = new JPanel();

    HopThoaiHoaDon(Window cha, HoaDon hd) {
        super(cha, "Hóa đơn " + TienIch.maHienThi(hd.maDon()), ModalityType.APPLICATION_MODAL);
        xayDung(hd);

        JPanel nen = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        nen.setBackground(KieuNV.NEN);
        nen.add(giay);
        JScrollPane khung = new JScrollPane(nen);
        khung.setBorder(null);
        khung.getVerticalScrollBar().setUnitIncrement(16);

        JButton in = KieuNV.nut("In hóa đơn");
        JButton dong = KieuNV.nutPhu("Đóng");
        in.addActionListener(e -> in());
        dong.addActionListener(e -> dispose());
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        nut.setBackground(Color.WHITE);
        nut.add(in);
        nut.add(dong);

        JPanel goc = new JPanel(new BorderLayout());
        goc.add(khung, BorderLayout.CENTER);
        goc.add(nut, BorderLayout.SOUTH);
        setContentPane(goc);
        getRootPane().setDefaultButton(in);
        setSize(460, 700);
        setLocationRelativeTo(cha);
    }

    private void xayDung(HoaDon hd) {
        giay.setLayout(new BoxLayout(giay, BoxLayout.Y_AXIS));
        giay.setBackground(Color.WHITE);
        giay.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        giay.setPreferredSize(null);

        LogoTron logo = new LogoTron(60);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        giay.add(logo);
        giay.add(Box.createVerticalStrut(6));
        giay.add(giuaDong("HIGHLANDS COFFEE", new Font("SansSerif", Font.BOLD, 16)));
        giay.add(giuaDong("HÓA ĐƠN BÁN HÀNG", F_DAM));
        giay.add(Box.createVerticalStrut(8));
        giay.add(hang("Mã hóa đơn:", TienIch.maHienThi(hd.maDon()), F_DAM));
        giay.add(hang("Ngày giờ:", hd.ngay().format(DINH_DANG), F));
        giay.add(hang("Nhân viên:", hd.nhanVien(), F));
        giay.add(gach());

        for (Dong d : hd.dong()) {
            giay.add(hang(d.soLuong() + " x " + d.ten(), KieuNV.tien(d.thanhTien()), F_DAM));
            giay.add(hang("   Đơn giá: " + KieuNV.tien(d.donGia()), "", F_NHO));
            if (!d.topping().isEmpty())
                giay.add(hang("   Topping: " + String.join(", ", d.topping()) + " (+" + KieuNV.tien(d.giaTopping()) + "/ly)", "", F_NHO));
            giay.add(Box.createVerticalStrut(4));
        }
        giay.add(gach());
        giay.add(hang("Tiền hàng:", KieuNV.tien(hd.tienHang()), F));
        String km = hd.khuyenMai() == null || hd.khuyenMai().isBlank() ? "Khuyến mãi:" : "Khuyến mãi (" + hd.khuyenMai() + "):";
        giay.add(hang(km, hd.giamGia().signum() > 0 ? "-" + KieuNV.tien(hd.giamGia()) : KieuNV.tien(hd.giamGia()), F));
        giay.add(hang("TỔNG THANH TOÁN:", KieuNV.tien(hd.tongThanhToan()), new Font("SansSerif", Font.BOLD, 14)));
        giay.add(gach());
        giay.add(hang("Phương thức:", hd.phuongThuc(), F));
        giay.add(hang("Trạng thái:", hd.trangThai() == null ? "" : hd.trangThai(), F));
        if (!com.highlands.service.ThanhToanService.CHUYEN_KHOAN.equals(hd.phuongThuc())) {
            giay.add(hang("Tiền khách đưa:", KieuNV.tien(hd.tienKhach()), F));
            giay.add(hang("Tiền thừa:", KieuNV.tien(hd.tienThua()), F));
        }
        if (hd.ghiChu() != null && !hd.ghiChu().isBlank()) giay.add(hang("Ghi chú: " + hd.ghiChu(), "", F_NHO));
        giay.add(Box.createVerticalStrut(12));
        giay.add(giuaDong("Cảm ơn quý khách – Hẹn gặp lại!", F_NHO));

        giay.setSize(new Dimension(380, giay.getPreferredSize().height));
        giay.setPreferredSize(new Dimension(380, giay.getPreferredSize().height));
        giay.setMaximumSize(giay.getPreferredSize());
    }

    private static JComponent giuaDong(String chu, Font f) {
        JLabel l = new JLabel(chu);
        l.setFont(f);
        l.setForeground(Color.BLACK);
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    private static JComponent hang(String trai, String phai, Font f) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel a = new JLabel("<html><div style='width:215px'>" + KieuNV.escape(trai) + "</div></html>");
        a.setFont(f);
        a.setForeground(Color.BLACK);
        a.setVerticalAlignment(SwingConstants.TOP);
        p.add(a, BorderLayout.WEST);
        if (!phai.isEmpty()) {
            JLabel b = new JLabel(phai);
            b.setFont(f);
            b.setForeground(Color.BLACK);
            b.setVerticalAlignment(SwingConstants.TOP);
            p.add(b, BorderLayout.EAST);
        }
        p.setAlignmentX(CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 2));
        return p;
    }

    private static JComponent gach() {
        JSeparator s = new JSeparator();
        s.setForeground(Color.GRAY);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        return s;
    }

    private void in() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Hóa đơn Highlands");
        job.setPrintable((Graphics g, PageFormat pf, int trang) -> {
            double tyLe = Math.min(1.0, pf.getImageableWidth() / giay.getWidth());
            double caoTrang = pf.getImageableHeight() / tyLe;
            int soTrang = (int) Math.ceil(giay.getHeight() / caoTrang);
            if (trang >= soTrang) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.scale(tyLe, tyLe);
            g2.translate(0, -trang * caoTrang);
            g2.setClip(0, (int) (trang * caoTrang), giay.getWidth(), (int) caoTrang + 1);
            giay.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (!job.printDialog()) return;
        try {
            job.print();
        } catch (PrinterException ex) {
            KieuNV.loi(this, "Không in được hóa đơn: " + ex.getMessage());
        }
    }
}
