package com.hemangkumar.capacitorgooglemaps

import com.getcapacitor.JSObject

public class BoundingRect {
    public var width: Int = 500
    public var height: Int = 500
    public var x: Int = 0
    public var y: Int = 0

    public fun updateFromJSObject(jsObject: JSObject?) {
        if (jsObject != null) {
            jsObject.getInteger("width")?.let { width = it }
            jsObject.getInteger("height")?.let { height = it }
            jsObject.getInteger("x")?.let { x = it }
            jsObject.getInteger("y")?.let { y = it }
        }
    }
}
