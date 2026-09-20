package com.hemangkumar.capacitorgooglemaps

import com.getcapacitor.JSObject

public interface CustomMapViewEvents {
    public fun onMapReady(callbackId: String?, result: JSObject?)

    public fun resultForCallbackId(callbackId: String?, result: JSObject?)
}
