package ohi.andre.consolelauncher.tuils

import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.view.MotionEvent
import android.widget.TextView
import android.text.Layout

class LongClickMovementMethod : LinkMovementMethod() {
    private var longClickDuration = 0
    private var lastLine = -1
    private var runnable: WasActivatedRunnable? = null

    private abstract class WasActivatedRunnable : Runnable {
        var wasActivated = false

        override fun run() {
            wasActivated = true
        }
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.action

        if (
            action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_DOWN ||
            action == MotionEvent.ACTION_MOVE ||
            action == MotionEvent.ACTION_CANCEL
        ) {
            var x = event.x.toInt()
            var y = event.y.toInt()

            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop

            x += widget.scrollX
            y += widget.scrollY

            val layout = widget.layout ?: return super.onTouchEvent(widget, buffer, event)
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())

            val link = buffer.getSpans(off, off, LongClickableSpan::class.java)
            if (link.isEmpty() && runnable == null) {
                return super.onTouchEvent(widget, buffer, event)
            }

            if (action == MotionEvent.ACTION_UP) {
                val activeRunnable = runnable
                if (activeRunnable != null) {
                    if (!activeRunnable.wasActivated) {
                        widget.removeCallbacks(activeRunnable)
                        if (link.isNotEmpty()) {
                            link[0].onClick(widget)
                        }
                    }

                    runnable = null
                }
            } else if (action == MotionEvent.ACTION_DOWN) {
                if (link.isNotEmpty()) {
                    val span = link[0]
                    val nextRunnable = object : WasActivatedRunnable() {
                        override fun run() {
                            super.run()
                            span.onLongClick(widget)
                        }
                    }
                    runnable = nextRunnable
                    widget.postDelayed(nextRunnable, longClickDuration.toLong())
                }
            } else {
                val activeRunnable = runnable
                if (line != lastLine && activeRunnable != null) {
                    widget.removeCallbacks(activeRunnable)
                    runnable = null
                }
            }

            lastLine = line

            return true
        }

        return super.onTouchEvent(widget, buffer, event)
    }

    companion object {
        private var sInstance: LongClickMovementMethod? = null

        @JvmStatic
        fun getInstance(longClickDuration: Int): MovementMethod {
            if (sInstance == null) {
                sInstance = LongClickMovementMethod()
                sInstance!!.longClickDuration = longClickDuration
            }

            return sInstance!!
        }
    }
}
