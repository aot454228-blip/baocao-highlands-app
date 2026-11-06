/* =====================================================================
   MIGRATION: Phân hệ Nhân viên – HighlandsDB (SQL Server)
   - Chỉ THÊM bảng mới, không sửa/xóa bảng cũ, không mất dữ liệu cũ.
   - GIẢ ĐỊNH: các bảng cũ tên don_hang, chi_tiet_don_hang, topping, khuyen_mai
     và cột id kiểu BIGINT. Nếu id của bạn là INT thì đổi BIGINT -> INT
     ở các cột *_id bên dưới cho khớp trước khi chạy.
   ===================================================================== */
USE HighlandsDB;
GO

IF OBJECT_ID('dbo.nhan_vien') IS NULL
CREATE TABLE dbo.nhan_vien (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_nhan_vien  NVARCHAR(20)  NOT NULL UNIQUE,
    ho_ten        NVARCHAR(100) NOT NULL,
    dang_lam      BIT           NOT NULL DEFAULT 1
);
GO

IF OBJECT_ID('dbo.tai_khoan') IS NULL
CREATE TABLE dbo.tai_khoan (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    ten_dang_nhap  NVARCHAR(50)  NOT NULL UNIQUE,
    mat_khau_hash  NVARCHAR(255) NOT NULL,          -- PBKDF2-HMAC-SHA256, KHÔNG lưu mật khẩu thuần
    nhan_vien_id   BIGINT        NOT NULL REFERENCES dbo.nhan_vien(id),
    hoat_dong      BIT           NOT NULL DEFAULT 1,
    ngay_tao       DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO

/* Thông tin bán hàng của 1 đơn (bảng mở rộng 1-1 với don_hang, không đụng vào don_hang) */
IF OBJECT_ID('dbo.thanh_toan_don_hang') IS NULL
CREATE TABLE dbo.thanh_toan_don_hang (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    don_hang_id      BIGINT        NOT NULL UNIQUE REFERENCES dbo.don_hang(id),
    nhan_vien_id     BIGINT        NOT NULL REFERENCES dbo.nhan_vien(id),
    khuyen_mai_id    BIGINT        NULL     REFERENCES dbo.khuyen_mai(id),
    ma_khuyen_mai    NVARCHAR(50)  NULL,
    tien_hang        DECIMAL(18,2) NOT NULL,
    giam_gia         DECIMAL(18,2) NOT NULL DEFAULT 0,
    tong_thanh_toan  DECIMAL(18,2) NOT NULL,
    phuong_thuc      NVARCHAR(30)  NOT NULL,
    tien_khach_dua   DECIMAL(18,2) NOT NULL,
    tien_thua        DECIMAL(18,2) NOT NULL,
    ngay_thanh_toan  DATETIME2     NOT NULL,
    ma_giao_dich     NVARCHAR(64)  NOT NULL UNIQUE   -- khóa chống tạo trùng đơn khi bấm nhiều lần
);
GO

IF OBJECT_ID('dbo.chi_tiet_don_hang_topping') IS NULL
CREATE TABLE dbo.chi_tiet_don_hang_topping (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    chi_tiet_don_hang_id  BIGINT        NOT NULL REFERENCES dbo.chi_tiet_don_hang(id),
    topping_id            BIGINT        NULL     REFERENCES dbo.topping(id),
    ten_topping           NVARCHAR(100) NOT NULL,   -- lưu bản chụp tên/giá tại thời điểm bán
    gia_topping           DECIMAL(18,2) NOT NULL
);
GO

/* ---------------------------------------------------------------------
   TẠO NHÂN VIÊN + TÀI KHOẢN ĐẦU TIÊN (tự thực hiện, KHÔNG có tài khoản mặc định)
   1) Sinh hash:  chạy class com.highlands.service.MatKhau (main) -> nhập mật khẩu -> copy chuỗi hash
   2) Chạy (thay giá trị):

   INSERT INTO dbo.nhan_vien (ma_nhan_vien, ho_ten) VALUES (N'NV001', N'Nguyễn Văn A');
   INSERT INTO dbo.tai_khoan (ten_dang_nhap, mat_khau_hash, nhan_vien_id)
   VALUES (N'nv001', N'<CHUỖI_HASH>', SCOPE_IDENTITY());
   --------------------------------------------------------------------- */
