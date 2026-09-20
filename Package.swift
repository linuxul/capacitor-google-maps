// swift-tools-version: 5.9
import Foundation
import PackageDescription

// Apps override this dependency with the @capacitor/ios they installed. To build this package on its own
// against a local runtime, point CAPACITOR_IOS_PATH at it.
let capacitor: Package.Dependency
if let path = ProcessInfo.processInfo.environment["CAPACITOR_IOS_PATH"] {
    capacitor = .package(name: "capacitor-swift-pm", path: path)
} else {
    capacitor = .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
}

let package = Package(
    name: "CapacitorCommunityGoogleMaps",
    platforms: [.iOS(.v17)],
    products: [
        .library(
            name: "CapacitorCommunityGoogleMaps",
            targets: ["CapacitorCommunityGoogleMaps"]
        )
    ],
    dependencies: [
        capacitor,
        .package(url: "https://github.com/googlemaps/ios-maps-sdk.git", from: "10.4.0"),
        .package(url: "https://github.com/SDWebImage/SDWebImage.git", from: "5.14.3")
    ],
    targets: [
        .target(
            name: "CapacitorCommunityGoogleMaps",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "GoogleMaps", package: "ios-maps-sdk"),
                .product(name: "SDWebImage", package: "SDWebImage")
            ],
            path: "ios/Sources/CapacitorGoogleMaps"
        )
    ]
)
