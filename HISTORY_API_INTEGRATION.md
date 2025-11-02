# 🎵 Music History Flow - API Integration Complete

## 📋 Tóm Tắt Thay Đổi

Đã cập nhật toàn bộ luồng lưu lịch sử bài hát theo API specification mới. Các thay đổi chính:

### ⭐ Điểm Nổi Bật
- **Sử dụng Auto-Save API** (khuyến nghị): `POST /api/spotify/tracks/{track_id}/play`
- Backend tự động lấy thông tin từ Spotify và lưu lịch sử
- Có fallback về manual API nếu auto-save thất bại
- Đầy đủ validation: token, thời gian phát, duplicate prevention

---

## 🔧 Các File Đã Thay Đổi

### 1. **SpotifyApi.java** - API Endpoints
```java
// ⭐ NEW: Auto-save API (recommended)
@POST("/api/spotify/tracks/{track_id}/play")
Call<PlayTrackResponse> playTrack(
    @Path("track_id") String trackId,
    @Query("play_duration_seconds") int playDurationSeconds
);

// Manual fallback
@POST("/api/history")
Call<Void> addHistoryRecord(@Body HistoryRecordRequest historyRecordRequest);

// Get history with optional filter
@GET("/api/history")
Call<HistoryResponse> getHistory(
    @Query("limit") int limit,
    @Query("offset") int offset,
    @Query("track_id") String trackId  // NEW: optional filter
);

// NEW: Stats endpoint
@GET("/api/history/stats")
Call<HistoryStatsResponse> getHistoryStats();
```

**Thay đổi:**
- ✅ Thêm endpoint `playTrack()` cho auto-save
- ✅ Đổi path từ `/history` → `/api/history` 
- ✅ Thêm optional `track_id` filter cho `getHistory()`
- ✅ Thêm endpoint `getHistoryStats()`

---

### 2. **PlayTrackResponse.java** - NEW DTO
Response cho endpoint auto-save API:
```java
{
  "code": 200,
  "message": "Track played and saved to history",
  "data": {
    "id": "3n3Ppam7vgaVa1iaRUc9Lp",
    "name": "Mr. Brightside",
    "artists": ["The Killers"],
    "album": "Hot Fuss",
    "duration_ms": 222200,
    ...
  }
}
```

---

### 3. **HistoryStatsResponse.java** - NEW DTO
Response cho endpoint thống kê:
```java
{
  "code": 200,
  "data": {
    "total_plays": 150,
    "unique_tracks": 45,
    "top_tracks": [...],
    "top_artists": [...]
  }
}
```

---

### 4. **HistoryManager.java** - Logic Lưu Lịch Sử

**Thêm method mới:**
```java
public void saveHistoryAutoSave(
    SpotifyApi spotifyApi,
    String trackId,
    int playDurationSeconds,
    HistorySaveCallback callback
)
```

**Flow:**
1. ✅ Check settings enabled
2. ✅ Validate track ID
3. ✅ Check minimum duration (default 15s)
4. ✅ Check duplicate trong 5 phút
5. ✅ Gọi API auto-save
6. ✅ Log kết quả

**Settings có sẵn:**
- `setHistoryEnabled(boolean)` - Bật/tắt
- `setMinPlayDuration(int)` - Thời gian tối thiểu (giây)
- `setDedupeWindow(int)` - Khoảng thời gian tránh duplicate (giây)

---

### 5. **PlayerActivity.java** - Tích Hợp Auto-Save

**Method chính: `saveHistoryOnComplete()`**

**Flow mới:**
```java
1. Validate song data, token, và thời gian phát
2. Calculate actual play duration
3. ⭐ Gọi historyManager.saveHistoryAutoSave()
   └─ Nếu thành công → ✅ Done
   └─ Nếu thất bại (không phải 401) → 🔄 Fallback về manual save
   └─ Nếu 401 → Redirect to login
```

**Triggers (khi nào lưu lịch sử):**
- ✅ Bài hát phát xong (onCompletion)
- ✅ Skip sang bài khác (playNext/playPrevious)
- ✅ Tắt app (onStop/onDestroy)

**Duplicate prevention:**
- Track `currentPlayingTrackId` để chỉ save 1 lần
- Reset về `null` sau khi save
- HistoryManager check duplicate trong dedupe window

---

### 6. **HistoryActivity.java** - Hiển Thị Lịch Sử

**Thay đổi:**
```java
// OLD
spotifyApi.getHistory(20, 0).enqueue(...)

// NEW - support optional filter
spotifyApi.getHistory(20, 0, null).enqueue(...)
```

Pass `null` để lấy tất cả, hoặc truyền `track_id` để filter theo bài hát cụ thể.

---

## 🎯 API Endpoints Sử dụng

### Base URL
```
http://192.168.30.28:5030
```

### Authentication
```java
Authorization: Bearer {token}
```

### 1. Auto-Save (Primary Method) ⭐
```
POST /api/spotify/tracks/{track_id}/play?play_duration_seconds=150
```
- Tự động lấy info từ Spotify
- Tự động lưu vào lịch sử
- Trả về thông tin track đầy đủ

### 2. Manual Save (Fallback)
```
POST /api/history
Body: {
  "track_id": "3n3Ppam7vgaVa1iaRUc9Lp",
  "track_name": "Mr. Brightside",
  "artist_name": "The Killers",
  "album": "Hot Fuss",
  "duration_ms": 222200,
  "play_duration_seconds": 150
}
```

### 3. Get History
```
GET /api/history?limit=20&offset=0&track_id=optional
```

### 4. Get Stats
```
GET /api/history/stats
```

---

## ✅ Checklist Hoàn Thành

### DTOs
- [x] `PlayTrackResponse.java` - Auto-save API response
- [x] `HistoryStatsResponse.java` - Stats API response
- [x] `HistoryRecordRequest.java` - Already updated (6 fields)
- [x] `HistoryResponse.java` - Already updated

### API Interface
- [x] `SpotifyApi.java` - All endpoints updated
  - [x] `playTrack()` - Auto-save
  - [x] `addHistoryRecord()` - Manual fallback
  - [x] `getHistory()` - With optional filter
  - [x] `getHistoryStats()` - New stats endpoint

### Business Logic
- [x] `HistoryManager.java`
  - [x] `saveHistoryAutoSave()` - Primary method
  - [x] `saveHistory()` - Fallback method
  - [x] Settings management
  - [x] Duplicate prevention

### UI Integration
- [x] `PlayerActivity.java`
  - [x] Use auto-save API
  - [x] Fallback to manual
  - [x] Token validation
  - [x] Duplicate prevention
  - [x] Lifecycle-aware saves
- [x] `HistoryActivity.java`
  - [x] Support optional track filter

---

## 🔍 Testing Guide

### 1. Test Auto-Save Flow
```java
// Khi phát bài hát
1. Play a song for >15 seconds
2. Skip or let it complete
3. Check Logcat for:
   ✅ "📤 Auto-saving history for track: {id} (played {X}s)"
   ✅ "✅ History auto-saved successfully: {name}"
```

### 2. Test Manual Fallback
```java
// Khi auto-save thất bại
1. Disconnect internet briefly
2. Play song >15s and complete
3. Check Logcat for:
   ❌ "❌ Failed to auto-save history"
   🔄 "🔄 Trying manual fallback for: {name}"
   ✅ "✅ History saved via manual fallback"
```

### 3. Test Duplicate Prevention
```java
1. Play same song 2 times within 5 minutes
2. Check Logcat:
   ✅ First time: "✅ History auto-saved successfully"
   ⏭️ Second time: "🔁 Track was recently played (within dedupe window), skipping"
```

### 4. Test Token Expiration
```java
1. Use expired token
2. Play song >15s
3. Check:
   ⚠️ "⚠️ No valid token, cannot save history"
   📱 "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại"
```

---

## 📊 Logcat Filters

### View all history logs:
```
tag:PlayerActivity OR tag:HistoryManager
```

### View only success:
```
tag:HistoryManager "✅"
```

### View errors:
```
tag:HistoryManager "❌"
```

---

## 🚀 Next Steps (Optional)

### 1. Settings UI
Tạo giao diện cho user điều khiển settings:
```java
// Backend đã sẵn sàng trong HistoryManager
- Enable/Disable history tracking
- Min play duration (5-60 seconds)
- Dedupe window (1-10 minutes)
```

### 2. History Stats Screen
Hiển thị thống kê:
```java
spotifyApi.getHistoryStats().enqueue(...)
// Show: total plays, unique tracks, top tracks, top artists
```

### 3. Offline Queue (Future)
Lưu lịch sử vào local DB khi offline, sync khi online lại.

---

## 📝 Summary

### Điểm Mạnh
✅ Sử dụng auto-save API (backend tự lấy info từ Spotify)  
✅ Có fallback về manual API  
✅ Đầy đủ validation và duplicate prevention  
✅ Lifecycle-aware (save đúng lúc)  
✅ Token validation (tránh 401)  
✅ Comprehensive logging  

### API Changes
| Endpoint | Old | New |
|----------|-----|-----|
| Add history | `POST /history` | `POST /api/history` |
| Get history | `GET /history` | `GET /api/history?track_id=optional` |
| Stats | ❌ Not exist | `GET /api/history/stats` |
| Auto-save | ❌ Not exist | `POST /api/spotify/tracks/{id}/play` ⭐ |

### Code Flow
```
PlayerActivity.saveHistoryOnComplete()
  ↓
HistoryManager.saveHistoryAutoSave()  ⭐ Primary
  ↓ (if fail)
HistoryManager.saveHistory()  🔄 Fallback
```

---

## 🎉 Kết Luận

Toàn bộ luồng lịch sử đã được cập nhật theo API specification mới. Code đã sẵn sàng để build và test trên device!

### Build Command
```powershell
.\gradlew.bat clean assembleDebug
```

### Test Checklist
- [ ] Build thành công không có lỗi
- [ ] Phát bài hát >15s → lịch sử được lưu
- [ ] Phát bài hát <15s → không lưu
- [ ] Phát bài giống 2 lần liên tiếp → chỉ lưu 1 lần
- [ ] Token hết hạn → redirect to login
- [ ] Xem lịch sử trong HistoryActivity → hiển thị đúng
