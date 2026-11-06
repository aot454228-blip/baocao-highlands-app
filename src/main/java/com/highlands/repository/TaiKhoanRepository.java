package com.highlands.repository;

import com.highlands.entity.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Long> {

    Optional<TaiKhoan> findByTenDangNhapIgnoreCase(String tenDangNhap);
}
