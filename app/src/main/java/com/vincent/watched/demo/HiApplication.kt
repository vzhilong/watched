package com.vincent.watched.demo

import android.app.Application
import android.os.Process
import com.vincent.watched.Watched
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

class HiApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (getCurProcessName() == packageName) {
            Watched.init(this, arrayOf("com.vincent.watched.demo:service", "com.vincent.watched.demo"), interval = 500L)
        }
    }

    /**
     * 获取当前进程名
     *
     * @return
     */
    fun getCurProcessName(): String {
        // 获取进程名有很多种方式，这种比较推荐
        var cmdlineReader: BufferedReader? = null
        try {
            cmdlineReader = BufferedReader(
                InputStreamReader(
                    FileInputStream("/proc/" + Process.myPid() + "/cmdline"), "iso-8859-1"
                )
            )
            var c: Int
            val processName = StringBuilder()
            while (true) {
                c = cmdlineReader.read()
                if (c <= 0) {
                    break
                }
                processName.append(c.toChar())
            }
            return processName.toString()
        } catch (e: IOException) {

        } finally {
            if (cmdlineReader != null) {
                try {
                    cmdlineReader.close()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        return ""
    }
}