package com.hemangkumar.capacitorgooglemaps

public class MapPreferencesControls :
    JSObjectDefaults(
        hashMapOf(
            COMPASS_BUTTON_KEY to true,
            INDOOR_LEVEL_PICKER_KEY to false,
            MAP_TOOLBAR_KEY to false,
            MY_LOCATION_BUTTON_KEY to true,
            ZOOM_BUTTONS_KEY to false
        )
    ) {
    public companion object {
        public const val COMPASS_BUTTON_KEY: String = "isCompassButtonEnabled"
        public const val INDOOR_LEVEL_PICKER_KEY: String = "isIndoorLevelPickerEnabled"
        public const val MAP_TOOLBAR_KEY: String = "isMapToolbarEnabled"
        public const val MY_LOCATION_BUTTON_KEY: String = "isMyLocationButtonEnabled"
        public const val ZOOM_BUTTONS_KEY: String = "isZoomButtonsEnabled"
    }
}
