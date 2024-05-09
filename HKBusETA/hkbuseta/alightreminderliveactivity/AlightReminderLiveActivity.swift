//
//  alightreminderliveactivityLiveActivity.swift
//  alightreminderliveactivity
//
//  Created by LOOHP on 15/2/2024.
//

import ActivityKit
import WidgetKit
import SwiftUI

struct AlightReminderLiveActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        let routeNumber: String
        let stopsRemaining: String
        let titleLeading: String
        let titleTrailing: String
        let content: String
        let color: Int64
        let url: String
    }
}

struct AlightReminderLiveActivityWidgetView: View {
    
    var state: AlightReminderLiveActivityAttributes.ContentState
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        VStack {
            Text(state.titleLeading).bold().multilineTextAlignment(.center)
            Text(state.titleTrailing).multilineTextAlignment(.center)
            Text(state.content).multilineTextAlignment(.center)
        }
        .padding(10)
        .activityBackgroundTint(state.color.asColor().adjustBrightness(percentage: colorScheme == .light ? 1.6 : 0.4))
    }
    
}

struct AlightReminderLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: AlightReminderLiveActivityAttributes.self) { context in
            AlightReminderLiveActivityWidgetView(state: context.state)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Text(context.state.routeNumber)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.state.stopsRemaining)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text(context.state.titleLeading).bold().multilineTextAlignment(.center)
                    Text(context.state.titleTrailing).multilineTextAlignment(.center)
                    Text(context.state.content).multilineTextAlignment(.center)
                }
            } compactLeading: {
                Text(context.state.routeNumber)
            } compactTrailing: {
                Text(context.state.stopsRemaining)
            } minimal: {
                Text("\(context.state.routeNumber) \(context.state.stopsRemaining)")
            }
            .widgetURL(URL(string: context.state.url))
            .keylineTint(context.state.color.asColor())
        }
    }
}
