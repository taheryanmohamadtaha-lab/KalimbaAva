# افزودن آهنگ یا درس (بدون تغییر در موتور برنامه)

## فرمت بسته‌ی آهنگ
فایل JSON با ساختار زیر. می‌توانی آن را در پوشه‌ی `app/src/main/assets/songs/` بگذاری، یا در برنامه از بخش «آهنگ‌ها ← +» وارد کنی
(پس از وارد کردن، برنامه را یک‌بار ببند و دوباره باز کن).

```json
{
  "pack_id": "my_pack", "pack_version": 1, "schema_version": 1,
  "title_fa": "بسته‌ی من",
  "songs": [{
    "id": "my_001",
    "title_fa": "ملودی من", "title_en": "My Melody",
    "composer": "نام آهنگساز", "artist": "",
    "category": "iranian_happy",
    "difficulty": "beginner",
    "tempo": 90, "time_signature": "4/4",
    "notes": "C4:1 D4:1 E4:2 | C4+E4+G4:4",
    "license": "original_cc0",
    "source": "توضیح منبع/مجوز"
  }]
}
```

### دستور نوشتن نت‌ها (`notes`)
- `C4:1` نت دو اُکتاو ۴ به مدت ۱ ضرب (سیاه)؛ `E5:.5` نیم‌ضرب؛ `C4:1/3` یک‌سوم ضرب
- `R:2` سکوت دو ضربی
- `C4+E4+G4:2` آکورد (نت‌های هم‌زمان)
- `|` فقط برای خوانایی است
- نت‌ها باید روی کالیمبای ۱۷ کلید دو ماژور باشند: فقط نت‌های طبیعی از C4 تا E6
- تبلچر، مدت، تمپو و بخش‌بندی تمرین خودکار از همین رشته ساخته می‌شود

### مقادیر مجاز
- `category`: `iranian_nostalgia iranian_traditional iranian_folk iranian_happy iranian_romantic iranian_calm iranian_kids iranian_film classical folk popular relaxing movie beginner advanced`
- `difficulty`: `beginner intermediate advanced professional`
- `license`: `public_domain original_cc0 cc_by licensed`
- `tempo`: ۲۰ تا ۲۴۰

آهنگ نامعتبر نادیده گرفته می‌شود و خطایش در بخش «درباره‌ی برنامه» شمارش می‌شود.
**فقط آهنگ‌هایی اضافه کن که حوزه‌ی عمومی‌اند یا مجوزش را داری.**

## درس‌ها
فایل `assets/lessons/lessons.json`. هر درس: `id، level (1..4)، title_fa، body_fa (پاراگراف‌ها)، highlight_keys (۱..۱۷)، demo، exercise {notes, tempo}، quiz`.
می‌توانی `tools/make_lessons.py` را ویرایش و اجرا کنی یا JSON را مستقیم تغییر دهی.

## مهاجرت پایگاه داده
نسخه‌ی Room در `data/Db.kt` است. برای هر تغییر ساختار، `version` را بالا ببر و یک `Migration` به `MIGRATIONS` اضافه کن (حذف خودکار داده‌ها فعال نیست).
