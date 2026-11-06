package com.highlands.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Tải ảnh theo đường dẫn lưu trong DB (VD: /images/phin-sua-da.jpg hoặc https://...)
// Tìm lần lượt: link http(s) -> file đầy đủ -> thư mục đang chạy app -> trong file jar (static/...)
// Tải ở luồng nền, có bộ nhớ đệm; tải xong thì vẽ lại bảng.
public final class Anh {

    private static final Map<String, Optional<BufferedImage>> BO_NHO = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> DANG_TAI = new ConcurrentHashMap<>();
    private static final ExecutorService LUONG = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "tai-anh");
        t.setDaemon(true);
        return t;
    });

    private Anh() {
    }

    // Trả về ảnh nếu đã có; chưa có thì bắt đầu tải và trả null (vẽ lại "ve" khi tải xong)
    public static BufferedImage lay(String url, Component ve) {
        if (url == null || url.isBlank()) return null;
        String u = url.trim();
        Optional<BufferedImage> co = BO_NHO.get(u);
        if (co != null) return co.orElse(null);
        if (DANG_TAI.putIfAbsent(u, Boolean.TRUE) == null) {
            LUONG.submit(() -> {
                BufferedImage img = null;
                try {
                    img = tai(u);
                } catch (Exception ignored) {
                }
                BO_NHO.put(u, Optional.ofNullable(img));
                DANG_TAI.remove(u);
                if (ve != null) SwingUtilities.invokeLater(ve::repaint);
            });
        }
        return null;
    }

    // Ảnh đã tải xong mà không có (sai đường dẫn)?
    public static boolean khongCo(String url) {
        if (url == null || url.isBlank()) return true;
        Optional<BufferedImage> co = BO_NHO.get(url.trim());
        return co != null && co.isEmpty();
    }

    // Tải ảnh ngay trong luồng đang chạy (gọi từ luồng nền, không gọi trên luồng giao diện).
    // Trả về null nếu không tìm thấy / lỗi. Có dùng chung bộ nhớ đệm với "lay".
    public static BufferedImage taiDongBo(String url) {
        if (url == null || url.isBlank()) return null;
        String u = url.trim();
        Optional<BufferedImage> co = BO_NHO.get(u);
        if (co != null) return co.orElse(null);
        BufferedImage img = null;
        try {
            img = tai(u);
        } catch (Exception ignored) {
        }
        BO_NHO.put(u, Optional.ofNullable(img));
        return img;
    }

    private static BufferedImage tai(String u) throws Exception {
        if (u.startsWith("http://") || u.startsWith("https://")) {
            URLConnection c = URI.create(u).toURL().openConnection();
            c.setConnectTimeout(3000);
            c.setReadTimeout(5000);
            try (InputStream in = c.getInputStream()) {
                return ImageIO.read(in);
            }
        }
        File f = new File(u);
        if (f.isAbsolute() && f.isFile()) return ImageIO.read(f);

        String tuongDoi = u.replace('\\', '/').replaceFirst("^/+", "");
        for (Path p : new Path[]{Path.of(tuongDoi), Path.of("static", tuongDoi),
                Path.of("src", "main", "resources", "static", tuongDoi)}) {
            if (p.toFile().isFile()) return ImageIO.read(p.toFile());
        }
        for (String r : new String[]{"/static/" + tuongDoi, "/" + tuongDoi}) {
            try (InputStream in = Anh.class.getResourceAsStream(r)) {
                if (in != null) return ImageIO.read(in);
            }
        }
        return null;
    }

    // Vẽ khung ảnh vuông bo góc: có ảnh thì cắt vừa khung, không có thì chữ "Chưa có ảnh"
    public static void veKhung(Graphics2D g, Rectangle o, String url, Component ve) {
        Theme.khuDangCua(g);
        Shape cu = g.getClip();
        java.awt.geom.RoundRectangle2D khung = new java.awt.geom.RoundRectangle2D.Float(o.x, o.y, o.width, o.height, 10, 10);
        g.setColor(Theme.HOVER);
        g.fill(khung);
        BufferedImage img = lay(url, ve);
        if (img != null) {
            g.clip(khung);
            double tl = Math.max(o.width / (double) img.getWidth(), o.height / (double) img.getHeight());
            int w = (int) Math.round(img.getWidth() * tl), h = (int) Math.round(img.getHeight() * tl);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, o.x + (o.width - w) / 2, o.y + (o.height - h) / 2, w, h, null);
            g.setClip(cu);
        } else {
            g.setColor(new Color(0x9C, 0xA3, 0xAF));
            g.setFont(Theme.font(Font.PLAIN, 9));
            FontMetrics fm = g.getFontMetrics();
            String[] dong = khongCo(url) ? new String[]{"Chưa có", "ảnh"} : new String[]{"..."};
            int y = o.y + (o.height - dong.length * fm.getHeight()) / 2 + fm.getAscent();
            for (String d : dong) {
                g.drawString(d, o.x + (o.width - fm.stringWidth(d)) / 2, y);
                y += fm.getHeight();
            }
        }
    }

    // Ô xem trước ảnh trong hộp thoại thêm/sửa
    public static class XemTruoc extends JComponent {
        private String url;

        public XemTruoc(int co) {
            setPreferredSize(new Dimension(co, co));
            setMinimumSize(new Dimension(co, co));
        }

        public void setUrl(String u) {
            url = u;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            int c = Math.min(getWidth(), getHeight());
            veKhung(g, new Rectangle(0, 0, c, c), url, this);
            g.dispose();
        }
    }
}
