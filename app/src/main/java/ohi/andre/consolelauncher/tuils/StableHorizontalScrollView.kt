package ohi.andre.consolelauncher.tuils

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import kotlin.math.max
import kotlin.math.min
import android.view.ViewParent

class StableHorizontalScrollView : HorizontalScrollView {
    private var stableScrollX = 0
    private var touchDownScrollX = -1
    private var touchDownActive = false
    private var restoringLayout = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (!restoringLayout && touchDownActive && touchDownScrollX > 0 && l == 0) {
            post { scrollTo(min(touchDownScrollX, maxScrollX()), scrollY) }
            return
        }
        if (!restoringLayout) {
            stableScrollX = l
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val target = stableScrollX
        restoringLayout = true
        super.onLayout(changed, l, t, r, b)
        if (target > 0) {
            scrollTo(min(target, maxScrollX()), scrollY)
        }
        restoringLayout = false
        stableScrollX = scrollX
    }

    override fun requestChildRectangleOnScreen(child: View, rectangle: Rect, immediate: Boolean): Boolean = false

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        updateTouchState(ev)
        updateParentIntercept(ev)
        return super.dispatchTouchEvent(ev)
    }

    fun preserveScrollX(scrollX: Int) {
        val target = max(0, scrollX)
        applyPreservedScrollX(target)
        post { applyPreservedScrollX(target) }
        postDelayed({ applyPreservedScrollX(target) }, 80)
    }

    fun getPreservedScrollX(): Int {
        if (touchDownActive && touchDownScrollX >= 0) {
            return touchDownScrollX
        }
        return stableScrollX
    }

    private fun updateTouchState(ev: MotionEvent?) {
        if (ev == null) {
            return
        }
        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            touchDownScrollX = scrollX
            touchDownActive = true
            stableScrollX = touchDownScrollX
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            post {
                touchDownActive = false
                touchDownScrollX = -1
                stableScrollX = scrollX
            }
        }
    }

    private fun updateParentIntercept(ev: MotionEvent?) {
        val parent = parent
        if (parent == null || ev == null) {
            return
        }
        val action = ev.actionMasked
        parent.requestDisallowInterceptTouchEvent(action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL)
    }

    private fun maxScrollX(): Int {
        if (childCount == 0) {
            return 0
        }
        val visibleWidth = width - paddingLeft - paddingRight
        return max(0, getChildAt(0).width - visibleWidth)
    }

    private fun applyPreservedScrollX(target: Int) {
        stableScrollX = min(max(0, target), maxScrollX())
        scrollTo(stableScrollX, scrollY)
    }
}
