package ohi.andre.consolelauncher.tuils

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.View
import kotlin.math.max
import kotlin.math.min
import ohi.andre.consolelauncher.managers.settings.AppearanceSettings

object TerminalBorderRuntime {
    fun panelDrawable(
        context: Context,
        fillColor: Int,
        borderColor: Int,
        strokeDp: Float,
        radiusDp: Int,
        dashed: Boolean
    ): TerminalBorderDrawable {
        return panelDrawablePx(
            context,
            fillColor,
            borderColor,
            strokeDp,
            Tuils.dpToPx(context, radiusDp.toFloat()).toFloat(),
            dashed
        )
    }

    fun panelDrawablePx(
        context: Context,
        fillColor: Int,
        borderColor: Int,
        strokeDp: Float,
        radiusPx: Float,
        dashed: Boolean
    ): TerminalBorderDrawable {
        val stroke = if (dashed) {
            max(1, Tuils.dpToPx(context, AppearanceSettings.dashedBorderStrokeWidthDp(strokeDp / 1.5f)).toInt())
        } else {
            0
        }
        return TerminalBorderDrawable(
            fillColor,
            borderColor,
            stroke,
            radiusPx,
            dashed,
            Tuils.dpToPx(context, AppearanceSettings.dashLength().toFloat()).toFloat(),
            Tuils.dpToPx(context, AppearanceSettings.dashGap().toFloat()).toFloat()
        )
    }

    fun tabDrawable(context: Context, fillColor: Int): GradientDrawable {
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.RECTANGLE
        bg.cornerRadius = Tuils.dpToPx(context, AppearanceSettings.headerCornerRadius().toFloat())
        bg.setColor(fillColor)
        val borderColor = AppearanceSettings.terminalHeaderTabBorderColor()
        if (AppearanceSettings.dashedBorders() && Color.alpha(borderColor) > 0) {
            val stroke = max(1, Tuils.dpToPx(context, AppearanceSettings.dashedBorderStrokeWidthDp()).toInt())
            val dashLength = AppearanceSettings.dashLength()
            val dashGap = AppearanceSettings.dashGap()
            if (dashLength > 0 && dashGap > 0) {
                bg.setStroke(
                    stroke,
                    borderColor,
                    Tuils.dpToPx(context, dashLength).toFloat(),
                    Tuils.dpToPx(context, dashGap).toFloat()
                )
            } else {
                bg.setStroke(stroke, borderColor)
            }
        } else {
            bg.setStroke(0, Color.TRANSPARENT)
        }
        return bg
    }

    fun bind(panel: View?, vararg cutoutViews: View?) {
        val border = panel ?: return
        val drawable = border.background as? TerminalBorderDrawable ?: return
        val views = cutoutViews.filterNotNull()
        if (views.isEmpty()) {
            drawable.setCutouts(emptyList(), emptyList())
            return
        }
        val runnable = CutoutRunnable(border, drawable, views)
        border.post(runnable)
        for (view in views) {
            view.post(runnable)
        }
    }

    private class CutoutRunnable(
        private val panel: View,
        private val drawable: TerminalBorderDrawable,
        private val cutoutViews: List<View>
    ) : Runnable {
        override fun run() {
            if (panel.width <= 0 || panel.height <= 0) {
                return
            }
            val panelLocation = IntArray(2)
            panel.getLocationOnScreen(panelLocation)
            val gutter = Tuils.dpToPx(panel.context, 6)
            val overlapSlop = Tuils.dpToPx(panel.context, 12)
            val cutoutHeight = max(drawable.strokeWidthPx * 4, Tuils.dpToPx(panel.context, 10)).toFloat()
            val topOut = ArrayList<RectF>()
            val bottomOut = ArrayList<RectF>()

            for (view in cutoutViews) {
                if (view.visibility != View.VISIBLE || view.width <= 0 || view.height <= 0) {
                    continue
                }
                val childLocation = IntArray(2)
                view.getLocationOnScreen(childLocation)
                val relativeTop = childLocation[1] - panelLocation[1]
                val relativeBottom = relativeTop + view.height
                val left = childLocation[0] - panelLocation[0] - gutter
                val right = childLocation[0] - panelLocation[0] + view.width + gutter
                val cutout = RectF(
                    max(0, left).toFloat(),
                    0f,
                    min(panel.width, right).toFloat(),
                    cutoutHeight
                )
                if (relativeBottom >= -overlapSlop && relativeTop <= overlapSlop) {
                    topOut.add(cutout)
                } else if (relativeTop <= panel.height + overlapSlop && relativeBottom >= panel.height - overlapSlop) {
                    bottomOut.add(
                        RectF(
                            cutout.left,
                            0f,
                            cutout.right,
                            cutoutHeight
                        )
                    )
                }
            }
            drawable.setCutouts(
                topOut,
                bottomOut
            )
        }
    }
}
