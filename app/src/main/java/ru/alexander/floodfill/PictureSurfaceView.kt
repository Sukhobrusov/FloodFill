package ru.alexander.floodfill

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import ru.alexander.floodfill.threads.FloodFillThread
import ru.alexander.floodfill.threads.FonNeymanNeighborhoodFillThread
import ru.alexander.floodfill.threads.LinearFillThread
import ru.alexander.floodfill.threads.MooreNeighborhoodFillThread
import java.lang.Exception
import java.util.concurrent.Executors

class PictureSurfaceView(context : Context, attr : AttributeSet) : SurfaceView(context, attr),SurfaceHolder.Callback {

    // speed of the pixel expansion (in case of LinearFill -
    // defines frame rate 1000 / speed,
    // in other cases frame rate is set to 60 frames per second max, the speed is there to
    // regulate the amount of pixels skipped to perform redraw)
    var speed = 15
    set(value) {
        floodFillThread?.speed = value.toLong()
        field = value
    }
    var isRunning = false
    var algorighm : Algorighm = Algorighm.FON_NEYMAN

    private val mThreadPool = Executors.newCachedThreadPool()
    private var mBitmapWidth    = 255
    private var mBitmapHeight   = 255


    private var bitmap          : Bitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.RGB_565)
    private var canvas          : Canvas = Canvas(bitmap)
    private var floodFillThread : FloodFillThread? = null



    init {
        holder.addCallback(this)
        holder.setFormat(PixelFormat.TRANSPARENT)
    }

    companion object {
        private const val TAG       = "PictureSurfaceView"
    }
    enum class Algorighm {
        FON_NEYMAN,
        MOORE,
        LINEAR
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(this.canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

//        super.onMeasure(mBitmapWidth, mBitmapHeight)
//        Log.d(TAG, "Measure - $widthMeasureSpec, $heightMeasureSpec")
        setMeasuredDimension(mBitmapWidth, mBitmapHeight)
//        setBackgroundColor(Color.BLUE)
//        invalidate()
    }


    override fun surfaceChanged(holder: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
        Log.d(TAG, "Surface changed")
        val lockedCanvas = holder?.lockCanvas()
        lockedCanvas?.drawBitmap(bitmap, 0f,0f, Paint(Paint.ANTI_ALIAS_FLAG))
        holder?.unlockCanvasAndPost(lockedCanvas)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        if (floodFillThread != null && floodFillThread!!.isRunning) {
            try {
                floodFillThread?.isRunning = false
                isRunning = false
                mThreadPool.shutdown()
            } catch (e : Exception){
                e.printStackTrace()
            } finally {
                floodFillThread = null
            }
        }
        holder?.removeCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        setWillNotDraw(true)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)

        if (event == null)
            return false

        if (MotionEvent.ACTION_DOWN != event.action)
            return false

        with(event) {
            Log.d(TAG, "-----------------------------------")
            if (!isRunning) {
                val sceneModelComposer = SceneModelComposer(x.toInt(), y.toInt(), bitmap)
                val onComplete = {
                    isRunning   = false
                }

                floodFillThread = when (algorighm){
                    Algorighm.FON_NEYMAN -> FonNeymanNeighborhoodFillThread(holder, sceneModelComposer, onComplete)
                    Algorighm.LINEAR     -> LinearFillThread(holder, sceneModelComposer, onComplete)
                    Algorighm.MOORE      -> MooreNeighborhoodFillThread(holder, sceneModelComposer, onComplete)
                }
                floodFillThread?.isRunning  = true
                isRunning                   = true
                floodFillThread?.speed      = speed.toLong()
                mThreadPool.submit(floodFillThread)
            }
            Log.d(TAG, "-----------------------------------")


        }
        return performClick()
    }



    fun setBitmap(bm : Bitmap){
        if (!isRunning) {
            mBitmapWidth = bm.width
            mBitmapHeight = bm.height
            val params = layoutParams
            params.height = bm.width
            params.width = bm.height
            layoutParams = params

            generateNewBitmap(bm)
            postInvalidate()
        }
    }



    private fun generateNewBitmap(bm : Bitmap) {

        Log.d(TAG, "generate new bitmap")
        // passed bitmap is immutable so we need to make it mutable
        // otherwise the canvas wouldn't be able to change it and our thread
        bitmap = bm.copy(Bitmap.Config.RGB_565, true)
        canvas.setBitmap(bitmap)

//        val holderCanvas = holder.lockCanvas()
//        holderCanvas.drawBitmap(bitmap, 0f,0f, Paint(Paint.ANTI_ALIAS_FLAG))
//        holder.unlockCanvasAndPost(holderCanvas)

        // free the resources of the old bitmap
        bm.recycle()
    }


    fun forceStop() {
        isRunning = false
        floodFillThread?.isRunning = false
    }




}
