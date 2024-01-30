import shared
import SwiftUI

struct ContentView: View {
    let platform = Platform_iosKt.getPlatform().name
    @EnvironmentObject var locationDataManager: LocationDataManager
    @EnvironmentObject var backend: BackendDispatcher

    var body: some View {
        VStack {
            Text(String(
                format: NSLocalizedString("hello_platform", comment: "Hello world greeting"),
                arguments: [platform]
            ))
            switch locationDataManager.authorizationStatus {
            case .notDetermined:
                Button("Allow Location", action: {
                    locationDataManager.locationFetcher.requestWhenInUseAuthorization()
                })
            case .authorizedAlways, .authorizedWhenInUse:
                Text(locationDataManager.currentLocation.debugDescription)
            case .denied, .restricted:
                Text("Location access denied or restricted")
            @unknown default:
                Text("Location access state unknown")
            }
            Spacer()
            if let location = locationDataManager.currentLocation {
                NearbyTransitView(viewModel: .init(location: location.coordinate, backend: backend))
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(LocationDataManager())
            .environmentObject(BackendDispatcher(backend: IdleBackend()))
    }
}
