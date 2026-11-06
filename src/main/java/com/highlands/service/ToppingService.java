package com.highlands.service;

import com.highlands.entity.Topping;
import com.highlands.repository.ToppingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToppingService {

    private final ToppingRepository repo;

    public ToppingService(ToppingRepository repo) {
        this.repo = repo;
    }

    public List<Topping> layTatCa() {
        return repo.findAll(Sort.by("id"));
    }

    public Topping layTheoId(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new LoiNghiepVu("Không tìm thấy topping id = " + id));
    }

    public Topping them(Topping t) {
        kiemTra(t, 0L);
        t.setId(null);
        return repo.save(t);
    }

    public Topping sua(Long id, Topping t) {
        layTheoId(id);
        kiemTra(t, id);
        t.setId(id);
        return repo.save(t);
    }

    public void xoa(Long id) {
        layTheoId(id);
        repo.deleteById(id);
    }

    // idHienTai = id đang sửa (0 khi thêm mới), dùng để kiểm tra trùng tên
    private void kiemTra(Topping t, Long idHienTai) {
        if (t.getTen() == null || t.getTen().isBlank()) {
            throw new LoiNghiepVu("Tên topping không được để trống");
        }
        if (t.getGia() < 0) {
            throw new LoiNghiepVu("Giá topping không được âm");
        }
        t.setTen(t.getTen().trim());
        if (repo.existsByTenIgnoreCaseAndIdNot(t.getTen(), idHienTai)) {
            throw new LoiNghiepVu("Topping \"" + t.getTen() + "\" đã tồn tại");
        }
    }
}
