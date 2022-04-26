package com.example.android.camera2.basic.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Nullable

class SignView : View, View.OnTouchListener {
    private val TAG = SignView::class.simpleName
    var mBitmap: Bitmap? = null
    var path: Path? = null
    var boundary: Rect? = null
    var canvas1: Canvas? = null

    //用于判断路径是否为空
    var isdraw = false

    //用来提供给Activity设置使用
    var mStroke = 0
    var mWidth = 0
    var mHeight = 0

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, @Nullable attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        path = Path()
        isdraw = false
        mStroke = 8
        setOnTouchListener(this)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        canvas1 = Canvas(mBitmap!!)
        boundary = Rect(0, 0, mWidth, mHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        paint.isAntiAlias = true
        paint.strokeWidth = mStroke.toFloat()
        //canvas.drawPath(path!!, paint)
        canvas1!!.drawPath(path!!, paint)
        canvas.drawRect(boundary!!, paint)
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        isdraw = true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path!!.moveTo(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                path!!.lineTo(event.x, event.y)
                invalidate()
            }
        }
        return true
    }


    fun clear() {
        path!!.reset()
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
        canvas1 = Canvas(mBitmap!!)
        invalidate()
    }
}