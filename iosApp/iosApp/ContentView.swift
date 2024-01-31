import shared
import SwiftUI

struct ContentView: View {
    let platform = Platform_iosKt.getPlatform().name
    @StateObject var searchObserver = TextFieldObserver()
    @EnvironmentObject var locationDataManager: LocationDataManager
    @EnvironmentObject var backend: BackendDispatcher

    var body: some View {
        NavigationView {
            VStack{
                if !searchObserver.debouncedText.isEmpty {
                    SearchResultView(viewModel: .init(query: searchObserver.debouncedText, backend: backend))
                }
                Text(String.init(
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
        .searchable(
            text: $searchObserver.searchText,
            placement: .navigationBarDrawer,
            prompt: "Find nearby transit"
        )
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(LocationDataManager())
            .environmentObject(BackendDispatcher(backend: IdleBackend()))
    }
}
