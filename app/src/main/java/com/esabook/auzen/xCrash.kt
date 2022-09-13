package com.esabook.auzen

import android.app.Application
import org.json.JSONObject
import timber.log.Timber
import xcrash.ICrashCallback
import xcrash.TombstoneManager
import xcrash.TombstoneParser
import xcrash.XCrash
import java.io.File
import java.io.FileWriter


object xCrash {

    fun init(app: Application){
        // callback for java crash, native crash and ANR
        val callback = ICrashCallback { logPath, emergency ->
            Timber.d(
                "log path: " + (logPath ?: "(null)") + ", emergency: " + (emergency ?: "(null)")
            )
            if (emergency != null) {
                debug(app, logPath!!, emergency)

                // Disk is exhausted, send crash report immediately.
                sendThenDeleteCrashLog(logPath, emergency)
            } else {
                // Add some expanded sections. Send crash report at the next time APP startup.

                // OK
                TombstoneManager.appendSection(logPath, "expanded_key_1", "expanded_content")
                TombstoneManager.appendSection(
                    logPath,
                    "expanded_key_2",
                    "expanded_content_row_1\nexpanded_content_row_2"
                )

                // Invalid. (Do NOT include multiple consecutive newline characters ("\n\n") in the content string.)
                // TombstoneManager.appendSection(logPath, "expanded_key_3", "expanded_content_row_1\n\nexpanded_content_row_2");
                debug(app, logPath!!, null)
            }
        }

        Timber.d("xCrash SDK init: start")

        // Initialize xCrash.

        // Initialize xCrash.
        XCrash.init(
            app, XCrash.InitParameters()
                .setAppVersion(BuildConfig.VERSION_NAME)
                .setJavaRethrow(true)
                .setJavaLogCountMax(10) //.setJavaDumpAllThreadsAllowList(new String[]{"^main$", "^Binder:.*", ".*Finalizer.*"})
                //.setJavaDumpAllThreadsCountMax(10)
                .setJavaCallback(callback)
                .setNativeRethrow(false)
                .setNativeLogCountMax(10)
                .setNativeDumpAllThreads(true) //.setNativeDumpAllThreadsAllowList(new String[]{"^xcrash\\.sample$", "^Signal Catcher$", "^Jit thread pool$", ".*(R|r)ender.*", ".*Chrome.*"})
                //.setNativeDumpAllThreadsCountMax(10)
                .setNativeCallback(callback)
                .setAnrRethrow(true)
                .setAnrLogCountMax(10)
                .setAnrCallback(callback)
                .setPlaceholderCountMax(3)
                .setPlaceholderSizeKb(512)
                .setLogFileMaintainDelayMs(1000)
        )

        Timber.d("xCrash SDK init: end")

        // Send all pending crash log files.

        // Send all pending crash log files.
        Thread {
            for (file in TombstoneManager.getAllTombstones()) {
                sendThenDeleteCrashLog(file.absolutePath, null)
            }
        }.start()
    }

    private fun debug(app: Application, logPath: String, emergency: String?) {
        // Parse and save the crash info to a JSON file for debugging.
        var writer: FileWriter? = null
        try {
            val debug = File(app.externalCacheDir.toString() + "/tombstones/debug.json")
            debug.createNewFile()
            writer = FileWriter(debug, false)
            writer.write(JSONObject((TombstoneParser.parse(logPath, emergency) as Map<*, *>?)!!).toString())
        } catch (e: Exception) {
            Timber.d("debug failed", e)
        } finally {
            if (writer != null) {
                try {
                    writer.close()
                } catch (ignored: Exception) {
                }
            }
        }
    }

    private fun sendThenDeleteCrashLog(logPath: String, emergency: String?) {
        // Parse
        //Map<String, String> map = TombstoneParser.parse(logPath, emergency);
        //String crashReport = new JSONObject(map).toString();

        // Send the crash report to server-side.
        // ......

        // If the server-side receives successfully, delete the log file.
        //
        // Note: When you use the placeholder file feature,
        //       please always use this method to delete tombstone files.
        //
        //TombstoneManager.deleteTombstone(logPath);
    }
}