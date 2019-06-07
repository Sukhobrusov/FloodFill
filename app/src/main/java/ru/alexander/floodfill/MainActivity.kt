package ru.alexander.floodfill

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.hiding_toolbar.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random



class MainActivity : AppCompatActivity() {

    private var bitmapDisposable    : Disposable? = null
    private var editTextDisposable  : Disposable? = null
    private var seekDisposable      : Disposable? = null
    private var currentWidth    = 255
    private var currentHeight   = 255

    companion object {
        private const val TAG = "MainActivity"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.app_name)


        val editTextObservable  = getEditTextObservable()
        val bitmapObservable= getBitmapObservable()
        val seekObservable      = getSeekBarObservable()

        val adapter = ArrayAdapter.createFromResource(this, R.array.spinner_algorithms, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, item: Int, p3: Long) {
                Log.d(TAG, "Selected - $item")

                surfaceView.algorighm = when(item){
                    0       -> PictureSurfaceView.Algorighm.LINEAR
                    1       -> PictureSurfaceView.Algorighm.FON_NEYMAN
                    2       -> PictureSurfaceView.Algorighm.MOORE
                    else    -> PictureSurfaceView.Algorighm.LINEAR
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        widthEditText.setOnEditorActionListener { _, id, _ -> checkDone(id) }
        heightEditText.setOnEditorActionListener { _, id, _ -> checkDone(id) }

        editTextDisposable = editTextObservable.subscribe { heightEditText.setText("$it") }

        // wait 100 ms until user stops changing the value and then set the speed
        seekDisposable =  seekObservable
            .debounce(100, TimeUnit.MILLISECONDS)
            .subscribe {
                val speed = remap(it, 0, 100, 15,  70)
                Log.d(TAG, "Progress - $speed")
                surfaceView.speed = speed

        }


        forceStopButton.setOnClickListener {
            surfaceView.forceStop()
        }
        generateButton.setOnClickListener {
            if (checkEditText())
            {
                val width   = widthEditText.text.toString().toInt()
                val height  = heightEditText.text.toString().toInt()

                currentHeight   = height
                currentWidth    = width

                bitmapDisposable?.dispose()
                bitmapDisposable = bitmapObservable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe ({
                        surfaceView.setBitmap(
                            it
//                            ,
//                            width = widthEditText.text.toString().toInt(),
//                            height = heightEditText.text.toString().toInt()
                        )
                    }, {error -> error.printStackTrace()})
            }


        }

    }


    private fun checkDone(id: Int): Boolean {
        return if (id == EditorInfo.IME_ACTION_DONE) {
            generateButton.performClick()
            true
        } else
            false
    }


    // Clear all the data
    override fun onDestroy() {
        super.onDestroy()
        editTextDisposable?.dispose()
        seekDisposable?.dispose()
        bitmapDisposable?.dispose()
    }


    private fun checkEditText(): Boolean {
        val width = widthEditText.text.toString().toIntOrNull()
        val height = heightEditText.text.toString().toIntOrNull()
        if (width == null || width < 100 || width > 1024) {
            widthEditText.error = "Введите чилсо от 100 до 1024"
            return false
        }
        if (height == null || height < 100 || height > 1024) {
            heightEditText.error = "Введите число от 100 до 1024"
            return false
        }
        return true
    }


    //Function that helps us to remap ranges
    // Example:
    // oldRange : 0-100, oldValue : 50
    // newRange : 15-55, newValue : 35
    private fun remap(oldValue : Int,
                      oldMin : Int,
                      oldMax : Int,
                      newMin : Int,
                      newMax : Int) : Int {

        val oldRange = oldMax - oldMin
        val newRange = newMax - newMin
        return (((oldValue - oldMin) * newRange) / oldRange) + newMin
    }


    // Function that returns observable over the widthEditText
    private fun getEditTextObservable() : Observable<Int> {
        return Observable.create<Int> {
            val watcher = object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    val number = text.toString().toIntOrNull()
                    if (number != null) {
                        it.onNext(number)
                    }
                }
            }
            widthEditText.addTextChangedListener(watcher)
            it.setCancellable {
                widthEditText.removeTextChangedListener(watcher)
            }
        }
    }

    // Function that returns observable over the SeekBar
    // whenever the progress is changed, we call onNext
    private fun getSeekBarObservable() : Observable<Int> {
        return Observable.create<Int> {
            val listener = object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    it.onNext(progress)
                }
            }

            seekBar.setOnSeekBarChangeListener(listener)
        }
    }

    // Function that returns random observable over random bitmap
    private fun getBitmapObservable(): Observable<Bitmap> {
        return Observable.create<Bitmap>{
            val randomX = Random.nextInt(0, 2048-currentWidth)
            val randomY = Random.nextInt(0, 2048-currentHeight)
            val region = Rect(randomX, randomY, randomX + currentWidth, randomY + currentHeight)

            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.RGB_565
            val inputStream = resources.openRawResource(0 + R.drawable.random)
            val bm = BitmapRegionDecoder.newInstance(inputStream, false)
                .decodeRegion(region, options)
            it.onNext(bm)
            it.setCancellable { bm?.recycle() }
        }

    }


    private inner class SpinnerAdapter(context : Context, textViewResourceId : Int, objects : Array<String>)
        : ArrayAdapter<String>(context,textViewResourceId, objects) {

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getDropDownView(position, convertView, parent)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getView(position, convertView, parent)
        }


        fun getCustomView(){}

    }

}