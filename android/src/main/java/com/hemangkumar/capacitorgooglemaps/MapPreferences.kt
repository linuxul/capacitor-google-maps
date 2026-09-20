package com.hemangkumar.capacitorgooglemaps

import com.getcapacitor.JSObject
import com.google.android.gms.maps.GoogleMapOptions

public class MapPreferences {
    public val gestures: MapPreferencesGestures = MapPreferencesGestures()
    public val controls: MapPreferencesControls = MapPreferencesControls()
    public val appearance: MapPreferencesAppearance = MapPreferencesAppearance()

    public fun updateFromJSObject(preferences: JSObject?) {
        if (preferences != null) {
            gestures.updateFromJSObject(preferences.getJSObject("gestures"))
            controls.updateFromJSObject(preferences.getJSObject("controls"))
            appearance.updateFromJSObject(preferences.getJSObject("appearance"))
        }
    }

    public fun generateGoogleMapOptions(): GoogleMapOptions {
        val googleMapOptions = GoogleMapOptions()

        // set gestures
        googleMapOptions.rotateGesturesEnabled(gestures.getBoolean(MapPreferencesGestures.ROTATE_ALLOWED_KEY))
        googleMapOptions.scrollGesturesEnabled(gestures.getBoolean(MapPreferencesGestures.SCROLL_ALLOWED_KEY))
        googleMapOptions.scrollGesturesEnabledDuringRotateOrZoom(
            gestures.getBoolean(MapPreferencesGestures.SCROLL_ALLOWED_DURING_ROTATE_OR_ZOOM_KEY)
        )
        googleMapOptions.tiltGesturesEnabled(gestures.getBoolean(MapPreferencesGestures.TILT_ALLOWED_KEY))
        googleMapOptions.zoomGesturesEnabled(gestures.getBoolean(MapPreferencesGestures.ZOOM_ALLOWED_KEY))

        // set controls
        googleMapOptions.compassEnabled(controls.getBoolean(MapPreferencesControls.COMPASS_BUTTON_KEY))
        googleMapOptions.mapToolbarEnabled(controls.getBoolean(MapPreferencesControls.MAP_TOOLBAR_KEY))
        googleMapOptions.zoomControlsEnabled(controls.getBoolean(MapPreferencesControls.ZOOM_BUTTONS_KEY))
        // controls.isIndoorLevelPickerEnabled can only be set through `UiSettings`
        // controls.isMyLocationButtonEnabled can only be set through `UiSettings`

        // set appearance
        googleMapOptions.mapType(appearance.type)
        // appearance.style can only be set through `GoogleMap`
        // appearance.isIndoorShown can only be set through `GoogleMap`
        // appearance.isBuildingsShown can only be set through `GoogleMap`
        // appearance.isMyLocationDotShown can only be set through `GoogleMap`
        // appearance.isTrafficShown can only be set through `GoogleMap`

        return googleMapOptions
    }
}
