package com.highlands.repository;

import com.highlands.entity.PhieuNhap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PhieuNhapRepository extends JpaRepository<PhieuNhap, Long> {

    // Mỗi dòng: [ngày, tổng chi phí nhập của ngày đó]
    @Query("select p.ngayNhap, sum(p.tongTien) from PhieuNhap p "
            + "where p.ngayNhap between :tu and :den group by p.ngayNhap order by p.ngayNhap")
    List<Object[]> chiPhiTheoNgay(@Param("tu") LocalDate tu, @Param("den") LocalDate den);
}
