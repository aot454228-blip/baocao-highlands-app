# Highlands Coffee - Phan he Nhan vien

Du an Java Swing + Spring Boot + Spring Data JPA, chi gom phan he Nhan vien.

## Chuc nang
- Dang nhap tai khoan nhan vien
- Ban hang, chon san pham va topping, quan ly gio hang
- Thanh toan va hien thi/in hoa don
- Xem lich su don hang

## Yeu cau
- JDK 17 tro len
- Maven
- SQL Server dang chay va co database `HighlandsDB`

## Cau hinh database
Mo `src/main/resources/application.properties`, sua:
- `spring.datasource.url` neu ten server/database khac
- `spring.datasource.username`
- `spring.datasource.password`

Khong commit mat khau that len GitHub. Neu moi may co cau hinh rieng, hay dung file `application-local.properties` va khong dua file nay len Git.

## Chay
Tai thu muc chua `pom.xml`, chay:
```bash
mvn spring-boot:run
```
Hoac mo project bang IntelliJ IDEA / VS Code va chay `com.highlands.HighlandsApplication`.

## Luu y
Day la ban tach rieng phan he Nhan vien tu ban du an da gop. Cac lop entity/repository/service dung chung duoc giu lai neu can cho ban hang, thanh toan va lich su don hang.
