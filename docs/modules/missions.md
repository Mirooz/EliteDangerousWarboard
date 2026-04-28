# Pirate mission stacking tool

## What is pirate mission stacking?

In *Elite Dangerous*, **pirate mission stacking** is when you **accept several massacre (pirate hunt) missions** that all require kills **in the same place** against the **same pirate minor faction**. The mission board still lists each contract on its own, but a single valid kill against that faction typically increments the kill count on **every** active mission that shares that target, until each is completed and cashed in separately.

- **Source systems** — stations or settlements where factions post missions on the mission board; that is where you **pick up** each contract.

- **Target systems** — the system named in each mission (often a **resource extraction** area); pirates spawned there belong to the **target faction** you must destroy.

Those contracts can be offered by **different factions** on **different mission boards**; that does not stop the stack as long as every line names the **same target faction** and you fight in the **same space**. You still **turn in each mission on its own** and collect that contract’s reward when it completes.

---

## What this tool provides

**Stack pirate massacre missions in one place.** The tool reads your **journal** and surfaces:

- **Live kill credit** toward every eligible stacked mission that shares a target faction.
- **Per–target-faction** “kills remaining” and credits still tied to open contracts.
- **Destroyed ships** log (bounties, timestamps) for proof and pacing.
- **EdTools** / **SiriusCorp** **combat system search** from the header.
- Optional **target overlay** so you do not need the main dashboard on screen (see [`docs/transversal.md`](../transversal.md)).

---

## What the tool shows

| Area | Purpose |
|------|---------|
| **Header** | **Active missions** count, **potential credits**, **Search combat systems**, and filters (**Type**, **Status**). |
| **Left column** | **Destroyed ships**: time, ship type, bounty per kill; summary counts and **pending credits** for the current stretch of combat. |
| **Center** | **Mission list** — active stacked contracts and their progress. |
| **Right sidebar** | **Targets** — per–target-faction remaining kills, overlay toggle, lock. **Mission history** (below) — scrollable **past sessions** you completed: route (e.g. source → target system), session type, missions / kills / credits totals, and **last activity** timestamp. |

<img src="../../elite-warboard-missions/src/main/resources/images/missionpanel.png" alt="Pirate mission stacking — dashboard" style="max-width: 1200px;">

### Target overlay

A separate **overlay** window lists **remaining kills per faction** so you do not need the main dashboard on screen. Position it on a second monitor or over the game; behavior is described in [`docs/transversal.md`](../transversal.md).

---

## Massacre stacking (behaviour)

- **Stacking** : Any `Bounty` event that matches your configured massacre targets increments **every eligible active mission** that shares that target faction.
- **Redeem / vouchers** : `RedeemVoucher` resets displayed bounty totals where applicable so you do not double-count after cashing in.
- **Session / history** : Completed massacre sessions can be reviewed with kills and earnings (see UI “history” affordances in-app).

---

## Combat system search (header tool)

Opens from **SEARCH COMBAT SYSTEMS** (wording may match your locale).

1. Pick the search mode offered in the dialog (fields depend on **EdTools** / **SiriusCorp**).
2. Fill criteria (distance, allegiance, etc.—exact fields follow the live UI).
3. **SEARCH** loads results from the network.
4. **Click a row** to copy the **system name** to the clipboard for plotting in the galaxy map.

**Massacre-oriented** results use **EdTools**-style PvE system data; **SiriusCorp** backs the complementary search path in the same wizard.

<img src="../../elite-warboard-missions/src/main/resources/images/searchmission2.png" alt="Combat system search UI" style="max-width: 1200px;">

---

## Journal events (primary for this tool)

### Mission lifecycle

`MissionAccepted`, `MissionCompleted`, `MissionFailed`, `MissionRedirected`, `MissionAbandoned`, `MissionExpired`, `MissionProgress`

### Combat and crime

`Bounty`, `RedeemVoucher`, `FactionKillBond`, `CommitCrime`, `Died`, `ShipTargeted`

### Shared with other tools

`Commander`, `LoadGame`, `Location`, `Docked`, `Undocked`, `FSDJump`, `Loadout`, `SupercruiseEntry`, `SupercruiseExit`, `ShutDown` — see **[Shared journal events](../transversal.md#shared-journal-events)**.

---

## Practical tips

- Keep the journal path correct **before** accepting a new wing’s missions so stacking starts immediately.
- If kills look “stuck”, confirm the destroyed ship’s **faction** matches the mission’s **target faction** filter in-game.
- Use the **overlay** in supercruise or over the game client so you never miss remaining kill counts.

---

- [← Documentation index](../README.md)
- [Mining](./mining.md) · [Exploration & Exobiology](./exploration.md) · [Colonization](./colonisation.md) · [Cross‑cutting features](../transversal.md)
