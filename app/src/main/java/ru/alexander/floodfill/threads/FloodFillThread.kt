package ru.alexander.floodfill.threads

import android.graphics.Color
import android.util.SparseBooleanArray
import android.view.SurfaceHolder
import ru.alexander.floodfill.SceneModelComposer
import java.util.*

abstract class FloodFillThread(
    protected val holder            : SurfaceHolder,
    protected val modelComposer     : SceneModelComposer,
    protected val onComplete        : () -> Unit
) : Thread() {

    var isRunning            = true
    var callCounter          = 0
    var fillColor            = -1
    var startColor  = intArrayOf(0, 0, 0)
    var tolerance   = intArrayOf(0, 0, 0)
    var speed                = 15L

    protected var pixels                        : IntArray = IntArray(modelComposer.width * modelComposer.height)
    protected var pixelsChecked                 : SparseBooleanArray

    init {
        modelComposer.getPixels(pixels)
        pixelsChecked = SparseBooleanArray(pixels.size)
    }

    abstract override fun run()


    open fun prepare() {
        // Called before starting recursive-fill
        pixelsChecked = SparseBooleanArray(pixels.size)

        if (startColor[0] == 0) {
            // ***Get starting color.
            val startPixel = pixels[modelComposer.width * modelComposer.y + modelComposer.x]
            startColor[0] = startPixel shr 16 and 0xff
            startColor[1] = startPixel shr 8 and 0xff
            startColor[2] = startPixel and 0xff
        }

        if (compareInitialColors()){
            invertInitialColors()
        }
    }


    //Sets target color
    protected fun setTargetColor(targetColor: Int) {
        startColor[0] = Color.red(targetColor)
        startColor[1] = Color.green(targetColor)
        startColor[2] = Color.blue(targetColor)
    }

    // Sees if a pixel is within the color tolerance range.
    protected fun checkPixel(px: Int): Boolean {
        val red     = pixels[px] shr 16 and 0xff
        val green   = pixels[px] shr 8 and 0xff
        val blue    = pixels[px] and 0xff

//        Log.d(TAG, "$red + $green + $blue" )
        return (           red      >= startColor[0] - tolerance[0]
                        && red      <= startColor[0] + tolerance[0]
                        && green    >= startColor[1] - tolerance[1]
                        && green    <= startColor[1] + tolerance[1]
                        && blue     >= startColor[2] - tolerance[2]
                        && blue     <= startColor[2] + tolerance[2])
    }

    protected fun checkPixelPosition(x : Int, y : Int) : Boolean{
        return     x >= 0
                && y >= 0
                && x < modelComposer.width
                && y < modelComposer.height
                && !pixelsChecked[y * modelComposer.width + x]
                && checkPixel(y * modelComposer.width + x)


    }

    protected fun compareInitialColors() : Boolean{
        val red     = fillColor shr 16 and 0xff
        val green   = fillColor shr 8 and 0xff
        val blue    = fillColor and 0xff

        return  red     == startColor[0] &&
                green   == startColor[1] &&
                blue    == startColor[2]

    }

    protected fun invertInitialColors(){
        fillColor = if (fillColor == 0){
            -1
        } else {
            0
        }
    }

}