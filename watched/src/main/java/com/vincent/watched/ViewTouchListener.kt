package com.vincent.watched

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * Created by brianplummer on 9/12/15.
 */
class ViewTouchListener(
    private val paramsF: WindowManager.LayoutParams,
    private val windowManager: WindowManager,
    private val gestureDetector: GestureDetector? = null
) : View.OnTouchListener {

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0.toFloat()
    private var initialTouchY: Float = 0.toFloat()

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        gestureDetector?.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = paramsF.x
                initialY = paramsF.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
            }
            MotionEvent.ACTION_MOVE -> {
                paramsF.x = initialX + (event.rawX - initialTouchX).toInt()
                paramsF.y = initialY + (event.rawY - initialTouchY).toInt()
                windowManager.updateViewLayout(v, paramsF)
            }
        }
        return false
    }

}
