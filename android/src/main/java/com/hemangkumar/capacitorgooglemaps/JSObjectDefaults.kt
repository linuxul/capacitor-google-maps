package com.hemangkumar.capacitorgooglemaps

import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import org.json.JSONException

public abstract class JSObjectDefaults(private val defaults: Map<String, Any>) {
    private val actualValues = HashMap<String, Any>()

    private fun getObject(key: String): Any? = actualValues[key] ?: defaults[key]

    public fun getBoolean(key: String): Boolean {
        val value = getObject(key) ?: return false
        return value as Boolean
    }

    public fun updateFromJSObject(jsObject: JSObject?) {
        if (jsObject != null) {
            for ((key, defaultValue) in defaults) {
                if (defaultValue is Boolean && jsObject.has(key)) {
                    actualValues[key] = jsObject.getBoolean(key, defaultValue) ?: defaultValue
                }
            }
        }
    }

    public companion object {
        public fun getJSObjectSafe(jsObject: JSObject, name: String, defaultValue: JSObject): JSObject =
            jsObject.getJSObject(name) ?: defaultValue

        public fun getJSObjectSafe(call: PluginCall?, name: String, defaultValue: JSObject): JSObject =
            call?.getObject(name, defaultValue) ?: JSObject()

        public fun getDoubleSafe(jsObject: JSObject, name: String, defaultValue: Double): Double {
            try {
                return jsObject.getDouble(name)
            } catch (ignored: JSONException) {
            }
            return defaultValue
        }

        public fun getFloatSafe(jsObject: JSObject, name: String, defaultValue: Float): Float =
            getDoubleSafe(jsObject, name, defaultValue.toDouble()).toFloat()

        public fun getIntegerSafe(jsObject: JSObject, name: String, defaultValue: Int): Int = jsObject.getInteger(name) ?: defaultValue

        // A missing or non-boolean value reads as false, as it always has; defaultValue is only a last resort.
        public fun getBooleanSafe(jsObject: JSObject, name: String, defaultValue: Boolean): Boolean =
            jsObject.getBoolean(name, false) ?: defaultValue
    }
}
