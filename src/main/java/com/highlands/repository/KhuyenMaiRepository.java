package com.highlands.repository;

import com.highlands.entity.KhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Long> {

    // Tìm theo tên, không phân biệt hoa thường
    List<KhuyenMai> findByTenContainingIgnoreCaseOrderByIdAsc(String tuKhoa);

    // true nếu đã có khuyến mãi khác (id khác) dùng mã này
    boolean existsByMaIgnoreCaseAndIdNot(String ma, Long id);
}
