# `elite-warboard-missions` (technical)

Maven module that contains the **JavaFX desktop application**—all main **UI areas** (Pirate mission stacking, Mining, Exploration & Exobiology, Colonization) live in this artifact.

**Functional documentation** is split by tool:

- [Missions](./missions.md) · [Mining](./mining.md) · [Exploration & Exobiology](./exploration.md) · [Colonization](./colonisation.md)  
- [Cross‑cutting features](../transversal.md)

## Package layout (summary)

```
elite-warboard-missions/
├── src/main/java/be/mirooz/elitedangerous/dashboard/
│   ├── controller/
│   ├── handlers/
│   ├── model/
│   ├── service/
│   └── view/
├── src/main/resources/
│   ├── css/, fxml/, images/
│   └── messages_*.properties
└── installer.iss
```

## Related

- [Changelog](../changelog.md)  
- [External APIs](./elite-clients.md#external-apis)  
- [Maven repo overview](../depot-maven.md)
