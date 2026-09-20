package com.hemangkumar.capacitorgooglemaps

import androidx.fragment.app.FragmentActivity
import com.getcapacitor.JSObject
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.UUID

public class CustomMarker {
    // generate id for the just added marker,
    // put this marker into a hashmap with the corresponding id,
    // so we can retrieve the marker by id later on
    public var markerId: String = UUID.randomUUID().toString()

    private val markerOptions = MarkerOptions()
    private var tag = JSObject()
    private var iconDescriptor = JSObject()

    public fun asyncLoadIcon(activity: FragmentActivity, consumer: ((BitmapDescriptor?) -> Unit)?) {
        AsyncIconLoader(iconDescriptor, activity).load { bitmap ->
            val bitmapDescriptor = bitmap?.let { BitmapDescriptorFactory.fromBitmap(it) }
            consumer?.invoke(bitmapDescriptor)
        }
    }

    public fun updateFromJSObject(marker: JSObject) {
        val position = JSObjectDefaults.getJSObjectSafe(marker, "position", JSObject())
        val latitude = JSObjectDefaults.getDoubleSafe(position, "latitude", 0.0)
        val longitude = JSObjectDefaults.getDoubleSafe(position, "longitude", 0.0)
        val latLng = LatLng(latitude, longitude)

        val preferences = JSObjectDefaults.getJSObjectSafe(marker, "preferences", JSObject())
        val title = preferences.getString("title", "")
        val snippet = preferences.getString("snippet", "")
        val opacity = JSObjectDefaults.getFloatSafe(preferences, "opacity", 1f)
        val isFlat = JSObjectDefaults.getBooleanSafe(preferences, "isFlat", false)
        val isDraggable = JSObjectDefaults.getBooleanSafe(preferences, "isDraggable", false)
        val zIndex = JSObjectDefaults.getIntegerSafe(preferences, "zIndex", 0)

        val anchor = JSObjectDefaults.getJSObjectSafe(preferences, "anchor", JSObject())
        val anchorX = JSObjectDefaults.getFloatSafe(anchor, "x", 0.5f)
        val anchorY = JSObjectDefaults.getFloatSafe(anchor, "y", 1f)

        markerOptions.position(latLng)
        markerOptions.title(title)
        markerOptions.snippet(snippet)
        markerOptions.alpha(opacity)
        markerOptions.flat(isFlat)
        markerOptions.draggable(isDraggable)
        markerOptions.zIndex(zIndex.toFloat())
        markerOptions.anchor(anchorX, anchorY)

        setMetadata(JSObjectDefaults.getJSObjectSafe(preferences, "metadata", JSObject()))

        iconDescriptor = JSObjectDefaults.getJSObjectSafe(preferences, "icon", JSObject())
    }

    public fun addToMap(activity: FragmentActivity, googleMap: GoogleMap, consumer: ((Marker) -> Unit)?) {
        asyncLoadIcon(activity) { bitmapDescriptor ->
            markerOptions.icon(bitmapDescriptor)
            // addMarker only returns null when the options have no position, and updateFromJSObject always sets
            // one. This has always thrown in that case.
            val marker = googleMap.addMarker(markerOptions)!!
            marker.tag = tag

            consumer?.invoke(marker)
        }
    }

    private fun setMetadata(jsObject: JSObject) {
        val tag = JSObject()
        // set id to tag
        tag.put("markerId", markerId)
        // set anchor to tag (because it cannot be retrieved from a marker instance)
        val anchorResult = JSObject()
        anchorResult.put("x", markerOptions.anchorU.toDouble())
        anchorResult.put("y", markerOptions.anchorV.toDouble())
        tag.put("anchor", anchorResult)
        // then set metadata to tag
        tag.put("metadata", jsObject)
        // save in tag variable
        this.tag = tag
    }

    public companion object {
        public fun getResultForMarker(marker: Marker, mapId: String?): JSObject {
            val tag = marker.tag as? JSObject ?: JSObject()

            // initialize JSObjects to return
            val result = JSObject()
            val markerResult = JSObject()
            val positionResult = JSObject()
            val preferencesResult = JSObject()

            result.put("marker", markerResult)
            markerResult.put("position", positionResult)
            markerResult.put("preferences", preferencesResult)

            // get map id
            markerResult.put("mapId", mapId)

            // get id
            markerResult.put("markerId", tag.optString("markerId", marker.id))

            // get position values
            positionResult.put("latitude", marker.position.latitude)
            positionResult.put("longitude", marker.position.longitude)

            // get preferences
            preferencesResult.put("title", marker.title)
            preferencesResult.put("snippet", marker.snippet)
            preferencesResult.put("opacity", marker.alpha.toDouble())
            preferencesResult.put("isFlat", marker.isFlat)
            preferencesResult.put("isDraggable", marker.isDraggable)
            preferencesResult.put("zIndex", marker.zIndex.toDouble())
            // anchor values
            preferencesResult.put("anchor", JSObjectDefaults.getJSObjectSafe(tag, "anchor", JSObject()))
            // metadata
            preferencesResult.put("metadata", JSObjectDefaults.getJSObjectSafe(tag, "metadata", JSObject()))

            return result
        }
    }
}
