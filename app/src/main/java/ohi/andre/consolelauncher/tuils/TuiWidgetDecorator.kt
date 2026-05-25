package ohi.andre.consolelauncher.tuils

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings

object TuiWidgetDecorator {
    @JvmStatic
    fun decorateWidget(widgetRoot: View?, borderViewId: Int, labelViewId: Int) {
        decorateWidget(
            widgetRoot,
            borderViewId,
            labelViewId,
            0,
            AppearanceSettings.musicWidgetBorderColor(),
            AppearanceSettings.musicWidgetTextColor()
        )
    }

    @JvmStatic
    fun decorateWidget(widgetRoot: View?, borderViewId: Int, labelViewId: Int, borderColor: Int, textColor: Int) {
        decorateWidget(widgetRoot, borderViewId, labelViewId, 0, borderColor, textColor)
    }

    @JvmStatic
    fun decorateWidget(
        widgetRoot: View?,
        borderViewId: Int,
        labelViewId: Int,
        closeViewId: Int,
        borderColor: Int,
        textColor: Int
    ) {
        if (widgetRoot == null) {
            return
        }

        val context = widgetRoot.context
        val widgetBgColor = AppearanceSettings.terminalWindowBackground()
        val labelMaskColor = AppearanceSettings.terminalHeaderBackground()
        val useDashed = AppearanceSettings.dashedBorders()

        val borderView = widgetRoot.findViewById<View>(borderViewId)
        if (borderView != null) {
            borderView.background = panelDrawable(
                context,
                widgetBgColor,
                borderColor,
                1.5f,
                AppearanceSettings.moduleCornerRadius(),
                useDashed
            )
        }

        val widgetLabel = widgetRoot.findViewById<TextView>(labelViewId)
        if (widgetLabel != null) {
            widgetLabel.setTextColor(textColor)
            widgetLabel.setTypeface(Tuils.getTypeface(context), Typeface.BOLD)
            widgetLabel.textSize = AppearanceSettings.moduleHeaderTextSize().toFloat()
            widgetLabel.background = TerminalBorderRuntime.tabDrawable(context, labelMaskColor)
        }

        if (borderView != null) {
            val closeView = if (closeViewId == 0) null else widgetRoot.findViewById<View?>(closeViewId)
            TerminalBorderRuntime.bind(borderView, widgetLabel, closeView)
        }
    }

    @JvmStatic
    fun getRowBackground(context: Context): GradientDrawable =
        getRowBackground(context, AppearanceSettings.notificationWidgetBorderColor())

    @JvmStatic
    fun getRowBackground(context: Context, borderColor: Int): GradientDrawable {
        val widgetBgColor = AppearanceSettings.terminalWindowBackground()
        val rowBackground = ColorUtils.blendARGB(widgetBgColor, Color.BLACK, 0.22f)
        val strokeColor = ColorUtils.setAlphaComponent(AppearanceSettings.terminalBorderColor(), 140)

        val bg = GradientDrawable()
        bg.shape = GradientDrawable.RECTANGLE
        bg.cornerRadius = Tuils.dpToPx(context, AppearanceSettings.moduleCornerRadius().toFloat())
        bg.setColor(rowBackground)
        if (AppearanceSettings.dashedBorders()) {
            bg.setStroke(
                dashedStrokePx(context, 0.8f),
                strokeColor,
                Tuils.dpToPx(context, AppearanceSettings.dashLength().toFloat()),
                Tuils.dpToPx(context, AppearanceSettings.dashGap().toFloat())
            )
        }
        return bg
    }

    @JvmStatic
    fun panelDrawable(
        context: Context,
        fillColor: Int,
        borderColor: Int,
        strokeDp: Float,
        radiusDp: Int,
        dashed: Boolean
    ): TerminalBorderDrawable = TerminalBorderRuntime.panelDrawable(
        context,
        fillColor,
        borderColor,
        strokeDp,
        radiusDp,
        dashed
    )

    private fun dashedStrokePx(context: Context, scale: Float): Int =
        max(1, Tuils.dpToPx(context, AppearanceSettings.dashedBorderStrokeWidthDp(scale)).toInt())
}
