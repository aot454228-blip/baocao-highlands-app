package com.highlands;

import com.highlands.ui.DangNhapNhanVien;
import com.highlands.ui.ManHinhCho;
import com.highlands.ui.Theme;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

@SpringBootApplication
public class HighlandsApplication {
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        Theme.caiDat();
        ManHinhCho cho = ManHinhCho.hien("Đang kết nối cơ sở dữ liệu...");
        try {
            context = new SpringApplicationBuilder(HighlandsApplication.class)
                    .headless(false)
                    .web(WebApplicationType.NONE)
                    .run(args);
            SwingUtilities.invokeLater(() -> {
                cho.dong();
                moDangNhap();
            });
        } catch (Throwable e) {
            cho.dong();
            JOptionPane.showMessageDialog(null,
                    "Không kết nối được cơ sở dữ liệu. Kiểm tra SQL Server và application.properties.\\n\\n"
                            + nguyenNhan(e),
                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static void moDangNhap() {
        new DangNhapNhanVien(context).setVisible(true);
    }

    private static String nguyenNhan(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
