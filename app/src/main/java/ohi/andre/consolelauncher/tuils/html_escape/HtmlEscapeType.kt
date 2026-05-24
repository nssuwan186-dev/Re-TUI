package ohi.andre.consolelauncher.tuils.html_escape

enum class HtmlEscapeType(
    private val useNCRs: Boolean,
    private val useHexa: Boolean,
    private val useHtml5: Boolean
) {
    HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL(true, false, false),
    HTML4_NAMED_REFERENCES_DEFAULT_TO_HEXA(true, true, false),
    HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL(true, false, true),
    HTML5_NAMED_REFERENCES_DEFAULT_TO_HEXA(true, true, true),
    DECIMAL_REFERENCES(false, false, false),
    HEXADECIMAL_REFERENCES(false, true, false);

    fun getUseNCRs(): Boolean = useNCRs

    fun getUseHexa(): Boolean = useHexa

    fun getUseHtml5(): Boolean = useHtml5
}
