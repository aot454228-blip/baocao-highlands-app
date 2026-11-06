package com.highlands.service;

import com.highlands.entity.KhuyenMai;
import com.highlands.repository.KhuyenMaiRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KhuyenMaiService {

    private static final String PHAN_TRAM = "PHAN_TRAM"; // giảm theo %
    private static final String GIAM_TIEN = "GIAM_TIEN"; // giảm số tiền cố định

    private final KhuyenMaiRepository repo;

    public KhuyenMaiService(KhuyenMaiRepository repo) {
        this.repo = repo;
    }

    public List<KhuyenMai> layTatCa() {
        return repo.findAll(Sort.by("id"));
    }

    public List<KhuyenMai> timKiem(String tuKhoa) {
        return repo.findByTenContainingIgnoreCaseOrderByIdAsc(tuKhoa.trim());
    }

    public KhuyenMai layTheoId(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new LoiNghiepVu("Không tìm thấy khuyến mãi id = " + id));
    }

    public KhuyenMai them(KhuyenMai k) {
        kiemTraHopLe(k, 0L);
        k.setId(null); // để CSDL tự sinh id
        return repo.save(k);
    }

    public KhuyenMai sua(Long id, KhuyenMai k) {
        layTheoId(id); // báo lỗi nếu không tồn tại
        kiemTraHopLe(k, id);
        k.setId(id);
        return repo.save(k);
    }

    public void xoa(Long id) {
        layTheoId(id);
        repo.deleteById(id);
    }

    // Các điều kiện (hình thoi) trong biểu đồ hoạt động nằm ở đây
    // idHienTai = id đang sửa (0 khi thêm mới), dùng để kiểm tra trùng mã
    private void kiemTraHopLe(KhuyenMai k, Long idHienTai) {
        if (k.getTen() == null || k.getTen().isBlank()) {
            throw new LoiNghiepVu("Tên khuyến mãi không được để trống");
        }

        // Loại giảm: để trống thì coi như giảm theo %
        String loai = (k.getLoai() == null || k.getLoai().isBlank()) ? PHAN_TRAM : k.getLoai().trim().toUpperCase();
        if (!loai.equals(PHAN_TRAM) && !loai.equals(GIAM_TIEN)) {
            throw new LoiNghiepVu("Loại giảm không hợp lệ");
        }
        k.setLoai(loai);

        if (loai.equals(PHAN_TRAM)) {
            if (k.getPhanTramGiam() <= 0 || k.getPhanTramGiam() > 100) {
                throw new LoiNghiepVu("Phần trăm giảm phải trong khoảng (0, 100]");
            }
            k.setSoTienGiam(null);
        } else {
            if (k.getSoTienGiam() == null || k.getSoTienGiam() <= 0) {
                throw new LoiNghiepVu("Số tiền giảm phải lớn hơn 0");
            }
            k.setPhanTramGiam(0);
        }

        if (k.getDonToiThieu() != null && k.getDonToiThieu() < 0) {
            throw new LoiNghiepVu("Đơn tối thiểu không được âm");
        }

        // Mã: có thể để trống; nếu có thì chỉ gồm chữ không dấu, số, - hoặc _ và không được trùng
        String ma = k.getMa() == null ? "" : k.getMa().trim();
        if (ma.isEmpty()) {
            k.setMa(null);
        } else {
            if (ma.length() > 50 || !ma.matches("[A-Za-z0-9_-]+")) {
                throw new LoiNghiepVu(
                        "Mã chỉ gồm chữ không dấu, số, dấu - hoặc _ (tối đa 50 ký tự)");
            }
            ma = ma.toUpperCase();
            if (repo.existsByMaIgnoreCaseAndIdNot(ma, idHienTai)) {
                throw new LoiNghiepVu("Mã \"" + ma + "\" đã tồn tại");
            }
            k.setMa(ma);
        }

        if (k.getNgayBatDau() != null && k.getNgayKetThuc() != null
                && k.getNgayKetThuc().isBefore(k.getNgayBatDau())) {
            throw new LoiNghiepVu("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }
}
