-- TÙY CHỌN: chỉ để demo trang Thống kê. Chạy tay trong SSMS, chỉ chạy MỘT lần.
-- Điều kiện: đã chạy Java ít nhất một lần (để các bảng được tạo)
-- và đã thêm ít nhất 1 sản phẩm trên trang Sản phẩm.
USE HighlandsDB;
GO
DECLARE @i INT = 0;
DECLARE @dh BIGINT;
WHILE @i < 10
BEGIN
    INSERT INTO don_hang (ngay_dat, tong_tien) VALUES (CAST(DATEADD(DAY, -@i, GETDATE()) AS DATE), 0);
    SET @dh = SCOPE_IDENTITY();

    INSERT INTO chi_tiet_don_hang (don_hang_id, san_pham_id, so_luong, don_gia)
    SELECT TOP 3 @dh, id, (@i % 3) + 1, gia FROM san_pham ORDER BY id;

    UPDATE don_hang
    SET tong_tien = (SELECT SUM(so_luong * don_gia) FROM chi_tiet_don_hang WHERE don_hang_id = @dh)
    WHERE id = @dh;

    IF @i % 2 = 0
        INSERT INTO phieu_nhap (ngay_nhap, tong_tien) VALUES (CAST(DATEADD(DAY, -@i, GETDATE()) AS DATE), 50000 + @i * 10000);

    SET @i = @i + 1;
END
GO
