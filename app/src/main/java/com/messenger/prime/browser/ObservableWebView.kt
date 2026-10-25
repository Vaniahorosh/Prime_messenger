package com.messenger.prime.browser

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class ObservableWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    fun interface OnScrollChangeListener {
        fun onScrollChanged(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int)
    }

    private var onObservableScrollChangeListener: OnScrollChangeListener? = null

    fun setOnObservableScrollChangeListener(listener: OnScrollChangeListener?) {
        this.onObservableScrollChangeListener = listener
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onObservableScrollChangeListener?.onScrollChanged(l, t, oldl, oldt)
    }

    /**
     * Calculates current vertical scroll progress as a percentage from 0 to 100.
     */
    fun getScrollPercentage(): Int {
        val range = computeVerticalScrollRange()
        val extent = computeVerticalScrollExtent()
        val maxScroll = range - extent
        if (maxScroll <= 0) return 0
        val offset = computeVerticalScrollOffset()
        val percentage = ((offset.toFloat() / maxScroll.toFloat()) * 100).toInt()
        return percentage.coerceIn(0, 100)
    }
}
