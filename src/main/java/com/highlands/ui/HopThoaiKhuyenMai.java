package com.highlands.ui;

import com.highlands.entity.KhuyenMai;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.concurrent.Callable;

// Hộp thoại thêm / sửa khuyến mãi
public class HopThoaiKhuyenMai extends HopThoai {

    private static final String[] LOAI = {"PHAN_TRAM", "GIAM_TIEN"};

    private final DichVu dv;
    private final KhuyenMai cu; // null = thêm mới

    private final ONhap ten = oChu("VD: Giảm giá khai trương", 100);
    private final ONhap ma = oChu("Để trống nếu không cần mã", 50);
    private final JComboBox<String> loai = new JComboBox<>(new String[]{"Giảm theo phần trăm", "Giảm số tiền cố định"});
    private final ONhap phanTram = oChu("VD: 20", 10);
    private final ONhap soTien = oChu("VD: 10000", 15);
    private final ONhap toiThieu = oChu("Để trống = không yêu cầu", 15);
    private final ONhap batDau = oChu("dd/mm/yyyy", 10);
    private final ONhap ketThuc = oChu("dd/mm/yyyy", 10);
    private final ONhap anh = oChu("/images/ten-anh.jpg", 500);
    private final Anh.XemTruoc xemTruoc = new Anh.XemTruoc(56);
    private final CardLayout theGiam = new CardLayout();
    private final JPanel oGiam = new JPanel(theGiam);
    private final JLabel nhanGiam = Theme.nhan("");

    public HopThoaiKhuyenMai(Window cha, DichVu dv, KhuyenMai k) {
        super(cha, k == null ? "Thêm khuyến mãi" : "Sửa khuyến mãi (ID " + k.getId() + ")",
                k == null ? "Thêm" : "Lưu thay đổi", 2);
        this.dv = dv;
        this.cu = k;

        Theme.kieuCombo(loai);
        oGiam.setOpaque(false);
        oGiam.add(phanTram, LOAI[0]);
        oGiam.add(soTien, LOAI[1]);
        loai.addActionListener(e -> doiLoai());

        themO("Tên khuyến mãi", ten);
        themO("Mã khuyến mãi (VD: GIAM10K)", ma);
        themO("Loại giảm", loai);
        // Nhãn của ô giảm đổi theo loại nên tự dựng thay vì dùng themO
        JPanel pGiam = new JPanel(new BorderLayout(0, 4));
        pGiam.setOpaque(false);
        pGiam.add(nhanGiam, BorderLayout.NORTH);
        pGiam.add(oGiam, BorderLayout.CENTER);
        themO("", pGiam);
        themO("Đơn tối thiểu (VND)", toiThieu);
        themO("Ngày bắt đầu", batDau);
        themO("Ngày kết thúc", ketThuc);
        themO("Đường dẫn ảnh (VD: /images/km-1.jpg)", anh);

        JPanel pAnh = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pAnh.setOpaque(false);
        pAnh.add(xemTruoc);
        themO("Xem trước ảnh", pAnh);
        anh.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { xemTruoc.setUrl(anh.getText().trim()); }
            public void removeUpdate(DocumentEvent e) { xemTruoc.setUrl(anh.getText().trim()); }
            public void changedUpdate(DocumentEvent e) { xemTruoc.setUrl(anh.getText().trim()); }
        });

        if (k != null) {
            ten.setText(k.getTen());
            ma.setText(k.getMa() == null ? "" : k.getMa());
            loai.setSelectedIndex("GIAM_TIEN".equals(k.getLoai()) ? 1 : 0);
            if (!"GIAM_TIEN".equals(k.getLoai())) {
                double p = k.getPhanTramGiam();
                phanTram.setText(p == Math.floor(p) ? String.valueOf((long) p) : String.valueOf(p));
            }
            soTien.setText(k.getSoTienGiam() == null ? "" : String.valueOf(k.getSoTienGiam()));
            toiThieu.setText(k.getDonToiThieu() == null ? "" : String.valueOf(k.getDonToiThieu()));
            batDau.setText(chuNgay(k.getNgayBatDau()));
            ketThuc.setText(chuNgay(k.getNgayKetThuc()));
            anh.setText(k.getHinhAnh() == null ? "" : k.getHinhAnh());
        }
        doiLoai();
        xemTruoc.setUrl(anh.getText().trim());
    }

    // Chọn loại giảm nào thì hiện ô nhập tương ứng
    private void doiLoai() {
        boolean laTien = loai.getSelectedIndex() == 1;
        theGiam.show(oGiam, LOAI[laTien ? 1 : 0]);
        nhanGiam.setText(laTien ? "Số tiền giảm (VND)" : "Phần trăm giảm (1 - 100)");
    }

    @Override
    protected Callable<Object> taoViecLuu() {
        boolean laTien = loai.getSelectedIndex() == 1;
        KhuyenMai k = new KhuyenMai();
        k.setTen(ten.getText().trim());
        k.setMa(ma.getText().trim().isEmpty() ? null : ma.getText().trim());
        k.setLoai(LOAI[laTien ? 1 : 0]);
        if (laTien) {
            Long t = docTien(soTien, "Số tiền giảm");
            if (t == null) throw new LoiNhap("Vui lòng nhập số tiền giảm");
            k.setSoTienGiam(t);
            k.setPhanTramGiam(0);
        } else {
            Double p = docSoThuc(phanTram, "Phần trăm giảm");
            if (p == null) throw new LoiNhap("Vui lòng nhập phần trăm giảm");
            k.setPhanTramGiam(p);
        }
        k.setDonToiThieu(docTien(toiThieu, "Đơn tối thiểu"));
        k.setNgayBatDau(docNgay(batDau, "Ngày bắt đầu"));
        k.setNgayKetThuc(docNgay(ketThuc, "Ngày kết thúc"));
        k.setHinhAnh(anh.getText().trim().isEmpty() ? null : anh.getText().trim());

        // Kiểm tra đầy đủ (trống tên, trùng mã, ngày...) nằm ở KhuyenMaiService
        return () -> cu == null ? dv.khuyenMai().them(k) : dv.khuyenMai().sua(cu.getId(), k);
    }
}
