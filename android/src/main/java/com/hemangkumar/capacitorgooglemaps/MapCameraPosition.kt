package com.hemangkumar.capacitorgooglemaps

import android.util.Log
import com.getcapacitor.JSObject
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import org.json.JSONException

public class MapCameraPosition {
    // Starts out at (0,0), so a camera target is always set.
    public var cameraPosition: CameraPosition = CameraPosition.builder().target(LatLng(0.0, 0.0)).build()

    public fun updateFromJSObject(cameraPosition: JSObject?, baseCameraPosition: CameraPosition?) {
        val cameraPositionBuilder =
            if (baseCameraPosition != null) {
                // use given cameraPosition as the base
                CameraPosition.Builder(baseCameraPosition)
            } else {
                // use default cameraPosition as the base
                CameraPosition.Builder()
            }

        if (cameraPosition != null) {
            val target = cameraPosition.getJSObject("target")

            if (target != null) {
                try {
                    if (target.has("latitude") && target.has("longitude")) {
                        val latitude = target.getDouble("latitude")
                        val longitude = target.getDouble("longitude")

                        cameraPositionBuilder.target(LatLng(latitude, longitude))
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            if (cameraPosition.has("bearing")) {
                try {
                    cameraPositionBuilder.bearing(cameraPosition.getDouble("bearing").toFloat())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            if (cameraPosition.has("tilt")) {
                try {
                    val tilt = cameraPosition.getDouble("tilt").toFloat()
                    if (tilt < 0.0f || tilt > 90.0f) {
                        Log.d("GoogleMap", "Tilt needs to be between 0 and 90 inclusive: $tilt")
                    } else {
                        cameraPositionBuilder.tilt(tilt)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            if (cameraPosition.has("zoom")) {
                try {
                    cameraPositionBuilder.zoom(cameraPosition.getDouble("zoom").toFloat())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }

        try {
            this.cameraPosition = cameraPositionBuilder.build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
