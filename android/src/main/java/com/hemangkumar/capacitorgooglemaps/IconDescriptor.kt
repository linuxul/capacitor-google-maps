package com.hemangkumar.capacitorgooglemaps

import android.content.res.Resources
import android.util.Size
import com.getcapacitor.JSObject

/**
 * Example source of JSObject:
 * {
 *   url: 'https://www.google.com/favicon.ico',
 *   size: {
 *     width: 64,
 *     height: 64
 *   }
 * }
 * @param jsIcon is a JSObject icon representation
 */
internal class IconDescriptor(jsIcon: JSObject) {
    val url: String = jsIcon.optString("url", "")
    val size: Size

    init {
        val density = Resources.getSystem().displayMetrics.density
        val jsSize = JSObjectDefaults.getJSObjectSafe(jsIcon, "size", JSObject())

        size = Size(
            Math.round(jsSize.optDouble("width", 30.0) * density).toInt(),
            Math.round(jsSize.optDouble("height", 30.0) * density).toInt()
        )
    }
}
