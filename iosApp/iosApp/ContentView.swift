import shared
import SwiftPhoenixClient
import SwiftUI

struct ContentView: View {
    let platform = Platform_iosKt.getPlatform().name
    @StateObject var searchObserver = TextFieldObserver()
    @EnvironmentObject var locationDataManager: LocationDataManager
    @EnvironmentObject var globalFetcher: GlobalFetcher
    @EnvironmentObject var nearbyFetcher: NearbyFetcher
    @EnvironmentObject var predictionsFetcher: PredictionsFetcher
    @EnvironmentObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @EnvironmentObject var searchResultFetcher: SearchResultFetcher

    var body: some View {
        NavigationView {
            VStack {
                SearchView(
                    query: searchObserver.debouncedText,
                    fetcher: searchResultFetcher
                )
                switch locationDataManager.authorizationStatus {
                case .notDetermined:
                    Button("Allow Location", action: {
                        locationDataManager.locationFetcher.requestWhenInUseAuthorization()
                    })
                case .authorizedAlways, .authorizedWhenInUse:
                    EmptyView()
                case .denied, .restricted:
                    Text("Location access denied or restricted")
                @unknown default:
                    Text("Location access state unknown")
                }
                HomeMapView(
                    globalFetcher: globalFetcher,
                    railRouteShapeFetcher: railRouteShapeFetcher
                )
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
            .environmentObject(GlobalFetcher(backend: IdleBackend()))
            .environmentObject(SearchResultFetcher(backend: IdleBackend()))
            .environmentObject(RailRouteShapeFetcher(backend: IdleBackend()))
            .environmentObject(PredictionsFetcher(socket: Socket(endPoint: "/socket", transport: { _ in PhoenixTransportMock() })))
    }
}
