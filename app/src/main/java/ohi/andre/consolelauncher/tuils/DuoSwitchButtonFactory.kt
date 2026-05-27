package ohi.andre.consolelauncher.tuils

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import androidx.core.graphics.ColorUtils

/**
 * Creates the small control used to move the launcher between split-duo panes.
 *
 * UIManager keeps the active pane state and supplies the click callback; this helper
 * only owns the stateless view chrome.
 */
object DuoSwitchButtonFactory {
    fun create(
        context: Context,
        targetMode: String?,
        moveToLeft: Boolean,
        textColor: Int,
        headerBackgroundColor: Int,
        cornerRadiusPx: Int,
        dashedBorders: Boolean,
        onClick: (String?) -> Unit
    ): TextView {
        val button = TextView(context)
        button.text = if (moveToLeft) "<<" else ">>"
        button.contentDescription = "Move Re:T-UI to $targetMode screen"
        button.gravity = Gravity.CENTER
        button.setTypeface(Tuils.getTypeface(context), Typeface.BOLD)
        button.textSize = 18f
        button.setTextColor(textColor)
        button.background = TerminalBorderRuntime.panelDrawablePx(
            context,
            ColorUtils.setAlphaComponent(headerBackgroundColor, 224),
            textColor,
            1.5f,
            cornerRadiusPx.toFloat(),
            dashedBorders
        )
        button.setOnClickListener { onClick(targetMode) }
        return button
    }
}
