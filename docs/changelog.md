# Changelog

## Version 1.4.0

> **Note on versioning:** The last **git tag** in this repository is `v1.3.1`. Version **1.4.0** on Maven aggregates all development since that tag (including earlier `1.3.2-SNAPSHOT` / `1.3.4-SNAPSHOT` work). There is no `v1.3.4` tag.

### Colonization (ED Colonize)

- Colonization **finder** UI with **filters**, **advanced filters**, and **pagination** for ED Colonize search
- **Architect** panel: construction list, **tier impact** icons, **system-wide** impact, T2/T3 rules refined for **completed** colonies only; completed sites removed from construction list
- **System visual** embedded in architect & search; **Barycentre** parent type; belt data structures; beacon deployment handling
- **Fleet market** integration for colonies: **optimal market** results, **distance** display, **max distance** filter, **“Use current system as source”**
- **Spansh** hydration in colonization system views (loading placeholder), registry **resync** after merge
- **Colonization overlay** + multi-monitor overlay improvements
- Exploration value indicators **suppressed** in colonization-specific views where appropriate

### Fleet Carrier

- **CAPI** fleet carrier calls and **market table** / **overlay** (missing commodities, layout, localization)
- **Commodity** ordering, labels, and **origin** logic for construction sourcing
- Efficient CAPI market traffic (e.g. rate limits)

### Frontier Companion API (CAPI) & backend

- **OAuth** login, **logout**, **decline**, long-polling for approval, **browser auth fallback** notification
- **Commander identity** checks before CAPI and persistence actions
- **Station market** upload on dock (via `CapiApiService`); backend URL updates (dev/QA/prod)
- **CAPI service downtime** user-visible handling

### EDDN

- Dedicated **`elite-eddn-client`** module: event routing, schema mapping, **personal data stripping**, publication toggles

### Exploration & Spansh

- **EDSM removed**; **Spansh** used for online system/body data (backend `SpanshFacade` / `SpanshSystemVisitedService`)
- **Optional exploration Spansh load** when changing the **displayed system** in the system view: merge bodies, enrich JSON / exobiology where journal data is absent; loading UI; preference toggle
- **Nav route** refactor (`NavRouteService`), notification refactor, **Spansh 500** inline toasts
- Species / genus matching and **Spansh exobio** mapping improvements; **JSON** panel prefers registry-enriched data with **Spansh source** hints
- **Exobiology radar** integration and **Status.json** handling improvements; exploration sale registry fixes (**current star** added when missing)
- Minor **FSSDiscoveryScan** mapping cleanup

### Persistence & commanders

- **Robust registry persistence**, journal cursor, deduplication in journal tail
- **Per-commander** preferences and snapshot stores; commander switch and **FID** change handling via `AppLifecycleService`

### UI / UX

- Multi-monitor **overlay** positioning; overlay binding to main stage (`WindowToggleService`)
- Search panel width / wrapping; donate CTA header; mission status label localization
- Verbose JavaFX logging toned down

### APIs & integrations

- **Inara API integration removed** from the app (Ardent/SiriusCorp remain for markets and combat system search; commodity registry still maps Inara IDs where relevant)
- Backend OpenAPI / client refactors (unified backend client)

### Technical

- Jackson / polymorphism fixes for **Mineral** serialization; various registry and `DashboardContext` → **`UIManager`** refactors

## Version 1.3.1

### Fix

- Resolved Java heap memory issue that could lead to OutOfMemoryError

## Version 1.3.0

### Navigation Route System (main feature)

- **Navigation Route Panel** : Complete navigation route management system integrated in exploration panel
- **Multiple Exploration Modes** : Support for Free Exploration, Stratum Undiscovered, Expressway to Exomastery, and Road to Riches
- **Visual Route Display** : Interactive horizontal route visualization with system circles and distance-based spacing
- **Remaining Jumps Tracking** : Real-time display of remaining jumps in navigation route from FSDTarget events
- **Route Overlay Window** : Detachable, draggable, and resizable overlay for navigation route display
  - Adjustable opacity with slider control
  - Auto-hide when going on foot, auto-reopen when returning to ship
  - Synchronized updates with main panel
- **System Status Indicators** : Visual markers for current system, visited systems, scoopable stars, and boost stars
- **One-Click System Copying** : Click any system circle to copy name to clipboard
- **Route Parameters Configuration** : Configure destination system and max systems for Expressway and Road to Riches
- **Spansh API Integration** : Automatic route generation using Spansh API for specialized exploration modes
- **Route Persistence** : Save and restore routes per exploration mode with visited systems tracking
- **Mode-Specific Routes** : Each exploration mode maintains its own route and visited systems state

### Exploration improvements

- **Enhanced Species Probability Sorting** : Species grouped by name with probability-based ordering (most probable first, grouped by equivalent names)

### Technical improvements

- **Memory Leak Fixes** : Fixed listener accumulation issues that could cause OutOfMemoryError
- **Thread Safety** : Improved JavaFX thread safety for UI updates from background threads
- **Error Handling** : Better error handling for popup display when windows are not registered

## Version 1.2.0

### Exploration

- **Exploration Panel** : Complete exploration tracking system with visual system representation
- **System Visual View** : Interactive orrery showing all celestial bodies with zoom and pan
- **Celestial Body Tracking** : Track all scanned planets, moons, and stars with detailed information
- **Exobiology Tracking** : Monitor organic species detection and collection with X/Y progress indicators
- **Mapping Tracking** : Track mapped planets with visual status indicators (X/Y mappable planets)
- **Exploration History** : Navigate through exploration groups with earnings and system statistics
- **System Value Calculation** : Real-time calculation including celestial bodies and exobiology bonuses
- **On-Hold Data Tracking** : Monitor unsold exploration and organic data with credit values
- **High-Value Filter** : Filter to show only high-value celestial bodies
- **JSON Detail Panel** : View complete JSON data for any celestial body
- **Exploration Overlay** : Display exploration bodies overlay on your Elite Dangerous game window
- **First Discovery Tracking** : Track first discoveries and first footfalls
- **Biological Analysis Support** : Track biological sample collection with position monitoring
- **Exobiology Probability Calculation** : Advanced species prediction using big data from Canonn Bioforge with histogram-based probability calculations
- **Exobiology Radar Navigation** : Real-time compass radar for biological sample collection with position tracking, distance display, and exclusion zone visualization

## Version 1.1.0

### Mining

- **Mining Operations Tracker** : Complete mining session tracking with real-time updates
- **Mining System Search** : Find optimal mining locations with mineral filters
- **Mineral Analysis** : Track quantities and values of mined materials
- **Price Comparison** : Toggle between best prices and station prices
- **Mining History** : View completed sessions with detailed statistics
- **Network error handling** : Translated error popups for connection issues
- **Styled ComboBoxes** : Enhanced mission filters with Elite Dangerous styling

### Massacre stacking

- **Improve massacre stacking UI** : Visual improvements
- **Massacre stacking overlay** : Real-time overlay showing how many kills are left for each faction
- **Combat History** : View completed massacre sessions with detailed statistics

## Version 1.0.0

### Massacre stacking

- **Massacre Stacking Management** : Real-time massacre mission tracking and stacking
- **System Search** : Find combat systems via **EdTools** and related APIs (**SiriusCorp** as a complement)
- **Destroyed Ships Journal** : Complete bounty tracking
- **Faction Statistics** : Detailed faction bounty analysis
- **Interactive Filtering** : Advanced mission and faction filtering
- **Elite Dangerous Theme** : Authentic game interface styling
- **Wing Mission Support** : Automatic wing mission detection
- **Automatic Reset** : Bounty statistics reset on redemption
- **Bilingual interface** : Complete English/French support
- **Multi account** : Automatic new commander detection
- **Automatic commander detection** : Notification popup with automatic journal re-reading
