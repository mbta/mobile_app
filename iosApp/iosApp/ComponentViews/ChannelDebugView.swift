import Foundation
import Shared
import SwiftUI

struct ChannelDebugView<Content: View>: View {
    @ObserveInjection var inject
    let content: () -> Content

    var body: some View {
        DebugView {
            VStack(alignment: .leading) {
                content()
                ChannelDebugInfo()
            }
        }
    }
}

extension ChannelDebugView where Content == EmptyView {
    init() {
        content = { EmptyView() }
    }
}

private struct ChannelDebugInfo: View {
    @ObserveInjection var inject
    @State private var debugRepository = RepositoryDI().debug
    @State private var debugState: DebugState? = nil
    @State private var now = EasternTimeInstant.now()
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(alignment: .leading) {
            if let channelUpdates = debugState?.channelUpdates {
                Text(verbatim: "channel connections:")
                ForEach(channelUpdates.keys.sorted(), id: \.self) { key in
                    let trimmedKey = if key.count > 25 { "\(key.prefix(25))..." } else { key }
                    let updateDuration = if let updateTime = channelUpdates[key] {
                        Duration(
                            secondsComponent: abs(now.minus(other: updateTime).inWholeSeconds),
                            attosecondsComponent: 0
                        )
                        .formatted(.units(allowed: [.seconds], width: .narrow))
                    } else { "??" }

                    Text(verbatim: "\(trimmedKey) last updated \(updateDuration) ago")
                }
            }
            if let socketConnected = debugState?.socketConnected {
                Text(verbatim: "socket connected: \(socketConnected)")
            }
        }
        .task {
            for await state in debugRepository.state {
                debugState = state
            }
        }
        .onReceive(timer) { input in now = input.toEasternInstant() }
    }
}
