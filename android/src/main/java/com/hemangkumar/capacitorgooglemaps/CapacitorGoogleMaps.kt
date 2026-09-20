package com.hemangkumar.capacitorgooglemaps

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.view.MotionEvent
import android.view.ViewGroup
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.google.android.gms.maps.MapsInitializer
import java.util.UUID

@CapacitorPlugin(
    name = "CapacitorGoogleMaps",
    permissions = [
        Permission(
            strings = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION],
            alias = "location"
        )
    ]
)
public class CapacitorGoogleMaps :
    Plugin(),
    CustomMapViewEvents {
    private val customMapViews = HashMap<String, CustomMapView>()
    internal var devicePixelRatio: Float? = null
    private var lastEventChainId: String? = null
    public val previousEvents: MutableList<MotionEvent> = ArrayList()
    private var delegateTouchEventsToMapId: String? = null

    private fun findMapView(mapId: String?): CustomMapView? = mapId?.let { customMapViews[it] }

    @PluginMethod
    public fun elementFromPointResult(call: PluginCall) {
        // This method should be called after we requested the WebView through notifyListeners("didRequestElementFromPoint").
        // It should tell us if the exact point that was being touched, is from an element in which a MapView exists.
        // Otherwise it is a 'normal' HTML element, and we should thus not delegate touch events.
        val eventChainId = call.getString("eventChainId")
        if (eventChainId != null && eventChainId == lastEventChainId) {
            if (call.getBoolean("isSameNode", false) == true) {
                // The WebView apparently has decides the touched point belongs to a certain MapView.
                // Now we should find out which one exactly.
                val mapId = call.getString("mapId")
                if (mapId != null) {
                    delegateTouchEventsToMapId = mapId
                }
            }
        }
        call.resolve()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun load() {
        super.load()

        MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST, null)

        bridge.webView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                // Throw away all previous state when starting a new touch gesture.
                // The framework may have dropped the up or cancel event for the previous gesture
                // due to an app switch, ANR, or some other state change.
                delegateTouchEventsToMapId = null
                previousEvents.clear()

                // Initialize JSObjects for resolve().
                val result = JSObject()
                val point = JSObject()

                // Generate a UUID, and assign it to lastId.
                // This way we can make sure we are always referencing the last chain of events.
                lastEventChainId = UUID.randomUUID().toString()
                // Then add it to result object, so the WebView can reference the correct events when needed.
                result.put("eventChainId", lastEventChainId)

                // Get the touched position.
                val x = event.x.toInt()
                val y = event.y.toInt()

                // Since pixels on a webpage are calculated differently, should convert them first.
                // Convert it to 'real' pixels in WebView by using devicePixelRatio.
                val devicePixelRatio = devicePixelRatio
                if (devicePixelRatio != null && devicePixelRatio > 0) {
                    point.put("x", (x / devicePixelRatio).toDouble())
                    point.put("y", (y / devicePixelRatio).toDouble())

                    // Then add it to result object.
                    result.put("point", point)
                }

                // Then notify the listener that we request to let the WebView determine
                // if the element touched is the same node as where some MapView is attached to.
                notifyListeners("didRequestElementFromPoint", result)
            }

            if (delegateTouchEventsToMapId != null) {
                val customMapView = findMapView(delegateTouchEventsToMapId)
                if (customMapView != null) {
                    // Apparently, all touch events should be delegated to a specific MapView.

                    // If previous events exist, we should execute those first
                    if (previousEvents.isNotEmpty()) {
                        for (previousEvent in previousEvents) {
                            // Delegate this previous event to the MapView.
                            dispatchTouchEvent(previousEvent, customMapView)
                        }
                        // Since we delegated all previous events, we can now forget about them.
                        previousEvents.clear()
                    }

                    // Finally delegate the current event to the MapView.
                    dispatchTouchEvent(event, customMapView)
                }
            } else {
                // If delegateTouchEventsToMapId is not set, but it could still be set later!
                // So we should save all past events.
                // That way we can still execute them later on.
                // It is important that we use MotionEvent.obtain() to copy the event first.
                // Otherwise the event does not work properly when delegating it later on.
                previousEvents.add(MotionEvent.obtain(event))
            }

            false
        }
    }

    private fun dispatchTouchEvent(event: MotionEvent, customMapView: CustomMapView) {
        // A map that is delegated to has always been expected to have its view by now.
        val mapView = checkNotNull(customMapView.mapView) { "The map view has not been created" }
        val offsetViewBounds = Rect()
        // returns the visible bounds
        mapView.getDrawingRect(offsetViewBounds)
        // calculates the relative coordinates to the parent
        val parentViewGroup = bridge.webView.parent as ViewGroup
        parentViewGroup.offsetDescendantRectToMyCoords(mapView, offsetViewBounds)

        val relativeTop = offsetViewBounds.top
        val relativeLeft = offsetViewBounds.left

        // Set location with offset points,
        // because if a map is positioned with a different top and left point than the WebView,
        // that should be accounted for.
        event.setLocation(event.x - relativeLeft, event.y - relativeTop)

        mapView.dispatchTouchEvent(event)
    }

    override fun handleOnStart() {
        super.handleOnStart()
        for (customMapView in customMapViews.values) {
            customMapView.handleOnStart()
        }
    }

    override fun handleOnResume() {
        super.handleOnResume()
        for (customMapView in customMapViews.values) {
            customMapView.handleOnResume()
        }
    }

    override fun handleOnPause() {
        for (customMapView in customMapViews.values) {
            customMapView.handleOnPause()
        }
        super.handleOnPause()
    }

    override fun handleOnStop() {
        super.handleOnStop()
        for (customMapView in customMapViews.values) {
            customMapView.handleOnStop()
        }
    }

    override fun handleOnDestroy() {
        for (customMapView in customMapViews.values) {
            customMapView.handleOnDestroy()
        }
        super.handleOnDestroy()
    }

    @PluginMethod
    public fun initialize(call: PluginCall) {
        /*
         *  TODO: Check API key
         */
        devicePixelRatio = call.getFloat("devicePixelRatio")
        call.resolve()
    }

    @PluginMethod
    public fun createMap(call: PluginCall) {
        bridge.saveCall(call)
        val callbackId = call.callbackId

        val boundingRect = BoundingRect()
        boundingRect.updateFromJSObject(call.getObject("boundingRect"))

        val mapCameraPosition = MapCameraPosition()
        mapCameraPosition.updateFromJSObject(call.getObject("cameraPosition"), null)

        val mapPreferences = MapPreferences()
        mapPreferences.updateFromJSObject(call.getObject("preferences"))

        activity.runOnUiThread {
            val customMapView = CustomMapView(activity, this)

            customMapViews[customMapView.id] = customMapView

            customMapView.createMap(callbackId, boundingRect, mapCameraPosition, mapPreferences)

            customMapView.addToView(bridge.webView.parent as ViewGroup)

            // Bring the WebView in front of the MapView
            // This allows us to overlay the MapView in HTML/CSS
            bridge.webView.bringToFront()

            // Hide the background
            bridge.webView.setBackgroundColor(Color.TRANSPARENT)
            bridge.webView.loadUrl("javascript:document.documentElement.style.backgroundColor = 'transparent';void(0);")
        }
    }

    @PluginMethod
    public fun updateMap(call: PluginCall) {
        val mapId = call.getString("mapId")

        activity.runOnUiThread {
            val customMapView = findMapView(mapId)

            if (customMapView != null) {
                customMapView.mapPreferences.updateFromJSObject(call.getObject("preferences"))

                call.resolve(customMapView.invalidateMap())
            } else {
                call.reject("map not found")
            }
        }
    }

    @PluginMethod
    public fun getMap(call: PluginCall) {
        val mapId = call.getString("mapId")

        activity.runOnUiThread {
            val customMapView = findMapView(mapId)

            if (customMapView != null) {
                call.resolve(customMapView.getMap())
            } else {
                call.reject("map not found")
            }
        }
    }

    @PluginMethod(returnType = PluginMethod.RETURN_NONE)
    public fun removeMap(call: PluginCall) {
        val mapId = call.getString("mapId")

        activity.runOnUiThread {
            val customMapView = findMapView(mapId)

            if (customMapView != null) {
                customMapView.removeFromView(bridge.webView.parent as ViewGroup)
                customMapViews.remove(customMapView.id)
                call.resolve()
            } else {
                call.reject("map not found")
            }
        }
    }

    @PluginMethod(returnType = PluginMethod.RETURN_NONE)
    public fun clearMap(call: PluginCall) {
        val mapId = call.getString("mapId")

        activity.runOnUiThread {
            val customMapView = findMapView(mapId)

            if (customMapView != null) {
                customMapView.clear()
                call.resolve()
            } else {
                call.reject("map not found")
            }
        }
    }

    @PluginMethod
    public fun moveCamera(call: PluginCall) {
        val mapId = call.getString("mapId")

        activity.runOnUiThread {
            val customMapView = findMapView(mapId)

            if (customMapView != null) {
                val useCurrentCameraPositionAsBase = call.getBoolean("useCurrentCameraPositionAsBase", true) == true
                val currentCameraPosition = if (useCurrentCameraPositionAsBase) customMapView.cameraPosition else null

                customMapView.mapCameraPosition.updateFromJSObject(call.getObject("cameraPosition"), currentCameraPosition)

                customMapView.moveCamera(call.getInt("duration", 0))

                call.resolve()
            } else {
                call.reject("map not found")
            }
        }
    }

    override fun onMapReady(callbackId: String?, result: JSObject?) {
        // createMap saved this call, so it is there.
        val call = bridge.getSavedCall(callbackId) ?: return
        call.resolve(result)
        bridge.releaseCall(call)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didTapInfoWindow(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_TAP_INFO_WINDOW)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didCloseInfoWindow(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_CLOSE_INFO_WINDOW)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didTapMap(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_TAP_MAP)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didLongPressMap(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_LONG_PRESS_MAP)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didTapMarker(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_TAP_MARKER)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didBeginDraggingMarker(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_BEGIN_DRAGGING_MARKER)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didDragMarker(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_DRAG_MARKER)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didEndDraggingMarker(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_END_DRAGGING_MARKER)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didTapMyLocationButton(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_TAP_MY_LOCATION_BUTTON)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didTapMyLocationDot(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_TAP_MY_LOCATION_DOT)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didTapPoi(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_TAP_POI)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didBeginMovingCamera(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_BEGIN_MOVING_CAMERA)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didMoveCamera(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_MOVE_CAMERA)
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public fun didEndMovingCamera(call: PluginCall) {
        setCallbackIdForEvent(call, CustomMapView.EVENT_DID_END_MOVING_CAMERA)
    }

    public fun setCallbackIdForEvent(call: PluginCall, eventName: String) {
        call.keepAlive = true
        val callbackId = call.callbackId

        val customMapView = findMapView(call.getString("mapId"))

        if (customMapView != null) {
            val preventDefault = call.getBoolean("preventDefault", false)

            activity.runOnUiThread {
                customMapView.setCallbackIdForEvent(callbackId, eventName, preventDefault)
            }
        }
    }

    override fun resultForCallbackId(callbackId: String?, result: JSObject?) {
        bridge.getSavedCall(callbackId)?.resolve(result)
    }

    @PluginMethod
    public fun addMarker(call: PluginCall) {
        val mapId = call.getString("mapId")

        activity.runOnUiThread {
            val customMapView = findMapView(mapId)

            if (customMapView != null) {
                val customMarker = CustomMarker()
                customMarker.updateFromJSObject(call.data)

                customMapView.addMarker(customMarker) { marker ->
                    call.resolve(CustomMarker.getResultForMarker(marker, mapId))
                }
            } else {
                call.reject("map not found")
            }
        }
    }

    @PluginMethod
    public fun addMarkers(call: PluginCall) {
        val customMapView = findMapView(call.getString("mapId"))
        if (customMapView == null) {
            call.reject("map not found")
            return
        }
        try {
            val jsMarkers = call.getArray("markers", JSArray()) ?: JSArray()
            MarkersAppender().addMarkers(customMapView, jsMarkers, activity) { call.resolve(it) }
        } catch (e: MarkersAppender.AppenderException) {
            call.reject("exception in addMarkers", ex = e)
        }
    }

    @PluginMethod(returnType = PluginMethod.RETURN_NONE)
    public fun removeMarker(call: PluginCall) {
        val mapId = call.getString("mapId")

        activity.runOnUiThread {
            val customMapView = findMapView(mapId)

            if (customMapView != null) {
                customMapView.removeMarker(call.getString("markerId"))

                call.resolve()
            } else {
                call.reject("map not found")
            }
        }
    }

    @PluginMethod
    public fun addPolygon(call: PluginCall) {
        val mapId = call.getString("mapId")

        activity.runOnUiThread {
            val customMapView = findMapView(mapId)

            if (customMapView != null) {
                val customPolygon = CustomPolygon()
                customPolygon.updateFromJSObject(call.data)

                customMapView.addPolygon(customPolygon) { polygon ->
                    call.resolve(customPolygon.getResultForPolygon(polygon, mapId))
                }
            } else {
                call.reject("map not found")
            }
        }
    }

    @PluginMethod(returnType = PluginMethod.RETURN_NONE)
    public fun removePolygon(call: PluginCall) {
        val mapId = call.getString("mapId")

        activity.runOnUiThread {
            val customMapView = findMapView(mapId)

            if (customMapView != null) {
                customMapView.removePolygon(call.getString("polygonId"))

                call.resolve()
            } else {
                call.reject("map not found")
            }
        }
    }
}
