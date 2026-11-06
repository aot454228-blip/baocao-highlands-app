package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

// Trang "Thống kê" (thong-ke.html của bản web): doanh thu, chi phí, lợi nhuận, món bán chạy
public class TrangThongKe extends JPanel implements FormChinh.Trang {

    private final DichVu dv;
    private final ThongBao thongBao = new ThongBao();
    private final JSpinner tuNgay = oNgay();
    private final JSpinner denNgay = oNgay();
    private final JLabel soDoanhThu = soLon(Theme.OK);
    private final JLabel soChiPhi = soLon(Theme.ERR);
    private final JLabel soLoiNhuan = soLon(Theme.TEXT);
    private final Bang<Map<String, Object>> bangDoanhThu = new Bang<>(42);
    private final Bang<Map<String, Object>> bangChiPhi = new Bang<>(42);
    private final Bang<Map<String, Object>> bangBanChay = new Bang<>(48);

    public TrangThongKe(DichVu dv) {
        super(new BorderLayout());
        this.dv = dv;
        setBackground(Theme.BG);

        JPanel cot = new JPanel();
        cot.setOpaque(false);
        cot.setLayout(new BoxLayout(cot, BoxLayout.Y_AXIS));
        cot.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Chọn khoảng ngày
        The khoang = new The(new FlowLayout(FlowLayout.LEFT, 12, 0), 14, 8, 14, 8);
        khoang.add(oCoNhan("Từ ngày", tuNgay));
        khoang.add(oCoNhan("Đến ngày", denNgay));
        Nut xem = new Nut("Xem báo cáo", Nut.Kieu.CHINH);
        xem.addActionListener(e -> xem());
        JPanel pXem = new JPanel(new BorderLayout());
        pXem.setOpaque(false);
        pXem.add(xem, BorderLayout.SOUTH);
        khoang.add(pXem);

        // 3 thẻ số liệu
        JPanel kpi = new JPanel(new GridLayout(1, 3, 14, 0));
        kpi.setOpaque(false);
        kpi.add(theSo("Tổng doanh thu", soDoanhThu));
        kpi.add(theSo("Tổng chi phí", soChiPhi));
        kpi.add(theSo("Lợi nhuận (doanh thu - chi phí)", soLoiNhuan));

        // Bảng doanh thu và chi phí theo ngày, đặt cạnh nhau
        for (Bang<Map<String, Object>> b : List.of(bangDoanhThu, bangChiPhi)) {
            b.datCot(List.of(
                    new Bang.Cot<>("ngay", "Ngày", 140, m -> chuNgay(m.get("ngay")), null),
                    new Bang.Cot<>("tien", b == bangDoanhThu ? "Doanh thu (VND)" : "Chi phí nhập (VND)", 180,
                            m -> Theme.so(m.get("tien")), O.chu(null, SwingConstants.RIGHT, false, null))));
        }
        JPanel haiBang = new JPanel(new GridLayout(1, 2, 14, 0));
        haiBang.setOpaque(false);
        haiBang.add(coTieuDe("Báo cáo doanh thu", bangDoanhThu, 300));
        haiBang.add(coTieuDe("Báo cáo chi phí", bangChiPhi, 300));

        // Món bán chạy: số thứ tự, tên, số lượng, doanh thu, thanh tỉ lệ
        bangBanChay.datCot(List.of(
                new Bang.Cot<>("stt", "#", 50, m -> m.get("stt"), O.chu(null, SwingConstants.LEFT, false, Theme.MUTED)),
                new Bang.Cot<>("ten", "Món", 220, m -> m.get("ten"), O.chu(null, SwingConstants.LEFT, true, null)),
                new Bang.Cot<>("sl", "Số lượng", 100, m -> Theme.so(m.get("soLuong")), O.chu(null, SwingConstants.RIGHT, false, null)),
                new Bang.Cot<>("dt", "Doanh thu (VND)", 150, m -> Theme.so(m.get("doanhThu")),
                        O.chu(null, SwingConstants.RIGHT, true, Theme.BRAND)),
                new Bang.Cot<>("thanh", "", 220, m -> m.get("tiLe"), O.thanh())));

        JComponent[] ds = {thongBao, khoang, kpi, haiBang, coTieuDe("Top 5 món bán chạy", bangBanChay, 340)};
        for (JComponent c : ds) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            cot.add(c);
            cot.add(Box.createVerticalStrut(14));
        }

        JScrollPane cuon = Theme.kieuCuon(new JScrollPane(cot));
        cuon.setBorder(BorderFactory.createEmptyBorder());
        cuon.getViewport().setBackground(Theme.BG);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        add(cuon, BorderLayout.CENTER);

        // Mặc định 30 ngày gần nhất, như bản web
        denNgay.setValue(sangDate(LocalDate.now()));
        tuNgay.setValue(sangDate(LocalDate.now().minusDays(30)));
    }

    @Override
    public void khiMo() {
        xem();
    }

    private void xem() {
        thongBao.an();
        LocalDate tu = sangLocalDate((Date) tuNgay.getValue());
        LocalDate den = sangLocalDate((Date) denNgay.getValue());
        if (tu.isAfter(den)) {
            thongBao.hien("Từ ngày phải trước hoặc bằng đến ngày", false);
            return;
        }
        Nen.chay(() -> new Object[]{dv.thongKe().doanhThu(tu, den), dv.thongKe().chiPhi(tu, den),
                dv.thongKe().banChay(tu, den, 5)}, kq -> hien(kq), loi -> thongBao.hien(loi, false));
    }

    @SuppressWarnings("unchecked")
    private void hien(Object[] kq) {
        Map<String, Object> dt = (Map<String, Object>) kq[0];
        Map<String, Object> cp = (Map<String, Object>) kq[1];
        List<Map<String, Object>> bc = (List<Map<String, Object>>) kq[2];

        long tongDt = ((Number) dt.get("tong")).longValue();
        long tongCp = ((Number) cp.get("tong")).longValue();
        long ln = tongDt - tongCp;
        soDoanhThu.setText(Theme.so(tongDt));
        soChiPhi.setText(Theme.so(tongCp));
        soLoiNhuan.setText(Theme.so(ln));
        soLoiNhuan.setForeground(ln >= 0 ? Theme.OK : Theme.ERR);

        bangDoanhThu.datDong((List<Map<String, Object>>) dt.get("theoNgay"), "Không có dữ liệu");
        bangChiPhi.datDong((List<Map<String, Object>>) cp.get("theoNgay"), "Không có dữ liệu");

        long max = 1;
        for (Map<String, Object> m : bc) max = Math.max(max, ((Number) m.get("soLuong")).longValue());
        List<Map<String, Object>> dong = new ArrayList<>();
        for (int i = 0; i < bc.size(); i++) {
            Map<String, Object> m = new java.util.HashMap<>(bc.get(i));
            m.put("stt", i + 1);
            m.put("tiLe", ((Number) m.get("soLuong")).doubleValue() / max);
            dong.add(m);
        }
        bangBanChay.datDong(dong, "Không có dữ liệu");
    }

    // ===== Các thành phần nhỏ =====
    private static JSpinner oNgay() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        s.setFont(Theme.font(Font.PLAIN, 13));
        s.setPreferredSize(new Dimension(150, 36));
        s.setBorder(new Theme.VienBo(Theme.VIEN_O, 8));
        JFormattedTextField t = ((JSpinner.DefaultEditor) s.getEditor()).getTextField();
        t.setBorder(new EmptyBorder(0, 10, 0, 6));
        t.setBackground(Color.WHITE);
        return s;
    }

    private static JComponent oCoNhan(String nhan, JComponent o) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(Theme.nhan(nhan), BorderLayout.NORTH);
        p.add(o, BorderLayout.CENTER);
        return p;
    }

    private static JLabel soLon(Color mau) {
        JLabel l = new JLabel("0");
        l.setFont(Theme.font(Font.BOLD, 22));
        l.setForeground(mau);
        return l;
    }

    private static JComponent theSo(String nhan, JLabel so) {
        The t = new The(new BorderLayout(0, 6), 16, 18, 16, 18);
        t.add(Theme.nhan(nhan), BorderLayout.NORTH);
        t.add(so, BorderLayout.CENTER);
        return t;
    }

    // Bảng có tiêu đề nhỏ phía trên, chiều cao cố định
    private static JComponent coTieuDe(String tieuDe, JComponent bang, int cao) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JLabel l = Theme.tieuDe(tieuDe, 15);
        l.setBorder(new EmptyBorder(0, 4, 0, 0));
        p.add(l, BorderLayout.NORTH);
        p.add(bang, BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(300, cao));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, cao));
        return p;
    }

    private static String chuNgay(Object v) {
        if (v == null) return "";
        try {
            return LocalDate.parse(v.toString()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return v.toString();
        }
    }

    private static Date sangDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate sangLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
