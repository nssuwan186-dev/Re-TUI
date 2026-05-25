package ohi.andre.consolelauncher.tuils

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.max

class TerminalBorderDrawable(
    private val fillColor: Int,
    private val borderColor: Int,
    val strokeWidthPx: Int,
    private val radiusPx: Float,
    private val dashed: Boolean,
    private val dashLengthPx: Float,
    private val dashGapPx: Float
) : Drawable() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = borderColor
        strokeWidth = strokeWidthPx.toFloat()
        if (dashed && strokeWidthPx > 0) {
            pathEffect = DashPathEffect(
                floatArrayOf(max(1f, dashLengthPx), max(1f, dashGapPx)),
                0f
            )
        }
    }
    private val cutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }
    private val topCutouts = ArrayList<RectF>()
    private val bottomCutouts = ArrayList<RectF>()

    fun setCutouts(top: List<RectF>, bottom: List<RectF>) {
        topCutouts.clear()
        topCutouts.addAll(top)
        bottomCutouts.clear()
        bottomCutouts.addAll(bottom)
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return

        val fillRect = RectF(b)
        canvas.drawRoundRect(fillRect, radiusPx, radiusPx, fillPaint)

        if (strokeWidthPx <= 0) {
            return
        }

        val inset = strokeWidthPx / 2f
        val strokeRect = RectF(
            b.left + inset,
            b.top + inset,
            b.right - inset,
            b.bottom - inset
        )
        canvas.drawRoundRect(strokeRect, radiusPx, radiusPx, strokePaint)

        if (topCutouts.isEmpty() && bottomCutouts.isEmpty()) {
            return
        }

        val cutoutHeight = max(strokeWidthPx * 3f, 6f)
        for (cutout in topCutouts) {
            canvas.drawRect(
                b.left + cutout.left,
                b.top.toFloat(),
                b.left + cutout.right,
                b.top + cutoutHeight,
                cutoutPaint
            )
        }
        for (cutout in bottomCutouts) {
            canvas.drawRect(
                b.left + cutout.left,
                b.bottom - cutoutHeight,
                b.left + cutout.right,
                b.bottom.toFloat(),
                cutoutPaint
            )
        }
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
        cutoutPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
        cutoutPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
