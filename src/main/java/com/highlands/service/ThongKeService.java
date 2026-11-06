package com.highlands.service;

import com.highlands.repository.ChiTietDonHangRepository;
import com.highlands.repository.DonHangRepository;
import com.highlands.repository.PhieuNhapRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ThongKeService {

    private final DonHangRepository donHangRepo;
    private final PhieuNhapRepository phieuNhapRepo;
    private final ChiTietDonHangRepository chiTietRepo;

    public ThongKeService(DonHangRepository donHangRepo, PhieuNhapRepository phieuNhapRepo,
                          ChiTietDonHangRepository chiTietRepo) {
        this.donHangRepo = donHangRepo;
        this.phieuNhapRepo = phieuNhapRepo;
        this.chiTietRepo = chiTietRepo;
    }

    // Không truyền ngày thì mặc định 30 ngày gần nhất
    private LocalDate[] khoang(LocalDate tu, LocalDate den) {
        LocalDate d = den != null ? den : LocalDate.now();
        LocalDate t = tu != null ? tu : d.minusDays(30);
        if (t.isAfter(d)) {
            throw new LoiNghiepVu("Từ ngày không được sau đến ngày");
        }
        return new LocalDate[]{t, d};
    }

    public Map<String, Object> doanhThu(LocalDate tu, LocalDate den) {
        LocalDate[] k = khoang(tu, den);
        return gop(donHangRepo.doanhThuTheoNgay(k[0], k[1]));
    }

    public Map<String, Object> chiPhi(LocalDate tu, LocalDate den) {
        LocalDate[] k = khoang(tu, den);
        return gop(phieuNhapRepo.chiPhiTheoNgay(k[0], k[1]));
    }

    public List<Map<String, Object>> banChay(LocalDate tu, LocalDate den, int top) {
        LocalDate[] k = khoang(tu, den);
        int n = Math.max(1, Math.min(top, 50));
        List<Map<String, Object>> kq = new ArrayList<>();
        for (Object[] r : chiTietRepo.banChay(k[0], k[1], PageRequest.of(0, n))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r[0]);
            m.put("ten", r[1]);
            m.put("soLuong", ((Number) r[2]).longValue());
            m.put("doanhThu", ((Number) r[3]).longValue());
            kq.add(m);
        }
        return kq;
    }

    // Chuyển các dòng [ngày, tiền] thành {tong, theoNgay:[{ngay, tien}]}
    private Map<String, Object> gop(List<Object[]> dong) {
        long tong = 0;
        List<Map<String, Object>> theoNgay = new ArrayList<>();
        for (Object[] r : dong) {
            long tien = ((Number) r[1]).longValue();
            tong += tien;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ngay", r[0].toString());
            m.put("tien", tien);
            theoNgay.add(m);
        }
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("tong", tong);
        kq.put("theoNgay", theoNgay);
        return kq;
    }
}
