package com.highlands.repository;

import com.highlands.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DonHangRepository extends JpaRepository<DonHang, Long> {

    // Mỗi dòng: [ngày, tổng doanh thu của ngày đó]
    @Query("select d.ngayDat, sum(d.tongTien) from DonHang d "
            + "where d.ngayDat between :tu and :den"
            + " and not exists (select 1 from ThanhToanDonHang t where t.donHang = d and t.trangThai = 'Đã hủy') "
            + "group by d.ngayDat order by d.ngayDat")
    List<Object[]> doanhThuTheoNgay(@Param("tu") LocalDate tu, @Param("den") LocalDate den);
}
