### 1, How to use it

In root `build.gradle`:

```gradle
allprojects {
    repositories {
        google()
        jcenter()

        maven { url 'https://jitpack.io' }
    }
}
```

In your app `build.gradle`:

```gradle
 dependencies {
    debugImplementation 'com.github.vzhilong.watched:watched:0.1.0'
    releaseImplementation 'com.github.vzhilong.watched:watched_noop:0.1.0'
    testImplementation 'com.github.vzhilong.watched:watched_noop:0.1.0'
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

### 2, About watched

Usually we monitor our process's memory usage(java heap, native heap, graphics, stack, code and others) by Profiler, like this:

![profiler sample](https://raw.githubusercontent.com/vzhilong/watched/master/art/profiler.jpg "profiler sample")

Now we can get the same result on the phone(java heap, native heap, graphics, stack, code, but others not include):

![watched sample](https://raw.githubusercontent.com/vzhilong/watched/master/art/watched.png "watched sample")
![dialog config](https://raw.githubusercontent.com/vzhilong/watched/master/art/dialog.jpg "dialog config")