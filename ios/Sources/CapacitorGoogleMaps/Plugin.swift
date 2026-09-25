import Foundation
import Capacitor
import GoogleMaps

@objc(CapacitorGoogleMaps)
public class CapacitorGoogleMaps: CustomMapViewEvents, CAPBridgedPlugin {

    public let identifier = "CapacitorGoogleMaps"

    public let jsName = "CapacitorGoogleMaps"

    // Every method stays synchronous on the bridge queue. The map operations hop to the main queue in the order
    // JavaScript called them, which keeps the operations on a map ordered; async methods would not wait for each other.
    public let pluginMethods: [CAPPluginMethod] = [
         .promise("initialize", CapacitorGoogleMaps.initialize),
         .promise("createMap", CapacitorGoogleMaps.createMap),
         .promise("updateMap", CapacitorGoogleMaps.updateMap),
         .none("clearMap", CapacitorGoogleMaps.clearMap),
         .promise("removeMap", CapacitorGoogleMaps.removeMap),
         .promise("moveCamera", CapacitorGoogleMaps.moveCamera),
         .promise("addMarker", CapacitorGoogleMaps.addMarker),
         .promise("addMarkers", CapacitorGoogleMaps.addMarkers),
         .promise("removeMarker", CapacitorGoogleMaps.removeMarker),
         .promise("addPolygon", CapacitorGoogleMaps.addPolygon),
         .promise("removePolygon", CapacitorGoogleMaps.removePolygon),
         .callback("didTapInfoWindow", CapacitorGoogleMaps.didTapInfoWindow),
         .callback("didCloseInfoWindow", CapacitorGoogleMaps.didCloseInfoWindow),
         .callback("didTapMap", CapacitorGoogleMaps.didTapMap),
         .callback("didLongPressMap", CapacitorGoogleMaps.didLongPressMap),
         .callback("didTapMarker", CapacitorGoogleMaps.didTapMarker),
         .callback("didBeginDraggingMarker", CapacitorGoogleMaps.didBeginDraggingMarker),
         .callback("didDragMarker", CapacitorGoogleMaps.didDragMarker),
         .callback("didEndDraggingMarker", CapacitorGoogleMaps.didEndDraggingMarker),
         .callback("didTapMyLocationButton", CapacitorGoogleMaps.didTapMyLocationButton),
         .callback("didTapMyLocationDot", CapacitorGoogleMaps.didTapMyLocationDot),
         .callback("didTapPoi", CapacitorGoogleMaps.didTapPoi),
         .callback("didBeginMovingCamera", CapacitorGoogleMaps.didBeginMovingCamera),
         .callback("didMoveCamera", CapacitorGoogleMaps.didMoveCamera),
         .callback("didEndMovingCamera", CapacitorGoogleMaps.didEndMovingCamera),
     ]

    var GOOGLE_MAPS_KEY: String = "";

    var customMarkers = [String : CustomMarker]();

    var customPolygons = [String: CustomPolygon]();

    var customWebView: CustomWKWebView?

    // Synchronous: the SDK must have its key before any map is created, and the reset of the map views below is
    // queued on the main queue ahead of the work of the calls that follow.
    func initialize(_ call: CAPPluginCall) throws {
        self.GOOGLE_MAPS_KEY = call.getString("key", "")

        if self.GOOGLE_MAPS_KEY.isEmpty {
            throw CAPPluginError("GOOGLE MAPS API key missing!")
        }

        GMSServices.provideAPIKey(self.GOOGLE_MAPS_KEY)

        self.customWebView = self.bridge?.webView as? CustomWKWebView

        DispatchQueue.main.async {
            // remove all custom maps views from the main view
            if let values = self.customWebView?.customMapViews.map({ $0.value }) {
                CAPLog.print("mapId \(values)")
                for mapView in values {
                    (mapView as CustomMapView).view.removeFromSuperview()
                }
            }
            // reset custom map views holder
            self.customWebView?.customMapViews = [:]
        }

        call.resolve([
            "initialized": true
        ])
    }

    func createMap(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            let customMapView : CustomMapView = CustomMapView(customMapViewEvents: self)

            self.bridge?.saveCall(call)
            customMapView.savedCallbackIdForCreate = call.callbackId

            let boundingRect = call.getObject("boundingRect", JSObject())
            customMapView.boundingRect.updateFromJSObject(boundingRect)

            let mapCameraPosition = call.getObject("cameraPosition", JSObject())
            customMapView.mapCameraPosition.updateFromJSObject(mapCameraPosition, baseCameraPosition: nil)

            let preferences = call.getObject("preferences", JSObject())
            customMapView.mapPreferences.updateFromJSObject(preferences)

            self.customWebView?.scrollView.addSubview(customMapView.view)

            if (customMapView.GMapView == nil) {
                call.reject("Map could not be created. Did you forget to update the class in Main.storyboard? If you do not know what that is, please read the documentation.")
                return
            }

            self.customWebView?.scrollView.sendSubviewToBack(customMapView.view)

            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                self.setupWebView()
            }

            customMapView.GMapView.delegate = customMapView;
            self.customWebView?.customMapViews[customMapView.id] = customMapView

            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                self.setupWebView()
            }

            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                self.setupWebView()
            }
        }
    }

    func updateMap(_ call: CAPPluginCall) {
        let mapId: String = call.getString("mapId", "")

        DispatchQueue.main.async {
            guard let customMapView = self.customWebView?.customMapViews[mapId] else {
                call.reject("map not found")
                return
            }

            let preferences = call.getObject("preferences", JSObject());
            customMapView.mapPreferences.updateFromJSObject(preferences);

            let result = customMapView.invalidateMap()

            call.resolve(result)
        }

    }


    func removeMap(_ call: CAPPluginCall) {
        let mapId: String = call.getString("mapId", "")

        DispatchQueue.main.async {
            guard let customMapView = self.customWebView?.customMapViews[mapId] else {
                call.reject("map not found")
                return
            }

            (customMapView).view.removeFromSuperview()
            self.customWebView?.customMapViews.removeValue(forKey: mapId)

            call.resolve()
        }
    }

    func getMap(_ call: CAPPluginCall) {
        let mapId: String = call.getString("mapId", "")

        DispatchQueue.main.async {
            guard let customMapView = self.customWebView?.customMapViews[mapId] else {
                call.reject("map not found")
                return
            }

            let result = customMapView.getMap()

            call.resolve(result)
        }

    }

    func clearMap(_ call: CAPPluginCall) {
        let mapId: String = call.getString("mapId", "")

        DispatchQueue.main.async {
            guard let customMapView = self.customWebView?.customMapViews[mapId] else {
                call.reject("map not found")
                return
            }

            customMapView.clearMap()

            call.resolve()
        }

    }

    func moveCamera(_ call: CAPPluginCall) {
        let mapId: String = call.getString("mapId", "")

        DispatchQueue.main.async {
            guard let customMapView = self.customWebView?.customMapViews[mapId] else {
                call.reject("map not found")
                return
            }

            let mapCameraPosition = customMapView.mapCameraPosition

            var currentCameraPosition: GMSCameraPosition?;

            let useCurrentCameraPositionAsBase = call.getBool("useCurrentCameraPositionAsBase", true)

            if (useCurrentCameraPositionAsBase) {
                currentCameraPosition = customMapView.getCameraPosition()
            }

            let cameraPosition = call.getObject("cameraPosition", JSObject())
            mapCameraPosition.updateFromJSObject(cameraPosition, baseCameraPosition: currentCameraPosition)

            let duration = call.getInt("duration", 0)

            customMapView.moveCamera(duration)

            call.resolve()
        }
    }

    func addMarker(_ call: CAPPluginCall) {
        let mapId: String = call.getString("mapId", "")

        DispatchQueue.main.async {
            guard let customMapView = self.customWebView?.customMapViews[mapId] else {
                call.reject("map not found")
                return
            }

            let position = call.getObject("position", JSObject())
            let preferences = call.getObject("preferences", JSObject())

            self.addMarker([
                "position": position,
                "preferences": preferences
            ], customMapView: customMapView) { marker in
                call.resolve(CustomMarker.getResultForMarker(marker, mapId: mapId))
            }
        }
    }

    func addMarkers(_ call: CAPPluginCall) throws {
        let mapId: String = call.getString("mapId", "")

        guard let customMapView = self.customWebView?.customMapViews[mapId] else {
            throw CAPPluginError("map not found")
        }

        if let markers = call.getArray("markers")?.capacitor.replacingNullValues() as? [JSObject?] {
            // Group markers by icon url and size
            let markersGroupedByIcon = Dictionary(grouping: markers) { (marker) -> String in
                let preferences = marker?["preferences"] as? JSObject ?? JSObject()

                if let icon = preferences["icon"] as? JSObject {
                    if let url = icon["url"] as? String {
                        let size = icon["size"] as? JSObject ?? JSObject()
                        let resizeWidth = size["width"] as? Int ?? 30
                        let resizeHeight = size["height"] as? Int ?? 30

                        // Generate custom key based on the size,
                        // so we can cache the resized variant of the image as well.
                        let groupByKey = "\(url)\(resizeWidth)\(resizeHeight)"

                        return groupByKey
                    }
                }

                return ""
            }

            for markersGroup in markersGroupedByIcon {
                // Get the icon for this group by using the first marker value
                // (which should be the same as the following ones, since they are grouped by icon).
                if let firstMarker = markersGroup.value[0] {
                    let preferences = firstMarker["preferences"] as? JSObject ?? JSObject()

                    if let icon = preferences["icon"] as? JSObject {
                        if let url = icon["url"] as? String {
                            let size = icon["size"] as? JSObject ?? JSObject()
                            let resizeWidth = size["width"] as? Int ?? 30
                            let resizeHeight = size["height"] as? Int ?? 30

                            // Preload this icon into the cache.
                            self.imageCache.image(at: url, resizeWidth: resizeWidth, resizeHeight: resizeHeight) { image in
                                // Since the icon is already loaded,
                                // it is now possible to quickly render all the markers with this icon.
                                for marker in markersGroup.value {
                                    let position = marker?["position"] as? JSObject ?? JSObject();
                                    let preferences = marker?["preferences"] as? JSObject ?? JSObject();

                                    self.addMarker([
                                        "position": position,
                                        "preferences": preferences
                                    ], customMapView: customMapView) { marker in
                                        // Image is loaded
                                    }
                                }
                            }

                            continue
                        }
                    }
                }

                // Render all markers on the map without a custom icon attached to them.
                for marker in markersGroup.value {
                    let position = marker?["position"] as? JSObject ?? JSObject();
                    let preferences = marker?["preferences"] as? JSObject ?? JSObject();

                    self.addMarker([
                        "position": position,
                        "preferences": preferences
                    ], customMapView: customMapView) { marker in
                        // Image is loaded
                    }
                }
            }
        }

        call.resolve()
    }

    func removeMarker(_ call: CAPPluginCall) {
        let markerId: String = call.getString("markerId", "");

        DispatchQueue.main.async {
            let customMarker = self.customMarkers[markerId];

            if (customMarker != nil) {
                customMarker?.map = nil;
                self.customMarkers[markerId] = nil;
                call.resolve();
            } else {
                call.reject("marker not found");
            }
        }
    }

    func addPolygon(_ call: CAPPluginCall) {
        let mapId: String = call.getString("mapId", "");

        DispatchQueue.main.async {
            guard let customMapView = self.customWebView?.customMapViews[mapId] else {
                call.reject("map not found")
                return
            }

            if let path = call.getArray("path")?.capacitor.replacingNullValues() as? [JSObject?] {
                let preferences = call.getObject("preferences", JSObject())

                self.addPolygon([
                    "path": path,
                    "preferences": preferences
                ], customMapView: customMapView) { polygon in
                    call.resolve(CustomPolygon.getResultForPolygon(polygon, mapId: mapId))
                }
            }
        }
    }

    func removePolygon(_ call: CAPPluginCall) {
        let polygonId: String = call.getString("polygonId", "");

        DispatchQueue.main.async {
            if let customPolygon = self.customPolygons[polygonId] {
                customPolygon.map = nil;
                customPolygon.layer.removeFromSuperlayer()
                self.customPolygons[polygonId] = nil;
                call.resolve();
            } else {
                call.reject("polygon not found");
            }
        }
    }

    func didTapInfoWindow(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_TAP_INFO_WINDOW);
    }

    func didCloseInfoWindow(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_CLOSE_INFO_WINDOW);
    }

    func didTapMap(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_TAP_MAP);
    }

    func didLongPressMap(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_LONG_PRESS_MAP);
    }

    func didTapMarker(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_TAP_MARKER);
    }

    func didBeginDraggingMarker(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_BEGIN_DRAGGING_MARKER);
    }

    func didDragMarker(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_DRAG_MARKER);
    }

    func didEndDraggingMarker(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_END_DRAGGING_MARKER);
    }

    func didTapMyLocationButton(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_TAP_MY_LOCATION_BUTTON);
    }

    func didTapMyLocationDot(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_TAP_MY_LOCATION_DOT);
    }

    func didTapPoi(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_TAP_POI);
    }

    func didBeginMovingCamera(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_BEGIN_MOVING_CAMERA);
    }

    func didMoveCamera(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_MOVE_CAMERA);
    }

    func didEndMovingCamera(_ call: CAPPluginCall) throws {
        try setCallbackIdForEvent(call: call, eventName: CustomMapView.EVENT_DID_END_MOVING_CAMERA);
    }

    func setCallbackIdForEvent(call: CAPPluginCall, eventName: String) throws {
        let mapId: String = call.getString("mapId", "")

        guard let customMapView = self.customWebView?.customMapViews[mapId] else {
            throw CAPPluginError("map not found")
        }

        call.keepAlive = true;
        let callbackId = call.callbackId;

        let preventDefault: Bool = call.getBool("preventDefault", false);
        customMapView.setCallbackIdForEvent(callbackId: callbackId, eventName: eventName, preventDefault: preventDefault);
    }

    override func lastResultForCallbackId(callbackId: String, result: PluginCallResultData) {
        let call = bridge?.savedCall(withID: callbackId);
        call?.resolve(result);
        bridge?.releaseCall(call!);
    }

    override func resultForCallbackId(callbackId: String, result: PluginCallResultData?) {
        let call = bridge?.savedCall(withID: callbackId);
        if (result != nil) {
            call?.resolve(result!);
        } else {
            call?.resolve();
        }
    }
}

private extension CapacitorGoogleMaps {
    func addMarker(_ markerData: JSObject, customMapView: CustomMapView, completion: @escaping VoidReturnClosure<GMSMarker>) {
        DispatchQueue.main.async {
            let marker = CustomMarker()

            marker.updateFromJSObject(markerData)

            self.customMarkers[marker.id] = marker

            let preferences = markerData["preferences"] as? JSObject ?? JSObject()

            if let icon = preferences["icon"] as? JSObject {
                if let url = icon["url"] as? String {
                    let size = icon["size"] as? JSObject ?? JSObject()
                    let resizeWidth = size["width"] as? Int ?? 30
                    let resizeHeight = size["height"] as? Int ?? 30
                    DispatchQueue.global(qos: .background).async {
                        self.imageCache.image(at: url, resizeWidth: resizeWidth, resizeHeight: resizeHeight) { image in
                            DispatchQueue.main.async {
                                marker.icon = image
                                marker.map = customMapView.GMapView
                                completion(marker)
                            }
                        }
                    }
                    return
                }
            }

            marker.map = customMapView.GMapView

            completion(marker)
        }
    }

    func addPolygon(_ polygonData: JSObject, customMapView: CustomMapView, completion: @escaping VoidReturnClosure<GMSPolygon>) {
        DispatchQueue.main.async {
            let polygon = CustomPolygon()

            polygon.updateFromJSObject(polygonData)

            polygon.map = customMapView.GMapView

            self.customPolygons[polygon.id] = polygon

            completion(polygon)
        }
    }



    func setupWebView() {
        DispatchQueue.main.async {
            self.customWebView?.isOpaque = false
            self.customWebView?.backgroundColor = .clear

            let javascript = "document.documentElement.style.backgroundColor = 'transparent'"
            self.customWebView?.evaluateJavaScript(javascript)
        }
    }
}

extension CapacitorGoogleMaps: ImageCachable {
    var imageCache: ImageURLLoadable {
        NativeImageCache.shared
    }
}
