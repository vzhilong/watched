package com.vincenthwang.watched

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onCheckedChange
import org.jetbrains.anko.sdk27.coroutines.onClick
import kotlin.math.roundToInt

@SuppressLint("StaticFieldLeak")
object Watched {

    private lateinit var application: Application
    //    private lateinit var watchedSave: SharedPreferences

    private var meterView: TextView? = null
    private var alertDialog: AlertDialog? = null
    private var checkInterval = 750L
    private var showMemDetail = false

    private val processNames = mutableListOf<String>()
    private val watchedProcessNames = mutableListOf<String>()


    private val timerHandler: Handler = object : Handler(HandlerThread("watcher-timer").apply { start() }.looper) {
        override fun handleMessage(msg: Message?) {
            val newWatched = mutableListOf<String>()
            synchronized(watchedProcessNames) {
                newWatched.addAll(watchedProcessNames)
            }

            if (newWatched.isEmpty()) {
                application.runOnUiThread {
                    meterView?.text = "select one process as least."
                }
                return
            }

            sendEmptyMessageDelayed(0, checkInterval)

            val watchedProcesses = mutableMapOf<String, Int>().toSortedMap()
            newWatched.forEach {
                watchedProcesses[it] = -1
                val res = ShellUtils.execCmd("/system/bin/pidof $it", false, true)
                if (res.successMsg.isNotEmpty() && res.successMsg.isDigitsOnly()) {
                    watchedProcesses[it] = res.successMsg.toInt()
                }
            }

            val cs = TextUtils.concat(
                *watchedProcesses.map {
                    val processName = it.key
                    val pid = it.value

                    if (pid == -1) {
                        return@map TextUtils.concat(
                            ellipsizeProcess("$processName[dead]: 0M"),
                            "\n"
                        )
                    }

                    application.activityManager.getProcessMemoryInfo(intArrayOf(pid))[0]?.let { memInfo ->
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            ellipsizeProcess("$processName[$pid]: ${(memInfo.totalPrivateClean + memInfo.totalPrivateDirty) / 1024}M")
                        } else {
                            val javaHeap = memInfo.getMemoryStat("summary.java-heap").toFloat() / 1024
                            val nativeHeap = memInfo.getMemoryStat("summary.native-heap").toFloat() / 1024
                            val graphics = memInfo.getMemoryStat("summary.graphics").toFloat() / 1024
                            val stack = memInfo.getMemoryStat("summary.stack").toFloat() / 1024
                            val code = memInfo.getMemoryStat("summary.code").toFloat() / 1024

                            if (showMemDetail) {
                                TextUtils.concat(
                                    ellipsizeProcess("$processName[$pid]: ${(javaHeap + nativeHeap + graphics + stack + code).roundToInt()}M"),
                                    "\n",
                                    """
                                    |  -- Java Heap: ${String.format("%.1f", javaHeap)}M
                                    |  -- Native Heap: ${String.format("%.1f", nativeHeap)}M
                                    |  -- Graphics: ${String.format("%.1f", graphics)}M
                                    |  -- Stack: ${String.format("%.1f", stack)}M
                                    |  -- Code: ${String.format("%.1f", code)}M
                                    """.trimMargin("|"),
                                    "\n"
                                )
                            } else {
                                TextUtils.concat(
                                    ellipsizeProcess("$processName[$pid]: ${(javaHeap + nativeHeap + graphics + stack + code).roundToInt()}M"),
                                    "\n"
                                )
                            }
                        }
                    }
                }.toTypedArray()
            )

            application.runOnUiThread {
                meterView?.text = cs.subSequence(0, cs.length - 1)
            }
        }
    }


    private val simpleOnGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent?): Boolean {

            var newShowDetail = showMemDetail
            val newWatched = mutableListOf<String>()
            synchronized(watchedProcessNames) {
                newWatched.addAll(watchedProcessNames)
            }

            alertDialog = application.alert {
                customView {
                    linearLayout {
                        orientation = LinearLayout.VERTICAL
                        topPadding = dip(10)
                        horizontalPadding = dip(20)

                        textView("Make process checked:") {
                            verticalPadding = dip(12)
                            textSize = 18F
                        }
                        processNames.forEach {
                            checkBox(it, checked = newWatched.contains(it)) {
                                verticalPadding = dip(4)
                                textSize = 16F
                                singleLine = true
                                ellipsize = TextUtils.TruncateAt.START
                            }.onCheckedChange { _, isChecked ->
                                if (isChecked) {
                                    newWatched.add(it)
                                } else {
                                    newWatched.remove(it)
                                }
                            }
                        }

                        linearLayout {
                            orientation = LinearLayout.HORIZONTAL

                            textView("Show usage detail:") {
                                verticalPadding = dip(12)
                                textSize = 18F
                            }

                            checkBox("", checked = newShowDetail)
                                .onCheckedChange { _, isChecked -> newShowDetail = isChecked }
                        }


                        button("OK") {
                            background = null
                            textSize = 18F
                        }.lparams {
                            gravity = Gravity.END
                        }.onClick {
                            alertDialog?.cancel()
                            showMemDetail = newShowDetail
                            synchronized(watchedProcessNames) {
                                watchedProcessNames.clear()
                                watchedProcessNames.addAll(newWatched)

                                timerHandler.removeMessages(0)
                                timerHandler.sendEmptyMessage(0)
                            }
                        }

                    }

                }
            }.build() as AlertDialog

            alertDialog!!.apply {
                window?.setType(PermissionCompat.flag)
            }.show()
            return true
        }
    }

    fun init(app: Application, process: Array<String>, interval: Long = 750L, memDetail: Boolean = true) {
        application = app
        checkInterval = interval
        showMemDetail = memDetail
        processNames.addAll(process)
        watchedProcessNames.addAll(process)
    }

    fun show() {
        if (!overlayPermRequest(application)) {
            return
        }

        if (meterView != null) {
            return
        }

        val paramsF = WindowManager.LayoutParams(
            application.dip(216),
            wrapContent,
            PermissionCompat.flag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = 100
            y = 200
            gravity = Gravity.TOP or Gravity.START
        }

        // create gesture detector to listen for double taps
        val gestureDetector = GestureDetector(
            application,
            simpleOnGestureListener
        )
        meterView = (LayoutInflater.from(application).inflate(
            R.layout.meter_view, null
        ) as TextView).apply {
            // attach touch listener
            setOnTouchListener(
                ViewTouchListener(
                    paramsF,
                    application.windowManager,
                    gestureDetector
                )
            )

            // disable haptic feedback
            isHapticFeedbackEnabled = false
        }

        // add view to the window
        application.windowManager.addView(meterView, paramsF)

        timerHandler.sendEmptyMessage(0)
    }

    fun hide() {
        if (meterView != null) {
            application.windowManager.removeView(meterView)
            timerHandler.removeMessages(0)
            meterView = null
        }
    }


    private fun overlayPermRequest(context: Context): Boolean {
        var permNeeded = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(context)) {
                permNeeded = true
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.packageName)
                )
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }
        return permNeeded
    }

    private fun ellipsizeProcess(s: String): CharSequence {
        return TextUtils.ellipsize(
            SpannableString(s).apply {
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(RelativeSizeSpan(1.3F), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            },

            meterView?.paint,
            application.dip(200) * 1.0F,
            TextUtils.TruncateAt.START
        )
    }
}