import SwiftUI
import shared

struct ContentView: View {
    let platform = Platform_iosKt.getPlatform().name
    var body: some View {
        Text(String.init(
            format: NSLocalizedString("hello_platform", comment: "Hello world greeting"),
            arguments: [platform]
        ))
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
