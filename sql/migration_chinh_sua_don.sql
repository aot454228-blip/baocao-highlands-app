/* =====================================================================
   MIGRATION: Chỉnh sửa đơn hàng (trạng thái đơn + ghi chú giao dịch) - HighlandsDB
   - Chỉ THÊM 2 cột vào thanh_toan_don_hang, không mất dữ liệu cũ.
   - Đơn cũ mặc định trạng thái 'Hoàn thành'.
   - Chạy lại nhiều lần vẫn an toàn (app cũng tự thêm cột nếu chưa có).
   ===================================================================== */
USE HighlandsDB;
GO

IF COL_LENGTH('dbo.thanh_toan_don_hang', 'trang_thai') IS NULL
    ALTER TABLE dbo.thanh_toan_don_hang
        ADD trang_thai NVARCHAR(30) NOT NULL CONSTRAINT DF_ttdh_trang_thai DEFAULT N'Hoàn thành';
GO

IF COL_LENGTH('dbo.thanh_toan_don_hang', 'ghi_chu') IS NULL
    ALTER TABLE dbo.thanh_toan_don_hang ADD ghi_chu NVARCHAR(200) NULL;
GO

SELECT trang_thai, COUNT(*) AS so_don FROM dbo.thanh_toan_don_hang GROUP BY trang_thai;
GO
