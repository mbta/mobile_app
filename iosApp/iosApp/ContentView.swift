import SwiftUI
import shared

struct ContentView: View {
    let platform = Platform_iosKt.getPlatform().name

    let homeMapVM = HomeMapViewModel()

    var body: some View {
        Text(String.init(
            format: NSLocalizedString("hello_platform", comment: "Hello world greeting"),
            arguments: [platform]
        ))
        HomeMapView(mapVM: homeMapVM)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
