# Tài liệu nguồn theo yêu cầu đề tài

Cập nhật: 2026-08-11

Bản này gom **nguồn có thể trích dẫn** cho luận văn / thuyết minh đồ án: nhận diện cây thông thường + cây thuốc hay gặp, badge “Cây thuốc”, chi tiết (mô tả, công dụng), YouTube / tài liệu trồng–chăm sóc, sâu bệnh + xử lý thân thiện môi trường, cảnh báo nấm độc / nhựa độc / cây độc + sơ cứu, bài thuốc dân gian đơn giản, nhật ký trồng cây.

> **Lưu ý dùng trong đồ án:** mọi công dụng, bài thuốc, sơ cứu chỉ mang tính **tham khảo giáo dục**. Không thay thế Dược sĩ / bác sĩ / 115. App đã gắn disclaimer này trên màn chi tiết.

---

## 1. Yêu cầu đề tài → nguồn nào dùng

| Yêu cầu đề tài | App đang có | Nguồn nên trích (ưu tiên) |
|---|---|---|
| Chụp ảnh → tên cây | Scan Render `POST /api/v1/plant/scan` | Tên khoa học đối chiếu Wikipedia / POWO / Dược điển |
| Badge **Cây thuốc** | KB local + catalog Dược điển (~355) | Chuyên luận DDVN V trên [duocdienvietnam.com](https://duocdienvietnam.com/duoc-lieu/) |
| Chi tiết: mô tả, công dụng | `thesis_plants.json` (14 cây) | DDVN V + Đỗ Tất Lợi + WHO monograph |
| AI dẫn YouTube / tài liệu trồng–chăm | Link search YouTube + Wikipedia | Video giáo dục (chọn tay) + khuyến nông / sách trồng |
| Nấm độc + tiếng cảnh báo | Local `R.raw.alert_toxic` + KB `nam-doc` | CDC MMWR + WHO Natural toxins |
| Sâu bệnh + ảnh + cách chữa thân thiện MT | Ảnh Wikimedia + text KB | CABI / FAO IPM / Wikimedia Commons |
| Nhựa độc, cây độc, sơ cứu | Trầu bà, xấu hổ, nấm | ASPCA + CDC + 115 |
| Bài thuốc dân gian đơn giản | `folk_remedies.json` | Đỗ Tất Lợi + Tradimed + DDVN (vị thuốc, **không** kê đơn) |
| Nhật ký trồng (ngày bắt đầu, đặc điểm, timeline kiểu Zalo) | SharedPreferences trên máy | Không cần nguồn y khoa — mô tả chức năng đồ án |
| Cây thuốc thường gặp / QR | Local catalog, **không gọi Render** | Cùng KB + chuyên luận bên dưới |

**Hiện trạng data:** Render production **không** có API danh mục / QR / bài thuốc. App đọc local:

- `app/src/main/assets/thesis_plants.json` — 14 cây demo
- `app/src/main/assets/folk_remedies.json` — bài thuốc dân gian
- Server local: `duocdien_catalog.json` (~355 dược liệu) dùng enrich khi scan (nếu chạy FastAPI local)

---

## 2. Nguồn khung (nên ghi vào tài liệu tham khảo)

### 2.1. Chính thống — ưu tiên trích luận văn

| # | Nguồn | Loại | URL |
|---|---|---|---|
| 1 | Dược điển Việt Nam V — danh mục dược liệu (online) | Quy chuẩn quốc gia | https://duocdienvietnam.com/duoc-lieu/ |
| 2 | Danh mục chuyên luận DDVN V | Mục lục | https://duocdienvietnam.com/danh-muc-cac-chuyen-luan/ |
| 3 | Quyết định 5358/QĐ-BYT ngày 28/11/2017 ban hành DDVN V (hiệu lực 01/07/2018) | Văn bản Bộ Y tế | https://luatvietnam.vn/y-te/quyet-dinh-5358-qd-byt-2017-ve-viec-ban-hanh-duoc-dien-viet-nam-v-318094-d1.html |
| 4 | WHO monographs on selected medicinal plants, Vol. 1 (Aloe, Curcuma, Zingiber…) | Tổ chức Y tế Thế giới | https://iris.who.int/handle/10665/42052 |
| 5 | WHO monographs Vol. 2 (Mentha / bạc hà) | WHO | https://iris.who.int/bitstream/handle/10665/42052/9241545372.pdf |
| 6 | WHO — Natural toxins in food (nấm độc) | Fact sheet | https://www.who.int/news-room/fact-sheets/detail/natural-toxins-in-food/ |
| 7 | CDC MMWR — ngộ độc *Amanita phalloides* | Y tế công cộng | https://www.cdc.gov/mmwr/volumes/66/wr/mm6621a1.htm |
| 8 | CDC MMWR — *Amanita* (cập nhật) | Y tế công cộng | https://www.cdc.gov/mmwr/volumes/75/wr/mm7520a2.htm |
| 9 | ASPCA — Golden Pothos / Devil’s Ivy (nhựa oxalate) | Độc thực vật | https://www.aspca.org/pet-care/aspca-poison-control/toxic-and-non-toxic-plants/golden-pothos |
| 10 | Viện Dược liệu — *Danh lục cây thuốc Việt Nam* | Sách / cổng viện | http://vienduoclieu.org.vn/tin-tuc/Vien_Duoc_lieu_xuat_ban_cuon_sach_Danh_luc_cay_thuoc_Viet_Nam_358 |
| 11 | Đỗ Tất Lợi — *Những cây thuốc và vị thuốc Việt Nam* | Sách kinh điển | https://vnras.com/nhung-cay-thuoc-va-vi-thuoc-viet-nam-do-tat-loi/ |
| 12 | Tra cứu Đỗ Tất Lợi (online) | Phụ | http://dotatloi.com/ |

### 2.2. Phụ — mô tả hình thái, ảnh bệnh, video

| Nguồn | Dùng cho | URL |
|---|---|---|
| Wikipedia / Wikimedia Commons | Tên khoa học, ảnh sâu bệnh (CC) | https://commons.wikimedia.org/ |
| Plants of the World Online (Kew) | Đồng danh khoa học | https://powo.science.kew.org/ |
| YouTube search (mở trong app) | Video trồng / chăm / phân biệt nấm | `https://www.youtube.com/results?search_query=...` |
| Tradimed (cây thuốc dân tộc) | Bổ sung YHCT | https://tradimed.org/ |

**Video YouTube:** không gắn sẵn `watch?v=` cố định trong docs này (dễ gỡ, bản quyền, nội dung lệch). Trong đồ án: chọn 1 video / cây, ghi rõ kênh + ngày xem, rồi thay vào `citations`. App hiện mở **trang tìm kiếm chủ đề** — vẫn đúng yêu cầu “dẫn nguồn YouTube”.

---

## 3. Từng cây trong KB đồ án (14 cây)

Gắn được chuyên luận DDVN thì dùng **URL chuyên luận**, không chỉ trang danh mục chung.

| ID / QR | Tên VN | Tên khoa học | Cây thuốc? | Chuyên luận / nguồn chính | Video search | Ghi chú data |
|---|---|---|---|---|---|---|
| `dinh-lang` | Đinh lăng | *Polyscias fruticosa* | Có | https://duocdienvietnam.com/dinh-lang-re/ (Radix Polysciacis) | `cách trồng đinh lăng chăm sóc` | Có trong DDVN V |
| `gung` | Gừng | *Zingiber officinale* | Có | https://duocdienvietnam.com/gung-than-re/ + WHO Rhizoma Zingiberis | `cách trồng gừng tại nhà` | Có DDVN + WHO |
| `nghe` | Nghệ vàng | *Curcuma longa* | Có | https://duocdienvietnam.com/nghe-than-re/ + WHO Rhizoma Curcumae Longae | `cách trồng nghệ` | Có DDVN + WHO |
| `bac-ha` | Bạc hà | *Mentha arvensis* | Có | https://duocdienvietnam.com/bac-ha-herba-menthae/ + WHO Folium Menthae (Vol. 2) | `cách trồng bạc hà` | Trẻ nhỏ hạn chế tinh dầu |
| `tia-to` | Tía tô | *Perilla frutescens* | Có | Lá https://duocdienvietnam.com/tia-to-la/ · Quả https://duocdienvietnam.com/tia-to-qua/ · Thân https://duocdienvietnam.com/tia-to-than/ | `cách trồng tía tô` | 3 chuyên luận |
| `la-lot` | Lá lốt | *Piper lolot* | Có | https://duocdienvietnam.com/la-lot/ | `cách trồng lá lốt` | Có DDVN |
| `nha-dam` | Nha đam / Lô hội | *Aloe vera* | Có | Nhựa khô: https://duocdienvietnam.com/lo-hoi-nhua/ + WHO Aloe / Aloe vera gel | `cách trồng nha đam aloe vera` | DDVN mô tả **nhựa cô đặc**, gel tươi = dân gian / WHO gel |
| `sa` | Sả | *Cymbopogon* spp. | Có (gia vị / tinh dầu) | Không thấy chuyên luận riêng trên catalog 355 — dùng Đỗ Tất Lợi / Wikipedia / Tradimed | `cách trồng sả` | **Thiếu chuyên luận DDVN** — ghi rõ trong luận văn |
| `xau-ho` | Xấu hổ / Trinh nữ | *Mimosa pudica* | Tham khảo | Không có chuyên luận DDVN trong catalog — Wikipedia + nghiên cứu dân gian | `cây xấu hổ mimosa pudica` | Dùng STEM / quan sát; **không** tự trị bệnh |
| `nam-doc` | Nấm nghi độc | *Amanita* sp. (suspect) | Không | WHO toxins + CDC MMWR + Wikipedia Mushroom poisoning | `phan biet nam doc amanita` | Luôn cảnh báo + âm thanh |
| `trau-ba` | Trầu bà / Pothos | *Epipremnum aureum* | Không | ASPCA Golden Pothos (oxalate) | `chăm sóc trầu bà pothos` | Nhựa độc — trẻ / thú cưng |
| `duong-si` | Dương sỉ | *Nephrolepis* / fern | Không | Wikipedia / hướng dẫn cây cảnh | `cách chăm sóc dương sỉ trong nhà` | Cây cảnh thường gặp |
| `mai-van-phuc` | Mai vạn phúc | *Adenium obesum* | Không | Nhựa độc (cảnh) — cần cảnh báo | `chăm sóc sứ thái adenium` | Nhựa độc |
| `trau-xanh` | Trầu xanh | *Dracaena* spp. | Không | Hướng dẫn cây cảnh | `chăm sóc cây trầu xanh dracaena` | Cây cảnh |

**Catalog scan (server local):** ~355 chuyên luận đã scrape từ https://duocdienvietnam.com/duoc-lieu/ → `app/data/duocdien_catalog.json`. Đây là nguồn **mở rộng tên cây thuốc** khi nhận diện, không phải 14 cây demo.

---

## 4. Công dụng / mô tả — lấy gì, không lấy gì

**Được lấy (trích dẫn):**

- Mô tả thực vật, bộ phận dùng, tên Latinh trong chuyên luận DDVN V.
- Chỉ định YHCT ghi trong chuyên luận (ví dụ Gừng: ôn trung, tán hàn; Nghệ: hành khí, phá huyết) — ghi *“theo Dược điển / YHCT”*.
- WHO: công dụng đã được monograph hóa (gừng, nghệ, lô hội, bạc hà).

**Không lấy làm “thuốc chữa bệnh” trong app:**

- Blog mẹo vặt, bài báo giải trí, comment YouTube.
- Liều dùng tự ý cho trẻ em, phụ nữ mang thai.
- Bài thuốc phức tạp nhiều vị chưa kiểm chứng.

Câu nên ghi dưới mọi màn chi tiết (đã có trong app):

> Chỉ tham khảo giáo dục / y học dân gian. Không tự điều trị bệnh nặng. Đối chiếu Dược điển: https://duocdienvietnam.com/duoc-lieu/

---

## 5. Trồng – chăm sóc – mùa vụ

Nguồn **kỹ thuật trồng** (không phải Dược điển):

| Cây | Gợi ý tài liệu trồng (phụ) |
|---|---|
| Đinh lăng | Giâm hom 15–25 cm, đất tơi xốp thoát nước, trồng đầu mùa mưa — bài kỹ thuật vườn / khuyến nông (vd. hướng dẫn giâm hom lấy củ) |
| Gừng, nghệ | Trồng củ/thân rễ, đất cao ráo, tránh úng; thu hoạch 8–10 tháng |
| Sả, tía tô, bạc hà, lá lốt | Tách khóm / giâm; nắng nhẹ–trung bình; cắt tỉa thường xuyên |
| Nha đam | Đất cát pha, ít nước, nắng nhiều |
| Cây cảnh (trầu bà, dương sỉ, sứ) | Ánh sáng gián tiếp; trầu bà: giâm nước |

Trong luận văn có thể viết: *phần chăm sóc tổng hợp từ khuyến nông + tài liệu cây cảnh; phần dược tính lấy từ DDVN/WHO.*

YouTube (copy vào citations hoặc thuyết minh):

```
https://www.youtube.com/results?search_query=cách+trồng+đinh+lăng+chăm+sóc
https://www.youtube.com/results?search_query=cách+trồng+gừng+tại+nhà
https://www.youtube.com/results?search_query=cách+trồng+nghệ
https://www.youtube.com/results?search_query=cách+trồng+nha+đam+aloe+vera
https://www.youtube.com/results?search_query=phan+biet+nam+doc+amanita
```

---

## 6. Nấm độc, nhựa độc, cây độc + sơ cứu

### 6.1. Nấm độc (bắt buộc cảnh báo + âm thanh)

| Nội dung đề tài | Nguồn |
|---|---|
| Không ăn nấm hoang khi chưa chuyên gia định loại | WHO Natural toxins in food |
| Độc tố không mất khi nấu / gọt | WHO cùng trang |
| Triệu chứng có thể chậm 6–24 giờ, độc gan | CDC MMWR *A. phalloides* |
| Sơ cứu app: gọi **115**, không tự gây nôn nếu chưa được hướng dẫn, mang mẫu nấm tới y tế, không uống rượu | CDC: bù dịch / xử trí tại cơ sở y tế — app chỉ hướng dẫn **ban đầu** |

Link:

- https://www.who.int/news-room/fact-sheets/detail/natural-toxins-in-food/
- https://www.cdc.gov/mmwr/volumes/66/wr/mm6621a1.htm
- https://en.wikipedia.org/wiki/Mushroom_poisoning

App: scan ra nấm độc / `nam-doc` → banner đỏ + `ToxicAlertPlayer` (`res/raw/alert_toxic.wav`). Không còn mục “Nấm độc” trên Home.

### 6.2. Nhựa độc / cây độc

| Cây | Cơ chế | Nguồn | Sơ cứu giáo dục |
|---|---|---|---|
| Trầu bà (*E. aureum*) | Tinh thể calcium oxalate không tan | ASPCA Golden Pothos / Devil’s Ivy | Súc miệng, uống nước; khó thở → y tế |
| Sứ / Mai vạn phúc (*Adenium*) | Nhựa độc (cảnh) | Tài liệu cây cảnh + cảnh báo chung | Không nuốt, rửa da; thú cưng tránh |
| Xấu hổ | Thận trọng dược liệu, không “an toàn tuyệt đối” | Không có chuyên luận DDVN | Không tự sắc uống liều cao |

ASPCA: https://www.aspca.org/pet-care/aspca-poison-control/toxic-and-non-toxic-plants/golden-pothos

---

## 7. Sâu bệnh + cách chữa thân thiện môi trường

Đề tài cần **ảnh minh họa** + **xử lý ít hóa chất**.

| Bệnh / sâu (trong KB) | Ảnh (Wikimedia, dùng được) | Hướng xử lý thân thiện |
|---|---|---|
| Rệp sáp | https://upload.wikimedia.org/wikipedia/commons/thumb/8/86/Icerya_purchasi_on_orange.jpg/320px-Icerya_purchasi_on_orange.jpg | Rửa nước, dầu neem, xà phòng nông nghiệp loãng, thiên địch |
| Rệp | https://upload.wikimedia.org/wikipedia/commons/thumb/1/19/Aphis_fabae01.jpg/320px-Aphis_fabae01.jpg | Tia nước, dầu neem |
| Đốm lá | https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Leaf_spot_on_tomato.jpg/320px-Leaf_spot_on_tomato.jpg | Cắt lá bệnh, thông gió, baking soda loãng / *Bacillus* |
| Phấn trắng | https://upload.wikimedia.org/wikipedia/commons/thumb/5/57/Powdery_mildew_on_cucumber_leaves.jpg/320px-Powdery_mildew_on_cucumber_leaves.jpg | Giảm ẩm, cắt lá, chế phẩm sinh học |
| Thối rễ | https://upload.wikimedia.org/wikipedia/commons/thumb/f/f8/Root_rot_on_orchid.jpg/320px-Root_rot_on_orchid.jpg | Giảm tưới, thay đất thoát nước |

Nguồn khung IPM (ghi tài liệu tham khảo): FAO Integrated Pest Management — https://www.fao.org/pest-and-pesticide-management/ipm/integrated-pest-management/en/

---

## 8. Bài thuốc dân gian (mục riêng trong app)

Chỉ các bài **đơn giản, phổ biến**, gắn vị thuốc có chuyên luận:

| Bài trong app | Vị chính | Nguồn vị thuốc | Không được viết |
|---|---|---|---|
| Nước sả – gừng ấm | Gừng DDVN; sả (dân gian) | https://duocdienvietnam.com/gung-than-re/ | “Chữa khỏi cảm / COVID” |
| Nghệ + mật ong | Nghệ | https://duocdienvietnam.com/nghe-than-re/ | “Chữa loét dạ dày” |
| Gel nha đam đắp ngoài | Lô hội / WHO gel | https://duocdienvietnam.com/lo-hoi-nhua/ | Đắp vết bỏng nặng / loét sâu |
| Trà bạc hà | Bạc hà | https://duocdienvietnam.com/bac-ha-herba-menthae/ | Cho trẻ nhũ nhi |
| Nước tía tô | Tía tô (lá) | https://duocdienvietnam.com/tia-to-la/ | Thay thuốc kê đơn |

Sách nền cho chương “y học dân gian”: Đỗ Tất Lợi; Tradimed (vd. Gừng https://tradimed.org/phat-trien-y-hoc-dan-toc-ban-dia/cay-thuoc-quy/danh-muc-cay-thuoc/zingiber-officinale-roscoe-gung.html ).

---

## 9. Nhật ký trồng cây — không thiếu “data y khoa”

Chức năng này là **sổ tay học sinh**, lưu máy:

- Ngày bắt đầu trồng
- Điểm đặc biệt (lá kép, dễ giâm…)
- Timeline + ảnh (kiểu feed Zalo)

Không cần API / Dược điển. Thuyết minh đồ án: mô tả UX + lưu local (`ThesisLocalStore`). Có thể dẫn 1 dòng về giáo dục STEM (quan sát sinh trưởng).

---

## 10. Việc nên làm tiếp nếu muốn “đủ data hơn”

Data đang mỏng ở **nội dung chi tiết**, không phải ở khung nguồn.

1. Đổi mọi citation “danh mục chung” → **URL chuyên luận** cột §3 (app + `plants_kb.py`).
2. Chọn **1 video YouTube / cây**, ghi kênh + ngày, thay search URL.
3. Bổ sung 10–20 cây thuốc hay gặp nữa từ catalog 355 (mã đề, kinh giới, hương nhu, húng chanh, riềng…) — mô tả lấy đúng `description` trong `duocdien_catalog.json`.
4. Sả / xấu hổ: ghi rõ *không có chuyên luận DDVN trong catalog hiện có*.
5. In / photocopy bìa + trang chuyên luận DDVN V (thư viện) để nộp phụ lục nếu hội đồng yêu cầu bản giấy.

---

## 11. Gợi ý trích dẫn (kiểu luận văn)

- Hội đồng Dược điển Việt Nam (2017). *Dược điển Việt Nam V*. Bộ Y tế. Tra cứu online: https://duocdienvietnam.com/duoc-lieu/
- Bộ Y tế (2017). Quyết định 5358/QĐ-BYT ban hành Dược điển Việt Nam V.
- Đỗ Tất Lợi. *Những cây thuốc và vị thuốc Việt Nam*. Nxb Y học (nhiều lần tái bản).
- Viện Dược liệu. *Danh lục cây thuốc Việt Nam*. Nxb Khoa học và Kỹ thuật.
- WHO (1999). *WHO monographs on selected medicinal plants*, Vol. 1. Geneva.
- WHO (2002). *WHO monographs on selected medicinal plants*, Vol. 2. Geneva.
- WHO. *Natural toxins in food*. Fact sheet.
- CDC (2017, 2026). *Amanita* mushroom poisonings. *MMWR*.
- ASPCA Animal Poison Control. *Golden Pothos (Epipremnum aureum)*.

---

## 12. File data tương ứng trong project

| File | Việc |
|---|---|
| `app/src/main/assets/thesis_plants.json` | 14 cây hiển thị QR / catalog / enrich scan |
| `app/src/main/assets/folk_remedies.json` | Mục bài thuốc dân gian |
| `/Users/tt/web/server/app/data/plants_kb.py` | Nguồn gốc KB (nếu chạy server local) |
| `/Users/tt/web/server/app/data/duocdien_catalog.json` | ~355 chuyên luận scrape |
| `docs/SCAN_FLOW.md` | Flow chụp ảnh → server nhận diện |

Scan tên cây vẫn có thể dùng `https://identifierapp-server-hmvz.onrender.com/` (chỉ `POST .../plant/scan`). **Không** dùng host này cho danh mục, QR, bài thuốc — các phần đó đọc local như trên.
