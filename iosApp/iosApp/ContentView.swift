import SwiftUI
import shared
import InstantSearchSwiftUI
import InstantSearchCore
import InstantSearch


struct SearchResultsView: View {


    @ObservedObject private(set) var viewModel: SearchViewModel

    var searchResults: [String] {
        if viewModel.searchText.isEmpty {
            return []
        } else {
            return viewModel.results
        }
    }

    var body: some View {
        List {
            ForEach(searchResults, id: \.self) { name in
                NavigationLink {
                    Text("HELLO \(name)")
                } label: {
                    Text(name)
                }
            }
        }
    }
}


struct ContentView: View {
    let names = ["Holly", "Josh", "Rhonda", "Ted"]

    @State private var searchText = ""
    @State private var toggled = false




    var body: some View {
        NavigationView {

            List {
                Button(action: {toggled.toggle()}) {
                    Text("Click me")
                }
                Text(toggled ? "I'm toggled" : "I'm not")
                Text("a")
                Text("a")

                Text("a")
                Text("a")
                Text("a")
                Text("a")

                Text("a")


                Text("a")

                Text("a")

                Text("a")

                Text("a")

                Text("a")

                Text("a")

                Text("a")

                Text("a")

                Text("a")

                Text("a")

                Text("a")
                Text("a")
                Text("b")
                Text("b")

                Text("b")
                Text("b")

                Text("b")

                Text("b")


                Text("b")

                Text("b")

                Text("b")

                Text("b")


                Text("b")

                Text("b")

                Text("b")

                Text("b")


            }.overlay(searchText == "" ? nil :
            SearchResultsView(viewModel: .init(givenSearch: searchText)))
        }
        .searchable(text: $searchText )
        }
    }




extension SearchResultsView  {
    class SearchViewModel: ObservableObject {

        let results1 = ["1", "3", "5"]
        let results2 = ["2", "4", "6"]


        @Published var searchText: String
        @Published var results: [String] = [];

        init(givenSearch: String) {
            print("HELLO")
        searchText =  givenSearch
            if (searchText == "1") {
                results = results1
            }

            else if (  searchText == "2") {
                results = results2
            }
            else {

                results = results1 + results2

            }


        }
    }}
