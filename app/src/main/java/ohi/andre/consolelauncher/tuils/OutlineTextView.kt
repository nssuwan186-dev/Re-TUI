package ohi.andre.consolelauncher.tuils

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.annotation.Nullable

class OutlineTextView : AppCompatTextView {
    private var drawTimes = -1

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun draw(canvas: Canvas) {
        if (drawTimes == -1) {
            drawTimes = if (tag == null) 1 else redrawTimes
        }

        for (c in 0 until drawTimes) {
            super.draw(canvas)
        }
    }

    companion object {
        @JvmField var SHADOW_TAG: String = "hasShadow"
        @JvmField var redrawTimes: Int = 1
    }
}
