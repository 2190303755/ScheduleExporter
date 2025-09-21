package eric.schedule_exporter

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat

class WebViewEx @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs), NestedScrollingChild3 {
    private val scrollingHelper: NestedScrollingChildHelper
    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)

    init {
        val helper = NestedScrollingChildHelper(this)
        helper.setNestedScrollingEnabled(true)
        this.scrollingHelper = helper
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (this.isNestedScrollingEnabled()) {
            val dy = t - oldt
            if (dy == 0) return
            if (!this.hasNestedScrollingParent()) {
                this.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
            }
            this.dispatchNestedPreScroll(0, dy, this.scrollConsumed, this.scrollOffset)
            this.dispatchNestedScroll(0, dy - this.scrollConsumed[1], 0, 0, this.scrollOffset)
        }
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        this.scrollingHelper.setNestedScrollingEnabled(enabled)
    }

    override fun isNestedScrollingEnabled() =
        this.scrollingHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int) =
        this.scrollingHelper.startNestedScroll(axes)

    override fun startNestedScroll(axes: Int, type: Int) =
        this.scrollingHelper.startNestedScroll(axes, type)

    override fun stopNestedScroll() =
        this.scrollingHelper.stopNestedScroll()

    override fun stopNestedScroll(type: Int) =
        this.scrollingHelper.stopNestedScroll(type)

    override fun hasNestedScrollingParent() =
        this.scrollingHelper.hasNestedScrollingParent()

    override fun hasNestedScrollingParent(type: Int) =
        this.scrollingHelper.hasNestedScrollingParent(type)

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ) = this.scrollingHelper.dispatchNestedPreScroll(
        dx,
        dy,
        consumed,
        offsetInWindow
    )

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ) = this.scrollingHelper.dispatchNestedPreScroll(
        dx,
        dy,
        consumed,
        offsetInWindow,
        type
    )

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ) = this.scrollingHelper.dispatchNestedScroll(
        dxConsumed,
        dyConsumed,
        dxUnconsumed,
        dyUnconsumed,
        offsetInWindow
    )

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ) = this.scrollingHelper.dispatchNestedScroll(
        dxConsumed,
        dyConsumed,
        dxUnconsumed,
        dyUnconsumed,
        offsetInWindow,
        type
    )

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) = this.scrollingHelper.dispatchNestedScroll(
        dxConsumed,
        dyConsumed,
        dxUnconsumed,
        dyUnconsumed,
        offsetInWindow,
        type,
        consumed
    )

    override fun dispatchNestedPreFling(
        velocityX: Float,
        velocityY: Float
    ) = this.scrollingHelper.dispatchNestedPreFling(velocityX, velocityY)

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ) = this.scrollingHelper.dispatchNestedFling(velocityX, velocityY, consumed)
}