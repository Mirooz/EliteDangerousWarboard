# Mining tool

The **mining tool** is the area of Elite Warboard dedicated to **active mining sessions**, **live cargo & prospector awareness**, **sell-price intelligence**, **mining-system search** (rings + sell stations), and a **prospector overlay** for use over the game. It is built from four UI blocks loaded together: **search**, **current prospector**, **current cargo**, and **session history**.

---

## Overview — what the mining tool contains

| Block | Role |
|-------|------|
| **Mining search** | Pick a **target mineral**, tune search options, run hotspot / station discovery, compare sell prices, copy system names. |
| **Current prospector** | Shows recent **`ProspectedAsteroid`** results (core vs laser icons), so you don’t need to focus the limpet to see the resource on the asteroid. |
| **Current cargo** | Live **ship cargo** snapshot (minerals + **limpets**) from the journal-driven cargo model, with **estimated prices**. |
| **Mining history** | Past **sessions**: start/end, system/body/ring context, refined tonnage, credits, totals across sessions. |

Sessions are driven by **`MiningRefined`** and related journal traffic; the model tracks **active / suspended** states, **core vs laser** session flavour where applicable, and **suspension** windows when you leave the ring briefly.

---

## Live session panel

- **Refined minerals** : each `MiningRefined` line adds commodity tonnage and feeds **estimated credits** for the open session.  
- **Materials table** : per-mineral **quantity**, **unit price**, **line total**; tooltips can show refined breakdown where the UI exposes them.  
- **Price mode** (**PRICE MODE**) : toggle **best price** (galaxy-wide max-sell style data) vs **station price** (prices relative to the **station you are docked at**, when the journal and market resolution know your market). Use this before undocking to decide whether to supercruise elsewhere to sell.  
- **Hints** : labels such as **mining system** / **selling system** reflect the context Warboard inferred from your commander state and search results.  
- **Loading / errors** : async price and search calls show **loading** states; **price not available**, **no market**, **no station**, **no hotspot**, or generic **search / price error** messages are localized.

<img src="../../elite-warboard-missions/src/main/resources/images/miningpanel.png" alt="Mining tool — session, cargo, and prices" style="max-width: 1200px;">

---

## Current cargo & limpets

- Lists **minerals currently in the hold** with quantities (and empty state when there are **no minerals**).  
- **Limpets** are tracked separately so you see collector limpet headroom alongside ore.  
- Refreshes when searches complete or journal events change cargo.

---

## Current prospector (recent scans)

- Maintains a **bounded list** of the latest prospected asteroids (newest at the end; oldest dropped after a cap).  
- **Core** vs **laser** icons match the **mining method** of the commodity / deposit type.  
- When a **mining session ends**, the prospector list is **cleared** so a new ring run starts clean.

---

## Mining system search (routes)

1. Open the **mining search** strip (localized title, e.g. *Mining search*).  
2. Choose a **target mineral** from the combo: minerals are grouped under **CORE** then **LASER** separators for quick picking.  
3. Optional filters:  
   - **Include Fleet Carrier** markets in viable sell locations.  
   - **Large pads only** if you fly a large hull and need L landing pads on the sell station.  
   - **Max distance** (light-years) from your **current system** to cap how far the tool suggests you travel for a sell.  
4. **Search** : Warboard calls **`MiningService.findMineralStation`** — online data for mining **hotspots / rings** and for **where to sell**, including price columns (**demand**, **price**, distance to station, station type icon, etc.). A **minimum demand** floor is derived from your **current cargo capacity** so tiny-demand markets are filtered out (stricter multiplier for **Fleet Carrier** rows vs regular stations).  
5. Use **previous / next** controls to walk alternate **rings** to mine and alternate **stations** to sell without re-running a full search from scratch.  
6. **Click** a result row to **copy the system name** to the clipboard for plotting in the galaxy map.

<img src="../../elite-warboard-missions/src/main/resources/images/miningsearch.png" alt="Mining system search — mineral, options, results" style="max-width: 1200px;">

---

## Session history

- Table (or card list) of **completed / inactive** sessions with **duration**, **total value**, and counts.  
- Aggregate footer often shows **number of sessions**, **total duration**, **total value** across the visible history.  
- Use it after a sell loop to compare ring profitability over time.

---

## Prospector overlay

- Small **always-on-top** window mirroring key mining metrics (commodities / values / session bits depending on build) so Elite can stay fullscreen.  
- **Open / Close** from the mining UI; geometry and multi-monitor behaviour are configured from the overlay and **Settings** while Warboard runs.

<img src="../../elite-warboard-missions/src/main/resources/images/mining/miningoverlay.png" alt="Mining prospector overlay" style="max-width: 1200px;">

---

## Journal events (primary for this tool)

| Event | Role |
|-------|------|
| `MiningRefined` | Adds refined tonnage and credits to the active session. |
| `Cargo` | Reflects hold changes (refined chunks, ejections, limpet swaps). |
| `ProspectedAsteroid` | Feeds the prospector strip (core / laser metadata). |
| `AsteroidCracked` | Core asteroid cracks. |
| `LaunchDrone`, `EjectCargo`, `BuyDrones`, `SellDrones` | Limpet and cargo logistics. |
| `MarketSell` | Selling mined goods—updates monetary context for sessions/history. |

**Also used** : `FSDJump` (location / session context), `Docked` / `Undocked` (station market for **station price** mode), plus shared commander / ship events such as `Commander`, `LoadGame`, `Location`, `Loadout`, `SupercruiseEntry`, `SupercruiseExit`, `ShutDown`.

---

## Practical tips

- **Dock** and capture a `Market` event before trusting **station price** mode at obscure outposts.  
- Tighten **max distance** when you want bubble-adjacent sells only.  
- After a long core run, **sell** and let the session close so history stays one row per outing.

---

- [← Main README](../../README.md) · **[Changelog](../changelog.md)**  
- [Pirate mission stacking](./missions.md) · [Exploration & Exobiology](./exploration.md) · [Colonization](./colonisation.md)
