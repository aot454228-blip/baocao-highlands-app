package com.highlands.ui;

import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

// Chạy việc nặng (truy vấn DB, băm mật khẩu...) ở luồng nền để cửa sổ không bị treo,
// xong thì gọi lại trên luồng giao diện. Lỗi được bóc ra thành nguyên nhân gốc để hiện thông báo dễ hiểu.
public final class ChayNen {

    private ChayNen() {
    }

    public static <T> void chay(Callable<T> viec, Consumer<T> xong, Consumer<Throwable> loi) {
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
                    loi.accept(e.getCause() != null ? e.getCause() : e);
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    loi.accept(e);
                    return;
                }
                xong.accept(kq);
            }
        }.execute();
    }

    // Câu thông báo cho người dùng (lỗi nghiệp vụ hiện nguyên văn, lỗi hệ thống hiện nguyên nhân gốc)
    public static String thongDiep(Throwable e) {
        return Nen.thongDiep(e);
    }
}
