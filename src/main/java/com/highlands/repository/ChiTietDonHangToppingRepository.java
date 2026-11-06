package com.highlands.repository;

import com.highlands.entity.ChiTietDonHangTopping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ChiTietDonHangToppingRepository extends JpaRepository<ChiTietDonHangTopping, Long> {

    List<ChiTietDonHangTopping> findByChiTietDonHang_IdIn(Collection<Long> ids);
}
