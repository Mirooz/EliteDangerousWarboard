# Elite Warboard — documentation index

In this repository, **“module” in user docs** means an application **tool** (Pirate mission stacking, Mining, Exploration & Exobiology, Colonization)—**not** a Maven artifact. Maven layout is under **[`depot-maven.md`](./depot-maven.md)**.

## Tool documentation (detailed)

| Tool | Document |
|-----|----------|
| **Pirate mission stacking** | [`modules/missions.md`](./modules/missions.md) — concept (source vs target), massacre stacking, combat search, overlay |
| **Mining** | [`modules/mining.md`](./modules/mining.md) — mining **tool**: sessions, cargo & prospector, price modes, EdTools+Ardent search (FC / large pads / max distance), history, overlay |
| **Exploration & Exobiology** | [`modules/exploration.md`](./modules/exploration.md) — orrery, exobiology, Spansh routes, synchronized system from Spansh, history (visited systems, scan value, discovered exobiology) |
| **Colonization** | [`modules/colonisation.md`](./modules/colonisation.md) — **ongoing builds** (Architect + journal), **optimal markets** that bundle many commodities for hauling, FC/CAPI, overlays; optional ED Colonize search |

## Cross‑cutting

- **[`transversal.md`](./transversal.md)** — language, commander scope, CAPI / EDDN / analytics, overlays, VR/HOTAS, **shared journal events**

## Developers

- **[`depot-maven.md`](./depot-maven.md)** — Maven modules (`elite-clients`, `elite-commons`, …)  
- **[`modules/elite-clients.md`](./modules/elite-clients.md)** — HTTP clients and **external API** reference  
- **[`modules/elite-warboard-missions.md`](./modules/elite-warboard-missions.md)** — JavaFX app **source tree** (technical)

## Other

| Page | Content |
|------|---------|
| [`changelog.md`](./changelog.md) | Release history |

## Conventions

- Use **relative links** between files under `docs/`.  
- Images in `docs/modules/*.md` use `../../elite-warboard-missions/src/main/resources/images/...`.
