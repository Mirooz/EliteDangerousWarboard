# Module `elite-clients`

**Packaging:** Maven aggregator (`pom` only) — no code at this level.  
**Role:** Groups every **HTTP / generated OpenAPI client** used by Elite Warboard.

## Submodules

| Submodule | Purpose |
|-----------|---------|
| `edtools-client` | EdTools PvE: massacre system search, mining / hotspot related calls |
| `ardent-api-client` | Ardent: station markets, commodity prices (Inara-backed URLs where applicable) |
| `siriuscorp-client` | SiriusCorp: additional combat system search (complements EdTools) |
| `elite-backend-client` | Elite Warboard **backend**: Spansh proxy, CAPI/OAuth orchestration, analytics — OpenAPI-generated DTOs and REST stubs |
| `elite-eddn-client` | **EDDN**: schema-aligned message types, configuration for publishing eligible journal-derived events |

Each submodule has its own `pom.xml` and is versioned with the parent (`1.4.0`).

## Build & OpenAPI

- Backend client code is generated from the backend OpenAPI description (`elite-clients/elite-backend-client/...`).
- EDDN schemas may be fetched or versioned per `elite-eddn-client` configuration (see module `pom.xml` and resources).

---

## External APIs

Third-party and online services consumed by the application:

- **EdTools API** : Massacre system search and hotspot detection
- **Ardent API** : Market information
- **SiriusCorp API** : Additional combat system search (complements EdTools)
- **Spansh API** : Navigation routes (Stratum Undiscovered, Expressway to Exomastery, Road to Riches); **system/body search** to enrich exploration and colonization views; replaces former **EDSM** integration for online system data
- **[ED Colonise](https://edcolonise.net/)** : Colonizable **system discovery** for the optional colonization finder (filters, body metrics, neighbours); reached via the Warboard **backend** (see `elite-backend-client` / OpenAPI)
- **Frontier Companion API (CAPI)** : OAuth-backed access for markets and fleet carrier
- **EDDN** : Optional player-contributed market telemetry (publish path in `elite-eddn-client`)
- **Canonn Bioforge** : Big data exobiology species probability calculations based on [bioforge.canonn.tech](https://bioforge.canonn.tech/)

### Client behaviour (application layer)

- **Error handling** : Translated network error popups with user-friendly messages
- **Real-time data** : Live API integration for up-to-date system information
- **Fallback mechanisms** : Graceful handling of API unavailability (including CAPI maintenance messaging); in-app EDDN configuration lists eligible journal events when publishing is enabled
