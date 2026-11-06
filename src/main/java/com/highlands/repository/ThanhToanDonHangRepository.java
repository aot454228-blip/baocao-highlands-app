package com.highlands.repository;

import com.highlands.entity.ThanhToanDonHang;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ThanhToanDonHangRepository extends JpaRepository<ThanhToanDonHang, Long> {

    Optional<ThanhToanDonHang> findByMaGiaoDich(String maGiaoDich);

    // Lịch sử theo khoảng thời gian [tu, den), mới nhất trước
    @Query("select t from ThanhToanDonHang t join fetch t.nhanVien join fetch t.donHang "
            + "where t.ngayThanhToan >= :tu and t.ngayThanhToan < :den "
            + "order by t.ngayThanhToan desc, t.id desc")
    List<ThanhToanDonHang> timTheoKhoang(@Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den, Pageable p);

    @Query("select t from ThanhToanDonHang t join fetch t.nhanVien join fetch t.donHang where t.donHang.id = :id")
    List<ThanhToanDonHang> timTheoDonHang(@Param("id") Long donHangId);
}
