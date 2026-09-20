package com.hemangkumar.capacitorgooglemaps

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.util.WebColor
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

public class CustomPolygon {
    // generate id for the just added polygon,
    // put this polygon into a hashmap with the corresponding id,
    // so we can retrieve the polygon by id later on
    public val polygonId: String = UUID.randomUUID().toString()

    private val polygonOptions = PolygonOptions()
    private var tag = JSObject()

    public fun updateFromJSObject(polygon: JSObject) {
        setPath(polygon.optJSONArray("path"))

        val preferences = JSObjectDefaults.getJSObjectSafe(polygon, "preferences", JSObject())

        setBasicFields(preferences)
        setHoles(preferences.optJSONArray("holes"))
        setMetadata(JSObjectDefaults.getJSObjectSafe(preferences, "metadata", JSObject()))
    }

    public fun addToMap(googleMap: GoogleMap, consumer: ((Polygon) -> Unit)?) {
        val polygon = googleMap.addPolygon(polygonOptions)
        polygon.tag = tag

        consumer?.invoke(polygon)
    }

    private fun setPath(path: JSONArray?) {
        if (path != null) {
            polygonOptions.addAll(getLatLngList(path))
        }
    }

    private fun setHoles(holes: JSONArray?) {
        if (holes != null) {
            for (i in 0 until holes.length()) {
                // For each hole, get the path.
                val path = holes.optJSONArray(i)
                if (path != null) {
                    polygonOptions.addHole(getLatLngList(path))
                }
            }
        }
    }

    private fun setBasicFields(preferences: JSObject) {
        val strokeWidth = preferences.optDouble("strokeWidth", 10.0).toFloat()
        val strokeColor = WebColor.parseColor(preferences.optString("strokeColor", "#000000"))
        val fillColor = WebColor.parseColor(preferences.optString("fillColor", "#00000000"))
        val zIndex = preferences.optDouble("zIndex", 0.0).toFloat()
        val isVisible = preferences.optBoolean("isVisible", true)
        val isGeodesic = preferences.optBoolean("isGeodesic", false)
        val isClickable = preferences.optBoolean("isClickable", false)

        polygonOptions.strokeWidth(strokeWidth)
        polygonOptions.strokeColor(strokeColor)
        polygonOptions.fillColor(fillColor)
        polygonOptions.zIndex(zIndex)
        polygonOptions.visible(isVisible)
        polygonOptions.geodesic(isGeodesic)
        polygonOptions.clickable(isClickable)
    }

    private fun setMetadata(jsObject: JSObject) {
        val tag = JSObject()
        tag.put("id", polygonId)
        tag.put("metadata", jsObject)
        this.tag = tag
    }

    public fun getResultForPolygon(polygon: Polygon, mapId: String?): JSObject {
        val tag = polygon.tag as? JSObject ?: JSObject()

        // initialize JSObjects to return
        val result = JSObject()
        val polygonResult = JSObject()
        val preferencesResult = JSObject()

        result.put("polygon", polygonResult)
        polygonResult.put("preferences", preferencesResult)

        // get map id
        polygonResult.put("mapId", mapId)

        // get id
        polygonResult.put("polygonId", tag.optString("polygonId", polygon.id))

        // get path values
        polygonResult.put("path", latLngsToJSArray(polygon.points))

        // get preferences
        preferencesResult.put("strokeWidth", polygon.strokeWidth.toDouble())
        preferencesResult.put("strokeColor", colorToString(polygon.strokeColor))
        preferencesResult.put("fillColor", colorToString(polygon.fillColor))
        preferencesResult.put("zIndex", polygon.zIndex.toDouble())
        preferencesResult.put("isVisible", polygon.isVisible)
        preferencesResult.put("isGeodesic", polygon.isGeodesic)
        preferencesResult.put("isClickable", polygon.isClickable)
        // holes
        val holesResult = JSArray()
        for (hole in polygon.holes) {
            holesResult.put(latLngsToJSArray(hole))
        }
        if (holesResult.length() > 0) {
            preferencesResult.put("holes", holesResult)
        }
        // metadata
        preferencesResult.put("metadata", JSObjectDefaults.getJSObjectSafe(tag, "metadata", JSObject()))

        return result
    }

    private companion object {
        fun getLatLngList(latLngArray: JSONArray): List<LatLng> {
            val latLngList = ArrayList<LatLng>()

            for (n in 0 until latLngArray.length()) {
                val latLngObject = latLngArray.optJSONObject(n)
                if (latLngObject != null) {
                    latLngList.add(getLatLng(latLngObject))
                }
            }

            return latLngList
        }

        fun getLatLng(latLngObject: JSONObject): LatLng {
            val latitude = latLngObject.optDouble("latitude", 0.0)
            val longitude = latLngObject.optDouble("longitude", 0.0)
            return LatLng(latitude, longitude)
        }

        fun latLngsToJSArray(positions: Collection<LatLng>): JSArray {
            val jsPositions = JSArray()
            for (pos in positions) {
                jsPositions.put(latLngToJSObject(pos))
            }
            return jsPositions
        }

        fun latLngToJSObject(latLng: LatLng): JSObject {
            val jsPos = JSObject()
            jsPos.put("latitude", latLng.latitude)
            jsPos.put("longitude", latLng.longitude)
            return jsPos
        }

        // String.format without a locale uses the default one; hex digits do not depend on it.
        fun colorToString(color: Int): String {
            val r = (color shr 16) and 0xff
            val g = (color shr 8) and 0xff
            val b = color and 0xff
            val a = (color shr 24) and 0xff
            return if (a != 255) {
                String.format("#%02X%02X%02X%02X", a, r, g, b)
            } else {
                String.format("#%02X%02X%02X", r, g, b)
            }
        }
    }
}
