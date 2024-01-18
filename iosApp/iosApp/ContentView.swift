import SwiftUI
import shared

struct ContentView: View {
    let greet = "Hello" // TODO: Add iOS i18n
    @EnvironmentObject var locationDataManager: LocationDataManager

    var body: some View {
        VStack {
            Text(greet)
            switch locationDataManager.authorizationStatus {
            case .notDetermined:
                Button("Allow Location", action: {
                    locationDataManager.locationManager.requestWhenInUseAuthorization()
                })
            case .authorizedAlways, .authorizedWhenInUse:
                Text(locationDataManager.currentLocation.debugDescription)
            case .denied, .restricted:
                Text("Location access denied or restricted")
            @unknown default:
                Text("Location access state unknown")
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(LocationDataManager())
    }
}
