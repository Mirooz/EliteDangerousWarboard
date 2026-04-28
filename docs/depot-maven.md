# Maven repository layout (developers)

**Maven modules** are **build artifacts**. They are **not** the same as the in-app **tools** (see [`README.md`](./README.md) in this folder).

| Artifact | Role |
|----------|------|
| `elite-warboard-parent` | Root aggregator `pom.xml`, project version. |
| `elite-warboard-missions` | Main **JavaFX** application (all user tools). |
| `elite-clients` | Parent POM for `edtools-client`, `ardent-api-client`, `siriuscorp-client`, `elite-backend-client`, `elite-eddn-client`. |
| `elite-commons` | Shared models, commodity registry, utilities. |
| `journal-analyzer` | Standalone journal viewer tool — see [`journal-analyzer/README.md`](../journal-analyzer/README.md). |
| `bioforge-biodatas` | Packaged Bioforge histogram JSON — see [`bioforge-biodatas/README.md`](../bioforge-biodatas/README.md). |

HTTP client details: **[`modules/elite-clients.md`](./modules/elite-clients.md)**.

---

- [← Documentation index](./README.md)
