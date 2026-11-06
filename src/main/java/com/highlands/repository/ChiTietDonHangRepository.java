package com.highlands.repository;

import com.highlands.entity.ChiTietDonHang;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Long> {

    // Mỗi dòng: [id sản phẩm, tên, tổng số lượng bán, tổng tiền], bán nhiều nhất xếp trước
    @Query("select s.id, s.ten, sum(c.soLuong), sum(c.soLuong * c.donGia) "
            + "from ChiTietDonHang c join c.sanPham s join c.donHang d "
            + "where d.ngayDat between :tu and :den"
            + " and not exists (select 1 from ThanhToanDonHang t where t.donHang = d and t.trangThai = 'Đã hủy') "
            + "group by s.id, s.ten order by sum(c.soLuong) desc")
    List<Object[]> banChay(@Param("tu") LocalDate tu, @Param("den") LocalDate den, Pageable pageable);

    // Phân hệ nhân viên: các dòng của một đơn (để dựng hóa đơn)
    List<ChiTietDonHang> findByDonHang_IdOrderByIdAsc(Long donHangId);
}
