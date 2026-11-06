package com.highlands.service;

import com.highlands.entity.DanhMuc;
import com.highlands.entity.SanPham;
import com.highlands.repository.DanhMucRepository;
import com.highlands.repository.SanPhamRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SanPhamService {

    private final SanPhamRepository repo;
    private final DanhMucRepository danhMucRepo;

    public SanPhamService(SanPhamRepository repo, DanhMucRepository danhMucRepo) {
        this.repo = repo;
        this.danhMucRepo = danhMucRepo;
    }

    // Xem và tìm kiếm: theo tên, theo danh mục, hoặc cả hai
    public List<SanPham> layDanhSach(String tuKhoa, Long danhMucId) {
        boolean coTuKhoa = tuKhoa != null && !tuKhoa.isBlank();
        if (coTuKhoa && danhMucId != null) {
            return repo.findByTenContainingIgnoreCaseAndDanhMuc_Id(tuKhoa.trim(), danhMucId);
        }
        if (coTuKhoa) {
            return repo.findByTenContainingIgnoreCase(tuKhoa.trim());
        }
        if (danhMucId != null) {
            return repo.findByDanhMuc_Id(danhMucId);
        }
        return repo.findAll(Sort.by("id"));
    }

    public SanPham layTheoId(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new LoiNghiepVu("Không tìm thấy sản phẩm id = " + id));
    }

    public SanPham them(SanPham s) {
        kiemTra(s, 0L);
        s.setId(null);
        return repo.save(s);
    }

    public SanPham sua(Long id, SanPham s) {
        layTheoId(id);
        kiemTra(s, id);
        s.setId(id);
        return repo.save(s);
    }

    public void xoa(Long id) {
        layTheoId(id);
        try {
            repo.deleteById(id);
            repo.flush();
        } catch (DataIntegrityViolationException e) {
            throw new LoiNghiepVu("Sản phẩm đã có trong đơn hàng, không thể xóa");
        }
    }

    // idHienTai = id đang sửa (0 khi thêm mới), dùng để kiểm tra trùng tên trong cùng danh mục
    private void kiemTra(SanPham s, Long idHienTai) {
        if (s.getTen() == null || s.getTen().isBlank()) {
            throw new LoiNghiepVu("Tên sản phẩm không được để trống");
        }
        if (s.getGia() <= 0) {
            throw new LoiNghiepVu("Giá sản phẩm phải lớn hơn 0");
        }
        if (s.getDanhMuc() == null || s.getDanhMuc().getId() == null) {
            throw new LoiNghiepVu("Vui lòng chọn danh mục");
        }
        DanhMuc d = danhMucRepo.findById(s.getDanhMuc().getId()).orElseThrow(() ->
                new LoiNghiepVu("Danh mục không tồn tại"));
        s.setDanhMuc(d);
        s.setTen(s.getTen().trim());
        if (repo.existsByTenIgnoreCaseAndDanhMuc_IdAndIdNot(s.getTen(), d.getId(), idHienTai)) {
            throw new LoiNghiepVu(
                    "Sản phẩm \"" + s.getTen() + "\" đã có trong danh mục này");
        }
    }
}
