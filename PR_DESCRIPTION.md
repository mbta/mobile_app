### Summary

_Ticket:_ No ticket — community contribution

Adds an Android home screen widget (Glance) that displays the user's next trip between two configured stops. Users can add the widget from the home screen, tap to configure their from/to stops via search or favorites, and see real-time departure info at a glance.

**Features:**
- **MBTA Trip Widget** — Glance-based widget showing next trip (route, headsign, from/to stops, minutes until departure, track/platform)
- **Config flow** — `WidgetConfigActivity` for from/to stop selection via search or favorites, with optional labels
- **WidgetTripUseCase** — Fetches trip data from the backend API
- **WorkManager** — Periodic updates (15 min) and immediate refresh after config
- **Debug logging** — Gated behind `BuildConfig.DEBUG` for troubleshooting

**States:** Configure prompt (unconfigured), trip data, no trips available, error (tap to open app)

iOS
- [x] N/A — Android-only change

android
- [x] All user-facing strings added to strings resource in alphabetical order
- [x] Expensive calculations are run in `withContext(Dispatchers.IO)` where possible (preferences, network in `WidgetTripUseCase`)

### Testing

- Added widget to home screen and verified it shows next trip for configured stops
- Tested config flow: selected from/to stops via search and favorites, confirmed widget updates
- Tested WorkManager refresh after config and after device reboot
- Verified error state when backend unavailable (tap opens app)
- Verified "no trips" state when no departures
- Ran `./gradlew androidApp:compileStagingReleaseKotlin` — build succeeds
