
In your `build.gradle`:

```gradle
 dependencies {
   debugImplementation "com.github.brianPlummer:tinydancer:0.1.2"
   releaseImplementation "com.github.brianPlummer:tinydancer-noop:0.1.2"
   testImplementation "com.github.brianPlummer:tinydancer-noop:0.1.2"
 }
```

In your `HiApplication` class:

```kotlin
class HiApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (getCurProcessName() == packageName) {
            Watched.init(this, arrayOf("com.vincent.watched.demo:service", "com.vincent.watched.demo"), interval = 500L)
        }
    }

    /**
     * get current process name
     *
     * @return
     */
    fun getCurProcessName(): String {
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
```

![watched sample](https://raw.githubusercontent.com/vzhilong/watched/master/art/watched.png "watched sample")

![profiler sample](https://raw.githubusercontent.com/vzhilong/watched/master/art/profiler.jpg "profiler sample")