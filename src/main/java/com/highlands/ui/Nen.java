package com.highlands.ui;

import com.highlands.service.LoiNghiepVu;
import org.springframework.dao.DataIntegrityViolationException;

import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

// Chạy truy vấn DB ở luồng nền để cửa sổ không bị treo, xong thì quay về luồng giao diện
public final class Nen {

    private Nen() {
    }

    public static <T> void chay(Callable<T> viec, Consumer<T> xong, Consumer<String> loi) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return viec.call();
            }

            @Override
            protected void done() {
                T kq;
                try {
                    kq = get();
                } catch (ExecutionException e) {
                    loi.accept(thongDiep(e.getCause()));
                    return;
                } catch (Exception e) {
                    loi.accept(thongDiep(e));
                    return;
                }
                xong.accept(kq);
            }
        }.execute();
    }

    // Câu thông báo dễ hiểu cho người dùng
    public static String thongDiep(Throwable e) {
        if (e instanceof LoiNghiepVu) return e.getMessage();
        if (e instanceof DataIntegrityViolationException) {
            return "Dữ liệu bị trùng hoặc đang được sử dụng ở nơi khác, không lưu/xóa được";
        }
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        String m = t.getMessage();
        return "Lỗi: " + (m == null || m.isBlank() ? t.getClass().getSimpleName() : m);
    }
}
