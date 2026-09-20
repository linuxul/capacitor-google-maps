package com.hemangkumar.capacitorgooglemaps

public class MapPreferencesGestures :
    JSObjectDefaults(
        hashMapOf(
            ROTATE_ALLOWED_KEY to true,
            SCROLL_ALLOWED_KEY to true,
            SCROLL_ALLOWED_DURING_ROTATE_OR_ZOOM_KEY to true,
            TILT_ALLOWED_KEY to true,
            ZOOM_ALLOWED_KEY to true
        )
    ) {
    public companion object {
        public const val ROTATE_ALLOWED_KEY: String = "isRotateAllowed"
        public const val SCROLL_ALLOWED_KEY: String = "isScrollAllowed"
        public const val SCROLL_ALLOWED_DURING_ROTATE_OR_ZOOM_KEY: String = "isScrollAllowedDuringRotateOrZoom"
        public const val TILT_ALLOWED_KEY: String = "isTiltAllowed"
        public const val ZOOM_ALLOWED_KEY: String = "isZoomAllowed"
    }
}
