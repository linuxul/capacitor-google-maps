package com.hemangkumar.capacitorgooglemaps

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.getcapacitor.JSObject
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.gms.maps.model.Polygon
import java.util.UUID

public class CustomMapView(private val activity: AppCompatActivity, private val customMapViewEvents: CustomMapViewEvents?) :
    OnMapReadyCallback,
    GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnInfoWindowCloseListener,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMarkerDragListener,
    GoogleMap.OnMyLocationClickListener,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnPoiClickListener,
    GoogleMap.OnCameraMoveStartedListener,
    GoogleMap.OnCameraMoveListener,
    GoogleMap.OnCameraIdleListener {
    public val id: String = UUID.randomUUID().toString()

    internal var mapView: MapView? = null
    internal var googleMap: GoogleMap? = null

    private val markers = HashMap<String, Marker>()
    private val polygons = HashMap<String, Polygon>()

    internal var savedCallbackIdForCreate: String? = null

    internal var savedCallbackIdForDidTapInfoWindow: String? = null

    internal var savedCallbackIdForDidCloseInfoWindow: String? = null

    internal var savedCallbackIdForDidTapMap: String? = null

    internal var savedCallbackIdForDidLongPressMap: String? = null

    internal var savedCallbackIdForDidTapMarker: String? = null
    internal var preventDefaultForDidTapMarker: Boolean = false

    internal var savedCallbackIdForDidBeginDraggingMarker: String? = null

    internal var savedCallbackIdForDidDragMarker: String? = null

    internal var savedCallbackIdForDidEndDraggingMarker: String? = null

    internal var savedCallbackIdForDidTapMyLocationButton: String? = null
    internal var preventDefaultForDidTapMyLocationButton: Boolean = false

    internal var savedCallbackIdForDidTapMyLocationDot: String? = null

    internal var savedCallbackIdForDidTapPoi: String? = null

    internal var savedCallbackIdForDidBeginMovingCamera: String? = null
    internal var savedCallbackIdForDidMoveCamera: String? = null
    internal var savedCallbackIdForDidEndMovingCamera: String? = null

    // Both are set by createMap, which the plugin calls right after it constructs this object.
    public lateinit var mapCameraPosition: MapCameraPosition
    public lateinit var mapPreferences: MapPreferences

    // Using the map before onMapReady has always thrown.
    private fun requireGoogleMap(): GoogleMap = checkNotNull(googleMap) { "The map is not ready yet" }

    private fun hasPermission(): Boolean =
        ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        // populate `googleMap` variable for other methods to use
        this.googleMap = googleMap

        // set controls
        val googleMapUISettings = googleMap.uiSettings
        googleMapUISettings.isIndoorLevelPickerEnabled = mapPreferences.controls.getBoolean(MapPreferencesControls.INDOOR_LEVEL_PICKER_KEY)
        googleMapUISettings.isMyLocationButtonEnabled = mapPreferences.controls.getBoolean(MapPreferencesControls.MY_LOCATION_BUTTON_KEY)

        // set appearance
        googleMap.setMapStyle(mapPreferences.appearance.style)
        googleMap.isBuildingsEnabled = mapPreferences.appearance.isBuildingsShown
        googleMap.isIndoorEnabled = mapPreferences.appearance.isIndoorShown
        if (hasPermission()) {
            googleMap.isMyLocationEnabled = mapPreferences.appearance.isMyLocationDotShown
        }
        googleMap.isTrafficEnabled = mapPreferences.appearance.isTrafficShown

        // execute callback
        if (customMapViewEvents != null && savedCallbackIdForCreate != null) {
            customMapViewEvents.onMapReady(savedCallbackIdForCreate, getResultForMap())
        }
    }

    override fun onInfoWindowClick(marker: Marker) {
        if (customMapViewEvents != null && savedCallbackIdForDidTapInfoWindow != null) {
            val result = CustomMarker.getResultForMarker(marker, id)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapInfoWindow, result)
        }
    }

    override fun onInfoWindowClose(marker: Marker) {
        if (customMapViewEvents != null && savedCallbackIdForDidCloseInfoWindow != null) {
            val result = CustomMarker.getResultForMarker(marker, id)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidCloseInfoWindow, result)
        }
    }

    override fun onMapClick(latLng: LatLng) {
        if (customMapViewEvents != null && savedCallbackIdForDidTapMap != null) {
            val result = getResultForPosition(latLng.latitude, latLng.longitude)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapMap, result)
        }
    }

    override fun onMapLongClick(latLng: LatLng) {
        if (customMapViewEvents != null && savedCallbackIdForDidLongPressMap != null) {
            val result = getResultForPosition(latLng.latitude, latLng.longitude)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidLongPressMap, result)
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (customMapViewEvents != null && savedCallbackIdForDidTapMarker != null) {
            val result = CustomMarker.getResultForMarker(marker, id)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapMarker, result)
        }
        return preventDefaultForDidTapMarker
    }

    override fun onMarkerDragStart(marker: Marker) {
        if (customMapViewEvents != null && savedCallbackIdForDidBeginDraggingMarker != null) {
            val result = CustomMarker.getResultForMarker(marker, id)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidBeginDraggingMarker, result)
        }
    }

    override fun onMarkerDrag(marker: Marker) {
        if (customMapViewEvents != null && savedCallbackIdForDidDragMarker != null) {
            val result = CustomMarker.getResultForMarker(marker, id)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidDragMarker, result)
        }
    }

    override fun onMarkerDragEnd(marker: Marker) {
        if (customMapViewEvents != null && savedCallbackIdForDidEndDraggingMarker != null) {
            val result = CustomMarker.getResultForMarker(marker, id)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidEndDraggingMarker, result)
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        if (customMapViewEvents != null && savedCallbackIdForDidTapMyLocationButton != null) {
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapMyLocationButton, null)
        }
        return preventDefaultForDidTapMyLocationButton
    }

    override fun onMyLocationClick(location: Location) {
        if (customMapViewEvents != null && savedCallbackIdForDidTapMyLocationDot != null) {
            val result = getResultForPosition(location.latitude, location.longitude)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapMyLocationDot, result)
        }
    }

    override fun onPoiClick(pointOfInterest: PointOfInterest) {
        if (customMapViewEvents != null && savedCallbackIdForDidTapPoi != null) {
            val result = getResultForPoi(pointOfInterest)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapPoi, result)
        }
    }

    override fun onCameraMoveStarted(i: Int) {
        if (customMapViewEvents != null && savedCallbackIdForDidBeginMovingCamera != null) {
            // Camera motion initiated in response to user gestures on the map.
            // For example: pan, tilt, pinch to zoom, or rotate.
            val reason = if (i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) 1 else 2
            val result = JSObject()
            result.put("reason", reason)
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidBeginMovingCamera, result)
        }
    }

    override fun onCameraMove() {
        if (customMapViewEvents != null && savedCallbackIdForDidMoveCamera != null) {
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidMoveCamera, null)
        }
    }

    override fun onCameraIdle() {
        if (customMapViewEvents != null && savedCallbackIdForDidEndMovingCamera != null) {
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidEndMovingCamera, getResultForCameraPosition(JSObject()))
        }
    }

    internal fun handleOnStart() {
        mapView?.onStart()
    }

    internal fun handleOnResume() {
        mapView?.onResume()
    }

    internal fun handleOnPause() {
        mapView?.onPause()
    }

    internal fun handleOnStop() {
        mapView?.onStop()
    }

    internal fun handleOnDestroy() {
        mapView?.onDestroy()
    }

    public fun setCallbackIdForEvent(callbackId: String?, eventName: String?, preventDefault: Boolean?) {
        if (callbackId == null || eventName == null) {
            return
        }
        when (eventName) {
            EVENT_DID_TAP_INFO_WINDOW -> {
                requireGoogleMap().setOnInfoWindowClickListener(this)
                savedCallbackIdForDidTapInfoWindow = callbackId
            }

            EVENT_DID_CLOSE_INFO_WINDOW -> {
                requireGoogleMap().setOnInfoWindowCloseListener(this)
                savedCallbackIdForDidCloseInfoWindow = callbackId
            }

            EVENT_DID_TAP_MAP -> {
                requireGoogleMap().setOnMapClickListener(this)
                savedCallbackIdForDidTapMap = callbackId
            }

            EVENT_DID_LONG_PRESS_MAP -> {
                requireGoogleMap().setOnMapLongClickListener(this)
                savedCallbackIdForDidLongPressMap = callbackId
            }

            EVENT_DID_TAP_MARKER -> {
                requireGoogleMap().setOnMarkerClickListener(this)
                savedCallbackIdForDidTapMarker = callbackId
                preventDefaultForDidTapMarker = preventDefault ?: false
            }

            EVENT_DID_BEGIN_DRAGGING_MARKER -> {
                requireGoogleMap().setOnMarkerDragListener(this)
                savedCallbackIdForDidBeginDraggingMarker = callbackId
            }

            EVENT_DID_DRAG_MARKER -> {
                requireGoogleMap().setOnMarkerDragListener(this)
                savedCallbackIdForDidDragMarker = callbackId
            }

            EVENT_DID_END_DRAGGING_MARKER -> {
                requireGoogleMap().setOnMarkerDragListener(this)
                savedCallbackIdForDidEndDraggingMarker = callbackId
            }

            EVENT_DID_TAP_MY_LOCATION_BUTTON -> {
                requireGoogleMap().setOnMyLocationButtonClickListener(this)
                savedCallbackIdForDidTapMyLocationButton = callbackId
                preventDefaultForDidTapMyLocationButton = preventDefault ?: false
            }

            EVENT_DID_TAP_MY_LOCATION_DOT -> {
                requireGoogleMap().setOnMyLocationClickListener(this)
                savedCallbackIdForDidTapMyLocationDot = callbackId
            }

            EVENT_DID_TAP_POI -> {
                requireGoogleMap().setOnPoiClickListener(this)
                savedCallbackIdForDidTapPoi = callbackId
            }

            EVENT_DID_BEGIN_MOVING_CAMERA -> {
                requireGoogleMap().setOnCameraMoveStartedListener(this)
                savedCallbackIdForDidBeginMovingCamera = callbackId
            }

            EVENT_DID_MOVE_CAMERA -> {
                requireGoogleMap().setOnCameraMoveListener(this)
                savedCallbackIdForDidMoveCamera = callbackId
            }

            EVENT_DID_END_MOVING_CAMERA -> {
                requireGoogleMap().setOnCameraIdleListener(this)
                savedCallbackIdForDidEndMovingCamera = callbackId
            }
        }
    }

    public fun createMap(
        callbackId: String?,
        boundingRect: BoundingRect,
        mapCameraPosition: MapCameraPosition,
        mapPreferences: MapPreferences
    ) {
        savedCallbackIdForCreate = callbackId

        this.mapCameraPosition = mapCameraPosition
        this.mapPreferences = mapPreferences

        val googleMapOptions = mapPreferences.generateGoogleMapOptions()
        googleMapOptions.camera(mapCameraPosition.cameraPosition)

        val mapView = MapView(activity, googleMapOptions)
        this.mapView = mapView

        val lp = FrameLayout.LayoutParams(getScaledPixels(boundingRect.width), getScaledPixels(boundingRect.height))
        lp.topMargin = getScaledPixels(boundingRect.y)
        lp.leftMargin = getScaledPixels(boundingRect.x)

        mapView.layoutParams = lp

        mapView.onCreate(null)
        mapView.onStart()
        mapView.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    public fun invalidateMap(): JSObject? {
        val googleMap = this.googleMap ?: return null

        val googleMapUISettings = googleMap.uiSettings
        val gestures = mapPreferences.gestures
        val controls = mapPreferences.controls
        val appearance = mapPreferences.appearance

        // set gestures
        googleMapUISettings.isRotateGesturesEnabled = gestures.getBoolean(MapPreferencesGestures.ROTATE_ALLOWED_KEY)
        googleMapUISettings.isScrollGesturesEnabled = gestures.getBoolean(MapPreferencesGestures.SCROLL_ALLOWED_KEY)
        googleMapUISettings.isScrollGesturesEnabledDuringRotateOrZoom =
            gestures.getBoolean(MapPreferencesGestures.SCROLL_ALLOWED_DURING_ROTATE_OR_ZOOM_KEY)
        googleMapUISettings.isTiltGesturesEnabled = gestures.getBoolean(MapPreferencesGestures.TILT_ALLOWED_KEY)
        googleMapUISettings.isZoomGesturesEnabled = gestures.getBoolean(MapPreferencesGestures.ZOOM_ALLOWED_KEY)

        // set controls
        googleMapUISettings.isCompassEnabled = controls.getBoolean(MapPreferencesControls.COMPASS_BUTTON_KEY)
        googleMapUISettings.isIndoorLevelPickerEnabled = controls.getBoolean(MapPreferencesControls.INDOOR_LEVEL_PICKER_KEY)
        googleMapUISettings.isMapToolbarEnabled = controls.getBoolean(MapPreferencesControls.MAP_TOOLBAR_KEY)
        googleMapUISettings.isMyLocationButtonEnabled = controls.getBoolean(MapPreferencesControls.MY_LOCATION_BUTTON_KEY)
        googleMapUISettings.isZoomControlsEnabled = controls.getBoolean(MapPreferencesControls.ZOOM_BUTTONS_KEY)

        // set appearance
        googleMap.mapType = appearance.type
        googleMap.setMapStyle(appearance.style)
        googleMap.isBuildingsEnabled = appearance.isBuildingsShown
        googleMap.isIndoorEnabled = appearance.isIndoorShown
        if (hasPermission()) {
            googleMap.isMyLocationEnabled = appearance.isMyLocationDotShown
        }
        googleMap.isTrafficEnabled = appearance.isTrafficShown

        return getResultForMap()
    }

    public fun getMap(): JSObject? = getResultForMap()

    public val cameraPosition: CameraPosition?
        get() = googleMap?.cameraPosition

    public fun moveCamera(duration: Int?) {
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(mapCameraPosition.cameraPosition)

        if (duration == null || duration <= 0) {
            requireGoogleMap().moveCamera(cameraUpdate)
        } else {
            requireGoogleMap().animateCamera(cameraUpdate, duration, null)
        }
    }

    private fun getScaledPixels(pixels: Int): Int {
        // Get the screen's density scale
        val scale = activity.resources.displayMetrics.density
        // Convert the dps to pixels, based on density scale
        return (pixels * scale + 0.5f).toInt()
    }

    public fun addToView(parent: ViewGroup) {
        parent.addView(mapView)
    }

    public fun removeFromView(parent: ViewGroup) {
        parent.removeView(mapView)
    }

    public fun clear() {
        requireGoogleMap().clear()
        markers.clear()
    }

    public fun addMarker(customMarker: CustomMarker, consumer: ((Marker) -> Unit)?) {
        customMarker.addToMap(activity, requireGoogleMap()) { marker ->
            markers[customMarker.markerId] = marker

            consumer?.invoke(marker)
        }
    }

    public fun removeMarker(markerId: String?) {
        markerId?.let { markers.remove(it) }?.remove()
    }

    public fun addPolygon(customPolygon: CustomPolygon, consumer: ((Polygon) -> Unit)?) {
        customPolygon.addToMap(requireGoogleMap()) { polygon ->
            polygons[customPolygon.polygonId] = polygon

            consumer?.invoke(polygon)
        }
    }

    public fun removePolygon(polygonId: String?) {
        polygonId?.let { polygons.remove(it) }?.remove()
    }

    private fun getResultForMap(): JSObject? {
        val googleMap = this.googleMap
        if (mapView == null || googleMap == null) {
            return null
        }

        // initialize JSObjects
        val result = JSObject()

        val resultGoogleMap = JSObject()
        result.put("googleMap", resultGoogleMap)

        val resultPreferences = JSObject()
        resultGoogleMap.put("preferences", resultPreferences)

        val resultGestures = JSObject()
        resultPreferences.put("gestures", resultGestures)

        val resultControls = JSObject()
        resultPreferences.put("controls", resultControls)

        val resultAppearance = JSObject()
        resultPreferences.put("appearance", resultAppearance)

        // get UISettings
        val googleMapUISettings = googleMap.uiSettings

        // return mapId
        resultGoogleMap.put("mapId", id)

        // return cameraPosition
        getResultForCameraPosition(resultGoogleMap)

        // return gestures
        resultGestures.put(MapPreferencesGestures.ROTATE_ALLOWED_KEY, googleMapUISettings.isRotateGesturesEnabled)
        resultGestures.put(MapPreferencesGestures.SCROLL_ALLOWED_KEY, googleMapUISettings.isScrollGesturesEnabled)
        resultGestures.put(
            MapPreferencesGestures.SCROLL_ALLOWED_DURING_ROTATE_OR_ZOOM_KEY,
            googleMapUISettings.isScrollGesturesEnabledDuringRotateOrZoom
        )
        resultGestures.put(MapPreferencesGestures.TILT_ALLOWED_KEY, googleMapUISettings.isTiltGesturesEnabled)
        resultGestures.put(MapPreferencesGestures.ZOOM_ALLOWED_KEY, googleMapUISettings.isZoomGesturesEnabled)

        // return controls
        resultControls.put(MapPreferencesControls.COMPASS_BUTTON_KEY, googleMapUISettings.isCompassEnabled)
        // resultControls.put(MapPreferencesControls.INDOOR_LEVEL_PICKER_KEY, googleMapUISettings.isIndoorLevelPickerEnabled)
        resultControls.put(MapPreferencesControls.MAP_TOOLBAR_KEY, googleMapUISettings.isMapToolbarEnabled)
        resultControls.put(MapPreferencesControls.MY_LOCATION_BUTTON_KEY, googleMapUISettings.isMyLocationButtonEnabled)
        resultControls.put(MapPreferencesControls.ZOOM_BUTTONS_KEY, googleMapUISettings.isZoomControlsEnabled)

        // return appearance
        resultAppearance.put(MapPreferencesAppearance.TYPE_KEY, googleMap.mapType)
        resultAppearance.put(MapPreferencesAppearance.BUILDINGS_SHOWN_KEY, googleMap.isBuildingsEnabled)
        resultAppearance.put(MapPreferencesAppearance.INDOOR_SHOWN_KEY, googleMap.isIndoorEnabled)
        resultAppearance.put(MapPreferencesAppearance.MY_LOCATION_DOT_SHOWN_KEY, googleMap.isMyLocationEnabled)
        resultAppearance.put(MapPreferencesAppearance.TRAFFIC_SHOWN_KEY, googleMap.isTrafficEnabled)

        return result
    }

    private fun getResultForCameraPosition(resultObjectToExtend: JSObject): JSObject {
        val resultCameraPosition = JSObject()
        resultObjectToExtend.put("cameraPosition", resultCameraPosition)

        val resultCameraPositionTarget = JSObject()
        resultCameraPosition.put("target", resultCameraPositionTarget)

        // get CameraPosition
        val cameraPosition = requireGoogleMap().cameraPosition

        // return cameraPosition
        resultCameraPositionTarget.put("latitude", cameraPosition.target.latitude)
        resultCameraPositionTarget.put("longitude", cameraPosition.target.longitude)
        resultCameraPosition.put("bearing", cameraPosition.bearing.toDouble())
        resultCameraPosition.put("tilt", cameraPosition.tilt.toDouble())
        resultCameraPosition.put("zoom", cameraPosition.zoom.toDouble())

        return resultObjectToExtend
    }

    private fun getResultForPosition(latitude: Double, longitude: Double): JSObject {
        // initialize JSObjects to return
        val result = JSObject()
        val positionResult = JSObject()
        result.put("position", positionResult)

        // get position values
        positionResult.put("latitude", latitude)
        positionResult.put("longitude", longitude)

        // return result
        return result
    }

    private fun getResultForPoi(pointOfInterest: PointOfInterest): JSObject {
        // initialize JSObjects to return
        val result = JSObject()
        val poiResult = JSObject()
        val positionResult = JSObject()

        result.put("poi", poiResult)
        poiResult.put("position", positionResult)

        // get position values
        positionResult.put("latitude", pointOfInterest.latLng.latitude)
        positionResult.put("longitude", pointOfInterest.latLng.longitude)

        // get other values
        poiResult.put("name", pointOfInterest.name)
        poiResult.put("placeId", pointOfInterest.placeId)

        // return result
        return result
    }

    public companion object {
        public const val EVENT_DID_TAP_INFO_WINDOW: String = "didTapInfoWindow"
        public const val EVENT_DID_CLOSE_INFO_WINDOW: String = "didCloseInfoWindow"
        public const val EVENT_DID_TAP_MAP: String = "didTapMap"
        public const val EVENT_DID_LONG_PRESS_MAP: String = "didLongPressMap"
        public const val EVENT_DID_TAP_MARKER: String = "didTapMarker"
        public const val EVENT_DID_BEGIN_DRAGGING_MARKER: String = "didBeginDraggingMarker"
        public const val EVENT_DID_DRAG_MARKER: String = "didDragMarker"
        public const val EVENT_DID_END_DRAGGING_MARKER: String = "didEndDraggingMarker"
        public const val EVENT_DID_TAP_MY_LOCATION_BUTTON: String = "didTapMyLocationButton"
        public const val EVENT_DID_TAP_MY_LOCATION_DOT: String = "didTapMyLocationDot"
        public const val EVENT_DID_TAP_POI: String = "didTapPoi"
        public const val EVENT_DID_BEGIN_MOVING_CAMERA: String = "didBeginMovingCamera"
        public const val EVENT_DID_MOVE_CAMERA: String = "didMoveCamera"
        public const val EVENT_DID_END_MOVING_CAMERA: String = "didEndMovingCamera"
    }
}
