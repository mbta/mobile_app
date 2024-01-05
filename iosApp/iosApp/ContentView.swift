import SwiftUI
import shared

struct ContentView: View {
	let greet = "Hello" // TODO: Add iOS i18n

	var body: some View {
		Text(greet)
	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}
