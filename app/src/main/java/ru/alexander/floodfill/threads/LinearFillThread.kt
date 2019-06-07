package ru.alexander.floodfill.threads

import android.graphics.Canvas
import android.util.Log
import android.util.SparseBooleanArray
import android.view.SurfaceHolder
import androidx.core.util.set
import ru.alexander.floodfill.SceneModelComposer
import java.lang.Exception
import java.util.*

class LinearFillThread(
    holder: SurfaceHolder,
    modelComposer : SceneModelComposer,
    onComplete : () -> Unit
)
    : FloodFillThread(holder, modelComposer, onComplete) {

    protected lateinit var ranges               : Queue<FloodFillRange>

    companion object {
        private const val TAG = "LinearFillThread"
    }

    init {
        this.modelComposer.getPixels(pixels)
    }


    // Fills the specified point on the bitmap with the currently selected fill
    // color.
    // int x, int y: The starting coords for the fill
    override fun run() {

        Log.d(TAG, "Drawing thread started")


        // initial preparation
        // resetting or initializing all arrays
        prepare()

        // we check if the pixel colors the user clicked on are the same as fill color
        // if they are, we just exit the thread
        if (compareInitialColors()){
            Log.d(TAG, "Initial colors are the same")
            return
        }


        linearFill(modelComposer.x, modelComposer.y)
        var range : FloodFillRange

        while (isRunning && ranges.size > 0) {
            var canvas : Canvas? = null
            try {
                sleep(1000 / speed)
                canvas = holder.lockCanvas()

                range = ranges.remove()
                // **Check Above and Below Each Pixel in the Floodfill Range
                val upY         = range.Y - 1 // so we can pass the y coord by ref
                val downY       = range.Y + 1
//                var downPxIdx   = modelComposer.width * (downY) + range.startX
//                var upPxIdx     = modelComposer.width * (upY) + range.startX
                if (!checkPixelPosition(range.startX, upY)){
                    Log.d(TAG, "${range.startX} $upY")
                }


                var upCount = 0
                var downCount = 0
                for (i in range.startX..range.endX) {
                    // *Start Fill Upwards
                    // if we're not above the top of the bitmap and the pixel above
                    // this one is within the color tolerance
                    if (checkPixelPosition(range.startX + upCount, upY))
                        linearFill(i, upY)

                    // *Start Fill Downwards
                    // if we're not below the bottom of the bitmap and the pixel
                    // below this one is within the color tolerance
                    if (checkPixelPosition(range.startX + downCount, downY))
                        linearFill(i, downY)

                    downCount++
                    upCount++

                }
                modelComposer.setPixels(pixels)
                modelComposer.drawOn(canvas)
            }catch (e : Exception){
                Log.e(TAG, "Error occurred while drawing on canvas", e)
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    Log.d(TAG, "Posting")
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
        Log.d(TAG, "Drawing thread ended")
        onComplete()
    }


    override fun prepare() {
        // Called before starting flood-fill
        super.prepare()
        ranges = LinkedList<FloodFillRange>()
    }


    // Finds the furthermost left and right boundaries of the fill area
    // on a given y coordinate, starting from a given x coordinate, filling as
    // it goes.
    // Adds the resulting horizontal range to the queue of floodfill ranges,
    // int x, int y: The starting coordinates
    protected fun linearFill(x: Int, y: Int) {
        Log.d(TAG, "Fill called ${callCounter++}")
        // ***Find Left Edge of Color Area
        var lFillLoc = x // the location to check/fill on the left
        var pxIdx = modelComposer.width * y + x


        while (true) {
            // **fill with the color
            pixels[pxIdx] = fillColor

            // **indicate that this pixel has already been checked and filled
            pixelsChecked[pxIdx] = true

            // **de-increment
            lFillLoc-- // de-increment counter
            pxIdx-- // de-increment pixel index

            // **exit loop if we're at edge of bitmap or color area
            if (lFillLoc < 0 || pixelsChecked[pxIdx] || !checkPixel(pxIdx)) {
                break
            }
        }

        lFillLoc++

        // ***Find Right Edge of Color Area
        var rFillLoc = x // the location to check/fill on the left

        pxIdx = modelComposer.width * y + x

        while (true) {
            // **fill with the color
            pixels[pxIdx] = fillColor

            // **indicate that this pixel has already been checked and filled
            pixelsChecked[pxIdx] = true

            // **increment
            rFillLoc++ // increment counter
            pxIdx++ // increment pixel index

            // **exit loop if we're at edge of bitmap or color area
            if (rFillLoc >= modelComposer.width || pixelsChecked[pxIdx] || !checkPixel(pxIdx)) {
                break
            }
        }

        rFillLoc--

        // add range to queue
        val r = FloodFillRange(lFillLoc, rFillLoc, y)

        ranges.offer(r)
    }

    protected data class FloodFillRange(var startX: Int, var endX: Int, var Y: Int)

}