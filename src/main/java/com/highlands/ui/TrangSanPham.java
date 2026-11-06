package com.highlands.ui;

import com.highlands.entity.DanhMuc;
import com.highlands.entity.SanPham;
import com.highlands.entity.Topping;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Trang "Sản phẩm & Menu" (san-pham.html của bản web): 3 tab Sản phẩm / Danh mục / Topping, mỗi trang 10 dòng
public class TrangSanPham extends JPanel implements FormChinh.Trang {

    public enum Loai { SAN_PHAM, DANH_MUC, TOPPING }

    // Một dòng trong bảng (đưa 3 loại dữ liệu về cùng một dạng)
    public record Dong(Long id, String ten, Long gia, Long danhMucId, String danhMucTen, String hinhAnh, Object goc) {
        static Dong tu(SanPham s) {
            DanhMuc d = s.getDanhMuc();
            return new Dong(s.getId(), s.getTen(), s.getGia(), d == null ? null : d.getId(),
                    d == null ? null : d.getTen(), s.getHinhAnh(), s);
        }

        static Dong tu(DanhMuc d) {
            return new Dong(d.getId(), d.getTen(), null, null, null, null, d);
        }

        static Dong tu(Topping t) {
            return new Dong(t.getId(), t.getTen(), t.getGia(), null, null, t.getHinhAnh(), t);
        }
    }

    private static final int SO_DONG_MOI_TRANG = 10;

    private final DichVu dv;
    private final ThongBao thongBao = new ThongBao();
    private final JLabel tieuDe = Theme.tieuDe("", 18);
    private final Nut nutThem = new Nut("", Nut.Kieu.CHINH);
    private final Nut tabSp = new Nut("Sản phẩm", Nut.Kieu.TAB_CHON);
    private final Nut tabDm = new Nut("Danh mục", Nut.Kieu.TAB);
    private final Nut tabTp = new Nut("Topping", Nut.Kieu.TAB);
    private final The theTim = new The(new BorderLayout(10, 0), 12, 12, 12, 12);
    private final ONhap oTim = new ONhap("Tìm theo tên sản phẩm...");
    private final JComboBox<DanhMuc> locDanhMuc = new JComboBox<>();
    private final Bang<Dong> bang = new Bang<>(64);
    private final JPanel phanTrang = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
    private final Nut truoc = new Nut("‹ Trước", Nut.Kieu.THUONG);
    private final Nut sau = new Nut("Sau ›", Nut.Kieu.THUONG);
    private final JLabel chuTrang = Theme.nhan("");
    private final Timer henTim = new Timer(250, e -> {
        trang = 1;
        lamMoi();
    });

    private Loai loai = Loai.SAN_PHAM;
    private List<Dong> tatCa = new ArrayList<>();
    private List<DanhMuc> danhMuc = new ArrayList<>();
    private Set<Long> hot = new HashSet<>();
    private int trang = 1;
    private int lanNap = 0;
    private boolean dangDoDanhMuc = false;

    public TrangSanPham(DichVu dv) {
        super(new BorderLayout(0, 14));
        this.dv = dv;
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        The dau = new The(new BorderLayout());
        dau.add(tieuDe, BorderLayout.WEST);
        nutThem.addActionListener(e -> moForm(null));
        dau.add(nutThem, BorderLayout.EAST);

        JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tab.setOpaque(false);
        tab.add(tabSp);
        tab.add(tabDm);
        tab.add(tabTp);
        tabSp.addActionListener(e -> chonTab(Loai.SAN_PHAM));
        tabDm.addActionListener(e -> chonTab(Loai.DANH_MUC));
        tabTp.addActionListener(e -> chonTab(Loai.TOPPING));

        Theme.kieuCombo(locDanhMuc);
        locDanhMuc.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean f) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, sel, f);
                c.setText(v == null ? "" : ((DanhMuc) v).getTen());
                c.setBorder(new EmptyBorder(4, 8, 4, 8));
                return c;
            }
        });
        locDanhMuc.addActionListener(e -> {
            if (dangDoDanhMuc) return;
            trang = 1;
            lamMoi();
        });
        theTim.add(oTim, BorderLayout.CENTER);
        theTim.add(locDanhMuc, BorderLayout.EAST);
        henTim.setRepeats(false);
        oTim.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { henTim.restart(); }
            public void removeUpdate(DocumentEvent e) { henTim.restart(); }
            public void changedUpdate(DocumentEvent e) { henTim.restart(); }
        });

        JPanel tren = new JPanel();
        tren.setOpaque(false);
        tren.setLayout(new BoxLayout(tren, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{thongBao, dau, tab, theTim}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height + 40));
            tren.add(c);
            tren.add(Box.createVerticalStrut(14));
        }
        tren.remove(tren.getComponentCount() - 1);
        add(tren, BorderLayout.NORTH);

        O.ganThaoTac(bang.bang(), i -> moForm(bang.dongTai(i)), i -> xoa(bang.dongTai(i)));
        add(bang, BorderLayout.CENTER);

        phanTrang.setOpaque(false);
        phanTrang.add(truoc);
        phanTrang.add(chuTrang);
        phanTrang.add(sau);
        truoc.addActionListener(e -> {
            trang--;
            hienTrang();
        });
        sau.addActionListener(e -> {
            trang++;
            hienTrang();
        });
        add(phanTrang, BorderLayout.SOUTH);

        chonTab(Loai.SAN_PHAM, false);
    }

    @Override
    public void khiMo() {
        taiHot();
        chonTab(loai);
    }

    // Top 5 bán chạy 30 ngày gần nhất -> nhãn "Hot"
    private void taiHot() {
        Nen.chay(() -> dv.thongKe().banChay(LocalDate.now().minusDays(30), LocalDate.now(), 5), ds -> {
            Set<Long> s = new HashSet<>();
            for (Map<String, Object> m : ds) s.add(((Number) m.get("id")).longValue());
            hot = s;
            bang.bang().repaint();
        }, loi -> hot = new HashSet<>());
    }

    private void chonTab(Loai l) {
        chonTab(l, true);
    }

    private void chonTab(Loai l, boolean napDuLieu) {
        loai = l;
        trang = 1;
        tabSp.setKieu(l == Loai.SAN_PHAM ? Nut.Kieu.TAB_CHON : Nut.Kieu.TAB);
        tabDm.setKieu(l == Loai.DANH_MUC ? Nut.Kieu.TAB_CHON : Nut.Kieu.TAB);
        tabTp.setKieu(l == Loai.TOPPING ? Nut.Kieu.TAB_CHON : Nut.Kieu.TAB);
        tieuDe.setText(switch (l) {
            case SAN_PHAM -> "Quản lý sản phẩm";
            case DANH_MUC -> "Quản lý Danh mục";
            case TOPPING -> "Quản lý Topping";
        });
        nutThem.setText(switch (l) {
            case SAN_PHAM -> "+ Thêm Sản phẩm mới";
            case DANH_MUC -> "+ Thêm danh mục";
            case TOPPING -> "+ Thêm topping";
        });
        nutThem.revalidate();
        theTim.setVisible(l == Loai.SAN_PHAM);
        oTim.setText("");
        henTim.stop(); // xóa ô tìm không cần tìm lại, chọn tab đã nạp

        List<Bang.Cot<Dong>> cot = new ArrayList<>();
        if (l != Loai.DANH_MUC) cot.add(new Bang.Cot<>("anh", "Ảnh", 72, Dong::hinhAnh, O.anh(48)));
        cot.add(new Bang.Cot<>("ten", l == Loai.SAN_PHAM ? "Tên món" : l == Loai.DANH_MUC ? "Tên danh mục" : "Tên topping",
                260, Dong::ten, O.chu(null, SwingConstants.LEFT, true, null)));
        if (l == Loai.SAN_PHAM) {
            cot.add(new Bang.Cot<>("danhMuc", "Danh mục", 170, d -> d, O.badge(v -> {
                Dong d = (Dong) v;
                if (d.danhMucTen() == null) return null;
                Color[] m = Theme.MAU_DANH_MUC[(int) (d.danhMucId() % Theme.MAU_DANH_MUC.length)];
                return new Object[]{d.danhMucTen(), m[0], m[1]};
            }, false)));
        }
        if (l != Loai.DANH_MUC) {
            cot.add(new Bang.Cot<>("gia", "Giá", 130, d -> Theme.tien(d.gia()),
                    O.chu(null, SwingConstants.LEFT, true, Theme.BRAND)));
        }
        if (l == Loai.SAN_PHAM) {
            cot.add(new Bang.Cot<>("hot", "Bán chạy", 110, d -> d, O.badge(v -> hot.contains(((Dong) v).id())
                    ? new Object[]{"Hot", Theme.BADGE_ON[0], Theme.BADGE_ON[1]} : null, true)));
        }
        cot.add(new Bang.Cot<>("thaoTac", "Thao tác", 120, d -> d, O.thaoTac()));
        bang.datCot(cot);
        tatCa = new ArrayList<>();
        hienTrang();

        if (napDuLieu) taiDanhMucRoiLamMoi();
    }

    // Nạp lại ô lọc danh mục (có dòng "Tất cả danh mục"), xong thì nạp bảng
    private void taiDanhMucRoiLamMoi() {
        Nen.chay(() -> dv.danhMuc().layTatCa(), ds -> {
            danhMuc = ds;
            DanhMuc dangChon = (DanhMuc) locDanhMuc.getSelectedItem();
            Long idChon = dangChon == null ? null : dangChon.getId();
            dangDoDanhMuc = true;
            locDanhMuc.removeAllItems();
            DanhMuc tatCaDm = new DanhMuc();
            tatCaDm.setTen("Tất cả danh mục");
            locDanhMuc.addItem(tatCaDm);
            for (DanhMuc d : ds) {
                locDanhMuc.addItem(d);
                if (idChon != null && idChon.equals(d.getId())) locDanhMuc.setSelectedItem(d);
            }
            dangDoDanhMuc = false;
            lamMoi();
        }, loi -> thongBao.hien(loi, false));
    }

    private void lamMoi() {
        Loai l = loai;
        String q = oTim.getText().trim();
        DanhMuc dm = (DanhMuc) locDanhMuc.getSelectedItem();
        Long dmId = dm == null ? null : dm.getId();
        int lan = ++lanNap;
        Nen.chay(() -> {
            List<Dong> kq = new ArrayList<>();
            switch (l) {
                case SAN_PHAM -> dv.sanPham().layDanhSach(q, dmId).forEach(s -> kq.add(Dong.tu(s)));
                case DANH_MUC -> dv.danhMuc().layTatCa().forEach(d -> kq.add(Dong.tu(d)));
                case TOPPING -> dv.topping().layTatCa().forEach(t -> kq.add(Dong.tu(t)));
            }
            return kq;
        }, ds -> {
            if (lan != lanNap || l != loai) return;
            tatCa = ds;
            hienTrang();
        }, loi -> thongBao.hien(loi, false));
    }

    // Hiện 10 dòng của trang hiện tại
    private void hienTrang() {
        int tong = Math.max(1, (int) Math.ceil(tatCa.size() / (double) SO_DONG_MOI_TRANG));
        trang = Math.max(1, Math.min(trang, tong));
        int dau = (trang - 1) * SO_DONG_MOI_TRANG;
        int cuoi = Math.min(tatCa.size(), trang * SO_DONG_MOI_TRANG);
        bang.datDong(tatCa.subList(dau, cuoi), "Chưa có " + tenMuc() + " nào");
        phanTrang.setVisible(tong > 1);
        chuTrang.setText("Trang " + trang + " / " + tong);
        truoc.setEnabled(trang > 1);
        sau.setEnabled(trang < tong);
    }

    private String tenMuc() {
        return switch (loai) {
            case SAN_PHAM -> "sản phẩm";
            case DANH_MUC -> "danh mục";
            case TOPPING -> "topping";
        };
    }

    private void moForm(Dong d) {
        HopThoaiMuc f = new HopThoaiMuc(SwingUtilities.getWindowAncestor(this), dv, loai, d, danhMuc);
        if (f.moVaCho()) {
            thongBao.hien(d == null ? "Đã thêm" : "Đã cập nhật", true);
            taiDanhMucRoiLamMoi();
        }
    }

    private void xoa(Dong d) {
        if (d == null) return;
        int chon = JOptionPane.showConfirmDialog(this, "Xóa " + tenMuc() + " \"" + d.ten() + "\"?", "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (chon != JOptionPane.YES_OPTION) return;
        Loai l = loai;
        Nen.chay(() -> {
            switch (l) {
                case SAN_PHAM -> dv.sanPham().xoa(d.id());
                case DANH_MUC -> dv.danhMuc().xoa(d.id());
                case TOPPING -> dv.topping().xoa(d.id());
            }
            return null;
        }, x -> {
            thongBao.hien("Đã xóa", true);
            taiDanhMucRoiLamMoi();
        }, loi -> thongBao.hien(loi, false));
    }
}
