package com.hemangkumar.capacitorgooglemaps

import android.app.Activity
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONException
import org.json.JSONObject

// java.lang.Object is used for its monitor: wait() and notify() are not available on kotlin.Any.
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
public class MarkersAppender {
    public class AppenderException(message: String?, cause: Throwable?) : Exception(message, cause)

    private val syncRoot = Object()
    private val executorService = Executors.newFixedThreadPool(4)

    @Volatile
    private var currentException: Throwable? = null
    private val isException = AtomicBoolean(false)

    @Throws(AppenderException::class)
    public fun addMarkers(customMapView: CustomMapView, jsMarkers: JSArray, activity: Activity, resultConsumer: (JSObject) -> Unit) {
        val customMarkers = createCustomMarkers(jsMarkers)
        addCustomMarkers(customMarkers, customMapView, activity, resultConsumer)
    }

    @Throws(AppenderException::class)
    private fun createCustomMarkers(jsMarkers: JSArray): List<CustomMarker> {
        val n = jsMarkers.length()
        val customMarkers = ArrayList<CustomMarker>(n)
        val syncRoot = Object()
        val nMarkersCounter = AtomicInteger(0)
        isException.set(false)
        currentException = null
        // prepare customMarkers as fast as possible. Really it doesn't increase the total
        // speed of this method :( noticeably.
        for (i in 0 until n) {
            if (isException.get()) {
                break
            }
            executorService.execute {
                try {
                    val jsonObject = jsMarkers.get(i) as JSONObject
                    val customMarker = CustomMarker()
                    customMarker.updateFromJSObject(JSObject.fromJSONObject(jsonObject))
                    synchronized(customMarkers) {
                        customMarkers.add(customMarker)
                    }
                    if (nMarkersCounter.addAndGet(1) == n) {
                        synchronized(syncRoot) {
                            syncRoot.notify()
                        }
                    }
                } catch (exception: JSONException) {
                    currentException = exception
                    isException.set(true)
                    synchronized(syncRoot) {
                        syncRoot.notify()
                    }
                }
            }
        }

        synchronized(syncRoot) {
            try {
                // Wait for customMarkers are populated
                // I follow https://www.baeldung.com/java-wait-notify#1-why-enclose-wait-in-a-while-loop
                while (nMarkersCounter.get() < n && !isException.get()) {
                    syncRoot.wait()
                }
            } catch (ignored: InterruptedException) {
            }
        }

        if (isException.get()) {
            throw AppenderException("exception in createCustomMarkers", currentException)
        }
        return customMarkers
    }

    private fun addCustomMarkers(
        customMarkers: List<CustomMarker>,
        customMapView: CustomMapView,
        activity: Activity,
        resultConsumer: (JSObject) -> Unit
    ) {
        val n = customMarkers.size
        val result = ArrayList<JSObject>(n)
        val nMarkersAdded = AtomicInteger(0)
        val isMarkerAdded = AtomicBoolean(false)

        executorService.execute {
            for (customMarker in customMarkers) {
                activity.runOnUiThread {
                    customMapView.addMarker(customMarker) { marker ->
                        result.add(CustomMarker.getResultForMarker(marker, customMapView.id).opt("marker") as JSObject)
                        synchronized(syncRoot) {
                            isMarkerAdded.set(true)
                            syncRoot.notify()
                        }
                        if (nMarkersAdded.addAndGet(1) == n) {
                            val jsResult = JSObject()
                            jsResult.put("mapId", customMapView.id)
                            jsResult.put("markers", JSArray.from(result.toTypedArray()))
                            resultConsumer(jsResult)
                        }
                    }
                }
                synchronized(syncRoot) {
                    try {
                        // wait for Marker is rendered before the next iteration
                        // here is a background thread -> No UI freeze
                        while (!isMarkerAdded.get()) {
                            syncRoot.wait()
                            if (!isMarkerAdded.get()) {
                                continue
                            }
                            isMarkerAdded.set(false)
                            break
                        }
                    } catch (ignored: InterruptedException) {
                        // Stops adding markers; nothing follows the loop.
                        return@execute
                    }
                }
            }
        }
    }
}
