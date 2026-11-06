package com.highlands.repository;

import com.highlands.entity.Topping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToppingRepository extends JpaRepository<Topping, Long> {

    // true nếu đã có topping khác (id khác) cùng tên, không phân biệt hoa thường
    boolean existsByTenIgnoreCaseAndIdNot(String ten, Long id);
}
