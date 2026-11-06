# Ôn tập bảo vệ phân hệ Nhân viên Highlands Coffee

Tài liệu đối chiếu mã nguồn hiện tại ngày 06/11/2026, sau ba bước đơn giản hóa đã được xác nhận. Mục tiêu là hiểu luồng dữ liệu và giải thích được quyết định trong code.

## 1. Phạm vi và tình trạng kiểm tra

Trong đợt viết tài liệu này chỉ đọc code và tạo Markdown; không sửa Java, cấu hình, SQL hoặc chạy ứng dụng. Các mô tả dưới đây là kết quả đọc mã nguồn, không phải kết quả chạy thử nghiệp vụ.

| Nội dung | Trạng thái có bằng chứng |
|---|---|
| Biên dịch sau bước 1, 2 và 3 | Đã chạy `mvn clean compile`, cả ba lần cuối đều `BUILD SUCCESS`; mỗi lần biên dịch 61 file Java |
| Đọc lại ba file đã sửa và các lớp liên quan | Đã thực hiện khi viết tài liệu |
| Kiểm thử tự động nghiệp vụ | Chưa chạy; `compile` không chạy kiểm thử |
| Khởi động ứng dụng và kết nối SQL Server | Chưa kiểm thử trong các bước này |
| Đăng nhập, bán hàng, topping, thanh toán, lịch sử | Chưa thao tác kiểm thử trên giao diện |
| Xem/in hóa đơn, nhập/xuất kho | Chưa thao tác kiểm thử trên giao diện hoặc thiết bị in |
| Mất kết nối, rollback, hai người thao tác đồng thời | Chưa kiểm thử thực tế |

Không suy ra “chạy ổn định” chỉ từ `BUILD SUCCESS`. Biên dịch xác nhận mã Java biên dịch được, chưa xác nhận dữ liệu, truy vấn lúc chạy hoặc kết quả giao diện.

## 2. Sơ đồ và cách đọc dự án

Các đường dẫn sau tính từ `src/main/java/com/highlands/`.

```text
HighlandsApplication.main()
  → tạo Spring context, mở DangNhapNhanVien
  → DangNhapService → TaiKhoanRepository → SQL Server
  → FormNhanVien
      ├─ TrangBanHang → BanHangService → Repository → SQL Server
      │    └─ TrangThanhToan → ThanhToanService → Repository → SQL Server
      │         └─ HopThoaiHoaDon (xem/in kết quả)
      ├─ TrangLichSuDonHang → ThanhToanService → Repository → SQL Server
      └─ TrangQuanLyKho → KhoService → Repository → SQL Server
```

Giao diện nhận thao tác và hiển thị kết quả. Service kiểm tra quy tắc và điều phối xử lý. Repository đọc/ghi dữ liệu qua JPA. SQL Server lưu dữ liệu lâu dài. Riêng giỏ hàng đang chọn chỉ nằm trong bộ nhớ Java, chưa được ghi thành đơn.

### Khởi động, chuyển trang và đăng xuất

`HighlandsApplication.java`: `main()` cài giao diện, khởi tạo Spring Boot ở chế độ không có web server (`WebApplicationType.NONE`), rồi gọi `moDangNhap()`. Lỗi khởi động được hiển thị bằng hộp thoại.

`ui/FormNhanVien.java`: constructor lấy các Service qua `ctx.getBean(...)`, tạo ba trang. `chuyen()` dùng `CardLayout` để đổi trang; mở lịch sử hoặc kho thì gọi `lamMoi()`. `dangXuat()` hỏi xác nhận rồi đóng form và mở đăng nhập; giỏ chưa thanh toán bị bỏ. `thoatUngDung()` hỏi xác nhận, đóng Spring context rồi thoát chương trình. Chuyển trang tự nó không ghi database.

**Ba câu hỏi:**

1. **Ứng dụng này là website không?** Không. Đây là ứng dụng desktop Swing; Spring Boot quản lý phần nghiệp vụ và dữ liệu.
2. **Đổi trang có tạo lại mọi cửa sổ không?** Ba trang được tạo trong form; `CardLayout` chọn trang cần hiện.
3. **Đăng xuất có lưu giỏ không?** Không. Form hiện tại bị đóng, giỏ trong bộ nhớ không được giữ lại.

## 3. Đăng nhập

### Mục đích và file

Xác định nhân viên hợp lệ trước khi mở màn hình bán hàng.

| File | Phương thức và nhiệm vụ |
|---|---|
| `ui/DangNhapNhanVien.java` | `dangNhap()` lấy tên, mật khẩu, vai trò và gọi Service qua `ChayNen` |
| `service/DangNhapService.java` | `dangNhap(...)` kiểm tra dữ liệu, mật khẩu, trạng thái và vai trò |
| `service/DangNhapService.java` | `kiemTraKhoa()`, `ghiNhanSai()` quản lý khóa tạm do sai liên tiếp |
| `service/MatKhau.java` | `kiemTra()` đối chiếu hash; `baoGia()` thực hiện băm giả khi tên không tồn tại |
| `repository/TaiKhoanRepository.java` | `findByTenDangNhapIgnoreCase()` tìm tài khoản |
| `entity/TaiKhoan.java`, `entity/NhanVien.java` | Ánh xạ dữ liệu tài khoản và nhân viên |

### Luồng giao diện → Service → Repository → SQL Server

1. Nhân viên nhập tên và mật khẩu, bấm Đăng nhập. Giao diện tạm khóa nút đăng nhập.
2. `DangNhapNhanVien.dangNhap()` gọi bản `dangNhap(ten, matKhau, vaiTro)` của Service trên luồng nền.
3. Service kiểm tra trống, độ dài và khóa tạm; Repository tìm trong bảng `tai_khoan`, lấy nhân viên liên quan ở `nhan_vien`.
4. `MatKhau.kiemTra()` dùng PBKDF2-HMAC-SHA256 với salt và số vòng ghi trong chuỗi hash để đối chiếu mật khẩu.
5. Nếu đúng, kiểm tra tài khoản hoạt động, đúng vai trò và nhân viên còn làm. Sai tên/sai mật khẩu dùng chung thông báo.
6. Thành công thì mở `FormNhanVien`; thất bại hiện lỗi và bật lại nút. Mảng mật khẩu được xóa bằng `Arrays.fill()` ở cả hai nhánh kết quả.

### Đoạn khó cần hiểu

- `char[]` là mảng ký tự mật khẩu, có thể ghi đè sau sử dụng. Hash không phải mật khẩu có thể giải mã ngược để đọc lại.
- `Optional.orElse(null)` nghĩa là lấy tài khoản nếu có, nếu không thì nhận `null` và xử lý tiếp.
- Sau 5 lần sai mật khẩu liên tiếp, tên đăng nhập bị khóa tạm 60 giây theo code. Bộ đếm nằm trong `ConcurrentHashMap` của ứng dụng, không lưu SQL Server; không nên trình bày là khóa tài khoản vĩnh viễn.
- Bản đăng nhập ba tham số có `@Transactional(readOnly = true)`. Luồng giao diện hiện gọi bản này.

### Ba câu hỏi bảo vệ

1. **Database có lưu mật khẩu gốc không?** Code sử dụng cột hash; nhập mật khẩu rồi băm để kiểm tra, không so với mật khẩu thuần.
2. **Đúng mật khẩu là được vào ngay không?** Chưa. Còn kiểm tra tài khoản hoạt động, vai trò và trạng thái nhân viên.
3. **Vì sao sai tên và sai mật khẩu báo giống nhau?** Để hạn chế tiết lộ tên tài khoản có tồn tại hay không.

## 4. Bán hàng, topping và giỏ hàng

### Mục đích và phương thức

Chọn món, topping, số lượng, tìm món và tính tiền tạm trước khi thanh toán.

| File | Phương thức quan trọng |
|---|---|
| `ui/TrangBanHang.java` | `taiThucDon()` tải món và topping; `capNhatGioTheoThucDon()` cập nhật tham chiếu trong giỏ |
| `ui/TrangBanHang.java` | `dungDanhMuc()`, `veLuoi()` tạo bộ lọc và hiển thị món phù hợp |
| `ui/TrangBanHang.java` | `chonSanPham()`, `themVaoGio()`, `doiSoLuong()`, `xoaDong()`, `xoaTatCa()` thao tác giỏ |
| `ui/TrangBanHang.java` | `thayDoiGio()`, `tienHang()`, `capNhatTong()` cập nhật mã giao dịch, bảng và tiền |
| `ui/HopThoaiTopping.java` | `getTopping()`, `getSoLuong()`, `daChon()` trả lựa chọn; `doiSl()`, `capNhatTong()` cập nhật số lượng và giá |
| `service/BanHangService.java` | `layThucDon()`, `layTopping()` lấy dữ liệu để bán |
| `service/DongGio.java` | `cungLoai()`, `donGia()`, `thanhTien()`, `moTaTopping()` xử lý một dòng giỏ |
| `service/TienIch.java` | `boDau()` chuẩn hóa từ khóa tìm món |

### Luồng dữ liệu

1. `taiThucDon()` gọi `BanHangService.layThucDon()` và `layTopping()` qua `ChayNen`.
2. Service gọi `SanPhamRepository.findAll(Sort.by("id"))` và `ToppingRepository.findAll(Sort.by("ten"))`.
3. JPA đọc `san_pham`, danh mục liên quan `danh_muc` và bảng `topping` trong SQL Server. Service sắp thực đơn theo tên danh mục rồi tên món sau khi bỏ dấu.
4. Giao diện lưu danh sách, tạo nút danh mục và thẻ món. Gõ tìm kiếm/lọc danh mục xử lý trên danh sách đã tải, không truy vấn SQL cho mỗi lần gõ.
5. Chọn món sẽ mở `HopThoaiTopping`; topping không bắt buộc. Nếu danh sách topping rỗng thì thêm ngay một món không topping, số lượng 1.
6. `themVaoGio()` tạo `DongGio` hoặc gộp vào dòng cùng sản phẩm và cùng tập ID topping, giới hạn 99. Tăng/giảm, xóa giỏ chỉ thay đổi bộ nhớ.
7. `DongGio.donGia()` cộng giá món và topping; `thanhTien()` nhân số lượng. `tienHang()` cộng các dòng.

```text
Giá một ly = giá sản phẩm + tổng giá topping của ly
Thành tiền dòng = giá một ly × số lượng
Tiền hàng = tổng thành tiền các dòng
Tổng tạm tính = tiền hàng − giảm giá
```

### Đoạn khó cần hiểu

- `List<DongGio>` giữ thứ tự các dòng; `Set<Long>` trong `cungLoai()` so tập ID topping, không xét thứ tự. Cùng món nhưng khác topping tạo hai dòng.
- `capNhatGioTheoThucDon()` duyệt từng dòng cũ, tìm sản phẩm mới theo ID bằng `for`. Có thì thay tham chiếu để dùng giá mới; không có thì giữ cũ. Topping cũng tìm theo ID nhưng duyệt danh sách cũ để giữ thứ tự và số lần xuất hiện. `break` lấy kết quả khớp đầu tiên. Số lượng và vị trí dòng giữ nguyên.
- Giữ món cũ khi làm mới không có nghĩa là vẫn bán được món đã xóa: Service thanh toán kiểm tra lại từ database.
- `model.fireTableDataChanged()` báo cho `JTable` đọc lại dữ liệu; không ghi SQL.
- `thayDoiGio()` tạo UUID mới cho thao tác thay đổi giỏ. `maGiaoDich` được dùng để nhận diện lần thanh toán; không phải mã hóa đơn hiển thị.
- `taiThucDon()` vẫn nhận kết quả nền qua `Object[]` và ép kiểu hai danh sách. Bước đơn giản hóa chỉ thay đoạn cập nhật giỏ, không thay cơ chế tải nền.

### Ba câu hỏi bảo vệ

1. **Vì sao cùng món có thể xuất hiện hai dòng?** Vì hai dòng có bộ topping khác nhau.
2. **Thêm vào giỏ đã ghi đơn vào SQL Server chưa?** Chưa. Giỏ là danh sách trong bộ nhớ; thanh toán mới lưu đơn.
3. **Làm mới thực đơn có mất giỏ không?** Không. Code giữ số lượng, cập nhật món/topping theo ID và giữ đối tượng cũ nếu không tìm thấy.

## 5. Khuyến mãi

### Mục đích, file và phương thức

Áp mã hợp lệ để giảm tiền hàng. `TrangBanHang.apDungMa()` nhận mã; `kiemTraLaiMa()` kiểm tra khi giỏ đổi; `boMa()` gỡ mã. `BanHangService.kiemTraKhuyenMai()` trả thông tin cho giao diện; `apDung()` chứa quy tắc dùng chung khi bán và thanh toán.

### Luồng dữ liệu

`TrangBanHang` → `BanHangService.kiemTraKhuyenMai()` → `apDung()` → `KhuyenMaiRepository.findFirstByMaIgnoreCase()` → bảng `khuyen_mai` trên SQL Server → kết quả hợp lệ/số tiền giảm hoặc thông báo lỗi về giao diện.

Service kiểm tra mã trống, giỏ trống, mã tồn tại, ngày bắt đầu/kết thúc và đơn tối thiểu. Loại `GIAM_TIEN` giảm số tiền cố định; nhánh còn lại tính phần trăm, làm tròn đến đồng bằng `HALF_UP`. Số giảm được chặn từ 0 đến tiền hàng.

### Đoạn khó cần hiểu

`soLanKiemTra` đánh số lần gửi kiểm tra nền. Kết quả về mà số lần không còn khớp thì bỏ qua, tránh kết quả cũ ghi đè giỏ mới. Khi thanh toán, Service vẫn kiểm tra khuyến mãi lại; giao diện không quyết định số giảm cuối cùng.

### Ba câu hỏi bảo vệ

1. **Giảm giá có thể lớn hơn tiền hàng không?** Code chặn số giảm tối đa bằng tiền hàng.
2. **Tại sao đổi số lượng phải kiểm tra mã lại?** Tiền hàng đổi có thể làm đơn không còn đủ điều kiện.
3. **Giao diện đã kiểm tra rồi, Service có kiểm tra nữa không?** Có, vì giá và điều kiện khuyến mãi có thể thay đổi trước lúc lưu.

## 6. Thanh toán tiền mặt và chuyển khoản

### Mục đích và phương thức

Kiểm tra số tiền, xác nhận giao dịch và lưu đơn cùng các thông tin liên quan.

| File | Phương thức và nhiệm vụ |
|---|---|
| `ui/TrangBanHang.java` | `thanhToan()` mở hộp thoại; chỉ xóa giỏ khi nhận `HoaDon` khác `null` |
| `ui/TrangThanhToan.java` | `capNhat()` hiện tiền thiếu/thừa và điều kiện bật nút |
| `ui/TrangThanhToan.java` | `xacNhan()` tìm đơn đã lưu trước; `kiemTraGia()` lấy báo giá mới |
| `ui/TrangThanhToan.java` | `luuThanhToan()` kiểm tra tiền, hỏi xác nhận rồi gọi Service; `datDangXuLy()` khóa/mở các điều khiển |
| `service/TienIch.java` | `docTienKhach()` đọc số nguyên VNĐ, kiểm tra định dạng và giới hạn |
| `service/ThanhToanService.java` | `timHoaDonDaLuu()`, `baoGia()`, `tinhBaoGia()`, các bản `thanhToan(...)`, `luuDon()` |

### Luồng dữ liệu

1. Giao diện truyền ID nhân viên, bản sao danh sách giỏ, mã khuyến mãi, tiền tạm tính và mã giao dịch vào hộp thoại.
2. Nhân viên chọn tiền mặt hoặc chuyển khoản. Tiền mặt phải đủ tổng tiền. Chuyển khoản phải tích đã nhận tiền sau khi nhân viên tự kiểm tra ngân hàng.
3. `xacNhan()` gọi `timHoaDonDaLuu(maGiaoDich)` qua `ThanhToanDonHangRepository.findByMaGiaoDich()`, đọc bảng `thanh_toan_don_hang`. Nếu đã có thì trả lại hóa đơn, không tạo đơn mới.
4. Nếu chưa có, `kiemTraGia()` gọi `baoGia()`; `tinhBaoGia()` đọc lại sản phẩm, topping qua `SanPhamRepository.findById()`, `ToppingRepository.findById()` và kiểm tra khuyến mãi.
5. Nếu tiền hàng hoặc số giảm đổi so với đang hiển thị, hộp thoại cập nhật tổng, xóa tiền đã nhập và bỏ tích xác nhận chuyển khoản để yêu cầu xác nhận lại.
6. `luuThanhToan()` đọc tiền, kiểm tra đủ tiền/đã nhận chuyển khoản, tạo nội dung hỏi và chỉ tiếp tục nếu chọn Có.
7. `ThanhToanService.thanhToan(...)` kiểm tra đầu vào, mã giao dịch và tiền; chạy `luuDon()` trong `ghiThanhToan.execute(...)`.
8. `luuDon()` kiểm tra lại mã giao dịch, báo giá, nhân viên còn làm; dựng dữ liệu món/topping từ database, tính lại tiền và ghi qua các Repository.
9. Thành công trả `HoaDon`; giao diện đóng hộp thoại, xóa giỏ, gỡ mã giảm và mở hóa đơn. Lỗi thì báo lỗi và giữ giỏ.

### Những bảng được ghi

| Repository | Bảng SQL Server | Dữ liệu |
|---|---|---|
| `DonHangRepository.save()` | `don_hang` | Ngày đặt, tổng tiền sau giảm |
| `ChiTietDonHangRepository.save()` | `chi_tiet_don_hang` | Món, số lượng, đơn giá riêng món |
| `ChiTietDonHangToppingRepository.save()` | `chi_tiet_don_hang_topping` | Topping từng dòng, bản lưu tên và giá topping |
| `ThanhToanDonHangRepository.save()` | `thanh_toan_don_hang` | Nhân viên, khuyến mãi, tiền hàng, giảm, tổng, phương thức, tiền khách/thừa, thời gian, mã giao dịch, trạng thái |

Đơn mới thanh toán được gán `HOAN_THANH`. Chuyển khoản được Service ghi tiền khách bằng tổng và tiền thừa bằng 0. Code không có API ngân hàng và không tự xác minh dòng tiền.

### Đoạn khó cần hiểu

- `BaoGia` là `record` chứa tiền hàng, giảm và `dauVanTay`; `tong()` trả tiền hàng trừ giảm. Dấu vân tay ở đây là chuỗi mô tả ID, giá, số lượng, topping và thông tin mã giảm, không phải sinh trắc học. `luuDon()` so toàn bộ báo giá đã xác nhận với báo giá hiện tại để phát hiện thay đổi trước lưu.
- `distinct().sorted()` trong báo giá lấy ID topping duy nhất, sắp ID để tạo dấu so sánh ổn định. Khi dựng món để lưu, `LinkedHashMap` loại ID topping trùng và giữ thứ tự gặp đầu tiên.
- `BigDecimal` tính tiền thập phân chính xác theo phép toán đã chọn; `compareTo()` so giá trị, `subtract()` trừ, `multiply()` nhân. `longValueExact()` báo lỗi nếu không chuyển được chính xác sang số nguyên.
- `ghiThanhToan` dùng `TransactionTemplate` với mức `SERIALIZABLE`. Mục tiêu là phối hợp kiểm tra mã giao dịch và ghi đơn trong transaction để hạn chế tạo trùng khi gửi đồng thời. Có nhánh bắt `DataIntegrityViolationException` và tìm lại hóa đơn. Hành vi đồng thời vẫn cần kiểm thử trên SQL Server thực tế.
- Nếu mất phản hồi sau commit, không được kết luận chắc chắn chưa lưu. Code giữ giỏ và cho tra lại cùng mã giao dịch khi thử lại. Sửa giỏ tạo mã mới nên cần kiểm tra lịch sử khi kết quả giao dịch cũ chưa rõ.
- `try/catch` xử lý lỗi để giao diện có thể mở lại nút và hiện thông báo. Các bản `thanhToan(...)` ít tham số chuyển tiếp sang bản đầy đủ để giữ cách gọi cũ.

### Ba câu hỏi bảo vệ

1. **Vì sao không lấy giá trên giao diện để lưu luôn?** Giá hiển thị có thể cũ; Service đọc lại database và tính lại trước lưu.
2. **Đang ghi đơn mà lỗi giữa chừng thì sao?** Các bước ghi nằm trong transaction; lỗi khiến transaction rollback thì không giữ đơn ghi dở.
3. **Bấm lại sau lỗi mạng có tạo trùng không?** Code tra cùng mã giao dịch để trả hóa đơn đã có; cần kiểm thử thực tế cơ chế đồng thời và mất phản hồi.

## 7. Xem và in hóa đơn

### Mục đích, file và phương thức

`ThanhToanService.dungHoaDon()` dựng dữ liệu hóa đơn; `toppingTheoDong()` nhóm topping theo chi tiết đơn; `layHoaDon()` tải hóa đơn đã lưu. `ui/HopThoaiHoaDon.java` có `xayDung()` tạo phần hiển thị và `in()` gửi nội dung tới hệ thống in Java.

### Luồng dữ liệu

- Sau thanh toán: Service dựng `HoaDon` từ dữ liệu đã ghi → `TrangBanHang` mở `HopThoaiHoaDon`.
- Xem lại: `TrangLichSuDonHang.xemChiTiet()` → `ThanhToanService.layHoaDon(thanhToanId)` → `ThanhToanDonHangRepository.findById()` → bảng `thanh_toan_don_hang` và dữ liệu liên quan trên SQL Server.
- `dungHoaDon()` gọi `ChiTietDonHangRepository.findByDonHang_IdOrderByIdAsc()` lấy món; `toppingTheoDong()` gọi `ChiTietDonHangToppingRepository.findByChiTietDonHang_IdIn()` lấy topping → `HoaDon` → giao diện.
- Bấm In dùng `PrinterJob`, không tạo đơn hay ghi lại hóa đơn vào SQL Server.

### Đoạn khó cần hiểu

`Map<Long, List<ChiTietDonHangTopping>>` giống danh sách các ngăn: mỗi ngăn mang ID chi tiết đơn và chứa topping của dòng đó. Chưa có ngăn thì tạo `ArrayList`; có rồi thì thêm tiếp. Danh sách chi tiết rỗng thì không truy vấn topping.

Trong `dungHoaDon()`, mỗi dòng cộng giá topping đã lưu, lấy tên topping đã lưu, tính `(đơn giá món đã lưu + tổng giá topping) × số lượng`. Tên món hiện lấy từ Entity `SanPham` liên quan, tên nhân viên từ Entity `NhanVien`; không nên nói mọi tên trên hóa đơn đều là bản chụp tại thời điểm bán. Tổng tiền và khuyến mãi ở phần cuối hóa đơn lấy từ `ThanhToanDonHang`.

Thứ tự sản phẩm theo ID chi tiết tăng dần. Phương thức lấy topping không có `ORDER BY`; code chỉ giữ thứ tự danh sách Repository trả về, không bảo đảm thứ tự topping cố định giữa mọi lần truy vấn. `Collections.unmodifiableList(tenTopping)` ngăn sửa danh sách tên qua kết quả trả về, giữ tính không sửa được của danh sách tên như cách dùng `stream().toList()` trước đây.

`in()` tính tỷ lệ co theo chiều rộng vùng in và số trang theo chiều cao; dùng `printAll()` vẽ phần hóa đơn. Hủy hộp thoại máy in thì dừng, lỗi `PrinterException` được báo. Kết quả in dài/nhiều trang phải kiểm thử trên máy in hoặc máy in PDF.

### Ba câu hỏi bảo vệ

1. **Vì sao đơn giá món không gồm topping?** Database lưu riêng giá món và topping; khi dựng hóa đơn mới cộng lại.
2. **Xem lại hóa đơn có dùng giá topping mới không?** Không. Phần topping dùng tên và giá đã lưu trong chi tiết topping của đơn.
3. **Bấm In có ghi thêm đơn không?** Không. Chỉ gửi phần giao diện hóa đơn cho hệ thống in.

## 8. Lịch sử và chỉnh sửa đơn hàng

### Mục đích và phương thức

| File | Phương thức quan trọng |
|---|---|
| `ui/TrangLichSuDonHang.java` | `lamMoi()` tìm theo mã/ngày; `xemChiTiet()` mở hóa đơn; `suaDon()` mở sửa |
| `ui/HopThoaiSuaDon.java` | `luu()` kiểm tra đầu vào, xác nhận rồi gọi Service; `tienHangMoi()`, `capNhat()` tính/hiện thông tin sửa |
| `service/ThanhToanService.java` | `timDonHang()`, `layHoaDon()`, `layDeSua()`, `capNhatDon()`, `giamKhuyenMai()` |
| `service/TienIch.java` | `docMaDon()` đổi mã gõ sang ID; `maHienThi()` định dạng như `HD000012` |

### Luồng dữ liệu

1. Giao diện lấy mã đơn và khoảng ngày nếu bật lọc, gọi `timDonHang()` qua luồng nền.
2. Service kiểm tra ngày, đọc mã. Có mã thì gọi `ThanhToanDonHangRepository.timTheoDonHang()` rồi lọc thời gian; không có mã thì gọi `timTheoKhoang()` giới hạn 500 kết quả.
3. Repository truy vấn `thanh_toan_don_hang` cùng `don_hang`, `nhan_vien` trên SQL Server. Khoảng thời gian là từ đầu ngày bắt đầu đến trước đầu ngày sau ngày kết thúc, tức bao gồm cả ngày cuối. Truy vấn theo khoảng xếp mới nhất trước.
4. Xem chi tiết gọi `layHoaDon()`. Sửa gọi `layDeSua()` lấy chi tiết và topping đã lưu để mở `HopThoaiSuaDon`.
5. `HopThoaiSuaDon.luu()` gửi trạng thái, bản đồ ID chi tiết → số lượng, phương thức, tiền khách và ghi chú tới `capNhatDon()`.
6. Service xử lý trong `ghi.execute(...)`, đọc đơn/chi tiết/topping, kiểm tra dữ liệu và ghi lại qua Repository. Giao diện tải lại lịch sử nếu lưu thành công.

### Quy tắc và đoạn khó

- Có thể đổi trạng thái, số lượng, phương thức, tiền khách và ghi chú. Màn hình này không thêm món mới hoặc đổi bộ topping.
- Số lượng từ 0 đến 99; 0 là xóa dòng. Phải còn ít nhất một món, nếu muốn bỏ cả đơn thì chọn Đã hủy. Service kiểm tra ID cần sửa có thuộc đơn đang sửa không.
- Xóa dòng phải xóa topping phụ thuộc trước rồi xóa chi tiết; `flush()` đẩy thao tác xóa topping xuống database trước. `flush()` không có nghĩa là commit toàn transaction.
- Nếu số lượng đổi, tính lại tiền bằng đơn giá món và giá topping đã lưu. Mã cũ bị bỏ nếu không còn đạt đơn tối thiểu; `giamKhuyenMai()` không kiểm tra lại ngày hiệu lực vì đây là sửa đơn đã dùng mã. Nếu số lượng không đổi thì giữ tiền hàng/giảm/tổng đã lưu.
- Đơn hủy chỉ đổi trạng thái và ghi chú, không xóa số liệu; không sửa lại đơn đã hủy. Truy vấn thống kê `banChay()` trong `ChiTietDonHangRepository` có điều kiện loại đơn đã hủy, nhưng chưa chạy kiểm tra màn hình Quản lý.
- Khi sửa thông thường, Service cập nhật cả `thanh_toan_don_hang` và `don_hang.tong_tien` để dữ liệu tổng nhất quán. Ghi chú tối đa 200 ký tự.
- Lịch sử hiện không truyền ID nhân viên đăng nhập để lọc riêng; không nên nói danh sách chỉ gồm đơn do người đang đăng nhập bán. Các phương thức lịch sử/sửa không có kiểm tra vai trò riêng bên trong như kho; không khẳng định mọi thao tác đều tái kiểm tra quyền tại Service.

### Ba câu hỏi bảo vệ

1. **Mã `HD000012` lưu nguyên trong database không?** Đó là định dạng hiển thị của ID đơn 12; `docMaDon()` đọc về ID.
2. **Hủy đơn có xóa dữ liệu không?** Không. Giữ dữ liệu để xem lại, đổi trạng thái thành Đã hủy và chặn sửa tiếp.
3. **Vì sao sửa tổng phải cập nhật hai nơi?** Vì cả đơn hàng và thông tin thanh toán đều lưu tổng, cần cùng phản ánh kết quả mới.

## 9. Quản lý kho

### Mục đích và phương thức

Xem/tìm tồn nguyên liệu, nhập kho, xuất kho và xem 100 giao dịch gần nhất theo bộ lọc.

| File | Phương thức quan trọng |
|---|---|
| `ui/TrangQuanLyKho.java` | `lamMoi()` tải tồn; `capNhatNut()` bật nhập/xuất khi chọn dòng; `moGiaoDich()` mở hộp thoại |
| `ui/TrangQuanLyKho.java` | `ketThucTai()` phục hồi trạng thái; `so()` định dạng lượng; `baoLoi()` hiện lỗi |
| `ui/HopThoaiGiaoDichKho.java` | Constructor gắn xử lý nút xác nhận; `daLuu()` báo có lưu thành công |
| `ui/HopThoaiLichSuKho.java` | Constructor tạo bộ lọc và `Runnable tai` để gọi lịch sử; không có phương thức `tai()` riêng |
| `service/KhoService.java` | `timNguyenLieu()`, `giaoDich()`, `lichSu()`, `docSoLuong()`, `kiemTraSoLuong()`, `kiemTraTaiKhoan()` |
| `repository/NguyenLieuRepository.java` | Phương thức tìm theo mã/tên và `timDeCapNhat()` |
| `repository/GiaoDichKhoRepository.java` | `timGanNhat()` lấy lịch sử có lọc và sắp xếp |
| `entity/NguyenLieu.java`, `entity/GiaoDichKho.java` | Ánh xạ `dbo.nguyen_lieu`, `dbo.giao_dich_kho` |

### Luồng xem và tìm tồn

`TrangQuanLyKho.lamMoi()` → `KhoService.timNguyenLieu(taiKhoanId, tuKhoa)` → kiểm tra tài khoản qua `TaiKhoanRepository.findById()` → `NguyenLieuRepository.findByMaNguyenLieuContainingIgnoreCaseOrTenNguyenLieuContainingIgnoreCaseOrderByMaNguyenLieuAsc()` → SQL Server đọc nguyên liệu theo mã hoặc tên, xếp mã tăng dần → `DongTon` → bảng giao diện.

Từ khóa tối đa 100 ký tự. Đang tải thì khóa thao tác nhập/xuất. Nếu tải thất bại, danh sách và bảng bị xóa để tránh thao tác trên số tồn cũ.

### Luồng nhập/xuất

1. Chọn nguyên liệu → `moGiaoDich(Loai.NHAP/XUAT)` → `HopThoaiGiaoDichKho`.
2. Giao diện gọi `KhoService.docSoLuong()` để đọc số, kiểm tra ghi chú tối đa 255 ký tự và hỏi xác nhận.
3. Qua luồng nền gọi `KhoService.giaoDich()`. Service kiểm tra tài khoản còn hoạt động, có nhân viên còn làm, vai trò Nhân viên hoặc Quản lý; kiểm tra nguyên liệu, loại, số lượng và ghi chú.
4. `NguyenLieuRepository.timDeCapNhat()` lấy nguyên liệu với khóa `PESSIMISTIC_WRITE`; đọc tồn hiện tại trong SQL Server, không dùng số tồn tham khảo ở hộp thoại để quyết định.
5. Nhập: tồn mới = tồn cũ + lượng. Xuất: tồn mới = tồn cũ − lượng; xuất quá tồn bị từ chối. Kiểm tra tồn hợp lệ và không vượt giới hạn.
6. `nguyenLieuRepo.save()` cập nhật `dbo.nguyen_lieu`; `giaoDichRepo.save()` ghi `dbo.giao_dich_kho`, gồm nguyên liệu, loại, lượng, thời gian, tài khoản thực hiện và ghi chú.
7. Hai thao tác nằm trong `@Transactional`; thành công trả tồn mới và trang tải lại. Lỗi hiển thị thông báo và mở lại nút.

### Luồng xem lịch sử kho

`HopThoaiLichSuKho` → `KhoService.lichSu(taiKhoanId, loai)` → kiểm tra tài khoản → `GiaoDichKhoRepository.timGanNhat(loai, PageRequest.of(0, 100))` → SQL Server đọc giao dịch cùng nguyên liệu, tài khoản và nhân viên → `DongLichSu` → bảng.

Lọc Tất cả/Nhập/Xuất trước khi giới hạn 100 dòng, xếp thời gian giảm dần rồi ID giảm dần. `lanTai` giúp bỏ qua kết quả tải cũ khi người dùng đổi bộ lọc nhanh.

### Đoạn khó và giới hạn

- `Loai` là `enum` gồm `NHAP`, `XUAT`, giúp dùng giá trị xác định thay vì chuỗi tùy ý.
- `docSoLuong()` chấp nhận như `2`, `1,25`, `1.25`; dấu chấm/phẩy ở đây là thập phân, không phải phân nhóm nghìn. Tối đa 3 chữ số sau dấu; số phải lớn hơn 0, không vượt `999999999999999.999`.
- `PESSIMISTIC_WRITE` yêu cầu khóa dữ liệu khi cập nhật để các giao dịch cạnh tranh không cùng tùy tiện dùng một số tồn cũ; vẫn cần thử đồng thời thực tế.
- `@Transactional` gom cập nhật tồn và ghi lịch sử thành một giao dịch. Nếu ghi lịch sử lỗi làm rollback thì tồn cũng không được giữ lại một phần.
- Kho chưa có mã giao dịch chống gửi trùng như thanh toán. Nếu mất phản hồi sau lưu, cần xem lịch sử trước khi thử nhập/xuất lại.
- Trong code đã đọc, thanh toán không gọi `KhoService` để tự trừ nguyên liệu. Kho là nhập/xuất thủ công; chưa có luồng công thức món → lượng nguyên liệu tiêu hao. Giao diện kho hiện cũng không có chức năng tạo nguyên liệu mới.
- Có file SQL kho trong dự án không chứng minh các bảng đã tồn tại trên database đang dùng. Chưa kiểm tra schema thực tế và không chạy migration trong đợt này.

### Ba câu hỏi bảo vệ

1. **Làm sao tránh xuất âm kho?** Service đọc tồn có khóa, so lượng xuất với tồn hiện tại và từ chối nếu vượt.
2. **Vì sao cập nhật tồn và lưu lịch sử cùng transaction?** Để không có tình trạng tồn đã đổi nhưng thiếu lịch sử, hoặc ngược lại khi transaction thất bại.
3. **Bán một ly có tự trừ kho không?** Chưa. Bản hiện tại nhập/xuất nguyên liệu thủ công, chưa nối thanh toán với công thức nguyên liệu.

## 10. Kiến thức nền để trả lời ngắn

| Khái niệm | Giải thích và ví dụ trong dự án |
|---|---|
| Java Swing | Bộ công cụ tạo giao diện desktop Java. `JFrame` là cửa sổ, `JPanel` là vùng chứa, `JDialog` là hộp thoại, `JTable` là bảng, `JButton` là nút |
| Spring Boot | Giúp khởi tạo ứng dụng và tự cấu hình các thành phần Spring theo thư viện/cấu hình có sẵn. Dự án dùng nó để quản lý Service, Repository và kết nối dữ liệu, không mở web server |
| Entity | Lớp ánh xạ dữ liệu bảng. Ví dụ `NguyenLieu` ánh xạ bảng `nguyen_lieu`; `@Id` xác định khóa chính; `@JoinColumn` mô tả cột liên kết |
| Repository | Nơi khai báo thao tác dữ liệu. Kế thừa `JpaRepository` có sẵn `findById()`, `save()`; tên phương thức tìm kiếm hoặc `@Query` mô tả truy vấn riêng |
| Service | Nơi xử lý quy tắc nghiệp vụ và gọi Repository. Ví dụ `KhoService.giaoDich()` kiểm tra tồn trước khi lưu |
| JPA | Chuẩn ánh xạ đối tượng Java với dữ liệu quan hệ. Dự án dùng Spring Data JPA và Hibernate thực hiện ánh xạ/truy vấn; JDBC driver SQL Server vận chuyển lệnh và kết quả |
| `@Transactional` | Đặt ranh giới giao dịch khi được Spring áp dụng. Commit khi hoàn thành; mặc định rollback với lỗi runtime không bị xử lý nuốt bên trong. `LoiNghiepVu` là lỗi runtime. Kho dùng annotation, thanh toán dùng `TransactionTemplate` để xác định rõ khối transaction |
| `readOnly = true` | Nêu mục đích chỉ đọc cho transaction, không phải cơ chế phân quyền và không nên hiểu là mọi database đều tự cấm toàn bộ lệnh ghi |
| SQL Server | Hệ quản trị dữ liệu quan hệ mà bài tập đang dùng chung. Phù hợp lưu đơn, chi tiết, tài khoản, khóa ngoại, transaction và truy vấn thống kê; không phải lựa chọn bắt buộc cho mọi ứng dụng Java |
| Luồng nền | Truy vấn và băm mật khẩu có thể mất thời gian. Nếu chạy trên luồng giao diện sẽ làm cửa sổ khó phản hồi; `ChayNen.chay()` dùng `SwingWorker` để xử lý nền rồi trả kết quả lên giao diện |

### Các cú pháp thường gặp

- `record HoaDon(...)`: kiểu chứa dữ liệu, Java tự tạo constructor và phương thức đọc như `maDon()`. Không phải Entity nếu không có ánh xạ Entity. Danh sách bên trong record không tự động trở thành bất biến sâu.
- `e -> thanhToan()`: lambda, tức hành động được gọi khi sự kiện xảy ra; lambda không tự tạo luồng nền. `ChayNen` mới là phần tạo `SwingWorker`.
- `stream().map(...)`: biến đổi từng phần tử; `filter(...)`: chọn phần tử đạt điều kiện; `reduce(...)`: gộp nhiều giá trị, ví dụ cộng giá. Một số chỗ trong dự án vẫn dùng những cú pháp này, chưa thay toàn bộ.
- `getOrDefault(id, List.of())`: lấy danh sách theo ID; nếu không có thì dùng danh sách rỗng.
- `@Service`: cho Spring nhận diện lớp nghiệp vụ và tạo đối tượng quản lý. Truyền Repository vào constructor là cách cung cấp phụ thuộc, không tự `new` Repository.
- `@Query` trong các Repository đang xem là JPQL viết theo Entity và thuộc tính Java; JPA chuyển sang SQL. Không phải mọi chuỗi truy vấn này là SQL thuần.
- `join fetch`: lấy dữ liệu quan hệ kèm kết quả truy vấn, ví dụ giao dịch kho kèm người thực hiện.
- `ChayNen.doInBackground()` chạy việc nền. `done()` gọi `get()` lấy kết quả, tách nguyên nhân lỗi rồi gọi nhánh thành công hoặc lỗi trên luồng giao diện Swing (EDT).
- `LoiNghiepVu`: thông báo lỗi quy tắc, như thiếu tiền hoặc thiếu tồn. Có xử lý lỗi trên giao diện không có nghĩa là mọi lỗi kết nối đều chứng minh transaction chưa commit.

### Lưu ý khi chuẩn bị môi trường

Cấu hình hiện tại có `spring.jpa.hibernate.ddl-auto=update`. Khi khởi động, Hibernate có thể thử cập nhật schema; đây là cấu hình sẵn có, không được thay đổi trong đợt tài liệu. Vì vậy tài liệu này không tự chạy ứng dụng. Nếu chuẩn bị chạy thử, cần thống nhất môi trường dữ liệu thử phù hợp trước; không chạy migration hoặc thử lỗi trên dữ liệu thật chỉ để minh họa.

## 11. Đối chiếu ba bước đơn giản hóa

| File | Thay đổi đã kiểm tra lại | Số dòng toàn file trước → sau của bước đó |
|---|---|---|
| `TrangBanHang.java` | `taiThucDon()` gọi `capNhatGioTheoThucDon()`; tìm theo ID bằng vòng lặp, giữ thứ tự và số lượng | 569 → 592 |
| `TrangThanhToan.java` | `luuThanhToan()` có tên biến rõ, nhánh `if/else`, tách tiền thừa và lựa chọn xác nhận | 312 → 342 |
| `ThanhToanService.java` | `dungHoaDon()` tái sử dụng `toppingTheoDong()`; gom topping và tính dòng bằng vòng lặp | 528 → 538 |

Số dòng gồm dòng trống. Tăng dòng ở những nơi cần diễn đạt từng bước là chủ ý. Đọc lại cho thấy các chữ ký phương thức công khai và ranh giới transaction được giữ; phần tái sử dụng là phương thức `private`. Chưa chạy so sánh hóa đơn trước/sau trên database. Không trình bày việc đọc code là kiểm thử hồi quy đã đạt.

## 12. Danh sách thao tác cần kiểm thử trước bảo vệ

Tất cả mục dưới đây **chưa được thực hiện trong phiên làm việc này**. Ô trống là trạng thái chưa kiểm thử, không phải kết quả đạt. Chỉ đánh dấu khi đã chạy và đối chiếu kết quả. Dùng dữ liệu thử được phép; tài liệu này không thực hiện các thao tác ghi dữ liệu.

### Khởi động, đăng nhập và điều hướng

- [ ] Khởi động trên môi trường thử: mở được đăng nhập, có thể kết nối đúng database; ghi lại JDK và cấu hình môi trường sử dụng.
- [ ] Đăng nhập đúng; sai mật khẩu; tên không tồn tại; bỏ trống; kiểm tra thông báo.
- [ ] Sai liên tiếp 5 lần và chờ 60 giây; đối chiếu khóa tạm theo code.
- [ ] Tài khoản ngừng hoạt động, nhân viên nghỉ, sai vai trò: bị từ chối với dữ liệu thử tương ứng.
- [ ] Chuyển Bán hàng/Lịch sử/Kho; lịch sử và kho tải lại khi mở. Đăng xuất có hỏi và giỏ chưa trả tiền không còn khi đăng nhập lại.

### Bán hàng, topping và làm mới thực đơn

- [ ] Thực đơn có ảnh hoặc ảnh thay thế; tìm có dấu/không dấu, chọn danh mục và tìm không có kết quả.
- [ ] Thêm món không topping; một topping; nhiều topping. So đơn giá, số lượng, thành tiền với phép tính tay.
- [ ] Thêm cùng món/cùng bộ topping theo thứ tự chọn khác nhau: gộp dòng; khác bộ topping: tách dòng.
- [ ] Tăng đến 99; giảm xuống 0 làm xóa dòng; xóa dòng, xóa hết và chọn hủy xác nhận xóa.
- [ ] Hủy hộp thoại topping không thêm món. Kiểm tra trường hợp danh sách topping rỗng nếu có dữ liệu thử phù hợp.
- [ ] Làm mới thực đơn khi có giỏ: số lượng, vị trí dòng và thứ tự topping giữ nguyên.
- [ ] Trên dữ liệu thử được chuẩn bị riêng, kiểm tra giá mới được cập nhật; món/topping không còn trong danh sách vẫn giữ tham chiếu cũ trong giỏ nhưng bị kiểm tra lại khi thanh toán.

### Khuyến mãi và thanh toán

- [ ] Mã hợp lệ giảm tiền/phần trăm; mã sai, chưa bắt đầu, hết hạn, chưa đủ đơn tối thiểu; gỡ mã và thay đổi số lượng.
- [ ] Gõ tiền `50000`, `50.000`, `50,000`; thử trống, chữ, số âm, nhóm nghìn sai và vượt giới hạn: không được lưu sai.
- [ ] Tiền mặt thiếu: chặn xác nhận; vừa đủ: thừa 0; dư: tính đúng tiền trả. Thử các nút tiền nhanh.
- [ ] Chuyển khoản chưa tích đã nhận: chặn; tích rồi xác nhận: lưu phương thức đúng, tiền khách bằng tổng, tiền thừa 0. Đối chiếu thủ công việc nhận tiền.
- [ ] Chọn Không trong hộp xác nhận hoặc hủy hộp thanh toán: không tạo đơn và vẫn giữ giỏ.
- [ ] Thanh toán thành công: đúng một đơn, đúng số dòng và topping, tổng sau giảm; giỏ được xóa và mở hóa đơn.
- [ ] Với dữ liệu thử riêng, kiểm tra thay đổi giá/khuyến mãi trước lúc lưu: yêu cầu xác nhận lại hoặc báo thay đổi, không âm thầm chốt báo giá cũ.
- [ ] Trong môi trường thử có kiểm soát, thử bấm nhanh/gửi lại cùng mã; mất phản hồi sau lưu: tra lịch sử và cùng mã giao dịch, xác nhận không tạo trùng. Đây chưa phải kết quả được bảo đảm bằng kiểm thử hiện có.
- [ ] Thử lỗi ghi giữa các bước bằng môi trường kiểm thử phù hợp: xác minh rollback, không còn đơn ghi dở. Không gây lỗi có chủ ý trên dữ liệu thật.

### Hóa đơn và lịch sử

- [ ] Hóa đơn vừa thanh toán khớp với giỏ: món, topping, số lượng, giá, giảm, tổng, nhân viên, phương thức và tiền thừa.
- [ ] Xem lại từ lịch sử khớp số tiền đã lưu; không đòi hỏi thứ tự topping cố định vì truy vấn chưa quy định thứ tự.
- [ ] In/print PDF hóa đơn ngắn và dài nhiều trang; kiểm tra tiếng Việt, cắt trang, tổng tiền; hủy in và lỗi in không làm thay đổi đơn.
- [ ] Tìm bằng `HD000012` và số ID thực tế; mã sai; lọc ngày đầu/cuối, từ ngày sau đến ngày; kiểm tra kết quả rỗng.
- [ ] Sửa số lượng, bỏ một món, thử bỏ hết món; kiểm tra tổng mới và mã giảm khi không đủ tối thiểu.
- [ ] Đổi phương thức/tiền khách, ghi chú; hủy đơn và thử sửa lại đơn hủy. Đối chiếu tổng ở cả đơn hàng và thanh toán.

### Kho

- [ ] Xác nhận schema và dữ liệu nguyên liệu của môi trường thử đã sẵn sàng; không tự chạy file migration để hoàn thành checklist.
- [ ] Tìm theo mã/tên; làm mới; chưa chọn dòng thì không nhập/xuất. Lỗi tải phải xóa danh sách tồn cũ.
- [ ] Nhập số nguyên và số thập phân như `1,25`/`1.25`; đối chiếu tồn mới và bản ghi lịch sử cùng người thực hiện.
- [ ] Xuất nhỏ hơn tồn và đúng bằng tồn; xuất vượt tồn bị chặn, không ghi giao dịch.
- [ ] Thử 0, số âm, chữ, hơn 3 số thập phân, ghi chú hơn 255 ký tự và số vượt giới hạn: bị từ chối.
- [ ] Tài khoản không hợp lệ/không còn quyền: Service từ chối đọc và ghi kho.
- [ ] Lọc lịch sử Tất cả/Nhập/Xuất, kiểm tra mới nhất trước và giới hạn 100 sau lọc.
- [ ] Hai phiên cùng xuất một nguyên liệu: xác minh khóa và kết quả tồn không âm trong môi trường thử.
- [ ] Lỗi lưu lịch sử gây rollback tồn; mất phản hồi sau lưu thì xem lịch sử trước khi gửi lại, tránh tự tạo giao dịch trùng.

### Mẫu ghi kết quả sau khi tự kiểm thử

| Ca thử | Dữ liệu/mã đơn | Kết quả mong đợi | Kết quả thực tế | Đạt/Không đạt | Bằng chứng, ngày thử |
|---|---|---|---|---|---|
| Chưa thực hiện | — | — | Chưa kiểm thử | Chưa kết luận | — |

## 13. Kịch bản trình bày gợi ý

1. Mở `HighlandsApplication`: giải thích Swing là giao diện, Spring quản lý Service/Repository.
2. Đăng nhập bằng tài khoản thử; chỉ đường gọi sang `DangNhapService` và `TaiKhoanRepository`.
3. Chọn món cùng topping, đổi số lượng; giải thích `DongGio` và phép tính một dòng.
4. Áp mã và thanh toán; giải thích kiểm tra lại giá, tiền khách, transaction và mã giao dịch.
5. Xem hóa đơn rồi mở lại trong lịch sử; chỉ `dungHoaDon()` và `toppingTheoDong()`.
6. Nhập/xuất một nguyên liệu thử; giải thích kiểm tra quyền, khóa tồn và lưu lịch sử cùng transaction.
7. Nếu được hỏi giới hạn, trả lời đúng hiện trạng: chuyển khoản xác nhận thủ công, kho chưa tự trừ theo món, các tình huống chưa chạy thử phải nói là chưa kiểm thử.

Câu mở đầu dễ nhớ: “Giao diện nhận thao tác; Service kiểm tra và tính toán; Repository đọc ghi; SQL Server lưu dữ liệu. Giỏ chỉ ở bộ nhớ, còn đơn chỉ được ghi khi thanh toán thành công.”
