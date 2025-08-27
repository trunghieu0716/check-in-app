# Hướng dẫn sửa lỗi Check-in App

## 1. Lỗi Firestore Index (FAILED_PRECONDITION)

### Cách 1: Sử dụng Firebase Console (Dễ nhất)
1. Truy cập link trong lỗi: https://console.firebase.google.com/v1/r/firestore/data/~2Fattendance~2F3kLiSWM8U3hwa6CTlo0W
2. Hoặc vào Firebase Console > Firestore Database > Indexes
3. Click "Create Index" 
4. Tạo composite index với:
   - Collection ID: `attendance`
   - Field 1: `userId` (Ascending)
   - Field 2: `timestamp` (Descending)
   - Query scope: Collection

### Cách 2: Sử dụng Firebase CLI
```bash
# Cài đặt Firebase CLI (nếu chưa có)
npm install -g firebase-tools

# Đăng nhập
firebase login

# Khởi tạo project (nếu chưa có)
firebase init firestore

# Deploy indexes
firebase deploy --only firestore:indexes
```

## 2. Lỗi Location null

### Những thay đổi đã được thực hiện:

1. **Cải thiện logic lấy location:**
   - Thử `getLastLocation()` trước
   - Nếu null, request fresh location
   - Timeout 20 giây để tránh chờ vô tận
   - Cho phép lưu record ngay cả khi không có location

2. **Thêm location settings check:**
   - Kiểm tra GPS settings trước khi request location
   - Hướng dẫn user bật GPS nếu cần

3. **Cleanup resources:**
   - Dừng location updates khi không cần
   - Remove callback trong onPause()

### Kiểm tra và thử nghiệm:

1. **Đảm bảo GPS được bật:**
   - Settings > Location > On
   - Mode: High accuracy

2. **Test location permissions:**
   - Mở app, cho phép location permission
   - Thử check-in/check-out

3. **Xem logs:**
   - Mở Android Studio > Logcat
   - Filter by tag: "MainActivity"
   - Kiểm tra location logs

## 3. Firestore Security Rules

File `firestore.rules` đã được tạo với quy tắc bảo mật cơ bản:
- User chỉ có thể đọc/ghi document của chính họ
- Attendance records được bảo vệ theo userId

Deploy rules:
```bash
firebase deploy --only firestore:rules
```

## 4. Troubleshooting

### Nếu location vẫn null:
1. Kiểm tra permissions trong app settings
2. Restart app sau khi cấp permissions
3. Thử di chuyển ra ngoài trời
4. Check device location services

### Nếu Firestore vẫn lỗi:
1. Đợi vài phút sau khi tạo index
2. Kiểm tra project ID đúng chưa
3. Xem Firebase Console > Usage để confirm index được tạo

### Debugging tips:
1. Enable Firebase debug logs
2. Check network connection
3. Verify google-services.json đúng project
