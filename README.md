# highlands-app (App desktop Java Swing + SQL Server)

Bản app của highlands-be: cùng database `HighlandsDB`, cùng Entity / Repository / Service,
nhưng giao diện là cửa sổ Swing thay cho trang web. Không còn web server, không cần trình duyệt.

## Chạy
1. Cài JDK 17+. Mở thư mục này bằng VS Code (Extension Pack for Java + Spring Boot Extension Pack) hoặc IntelliJ.
2. Sửa mật khẩu SQL Server trong `src/main/resources/application.properties`.
3. Chạy `HighlandsApplication.java` (nút Run), hoặc `mvn spring-boot:run`.
   - Hiện màn hình "Đang kết nối SQL Server...", vài giây sau mở cửa sổ chính.
   - SQL Server chưa chạy / sai mật khẩu: hiện hộp thoại báo lỗi sau khoảng 5 giây.
4. Đóng gói 1 file chạy được: `mvn package` -> `target/highlands-app-0.0.1.jar`,
   chạy bằng `java -jar target/highlands-app-0.0.1.jar` (hoặc nhấp đúp trên Windows).

## Giao diện (giống bản web)
- Thanh đỏ trên cùng + menu trái (Thống kê / Sản phẩm & Menu / Khuyến mãi).
- **Khuyến mãi**: tìm theo tên, bảng có ảnh, mã, mức giảm, đơn tối thiểu, ngày, trạng thái (Đang áp dụng / Sắp diễn ra / Đã kết thúc).
- **Sản phẩm & Menu**: 3 tab Sản phẩm / Danh mục / Topping, tìm + lọc theo danh mục, 10 dòng mỗi trang, nhãn "Hot" (top 5 bán chạy 30 ngày).
- **Thống kê**: chọn khoảng ngày, tổng doanh thu / chi phí / lợi nhuận, bảng theo ngày, top 5 món bán chạy.
- Thêm / sửa trong hộp thoại; lưu bị lỗi thì hộp thoại vẫn mở, giữ dữ liệu đã nhập.
- Bấm "Sửa" / "Xóa" trong cột Thao tác, hoặc nhấp đúp một dòng để sửa.
- Ngày nhập dạng `30/11/2026` (hoặc `2026-11-30`); số tiền nhập `25000` hoặc `25.000`.

## Cấu trúc
```
src/main/java/com/highlands/
  HighlandsApplication.java   khởi động Spring (không web) rồi mở cửa sổ
  entity/ repository/          giữ nguyên như bản web
  service/                     giữ nguyên quy tắc kiểm tra; báo lỗi bằng LoiNghiepVu
  ui/                          giao diện Swing
    FormChinh                  cửa sổ chính (thanh đỏ, menu, đổi trang)
    TrangKhuyenMai, TrangSanPham, TrangThongKe
    HopThoaiKhuyenMai, HopThoaiMuc (sản phẩm/danh mục/topping), HopThoai (khung chung)
    Bang, O                    bảng dữ liệu và các kiểu ô (ảnh, badge, Sửa/Xóa, thanh tỉ lệ)
    Nen                        chạy truy vấn DB ở luồng nền để cửa sổ không bị treo
    Theme, Nut, The, ONhap, ThongBao, Anh, ManHinhCho
```

## Thay đổi so với bản web (v3)
- Bỏ `spring-boot-starter-web`, các Controller và trang HTML; `spring.main.web-application-type=none`.
- Service ném `LoiNghiepVu` thay cho `ResponseStatusException` (lớp của web).
- Kiểm tra trùng mã khuyến mãi dùng `existsByMaIgnoreCaseAndIdNot` (DB tự đếm, không tải cả bảng).
- Tìm kiếm khuyến mãi trả về theo thứ tự id; gõ nhanh thì chỉ hiện kết quả của lần tìm mới nhất.
- Kết nối SQL Server có `loginTimeout=5`, `hikari.connection-timeout=5000`.

## Ảnh
DB lưu dạng `/images/xxx.jpg`. App tìm lần lượt: link `http(s)://...` -> đường dẫn đầy đủ ->
thư mục `images/` cạnh nơi chạy app -> `src/main/resources/static/images/` -> trong file jar.
Không tìm thấy thì hiện "Chưa có ảnh".

## Lưu ý DB cũ
Nếu bảng `khuyen_mai` trước đây do Hibernate tự tạo (cột `ten` là VARCHAR, mất dấu tiếng Việt), chạy một lần trong SSMS:
```sql
ALTER TABLE khuyen_mai ALTER COLUMN ten NVARCHAR(100) NOT NULL;
```
