import CoreLocation
import shared
import SwiftPhoenixClient
import SwiftUI

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase

    let platform = Platform_iosKt.getPlatform().name
    @EnvironmentObject var locationDataManager: LocationDataManager
    @EnvironmentObject var backendProvider: BackendProvider
    @EnvironmentObject var globalFetcher: GlobalFetcher
    @EnvironmentObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @EnvironmentObject var socketProvider: SocketProvider
    @EnvironmentObject var tripPredictionsFetcher: TripPredictionsFetcher
    @EnvironmentObject var vehicleFetcher: VehicleFetcher
    @EnvironmentObject var vehiclesFetcher: VehiclesFetcher
    @EnvironmentObject var viewportProvider: ViewportProvider
    @State private var sheetHeight: CGFloat = .zero
    @StateObject var nearbyVM: NearbyViewModel = .init()

    private enum SelectedTab: Hashable {
        case nearby
        case settings
    }

    @State private var selectedTab = SelectedTab.nearby

    var body: some View {
        if selectedTab == .settings {
            TabView(selection: $selectedTab) {
                nearbyTab
                    .tag(SelectedTab.nearby)
                    .tabItem { Label("Nearby", systemImage: "mappin") }
                SettingsPage()
                    .tag(SelectedTab.settings)
                    .tabItem { Label("Settings", systemImage: "gear") }
            }
        } else {
            nearbyTab
        }
    }

    @State var selectedDetent: PresentationDetent = .halfScreen
    @State var visibleNearbySheet: SheetNavigationStackEntry = .nearby

    @ViewBuilder var nearbySheetContents: some View {
        // Putting the TabView in a VStack prevents the tabs from covering the nearby transit contents
        // when re-opening nearby transit
        VStack {
            TabView(selection: $selectedTab) {
                NearbyTransitPageView(
                    globalFetcher: globalFetcher,
                    nearbyVM: nearbyVM,
                    viewportProvider: viewportProvider
                )
                .tag(SelectedTab.nearby)
                .tabItem { Label("Nearby", systemImage: "mappin") }
                // we want to show nothing in the sheet when the settings tab is open,
                // but an EmptyView here causes the tab to not be listed
                VStack {}
                    .tag(SelectedTab.settings)
                    .tabItem { Label("Settings", systemImage: "gear") }
            }
        }
    }

    var nearbyTab: some View {
        VStack {
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
                nearbyVM: nearbyVM,
                railRouteShapeFetcher: railRouteShapeFetcher,
                vehiclesFetcher: vehiclesFetcher,
                viewportProvider: viewportProvider,
                sheetHeight: $sheetHeight
            )
            .sheet(
                item: .constant($nearbyVM.navigationStack.wrappedValue.lastSafe().sheetItemIdentifiable()),
                onDismiss: {
                    selectedDetent = .halfScreen

                    if visibleNearbySheet == nearbyVM.navigationStack.last {
                        // When the visible sheet matches the last nav entry, then a dismissal indicates
                        // an intentional action remove the sheet and replace it with the previous one.

                        // When the visible sheet *doesn't* match the latest item in the nav stack, it is
                        // being dismissed so that it can be automatically replaced with the new one.
                        nearbyVM.goBack()
                    } else {}
                }
            ) { sheetIdentityEntry in
                let entry = sheetIdentityEntry.stackEntry
                GeometryReader { proxy in
                    NavigationStack {
                        switch entry {
                        case let .stopDetails(stop, _):
                            StopDetailsPage(
                                globalFetcher: globalFetcher,
                                viewportProvider: viewportProvider,
                                stop: stop, filter: $nearbyVM.navigationStack.lastStopDetailsFilter,
                                nearbyVM: nearbyVM
                            ).onAppear {
                                visibleNearbySheet = entry
                            }

                        case let .tripDetails(tripId: tripId, vehicleId: vehicleId, target: target):
                            TripDetailsPage(
                                tripId: tripId,
                                vehicleId: vehicleId,
                                target: target,
                                globalFetcher: globalFetcher,
                                nearbyVM: nearbyVM,
                                tripPredictionsFetcher: tripPredictionsFetcher,
                                vehicleFetcher: vehicleFetcher
                            ).onAppear {
                                visibleNearbySheet = entry
                            }

                        case .nearby:
                            nearbySheetContents
                                .onAppear {
                                    visibleNearbySheet = entry
                                }
                        }
                    }
                    .onAppear {
                        recordSheetHeight(proxy.size.height)
                    }
                    .onChange(of: proxy.size.height) { newValue in
                        recordSheetHeight(newValue)
                    }
                    // Adding id here prevents the next sheet from opening at the large detent.
                    // https://stackoverflow.com/a/77429540
                    .id(sheetIdentityEntry.id)
                    .presentationDetents([.small, .halfScreen, .almostFull], selection: $selectedDetent)
                    .interactiveDismissDisabled(visibleNearbySheet == .nearby)
                    .modifier(AllowsBackgroundInteraction())
                }
            }
        }
        .onAppear {
            socketProvider.socket.attach()
            Task {
                try await globalFetcher.getGlobalData()
            }
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                socketProvider.socket.attach()
            } else if newPhase == .background {
                socketProvider.socket.detach()
            }
        }
    }

    private func recordSheetHeight(_ newSheetHeight: CGFloat) {
        /*
         Only update this if we're less than half way up the users screen. Otherwise,
         the entire map is blocked by the sheet anyway, so it doesn't need to respond to height changes
         */
        guard newSheetHeight < (UIScreen.main.bounds.height / 2) else { return }
        sheetHeight = newSheetHeight
    }

    struct AllowsBackgroundInteraction: ViewModifier {
        func body(content: Content) -> some View {
            if #available(iOS 16.4, *) {
                content.presentationBackgroundInteraction(.enabled(upThrough: .halfScreen))
            } else {
                // This is actually a purely cosmetic issue - the interaction still works, things are just greyed out
                // We might need to fix that later if it looks too bad to even ship, but for now, it's probably fine
                content
            }
        }
    }
}
