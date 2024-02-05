import shared
import SwiftUI

struct ContentView: View {
    let platform = Platform_iosKt.getPlatform().name
    @StateObject var searchObserver = TextFieldObserver()
    @EnvironmentObject var locationDataManager: LocationDataManager
    @EnvironmentObject var nearbyFetcher: NearbyFetcher
    @EnvironmentObject var searchResultFetcher: SearchResultFetcher
    @EnvironmentObject var predictionsFetcher: PredictionsFetcher

    var body: some View {
        NavigationView {
            VStack {
                SearchView(
                    query: searchObserver.debouncedText,
                    fetcher: searchResultFetcher
                )
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
                    NearbyTransitView(
                        location: location.coordinate,
                        nearbyFetcher: nearbyFetcher,
                        predictionsFetcher: predictionsFetcher
                    )
                }
            }
        }
        .searchable(
            text: $searchObserver.searchText,
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: "Find nearby transit"
        )
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(LocationDataManager())
            .environmentObject(NearbyFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(PredictionsFetcher(backend: IdleBackend()))
    }
}
