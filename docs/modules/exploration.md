# Exploration & Exobiology tool

This area combines a **system map (orrery)**, **exobiology** tools (species probabilities, sampling radar), **navigation routes**, **exploration history**, and optional **Synchronized system from Spansh** when you change the selected system.

---

## Navigation routes

### Modes

| Mode | Data source | Use case |
|------|-------------|----------|
| **Free exploration** | `NavRoute.json` + journal `NavRoute` / `FSDTarget` | Follow Elite’s plotted multi-jump route; remaining jumps from `FSDTarget`. |
| **Stratum Undiscovered** | **Spansh** | Hunt undiscovered Stratum Tectonica targets along a generated route. |
| **Expressway to Exomastery** | **Spansh** | Parameterised exobiology-oriented routing. |
| **Road to Riches** | **Spansh** | Valuable-body oriented routing. |

### Route UI

- Horizontal **timeline** of systems with distance-aware spacing.  
- **Current system** enlarged; **visited** systems styled differently; **scoopable** and **neutron / white dwarf boost** markers.  
- **Click** a system disc to **copy** its name to the clipboard.  
- **Reload** for Spansh-backed modes if the route data goes stale.  
- **Route overlay** : detached window with opacity slider; can auto-hide on foot depending on build—synced with the main panel.

**Spansh route errors** (e.g. HTTP 500) show as **inline toasts** in the route strip so you know the request failed without a modal blocking play.

<img src="../../elite-warboard-missions/src/main/resources/images/readme/nav.png" alt="Navigation route strip" style="max-width: 1200px;">

---

## System view (orrery) and body list

- **Pan / zoom** the orrery; reset when switching to another **system card** or the current location.  
- **Body list** : filter **high-value** bodies, jump to a body in the orrery, open **JSON** detail.  
- **JSON panel** prefers **registry-merged** journal data; when **Spansh** fills gaps you may see a **source** hint in the UI.  
- **On-foot** and **disembark** states affect overlays and some biology prompts—see journal list below.

### Synchronized system from Spansh

When **“Spansh exploration load”** is on in preferences (default), Warboard **pulls planet data from Spansh** for the system you are viewing and **merges** it with your **journal**: anything you have not scanned yet (or only partly know) is filled in from Spansh so you get a **clearer preview of the whole system**—bodies, gaps, and exobiology hints—without doing every step in the game first. Turn the toggle **off** if you prefer no online body lookups.

<img src="../../elite-warboard-missions/src/main/resources/images/readme/exploboard.png" alt="Exploration board and orrery" style="max-width: 1200px;">

---

## Exobiology

**After a planet (body) is scanned**, Warboard **estimates which exobiology species are likely present** there—so you see a ranked view of candidates before you commit to landing and sampling. Those estimates use **`bioforge-biodatas`** (Canonn Bioforge–style histograms): body class, atmosphere, volcanism, temperature, gravity, pressure, and you can **hide low-probability** species in the UI.

- **`FSSBodySignals`** : narrows which species are even in the running for that world.  
- **Collection X/Y** : journal counts toward a complete biological set on that body.  
- **Exploration overlay** : optional floating body list / highlights on top of the game.

<img src="../../elite-warboard-missions/src/main/resources/images/readme/overlayexplo.png" alt="Exploration overlay exobiology" style="max-width: 1200px;">

### Sampling radar

On foot, a **radar** (compass view, live from **Status.json** ~ every 500 ms) shows **where your last exobiology sample was taken** and **how much ground you still need to cover** before you can safely scan the **next** one of the same species—**colony range** / minimum spacing is drawn as **dashed rings**, with **metre distances** to each sample and your **heading** so you can line up the next placement without guessing.

<img src="../../elite-warboard-missions/src/main/resources/images/readme/radar3.png" alt="Exobiology sampling radar" style="max-width: 1200px;">

---

## Exploration history

Each **group** is the **set of systems you visited** between two exploration sell runs (journal `SellExplorationData` / `MultiSellExplorationData`). For that batch you see **estimated scan value** (credits), **exobiology you actually discovered**, and how many systems were in the trip. **Previous / Next** walks the groups; click a **system card** to reopen it in the orrery, or jump to **current system** when the journal position is known.

<img src="../../elite-warboard-missions/src/main/resources/images/readme/explohistory.png" alt="Exploration history groups" style="max-width: 1200px;">

---

## Journal events (primary for this tool)

**Bodies & scans**  
`Scan`, `ScanOrganic`, `FSSBodySignals`, `SAAScanComplete`, `SAASignalsFound`, `ApproachBody`, `LeaveBody`, `Embark`, `Disembark`

**Sales & grouping**  
`SellExplorationData`, `MultiSellExplorationData`, `SellOrganicData`

**Routing**  
`NavRoute`, `NavRouteClear`, `FSDTarget`

**Signals**  
`FSSSignalDiscovered` (exploration-side handling)

**Shared** with other tools: `Commander`, `LoadGame`, `Location`, `Docked`, `Undocked`, `FSDJump`, `Loadout`, `SupercruiseEntry`, `SupercruiseExit`, `ShutDown`.

---

## Practical tips

- Run **FSS discovery** and **honk** before expecting full body lists; Spansh merge still respects journal as source of truth.  
- For long **Road to Riches** chains, keep the **route overlay** on a second screen to avoid tabbing out for the next system name.  
- If **Synchronized system from Spansh** is slow on your connection, turn **Spansh exploration load** off when crossing dense bubble regions.

---

- [← Main README](../../README.md) · **[Changelog](../changelog.md)**  
- [Pirate mission stacking](./missions.md) · [Mining](./mining.md) · [Colonization](./colonisation.md)
