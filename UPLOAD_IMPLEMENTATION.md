# 📚 Hệ Thống Upload Sách - Cài Đặt Hoàn Chỉnh

## ✅ Các Tính Năng

1. **Upload File Thủ Công (Không SDK)**
   - Sử dụng Retrofit + OkHttp
   - Kiểm soát toàn bộ quá trình upload
   - Hỗ trợ tất cả loại file

2. **Quản Lý API Key Riêng Biệt**
   - File `cloudinary.properties` ngoài mã nguồn
   - Không cần commit vào repository
   - An toàn hơn

3. **Dialog Upload Chi Tiết**
   - Tên sách (bắt buộc)
   - Tác giả (bắt buộc)
   - Mô tả sách (bắt buộc)
   - Tags (checklist từ database - bắt buộc)
   - Chọn file sách (bắt buộc)

4. **Lưu Vào Database**
   - Status: "pending" (chờ phê duyệt)
   - Tất cả thông tin sách
   - Thông tin người upload

---

## 📋 Files Được Tạo/Cập Nhật

### Cấu Hình
- `cloudinary.properties` - Thông tin Cloudinary (chứa API keys)
- `app/src/main/assets/cloudinary.properties` - Config trong app

### Services
- `CloudinaryConfig.kt` - Quản lý config từ file properties
- `CloudinaryUploadService.kt` - Upload file bằng HTTP
- `FirebaseService.kt` - Lưu data vào Firestore

### UI
- `dialog_upload_book_new.xml` - Layout dialog upload
- `item_tag_checkbox.xml` - Layout checkbox tag
- `UploadBookDialog.kt` - Logic dialog upload
- `TagAdapter.kt` - Adapter cho danh sách tags

### Models
- `Book.kt` - Cập nhật với fields mới (description, url, status, uploadedBy)
- `Tag.kt` - Model Tag

### Activity
- `MainActivity.kt` - Thêm nút upload + khởi tạo Cloudinary config

### Menu
- `nav_menu.xml` - Thêm item "Upload Sách"

---

## 🚀 Hướng Dẫn Setup

### 1. Cấu Hình Cloudinary

Mở file `cloudinary.properties` (tại root folder) hoặc `app/src/main/assets/cloudinary.properties`:

```properties
cloudinary.cloudName=YOUR_CLOUD_NAME
cloudinary.uploadPreset=YOUR_UNSIGNED_PRESET
cloudinary.uploadUrl=https://api.cloudinary.com/v1_1/YOUR_CLOUD_NAME/auto/upload
```

**Thay thế:**
- `YOUR_CLOUD_NAME` - Cloud name từ Cloudinary
- `YOUR_UNSIGNED_PRESET` - Upload preset (tạo trên Cloudinary)

### 2. Tạo Upload Preset (Cloudinary Dashboard)

1. Vào **Settings → Upload**
2. Tìm **Upload presets**
3. Click **Add upload preset**
4. Cấu hình:
   - Name: `finallib_upload`
   - Unsigned: **ON**
   - Folder: `finallib/books`
5. Save

### 3. Firestore Rules

Cho phép user đọc/ghi collection "books":

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /books/{document=**} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null;
    }
    match /tags/{document=**} {
      allow read: if true;
    }
  }
}
```

### 4. Thêm Tags vào Database

Vào Firestore > Collection **tags** > Thêm documents:

```json
{
  "id": "tag_001",
  "name": "Lập Trình",
  "description": "Sách về lập trình",
  "color": "#6200EE"
}
```

---

## 📝 Quy Trình Upload

### 1. Nhấn Nút Upload
```
Menu ☰ → Upload Sách
```

### 2. Điền Thông Tin (Bắt Buộc)
- **Tên sách**: "Lập Trình Android Kotlin"
- **Tác giả**: "Nguyễn Văn A"
- **Mô tả sách**: "Học lập trình Android với Kotlin"
- **Tags**: Chọn ít nhất 1 tag (Checkbox)
- **File**: Chọn file PDF, EPUB, ...

### 3. Upload
- Nhấn "Upload Sách"
- Chờ file tải lên Cloudinary
- Dữ liệu lưu vào Firestore

### 4. Xác Nhận
- ✅ Upload thành công
- Sách có status: "pending"
- Chờ admin phê duyệt

---

## 📊 Dữ Liệu Trong Database

```json
{
  "id": "doc_id",
  "title": "Lập Trình Android Kotlin",
  "author": "Nguyễn Văn A",
  "description": "Học lập trình Android với Kotlin",
  "language": "Tiếng Việt",
  "tags": ["Lập Trình", "Android"],
  "cover": "",
  "url": "https://res.cloudinary.com/.../file.pdf",
  "status": "pending",
  "uploadedAt": 1705004400000,
  "sellerId": "user_uid",
  "uploadedBy": "user@email.com"
}
```

---

## 🔒 Bảo Mật

- ✅ API keys trong `cloudinary.properties` (không commit)
- ✅ Upload unsigned (không cần secret key)
- ✅ Firestore rules: Chỉ user đăng nhập mới upload
- ✅ Status mặc định "pending" (cần phê duyệt)

---

## ⚙️ Dependencies Cần Thiết

Đã thêm vào `build.gradle.kts`:
- Retrofit + Gson converter
- OkHttp
- Firebase (Auth, Firestore)
- Coroutines
- Material Design (TextInputLayout, RecyclerView)

---

## 🧪 Test

1. Build & Run app
2. Đăng nhập
3. Menu → Upload Sách
4. Điền đầy đủ thông tin
5. Chọn file test
6. Nhấn Upload
7. Kiểm tra:
   - Cloudinary dashboard: folder `finallib/books`
   - Firestore: collection `books` có document mới
   - Status: `"pending"`

---

## 🎯 Quy Trình Phê Duyệt (Admin)

**Sẽ cài đặt sau:**
- Admin page để xem sách pending
- Approve/Reject sách
- Publish sách lên hệ thống

---

## 📱 File Quan Trọng

| File | Mục Đích |
|------|---------|
| `cloudinary.properties` | Config API keys |
| `CloudinaryConfig.kt` | Load config từ file |
| `CloudinaryUploadService.kt` | Upload HTTP |
| `UploadBookDialog.kt` | Dialog UI + logic |
| `TagAdapter.kt` | Hiển thị tags checklist |
| `MainActivity.kt` | Khởi tạo + show dialog |

---

**Chúc mừng! Upload sách đã sẵn sàng 🎉**
