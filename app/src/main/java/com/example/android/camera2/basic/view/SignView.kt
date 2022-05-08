package com.example.android.camera2.basic.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.annotation.Nullable
import kotlin.math.min

class SignView : View, View.OnTouchListener {
    private val TAG = SignView::class.simpleName
    private val paint = Paint()
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
    var ratio = 1F
    var offset = 0F
    var image: ImageView? = null

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

    fun prepare(bitmap: Bitmap, screenWidth: Int, screenHeight: Int, image: ImageView) {
        mBitmap = bitmap
        mWidth = bitmap.width
        mHeight = bitmap.height
        canvas1 = Canvas(mBitmap!!)
        boundary = Rect(0, 0, mWidth, mHeight)
        val wf = screenWidth / mWidth.toFloat()
        val hf = screenHeight / mHeight.toFloat()
        ratio = min(wf, hf)
        offset = (screenHeight / 2) - (mHeight * ratio / 2)
        this.image = image
        Log.d(TAG, "prepare: $wf $hf")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        paint.isAntiAlias = true
        paint.strokeWidth = mStroke.toFloat()
//        canvas.drawPath(path!!, paint)
        canvas1!!.drawPath(path!!, paint)
//        canvas.drawRect(boundary!!, paint)
        image?.setImageBitmap(mBitmap)
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        isdraw = true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path!!.moveTo(event.x / ratio, (event.y - offset) / ratio)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                path!!.lineTo(event.x / ratio, (event.y - offset) / ratio)
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
