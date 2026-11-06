package com.highlands.repository;

import com.highlands.entity.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SanPhamRepository extends JpaRepository<SanPham, Long> {

    List<SanPham> findByTenContainingIgnoreCase(String tuKhoa);

    List<SanPham> findByDanhMuc_Id(Long danhMucId);

    List<SanPham> findByTenContainingIgnoreCaseAndDanhMuc_Id(String tuKhoa, Long danhMucId);

    boolean existsByDanhMuc_Id(Long danhMucId);

    // true nếu trong cùng danh mục đã có sản phẩm khác (id khác) cùng tên
    boolean existsByTenIgnoreCaseAndDanhMuc_IdAndIdNot(String ten, Long danhMucId, Long id);
}
