# Cross‑cutting application features

Topics that are **not limited to a single tool**: language, theme, commander lifecycle, **CAPI / EDDN / analytics**, overlays, VR bindings, and **journal events consumed everywhere**.

---

## Language & theme

- Full UI in **five languages**: **English**, **French**, **German**, **Italian**, and **Spanish** (`messages_en`, `messages_fr`, `messages_de`, `messages_it`, `messages_es` — menus, errors, mission labels, search hints, etc.).  
- Visual theme aligned with **Elite Dangerous** (orange / cyan), styled controls, responsive spacing.

---

## Commander & persistence

- **New commander** : when the journal `Commander` / `LoadGame` identity changes, Warboard can prompt and **re-read** journals so you do not mix commanders.  
- **Per-commander state** : preferences and persisted registry snapshots are keyed by **FID** so alts do not overwrite each other.

---

## Online services (CAPI, EDDN, analytics)

### Frontier Companion API (CAPI)

- Optional **OAuth** login (no Frontier API keys stored in the client).  
- Unlocks **station commodity snapshots** on dock and **Fleet Carrier** market sync.  
- Supports **logout**, **decline**, long-polling while waiting for Frontier approval, and a **fallback notification** if browser-based auth fails.  
- **Downtime / errors** surface as user-visible messages so you know the service is degraded.

### EDDN

- **Separate toggle** from CAPI: when enabled, eligible **journal-derived** events are published with **personal identifiers stripped**, following schema rules in `elite-eddn-client`.  
- The in-app **EDDN hint** lists which journal events may be forwarded.

### Analytics error logs

- Optional upload of **error logs** to the analytics backend.  
- **First-run consent** dialog; default is **off** until you opt in.

**Full third-party list** : **[External APIs](./modules/elite-clients.md#external-apis)**

---

## Overlays & multi-monitor

Overlays exist for **missions** (kills remaining), **exploration** (bodies / route), **colonization**, and **mining** (prospector). They:

- Can be placed on **secondary monitors** and remember geometry where supported.  
- Stay loosely **in sync** with the main JavaFX stage via the window toggle service.  
- May **auto-hide** on foot depending on overlay type—see each tool doc for specifics.

---

## VR / HOTAS dashboard toggle

Bind a **keyboard key** or **HOTAS button** (in preferences) to show or hide the whole dashboard—useful in VR or supercruise-only setups.

<img src="../elite-warboard-missions/src/main/resources/images/readme/screenexplo2.jpeg" alt="Dashboard over game — example" style="max-width: 1200px;">

---

## Live journal tail

The watcher continuously reads new lines from the latest `.log` files so the tools update while you play—no manual refresh.

---

## Shared journal events

Many handlers subscribe to the same core ship/commander events:

| Event | Typical use |
|-------|-------------|
| `Commander` | Commander switch detection |
| `LoadGame` | Session start, online flag |
| `Location` | Star system / body / station context |
| `Docked` / `Undocked` | Station services, market context, colonization refresh |
| `FSDJump` | System change—mining session context, exploration route advance |
| `Loadout` | Ship loadout and cargo capacity |
| `SupercruiseEntry` / `SupercruiseExit` | Flight mode |
| `ShutDown` | Clean shutdown hooks |

---

- [← Documentation index](./README.md)  
- Areas: [Missions](./modules/missions.md) · [Mining](./modules/mining.md) · [Exploration & Exobiology](./modules/exploration.md) · [Colonization](./modules/colonisation.md)
