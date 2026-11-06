package com.highlands.ui;

import com.highlands.entity.DanhMuc;
import com.highlands.entity.SanPham;
import com.highlands.entity.Topping;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;

// Hộp thoại thêm / sửa cho 3 loại: sản phẩm, danh mục, topping
public class HopThoaiMuc extends HopThoai {

    private final DichVu dv;
    private final TrangSanPham.Loai loai;
    private final TrangSanPham.Dong cu; // null = thêm mới

    private final ONhap ten = oChu("", 100);
    private final ONhap gia = oChu("VD: 45000", 15);
    private final JComboBox<DanhMuc> danhMuc = new JComboBox<>();
    private final ONhap anh = oChu("/images/ten-anh.jpg", 500);
    private final Anh.XemTruoc xemTruoc = new Anh.XemTruoc(64);

    public HopThoaiMuc(Window cha, DichVu dv, TrangSanPham.Loai loai, TrangSanPham.Dong d, List<DanhMuc> dsDanhMuc) {
        super(cha, tieuDe(loai, d), d == null ? "Thêm" : "Lưu thay đổi", 1);
        this.dv = dv;
        this.loai = loai;
        this.cu = d;

        String tenMuc = loai == TrangSanPham.Loai.SAN_PHAM ? "Tên sản phẩm"
                : loai == TrangSanPham.Loai.DANH_MUC ? "Tên danh mục" : "Tên topping";
        ten.setColumns(28);
        themO(tenMuc, ten);

        if (loai != TrangSanPham.Loai.DANH_MUC) themO("Giá (VND)", gia);

        if (loai == TrangSanPham.Loai.SAN_PHAM) {
            Theme.kieuCombo(danhMuc);
            danhMuc.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean f) {
                    JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, sel, f);
                    c.setText(v == null ? "-- Chọn danh mục --" : ((DanhMuc) v).getTen());
                    c.setBorder(new EmptyBorder(4, 8, 4, 8));
                    return c;
                }
            });
            danhMuc.addItem(null);
            for (DanhMuc x : dsDanhMuc) {
                danhMuc.addItem(x);
                if (d != null && x.getId().equals(d.danhMucId())) danhMuc.setSelectedItem(x);
            }
            themO("Danh mục", danhMuc);
        }

        if (loai != TrangSanPham.Loai.DANH_MUC) {
            themO("Đường dẫn ảnh (VD: /images/sp-1.jpg)", anh);
            JPanel pAnh = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            pAnh.setOpaque(false);
            pAnh.add(xemTruoc);
            themO("Xem trước ảnh", pAnh);
            anh.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { xemTruoc.setUrl(anh.getText().trim()); }
                public void removeUpdate(DocumentEvent e) { xemTruoc.setUrl(anh.getText().trim()); }
                public void changedUpdate(DocumentEvent e) { xemTruoc.setUrl(anh.getText().trim()); }
            });
        }

        if (d != null) {
            ten.setText(d.ten());
            gia.setText(d.gia() == null ? "" : String.valueOf(d.gia()));
            anh.setText(d.hinhAnh() == null ? "" : d.hinhAnh());
        }
        xemTruoc.setUrl(anh.getText().trim());
    }

    private static String tieuDe(TrangSanPham.Loai l, TrangSanPham.Dong d) {
        String ten = l == TrangSanPham.Loai.SAN_PHAM ? "sản phẩm" : l == TrangSanPham.Loai.DANH_MUC ? "danh mục" : "topping";
        if (d != null) return "Sửa " + ten + " (ID " + d.id() + ")";
        return l == TrangSanPham.Loai.SAN_PHAM ? "Thêm Sản phẩm mới" : "Thêm " + ten;
    }

    @Override
    protected Callable<Object> taoViecLuu() {
        String t = ten.getText().trim();
        String a = anh.getText().trim().isEmpty() ? null : anh.getText().trim();
        Long g = loai == TrangSanPham.Loai.DANH_MUC ? null : docTien(gia, "Giá");
        if (loai != TrangSanPham.Loai.DANH_MUC && g == null) throw new LoiNhap("Vui lòng nhập giá");

        // Kiểm tra đầy đủ (trống tên, giá, trùng tên...) nằm ở các Service
        switch (loai) {
            case SAN_PHAM -> {
                SanPham s = new SanPham();
                s.setTen(t);
                s.setGia(g);
                s.setHinhAnh(a);
                s.setDanhMuc((DanhMuc) danhMuc.getSelectedItem());
                return () -> cu == null ? dv.sanPham().them(s) : dv.sanPham().sua(cu.id(), s);
            }
            case DANH_MUC -> {
                DanhMuc d = new DanhMuc();
                d.setTen(t);
                return () -> cu == null ? dv.danhMuc().them(d) : dv.danhMuc().sua(cu.id(), d);
            }
            default -> {
                Topping tp = new Topping();
                tp.setTen(t);
                tp.setGia(g);
                tp.setHinhAnh(a);
                return () -> cu == null ? dv.topping().them(tp) : dv.topping().sua(cu.id(), tp);
            }
        }
    }
}
