package com.kalimba.ava.core

data class Badge(val id: String, val title: String, val description: String, val earned: Boolean)

/** XP, levels, streaks and achievement rules (pure logic, unit-tested). */
object Progress {
    const val XP_LESSON = 50
    const val XP_SONG_FIRST = 40

    val LEVEL_NAMES = listOf("مبتدی", "متوسط", "پیشرفته", "حرفه‌ای")
    private val LEVEL_XP = intArrayOf(0, 500, 1500, 3500)

    fun levelIndex(xp: Int): Int = LEVEL_XP.indexOfLast { xp >= it }.coerceAtLeast(0)
    fun levelName(xp: Int) = LEVEL_NAMES[levelIndex(xp)]

    /** Fraction (0..1) of the way to the next level; 1 at the top level. */
    fun levelProgress(xp: Int): Float {
        val i = levelIndex(xp)
        if (i >= LEVEL_XP.lastIndex) return 1f
        return ((xp - LEVEL_XP[i]).toFloat() / (LEVEL_XP[i + 1] - LEVEL_XP[i])).coerceIn(0f, 1f)
    }
    fun xpToNext(xp: Int): Int { val i = levelIndex(xp); return if (i >= LEVEL_XP.lastIndex) 0 else LEVEL_XP[i + 1] - xp }

    fun sessionXp(accuracy: Double) = 5 + (accuracy / 10.0).toInt()

    /** Consecutive practice days ending today (or yesterday, so the streak survives until the day is over). */
    fun streak(practiceDays: Set<Long>, today: Long): Int {
        var day = if (today in practiceDays) today else today - 1
        var n = 0
        while (day in practiceDays) { n++; day-- }
        return n
    }

    fun badges(lessonsDone: Int, songsDone: Int, bestAccuracy: Double, streak: Int, totalMinutes: Int, xp: Int, level1Done: Boolean): List<Badge> = listOf(
        Badge("first_lesson", "اولین گام", "اولین درس را کامل کن", lessonsDone >= 1),
        Badge("five_lessons", "شاگرد پیگیر", "۵ درس را کامل کن", lessonsDone >= 5),
        Badge("level1", "پایان سطح مبتدی", "همه‌ی درس‌های سطح ۱", level1Done),
        Badge("first_song", "اولین آهنگ", "یک آهنگ را با دقت ۸۰٪ یا بیشتر بنواز", songsDone >= 1),
        Badge("five_songs", "کلکسیونر ملودی", "۵ آهنگ را کامل کن", songsDone >= 5),
        Badge("accuracy90", "گوش تیز", "دقت ۹۰٪ در یک تمرین", bestAccuracy >= 90.0),
        Badge("streak3", "سه روز پشت‌سرهم", "۳ روز تمرین متوالی", streak >= 3),
        Badge("streak7", "هفته‌ی طلایی", "۷ روز تمرین متوالی", streak >= 7),
        Badge("hour", "یک ساعت تمرین", "مجموع ۶۰ دقیقه تمرین", totalMinutes >= 60),
        Badge("xp1000", "هزار امتیاز", "۱۰۰۰ امتیاز تجربه", xp >= 1000),
    )
}
