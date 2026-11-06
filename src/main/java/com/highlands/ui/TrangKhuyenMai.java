package com.highlands.ui;

import com.highlands.entity.KhuyenMai;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

// Trang "Quản lý Khuyến mãi" (index.html của bản web)
public class TrangKhuyenMai extends JPanel implements FormChinh.Trang {

    private final DichVu dv;
    private final ThongBao thongBao = new ThongBao();
    private final ONhap oTim = new ONhap("Tìm kiếm khuyến mãi theo tên...");
    private final Bang<KhuyenMai> bang = new Bang<>(64);
    private final Timer henTim = new Timer(250, e -> lamMoi());
    private int lanNap = 0; // chỉ hiện kết quả của lần tìm mới nhất

    public TrangKhuyenMai(DichVu dv) {
        super(new BorderLayout(0, 14));
        this.dv = dv;
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        // Đầu trang: tiêu đề + nút thêm
        The dau = new The(new BorderLayout());
        dau.add(Theme.tieuDe("Quản lý Khuyến mãi", 18), BorderLayout.WEST);
        Nut them = new Nut("+ Thêm khuyến mãi", Nut.Kieu.CHINH);
        them.addActionListener(e -> moForm(null));
        dau.add(them, BorderLayout.EAST);

        // Ô tìm kiếm: chờ 250ms sau khi gõ xong mới tìm
        The tim = new The(new BorderLayout(), 12, 12, 12, 12);
        tim.add(oTim, BorderLayout.CENTER);
        henTim.setRepeats(false);
        oTim.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { henTim.restart(); }
            public void removeUpdate(DocumentEvent e) { henTim.restart(); }
            public void changedUpdate(DocumentEvent e) { henTim.restart(); }
        });

        JPanel tren = new JPanel();
        tren.setOpaque(false);
        tren.setLayout(new BoxLayout(tren, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{thongBao, dau, tim}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height + 40));
            tren.add(c);
            tren.add(Box.createVerticalStrut(14));
        }
        tren.remove(tren.getComponentCount() - 1);
        add(tren, BorderLayout.NORTH);

        bang.datCot(List.of(
                new Bang.Cot<>("anh", "Ảnh", 72, KhuyenMai::getHinhAnh, O.anh(48)),
                new Bang.Cot<>("ten", "Tên", 200, KhuyenMai::getTen, O.chu(null, SwingConstants.LEFT, true, null)),
                new Bang.Cot<>("ma", "Mã", 120, k -> k.getMa() == null ? "-" : k.getMa(), null),
                new Bang.Cot<>("giam", "Giảm", 90, TrangKhuyenMai::chuGiam,
                        O.chu(null, SwingConstants.LEFT, true, Theme.BRAND)),
                new Bang.Cot<>("toiThieu", "Tối thiểu", 100,
                        k -> k.getDonToiThieu() == null || k.getDonToiThieu() == 0 ? "-" : Theme.tien(k.getDonToiThieu()), null),
                new Bang.Cot<>("batDau", "Bắt đầu", 105, k -> HopThoai.chuNgay(k.getNgayBatDau()), null),
                new Bang.Cot<>("ketThuc", "Kết thúc", 105, k -> HopThoai.chuNgay(k.getNgayKetThuc()), null),
                new Bang.Cot<>("trangThai", "Trạng thái", 120, k -> k, O.badge(v -> trangThai((KhuyenMai) v), true)),
                new Bang.Cot<>("thaoTac", "Thao tác", 105, k -> k, O.thaoTac())));
        O.ganThaoTac(bang.bang(), i -> moForm(bang.dongTai(i)), i -> xoa(bang.dongTai(i)));
        add(bang, BorderLayout.CENTER);
    }

    @Override
    public void khiMo() {
        lamMoi();
    }

    private static String chuGiam(KhuyenMai k) {
        if ("GIAM_TIEN".equals(k.getLoai())) return Theme.tien(k.getSoTienGiam());
        double p = k.getPhanTramGiam();
        return (p == Math.floor(p) ? String.valueOf((long) p) : String.valueOf(p)) + "%";
    }

    // Trạng thái theo ngày hôm nay: Sắp diễn ra / Đã kết thúc / Đang áp dụng
    private static Object[] trangThai(KhuyenMai k) {
        LocalDate hom = LocalDate.now();
        if (k.getNgayBatDau() != null && hom.isBefore(k.getNgayBatDau()))
            return new Object[]{"Sắp diễn ra", Theme.BADGE_SOON[0], Theme.BADGE_SOON[1]};
        if (k.getNgayKetThuc() != null && hom.isAfter(k.getNgayKetThuc()))
            return new Object[]{"Đã kết thúc", Theme.BADGE_OFF[0], Theme.BADGE_OFF[1]};
        return new Object[]{"Đang áp dụng", Theme.BADGE_ON[0], Theme.BADGE_ON[1]};
    }

    private void lamMoi() {
        String q = oTim.getText().trim();
        int lan = ++lanNap;
        Nen.chay(() -> q.isEmpty() ? dv.khuyenMai().layTatCa() : dv.khuyenMai().timKiem(q),
                ds -> {
                    if (lan == lanNap) bang.datDong(ds, "Chưa có khuyến mãi nào");
                },
                loi -> thongBao.hien(loi, false));
    }

    private void moForm(KhuyenMai k) {
        if (new HopThoaiKhuyenMai(SwingUtilities.getWindowAncestor(this), dv, k).moVaCho()) {
            thongBao.hien(k == null ? "Đã thêm khuyến mãi" : "Đã cập nhật khuyến mãi", true);
            lamMoi();
        }
    }

    private void xoa(KhuyenMai k) {
        if (k == null) return;
        int chon = JOptionPane.showConfirmDialog(this, "Xóa khuyến mãi \"" + k.getTen() + "\"?", "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (chon != JOptionPane.YES_OPTION) return;
        Nen.chay(() -> {
            dv.khuyenMai().xoa(k.getId());
            return null;
        }, x -> {
            thongBao.hien("Đã xóa khuyến mãi", true);
            lamMoi();
        }, loi -> thongBao.hien(loi, false));
    }
}
