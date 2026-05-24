package ohi.andre.consolelauncher.tuils.html_escape

enum class HtmlEscapeLevel(private val escapeLevel: Int) {
    LEVEL_0_ONLY_MARKUP_SIGNIFICANT_EXCEPT_APOS(0),
    LEVEL_1_ONLY_MARKUP_SIGNIFICANT(1),
    LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT(2),
    LEVEL_3_ALL_NON_ALPHANUMERIC(3),
    LEVEL_4_ALL_CHARACTERS(4);

    fun getEscapeLevel(): Int = escapeLevel

    companion object {
        @JvmStatic
        fun forLevel(level: Int): HtmlEscapeLevel {
            return when (level) {
                0 -> LEVEL_0_ONLY_MARKUP_SIGNIFICANT_EXCEPT_APOS
                1 -> LEVEL_1_ONLY_MARKUP_SIGNIFICANT
                2 -> LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT
                3 -> LEVEL_3_ALL_NON_ALPHANUMERIC
                4 -> LEVEL_4_ALL_CHARACTERS
                else -> throw IllegalArgumentException("No escape level enum constant defined for level: $level")
            }
        }
    }
}
