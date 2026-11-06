-- Chạy trong SSMS (không bắt buộc nếu để ddl-auto=update)
IF DB_ID('HighlandsDB') IS NULL CREATE DATABASE HighlandsDB;
GO
USE HighlandsDB;
GO
IF OBJECT_ID('khuyen_mai') IS NULL
CREATE TABLE khuyen_mai (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    ten            NVARCHAR(100) NOT NULL,
    phan_tram_giam FLOAT NOT NULL,
    ngay_bat_dau   DATE NULL,
    ngay_ket_thuc  DATE NULL
);
GO
INSERT INTO khuyen_mai (ten, phan_tram_giam, ngay_bat_dau, ngay_ket_thuc) VALUES
(N'Giảm giá khai trương', 20, '2026-11-01', '2026-11-15'),
(N'Mua 2 tặng topping',   10, '2026-11-10', '2026-12-10');
GO
