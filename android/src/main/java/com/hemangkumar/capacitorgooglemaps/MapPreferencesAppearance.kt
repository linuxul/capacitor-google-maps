package com.hemangkumar.capacitorgooglemaps

import com.getcapacitor.JSObject
import com.google.android.gms.maps.model.MapStyleOptions

public class MapPreferencesAppearance {
    public var type: Int = 1
    public var style: MapStyleOptions? = null
    public var isBuildingsShown: Boolean = true
    public var isIndoorShown: Boolean = true
    public var isMyLocationDotShown: Boolean = false
    public var isTrafficShown: Boolean = false

    public fun updateFromJSObject(jsObject: JSObject?) {
        if (jsObject != null) {
            // update mapType
            if (jsObject.has(TYPE_KEY)) {
                jsObject.getInteger(TYPE_KEY, 1)?.let { mapType ->
                    type = if (mapType < 0 || mapType > 4) 1 else mapType
                }
            }
            if (jsObject.has(STYLE_KEY)) {
                style = jsObject.getString(STYLE_KEY, null)?.let { MapStyleOptions(it) }
            }
            // A value that is not a boolean used to end up as null here and crash once it was applied to the
            // map; it now leaves the preference as it was.
            jsObject.getBool(BUILDINGS_SHOWN_KEY)?.let { isBuildingsShown = it }
            jsObject.getBool(INDOOR_SHOWN_KEY)?.let { isIndoorShown = it }
            jsObject.getBool(MY_LOCATION_DOT_SHOWN_KEY)?.let { isMyLocationDotShown = it }
            jsObject.getBool(TRAFFIC_SHOWN_KEY)?.let { isTrafficShown = it }
        }
    }

    public companion object {
        public const val TYPE_KEY: String = "type"
        public const val STYLE_KEY: String = "style"
        public const val BUILDINGS_SHOWN_KEY: String = "isBuildingsShown"
        public const val INDOOR_SHOWN_KEY: String = "isIndoorShown"
        public const val MY_LOCATION_DOT_SHOWN_KEY: String = "isMyLocationDotShown"
        public const val TRAFFIC_SHOWN_KEY: String = "isTrafficShown"
    }
}
