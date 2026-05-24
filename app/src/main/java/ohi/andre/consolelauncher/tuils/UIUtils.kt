package ohi.andre.consolelauncher.tuils

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import java.io.File
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings
import android.util.DisplayMetrics

object UIUtils {
    @JvmField var globalTypeface: Typeface? = null
    @JvmField var fontPath: String? = null

    @JvmStatic
    fun getTypeface(context: Context): Typeface {
        if (globalTypeface != null) {
            return globalTypeface!!
        }

        val systemFont = AppearanceSettings.useSystemFont()
        if (systemFont) {
            globalTypeface = Typeface.MONOSPACE
            return globalTypeface!!
        }

        val fontName = AppearanceSettings.fontFile()
        if (fontName == null || fontName.isEmpty()) {
            globalTypeface = Typeface.MONOSPACE
            return globalTypeface!!
        }

        val tuiFolder = Tuils.getFolder()
        val fontFile = File(tuiFolder, fontName)

        if (fontFile.exists()) {
            try {
                globalTypeface = Typeface.createFromFile(fontFile)
                fontPath = fontFile.absolutePath
            } catch (e: Exception) {
                globalTypeface = Typeface.MONOSPACE
            }
        } else {
            globalTypeface = Typeface.MONOSPACE
        }

        return globalTypeface!!
    }

    @JvmStatic
    fun cancelFont() {
        globalTypeface = null
        fontPath = null
    }

    @JvmStatic
    fun dpToPx(context: Context, valueInDp: Float): Float {
        val metrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics)
    }

    @JvmStatic
    fun dpToPx(context: Context, valueInDp: Int): Int = dpToPx(context, valueInDp.toFloat()).toInt()

    @JvmStatic
    fun convertSpToPixels(sp: Float, context: Context): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics).toInt()

    @JvmStatic
    fun span(text: CharSequence?, color: Int): SpannableString? {
        if (text == null) {
            return null
        }
        val ss = SpannableString(text)
        ss.setSpan(ForegroundColorSpan(color), 0, text.length, 0)
        return ss
    }

    @JvmStatic
    fun span(context: Context, size: Int, text: CharSequence?): SpannableString =
        span(context, text, Int.MAX_VALUE, size)

    @JvmStatic
    fun span(context: Context, text: CharSequence?, color: Int, size: Int): SpannableString =
        span(context, Int.MAX_VALUE, color, text, size)

    @JvmStatic
    fun span(bgColor: Int, foreColor: Int, text: CharSequence?): SpannableString =
        span(null, bgColor, foreColor, text, Int.MAX_VALUE)

    @JvmStatic
    fun span(context: Context?, bgColor: Int, foreColor: Int, text: CharSequence?, size: Int): SpannableString {
        val safeText = text ?: Tuils.EMPTYSTRING

        val spannableString = if (safeText is SpannableString) safeText else SpannableString(safeText)

        if (size != Int.MAX_VALUE && context != null) {
            spannableString.setSpan(
                AbsoluteSizeSpan(convertSpToPixels(size.toFloat(), context)),
                0,
                safeText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (foreColor != Int.MAX_VALUE) {
            spannableString.setSpan(ForegroundColorSpan(foreColor), 0, safeText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (bgColor != Int.MAX_VALUE) {
            spannableString.setSpan(BackgroundColorSpan(bgColor), 0, safeText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannableString
    }

    @JvmStatic
    fun span(bgColor: Int, text: SpannableString, section: String, fromIndex: Int): Int {
        val index = text.toString().indexOf(section, fromIndex)
        if (index == -1) {
            return index
        }

        text.setSpan(BackgroundColorSpan(bgColor), index, index + section.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return index + section.length
    }

}
