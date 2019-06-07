package ru.alexander.floodfill.threads

import android.graphics.Canvas
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.util.set
import ru.alexander.floodfill.SceneModelComposer
import java.util.*

class MooreNeighborhoodFillThread
    (
    holder: SurfaceHolder,
    modelComposer: SceneModelComposer,
    onComplete : () -> Unit
)
    : FloodFillThread(holder, modelComposer, onComplete) {

    var count = 0
    protected lateinit var ranges               : Queue<Pixel>


    companion object {
        private const val TAG = "RecursiveFill"
    }



    override fun run() {
        // do something recursive until thread stops running or it finishes
        Log.d(TAG, "Thread started")

        // initial preparation
        // resetting all arrays
        prepare()

        // we check if the pixel colors the user clicked on are the same as fill color
        // if they are, we just exit the thread
        if (compareInitialColors()){
             Log.d(TAG, "Initial colors are the same")
             return
        }


        val startTime = System.currentTimeMillis()
        try {
            fill(modelComposer.x, modelComposer.y)
        }catch (e : Exception){
            e.printStackTrace()
        }
        val canvas = holder.lockCanvas()
        modelComposer.setPixels(pixels)
        modelComposer.drawOn(canvas)
        holder.unlockCanvasAndPost(canvas)
        val endTime = System.currentTimeMillis()

        Log.d(TAG, "${endTime - startTime}")
        Log.d(TAG, "Thread ended")
        onComplete()


    }

    override fun prepare() {
        // Called before starting recursive-fill
        super.prepare()
        ranges = LinkedList<Pixel>()
    }


    // Moore neighborhood algorithm is a fill of all 8 surrounding pixels
    // Very similar to Fon Neyman Neighborhood, but instead of 4 surrounding pixels
    // we have 8, and we are adding them for the check as well, that repeats until
    // there are no more pixels with startColor around or the thread stop running
    // West, East, North, West, NW, NE, SW, SE pixels
    protected fun fill(x : Int, y : Int) {
        if (x > modelComposer.width - 1  || x < 0 || y < 0 || y > modelComposer.height - 1)
            return

        //Initial fill of the starting pixels
        fillQueue(x, y)

        while (ranges.size > 0 && isRunning){
            var canvas : Canvas? = null
            try {
                // counting cycles, so we don`t use draw to often
                count++
                if (count % (10*speed.toInt()) == 0) {
                    sleep(15L)
                    canvas = holder.lockCanvas()
                    modelComposer.setPixels(pixels)
                    modelComposer.drawOn(canvas)
                }

                val pixel = ranges.remove()
                val upY     = pixel.y - 1
                val downY   = pixel.y + 1
                val leftX   = pixel.x - 1
                val rightX  = pixel.x + 1


                // West (left)
                if (checkPixelPosition(leftX, pixel.y))
                    fillQueue(leftX, pixel.y)
                // East (right)
                if (checkPixelPosition(rightX, pixel.y))
                    fillQueue(rightX, pixel.y)
                // North (up)
                if (checkPixelPosition(pixel.x, upY))
                    fillQueue(pixel.x, upY)
                // South (down)
                if (checkPixelPosition(pixel.x, downY))
                    fillQueue(pixel.x, downY)

                // North-west (up left)
                if (checkPixelPosition(leftX, upY))
                    fillQueue(leftX, upY)
                // North-east (up right)
                if (checkPixelPosition(rightX, upY))
                    fillQueue(rightX, upY)
                // South-west (down left)
                if (checkPixelPosition(leftX, downY))
                    fillQueue(leftX, downY)
                // South-east (down right)
                if (checkPixelPosition(rightX, downY))
                    fillQueue(rightX, downY)
            } catch (e : Exception){
                Log.e(TAG, "Error occurred while drawing on canvas", e)
                e.printStackTrace()
            } finally {
                if (canvas != null) {
//                    Log.d(TAG, "Posting")
                    holder.unlockCanvasAndPost(canvas)
                }
            }

        }
        Log.d(TAG, "$count")
    }


    // Simple function of filling the queue with the pixel and setting pixel color
    // and the flag for the future runs
    private fun fillQueue(x : Int, y : Int){
        val pxIdx = modelComposer.width * y + x

        pixels[pxIdx]        = fillColor
        pixelsChecked[pxIdx] = true

        val px = Pixel(x, y)
        ranges.offer(px)
    }

    protected data class Pixel(val x : Int, val y : Int)
}