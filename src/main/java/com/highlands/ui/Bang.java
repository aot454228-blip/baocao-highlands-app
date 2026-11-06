package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// Bảng dữ liệu dùng chung: khai báo cột (tiêu đề, độ rộng, lấy giá trị, kiểu ô) rồi đổ danh sách vào
public class Bang<T> extends JPanel {

    public record Cot<T>(String id, String tieuDe, int rong, Function<T, Object> lay, TableCellRenderer o) {
    }

    // Bảng giãn theo khung; khung hẹp hơn tổng độ rộng cột thì hiện thanh cuộn ngang
    private final JTable bang = new JTable() {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            if (getParent() == null) return true;
            int toiThieu = 0;
            for (int i = 0; i < getColumnCount(); i++) toiThieu += getColumnModel().getColumn(i).getMinWidth();
            return toiThieu <= getParent().getWidth();
        }
    };
    private final JLabel trong = new JLabel("", SwingConstants.CENTER);
    private List<Cot<T>> cot = new ArrayList<>();
    private List<T> dong = new ArrayList<>();

    private final AbstractTableModel mau = new AbstractTableModel() {
        @Override
        public int getRowCount() {
            return dong.size();
        }

        @Override
        public int getColumnCount() {
            return cot.size();
        }

        @Override
        public String getColumnName(int c) {
            return cot.get(c).tieuDe();
        }

        @Override
        public Object getValueAt(int r, int c) {
            return cot.get(c).lay().apply(dong.get(r));
        }
    };

    public Bang(int chieuCaoDong) {
        super(new BorderLayout());
        setOpaque(false);
        bang.setModel(mau);
        bang.setAutoCreateColumnsFromModel(false);
        Theme.kieuBang(bang, chieuCaoDong);

        JScrollPane cuon = Theme.kieuCuon(new JScrollPane(bang));
        cuon.setBorder(BorderFactory.createEmptyBorder());
        cuon.getViewport().setBackground(Color.WHITE);

        // Khi không có dòng nào: giữ tiêu đề cột, hiện chữ "Chưa có ..." trong vùng bảng
        trong.setForeground(Theme.MUTED);
        trong.setFont(Theme.font(Font.PLAIN, 13));
        trong.setBorder(new EmptyBorder(28, 0, 28, 0));
        trong.setVisible(false);
        bang.setLayout(new BorderLayout());
        bang.add(trong, BorderLayout.NORTH);

        The khung = new The(new BorderLayout(), 4, 4, 4, 4);
        khung.add(cuon, BorderLayout.CENTER);
        add(khung, BorderLayout.CENTER);
    }

    public JTable bang() {
        return bang;
    }

    public void datCot(List<Cot<T>> ds) {
        cot = new ArrayList<>(ds);
        while (bang.getColumnCount() > 0) bang.removeColumn(bang.getColumnModel().getColumn(0));
        for (int i = 0; i < cot.size(); i++) {
            Cot<T> c = cot.get(i);
            TableColumn tc = new TableColumn(i);
            tc.setIdentifier(c.id());
            tc.setHeaderValue(c.tieuDe());
            tc.setCellRenderer(c.o() != null ? c.o() : O.chu());
            // Cột tên giãn/co theo cửa sổ, các cột còn lại giữ đủ rộng để không bị cắt chữ
            tc.setPreferredWidth(c.rong());
            tc.setMinWidth(c.id().equals("ten") ? Math.min(c.rong(), 140) : c.rong());
            bang.addColumn(tc);
        }
        mau.fireTableStructureChanged();
    }

    public void datDong(List<T> ds, String chuKhiTrong) {
        dong = new ArrayList<>(ds);
        trong.setText(chuKhiTrong);
        trong.setVisible(dong.isEmpty());
        mau.fireTableDataChanged();
        bang.clearSelection();
    }

    public T dongTai(int i) {
        return i >= 0 && i < dong.size() ? dong.get(i) : null;
    }
}
