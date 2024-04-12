package org.cezkor.towardsgoalsapp.etc

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet

class TopDownVerticalTextView
    : androidx.appcompat.widget.AppCompatTextView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int)
            : super(context, attrs, defStyle)

    companion object {
        const val ROTATION_ANGLE = -90f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(0f, height.toFloat())
        canvas.rotate(ROTATION_ANGLE)
        canvas.translate(compoundPaddingLeft.toFloat(), extendedPaddingTop.toFloat())
        super.onDraw(canvas)
    }

}