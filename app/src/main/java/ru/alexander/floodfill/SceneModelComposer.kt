package ru.alexander.floodfill

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.SurfaceView
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class SceneModelComposer(val x : Int, val y : Int, val image : Bitmap) {


    var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val height : Int get() {
        return image.height
    }
    val width : Int get() {
        return image.width
    }


    fun drawOn(canvas: Canvas){
        synchronized(this) {
            canvas.drawBitmap(image, 0f,0f, paint)
        }
    }

    fun setPixels(pixels : IntArray){
        image.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    fun getPixels(pixels: IntArray){
        image.getPixels(pixels, 0, width, 0, 0, width, height)
    }


}