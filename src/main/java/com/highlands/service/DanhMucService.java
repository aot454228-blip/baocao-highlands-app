package com.highlands.service;

import com.highlands.entity.DanhMuc;
import com.highlands.repository.DanhMucRepository;
import com.highlands.repository.SanPhamRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DanhMucService {

    private final DanhMucRepository repo;
    private final SanPhamRepository sanPhamRepo;

    public DanhMucService(DanhMucRepository repo, SanPhamRepository sanPhamRepo) {
        this.repo = repo;
        this.sanPhamRepo = sanPhamRepo;
    }

    public List<DanhMuc> layTatCa() {
        return repo.findAll(Sort.by("id"));
    }

    public DanhMuc layTheoId(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new LoiNghiepVu("Không tìm thấy danh mục id = " + id));
    }

    public DanhMuc them(DanhMuc d) {
        kiemTra(d, 0L);
        d.setId(null);
        return repo.save(d);
    }

    public DanhMuc sua(Long id, DanhMuc d) {
        layTheoId(id);
        kiemTra(d, id);
        d.setId(id);
        return repo.save(d);
    }

    public void xoa(Long id) {
        layTheoId(id);
        if (sanPhamRepo.existsByDanhMuc_Id(id)) {
            throw new LoiNghiepVu(
                    "Danh mục đang có sản phẩm, hãy xóa hoặc chuyển sản phẩm trước");
        }
        repo.deleteById(id);
    }

    // idHienTai = id đang sửa (0 khi thêm mới), dùng để kiểm tra trùng tên
    private void kiemTra(DanhMuc d, Long idHienTai) {
        if (d.getTen() == null || d.getTen().isBlank()) {
            throw new LoiNghiepVu("Tên danh mục không được để trống");
        }
        d.setTen(d.getTen().trim());
        if (repo.existsByTenIgnoreCaseAndIdNot(d.getTen(), idHienTai)) {
            throw new LoiNghiepVu("Danh mục \"" + d.getTen() + "\" đã tồn tại");
        }
    }
}
