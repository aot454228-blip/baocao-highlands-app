package com.highlands.repository;

import com.highlands.entity.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DanhMucRepository extends JpaRepository<DanhMuc, Long> {

    // true nếu đã có danh mục khác (id khác) cùng tên, không phân biệt hoa thường
    boolean existsByTenIgnoreCaseAndIdNot(String ten, Long id);
}
