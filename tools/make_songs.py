#!/usr/bin/env python3
"""Builds the built-in song packs (assets/songs/*.json). Edit the lists below or drop your own pack JSON next to them."""
import json, os
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "songs")

def S(id, fa, en, cat, diff, tempo, ts, notes, lic, src, composer="", artist=""):
    return dict(id=id, title_fa=fa, title_en=en, composer=composer or "ناشناس / سنتی", artist=artist, category=cat,
                difficulty=diff, tempo=tempo, time_signature=ts, notes=" ".join(notes.split()), license=lic, source=src)

TRAD = "ملودی سنتی؛ ترانه‌ی حوزه‌ی عمومی. تنظیم ساده برای کالیمبا توسط پروژه."
ORIG = "ساخته‌شده برای این برنامه و منتشرشده با مجوز CC0."

international = [
 S("intl_001","ای ستاره‌ی کوچک","Twinkle, Twinkle, Little Star","beginner","beginner",100,"4/4",
   "C4 C4 G4 G4 A4 A4 G4:2 | F4 F4 E4 E4 D4 D4 C4:2 | G4 G4 F4 F4 E4 E4 D4:2 | G4 G4 F4 F4 E4 E4 D4:2 | C4 C4 G4 G4 A4 A4 G4:2 | F4 F4 E4 E4 D4 D4 C4:2",
   "public_domain",TRAD,"W. A. Mozart (واریاسیون‌ها)"),
 S("intl_002","سرود شادی","Ode to Joy","classical","beginner",100,"4/4",
   "E5 E5 F5 G5 G5 F5 E5 D5 C5 C5 D5 E5 E5:1.5 D5:.5 D5:2 | E5 E5 F5 G5 G5 F5 E5 D5 C5 C5 D5 E5 D5:1.5 C5:.5 C5:2 | D5 D5 E5 C5 D5 E5:.5 F5:.5 E5 C5 D5 E5:.5 F5:.5 E5 D5 C5 D5 G4:2 | E5 E5 F5 G5 G5 F5 E5 D5 C5 C5 D5 E5 D5:1.5 C5:.5 C5:2",
   "public_domain","آهنگ حوزه‌ی عمومی (۱۸۲۴).","L. van Beethoven"),
 S("intl_003","مری یک بره‌ی کوچک داشت","Mary Had a Little Lamb","beginner","beginner",100,"4/4",
   "E4 D4 C4 D4 E4 E4 E4:2 | D4 D4 D4:2 E4 G4 G4:2 | E4 D4 C4 D4 E4 E4 E4 E4 | D4 D4 E4 D4 C4:4",
   "public_domain",TRAD),
 S("intl_004","نان داغ","Hot Cross Buns","beginner","beginner",100,"4/4",
   "E4 D4 C4:2 | E4 D4 C4:2 | C4:.5 C4:.5 C4:.5 C4:.5 D4:.5 D4:.5 D4:.5 D4:.5 | E4 D4 C4:2",
   "public_domain",TRAD),
 S("intl_005","زنگ‌های شادی (کر)","Jingle Bells (chorus)","popular","beginner",120,"4/4",
   "E4 E4 E4:2 E4 E4 E4:2 E4 G4 C4:1.5 D4:.5 E4:4 | F4 F4 F4:1.5 F4:.5 F4 E4 E4 E4:.5 E4:.5 E4 D4 D4 E4 D4:2 G4:2",
   "public_domain","آهنگ حوزه‌ی عمومی (۱۸۵۷).","J. Pierpont"),
 S("intl_006","برادر ژاک","Frère Jacques","folk","beginner",100,"4/4",
   "C5 D5 E5 C5 | C5 D5 E5 C5 | E5 F5 G5:2 | E5 F5 G5:2 | G5:.5 A5:.5 G5:.5 F5:.5 E5 C5 | G5:.5 A5:.5 G5:.5 F5:.5 E5 C5 | C5 G4 C5:2 | C5 G4 C5:2",
   "public_domain",TRAD),
 S("intl_007","در نور مهتاب","Au clair de la lune","folk","beginner",90,"4/4",
   "C4 C4 C4 D4 E4:2 D4:2 | C4 E4 D4 D4 C4:4 | C4 C4 C4 D4 E4:2 D4:2 | C4 E4 D4 D4 C4:4 | D4 D4 D4 D4 A4:2 A4:2 | D4 C4 B4 A4 G4:4 | C4 C4 C4 D4 E4:2 D4:2 | C4 E4 D4 D4 C4:4",
   "public_domain",TRAD),
 S("intl_008","پل لندن","London Bridge","folk","beginner",110,"4/4",
   "G4:1.5 A4:.5 G4 F4 E4 F4 G4:2 | D4 E4 F4:2 E4 F4 G4:2 | G4:1.5 A4:.5 G4 F4 E4 F4 G4:2 | D4:2 G4:2 E4:2 C4:2",
   "public_domain",TRAD),
 S("intl_009","قایقت را بران","Row, Row, Row Your Boat","beginner","beginner",100,"6/8",
   "C4:1.5 C4:1.5 C4:1 D4:.5 E4:1.5 | E4:1 D4:.5 E4:1 F4:.5 G4:3 | C5:.5 C5:.5 C5:.5 G4:.5 G4:.5 G4:.5 E4:.5 E4:.5 E4:.5 C4:.5 C4:.5 C4:.5 | G4:1 F4:.5 E4:1 D4:.5 C4:3",
   "public_domain",TRAD),
 S("intl_010","تولدت مبارک","Happy Birthday","popular","beginner",100,"3/4",
   "G4:.75 G4:.25 A4 G4 C5 B4:3 | G4:.75 G4:.25 A4 G4 D5 C5:3 | G4:.75 G4:.25 G5 E5 C5 B4 A4:2 | F5:.75 F5:.25 E5 C5 D5 C5:3",
   "public_domain","ملودی (۱۸۹۳)؛ در آمریکا حوزه‌ی عمومی است. در برخی کشورها شرایط قانونی متفاوت است.","P. S. Hill و M. Hill"),
 S("intl_011","شب خاموش","Silent Night","relaxing","intermediate",72,"6/8",
   "G4:.75 A4:.25 G4:.5 E4:1.5 | G4:.75 A4:.25 G4:.5 E4:1.5 | D5:1 D5:.5 B4:1.5 | C5:1 C5:.5 G4:1.5 | A4:1 A4:.5 C5:.75 B4:.25 A4:.5 | G4:.75 A4:.25 G4:.5 E4:1.5 | A4:1 A4:.5 C5:.75 B4:.25 A4:.5 | G4:.75 A4:.25 G4:.5 E4:1.5 | D5:1 D5:.5 F5:.75 D5:.25 B4:.5 | C5:3 | E5:1 E5:.5 C5:.75 G4:.25 E4:.5 G4:.75 F4:.25 D4:.5 C4:1.5",
   "public_domain","آهنگ حوزه‌ی عمومی (۱۸۱۸).","F. X. Gruber"),
 S("intl_012","آقای مک‌دونالد مزرعه داشت","Old MacDonald Had a Farm","folk","beginner",110,"4/4",
   "C4 C4 C4 G4 A4 A4 G4:2 | E4 E4 D4 D4 C4:2 R:1 G4 | C4 C4 C4 G4 A4 A4 G4:2 | E4 E4 D4 D4 C4:4",
   "public_domain",TRAD),
 S("intl_013","گام دو ماژور (بالا و پایین)","C Major Scale","beginner","beginner",90,"4/4",
   "C4 D4 E4 F4 G4 A4 B4 C5 | D5 E5 F5 G5 A5 B5 C6 D6 | E6 D6 C6 B5 A5 G5 F5 E5 | D5 C5 B4 A4 G4 F4 E4 D4 | C4:4",
   "original_cc0",ORIG,"پروژه کالیمبا آوا"),
 S("intl_014","آرپژ ساده","Arpeggio Study","beginner","intermediate",90,"4/4",
   "C4 E4 G4 C5 G4 E4 C4:2 | D4 F4 A4 D5 A4 F4 D4:2 | E4 G4 B4 E5 B4 G4 E4:2 | F4 A4 C5 F5 C5 A4 F4:2 | G4 B4 D5 G5 D5 B4 G4:2 | C4+E4+G4:4",
   "original_cc0",ORIG,"پروژه کالیمبا آوا"),
 S("intl_015","دو شست","Two Thumbs Study","relaxing","intermediate",100,"4/4",
   "C4:.5 E4:.5 D4:.5 F4:.5 E4:.5 G4:.5 F4:.5 A4:.5 | G4:.5 B4:.5 A4:.5 C5:.5 B4:.5 D5:.5 C5:.5 E5:.5 | D5:.5 F5:.5 E5:.5 G5:.5 F5:.5 A5:.5 G5:.5 B5:.5 | A5:.5 G5:.5 F5:.5 E5:.5 D5:.5 C5:.5 B4:.5 A4:.5 | G4:.5 F4:.5 E4:.5 D4:.5 C4:2",
   "original_cc0",ORIG,"پروژه کالیمبا آوا"),
 S("intl_016","بازی پنتاتونیک","Pentatonic Play","beginner","beginner",100,"4/4",
   "C4 D4 E4 G4 A4 G4 E4 D4 | C5 A4 G4 E4 D4 E4 C4:2 | E5 D5 C5 A4 G4 A4 C5:2 | D5 E5 G5 E5 D5 C5 A4 G4 | C4+G4:4",
   "original_cc0",ORIG,"پروژه کالیمبا آوا"),
 S("intl_017","کاروان","Caravan (advanced original)","advanced","advanced",112,"4/4",
   "C4+G4:1 E5:.5 D5:.5 C5:1 G4:1 | A4+E5:1 G5:.5 F5:.5 E5:1 C5:1 | D4+A4:1 F5:.5 E5:.5 D5:1 A4:1 | B4+G5:1 A5:.5 G5:.5 E5:2 | C4+G4:1 E5:.5 G5:.5 C6:1 G5:1 | A5:.5 G5:.5 F5:.5 E5:.5 D5:1 B4:1 | G4+D5:.5 B4:.5 D5:.5 G5:.5 B5:1 G5:1 | C6+E5+G4+C4:4",
   "original_cc0",ORIG,"پروژه کالیمبا آوا"),
 S("intl_018","باران ملایم","Gentle Rain","relaxing","intermediate",84,"4/4",
   "C4 G4 E5 G4 | C4 G4 D5 G4 | D4 A4 F5 A4 | D4 A4 E5 A4 | E4 B4 G5 B4 | E4 B4 F5 B4 | F4 C5 A5 C5 | G4 D5 B5 D5 | C4+G4+E5:4",
   "original_cc0",ORIG,"پروژه کالیمبا آوا"),
]

iranian = [
 S("irn_001","سپیده‌دم","Dawn","iranian_calm","beginner",70,"4/4",
   "C4:2 E4:2 G4:2 E4:2 | A4:2 G4:2 E4:2 D4:2 | C4:2 E4:2 G4:2 C5:2 | B4:2 G4:2 C5:4 | E5:2 D5:2 C5:2 G4:2 | A4:2 G4:2 E4:2 D4:2 | C4+E4+G4:4",
   "original_cc0",ORIG,"پروژه کالیمبا آوا"),
 S("irn_002","مهتاب عاشقانه","Moonlit Love","iranian_romantic","intermediate",72,"3/4",
   "E4 G4 C5 | B4:2 A4 | G4 E4 D4 | E4:3 | F4 A4 C5 | D5:2 C5 | B4 G4 E4 | D4:3 | E4 G4 C5 | E5:2 D5 | C5 A4 F4 | E4:2 D4 | C4:3",
   "original_cc0",ORIG,"پروژه کالیمبا آوا"),
 S("irn_003","بهار شاد","Joyful Spring","iranian_happy","beginner",118,"4/4",
   "E4:.5 F4:.5 G4 G4:.5 A4:.5 G4 | F4:.5 E4:.5 D4 E4:2 | A4:.5 B4:.5 C5 C5:.5 D5:.5 C5 | B4:.5 A4:.5 G4 A4:2 | E5:.5 D5:.5 C5 D5:.5 C5:.5 B4 | A4:.5 G4:.5 F4 E4:2",
   "original_cc0",ORIG+" فضای مایه‌ای شاد با کوک ماژور.","پروژه کالیمبا آوا"),
 S("irn_004","لالایی کوچولو","Little Lullaby","iranian_kids","beginner",66,"3/4",
   "G4 E4 G4 | A4:2 G4 | F4 D4 F4 | E4:3 | G4 E4 G4 | C5:2 B4 | A4 F4 D4 | C4:3",
   "original_cc0",ORIG,"پروژه کالیمبا آوا"),
 S("irn_005","شب شرقی","Eastern Night","iranian_traditional","intermediate",80,"4/4",
   "E4 F4:.5 G4:.5 A4 G4 | F4:.5 G4:.5 F4 E4:2 | A4 B4:.5 C5:.5 B4 A4 | G4 F4 E4:2 | E5 F5:.5 G5:.5 F5 E5 | D5:.5 E5:.5 D5 C5:2 | B4 C5:.5 D5:.5 C5 B4 | A4:.5 G4:.5 F4 E4:2",
   "original_cc0","ساخته‌شده در فضای مد فریژین (می)، الهام‌گرفته از حال‌وهوای موسیقی ایرانی؛ این قطعه از ملودی سنتی خاصی برداشته نشده است.","پروژه کالیمبا آوا"),
 S("irn_006","کوچه‌باغ‌های یاد","Nostalgic Alley","iranian_nostalgia","intermediate",76,"4/4",
   "A4 C5 B4 A4 G4:2 E4:2 | E4 G4 A4 B4 C5:2 B4:2 | D5 C5 B4 A4 G4 A4 B4:2 | A4 G4 E4 D4 E4:4 | C5 D5 E5 D5 C5 B4 A4:2 | G4 A4 B4 C5 D5:2 E5:2 | D5:2 C5 B4 A4:2 G4:2 | E4:4",
   "original_cc0",ORIG+" الهام‌گرفته از حال‌وهوای نوستالژی؛ تقلید از آهنگ خاصی نیست.","پروژه کالیمبا آوا"),
 S("irn_007","رقص محلی","Folk Dance","iranian_folk","intermediate",126,"6/8",
   "C4:.5 D4:.25 E4:.25 G4:.5 E4:.5 D4:1 | C4:.5 D4:.25 E4:.25 A4:.5 G4:.5 E4:1 | F4:.5 E4:.25 D4:.25 E4:.5 D4:.5 C4:1 | D4:.5 E4:.5 D4:.5 C4:1.5",
   "original_cc0",ORIG+" سبک رقص‌های ریتمیک محلی؛ تقلید از ترانه‌ی مشخصی نیست.","پروژه کالیمبا آوا"),
 S("irn_008","تیتراژ سفر","Journey Theme","iranian_film","advanced",96,"4/4",
   "C4+G4:2 E5:1 D5:1 | C5:2 G4:1 A4:1 | F4+C5:2 A5:1 G5:1 | E5:2 D5:1 C5:1 | D4+A4:2 F5:1 E5:1 | D5:2 C5:1 B4:1 | G4+D5:2 B5:1 A5:1 | G5:2 E5:1 C5:1 | C4+E4+G4+C5:4",
   "original_cc0",ORIG,"پروژه کالیمبا آوا"),
]
os.makedirs(OUT, exist_ok=True)
for name, songs, title in [("pack_international", international, "ملودی‌های بین‌المللی و تمرینی"), ("pack_iranian_original", iranian, "قطعات اورجینال با فضای ایرانی")]:
    pack = dict(pack_id=name, pack_version=1, schema_version=1, title_fa=title, songs=songs)
    with open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8") as f:
        json.dump(pack, f, ensure_ascii=False, indent=1)
print(len(international)+len(iranian), "songs")
# dump for the Kotlin validation harness
with open("/tmp/songs.tsv", "w", encoding="utf-8") as f:
    for s in international + iranian:
        f.write(f"{s['id']}\t{s['time_signature']}\t{s['tempo']}\t{s['notes']}\n")
