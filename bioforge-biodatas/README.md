# Bioforge Biodatas

This module contains statistical data used by [Canonn Bioforge](https://bioforge.canonn.tech/) to calculate exobiology species appearance probabilities in Elite Dangerous.

## 📊 Data Used by Canonn Bioforge

The data comes from analyzing thousands of real biological species discoveries in Elite Dangerous. Each JSON file contains statistical histograms that allow predicting which species can appear on a given planet based on its physical characteristics.

## 📁 File Structure

JSON files are organized by species genus and colony range:
- Format: `{genus}_{colony_range}m.json`
- Example: `bacterium_500m.json` (Bacteria with 500m colony range)

### Available Species List

- `aleoida_150m.json` - Aleoida (150m)
- `bacterium_500m.json` - Bacterium (500m)
- `cactoida_300m.json` - Cactoida (300m)
- `clypeus_150m.json` - Clypeus (150m)
- `concha_150m.json` - Concha (150m)
- `electricae_1000m.json` - Electricae (1000m)
- `fonticulua_500m.json` - Fonticulua (500m)
- `frutexa_150m.json` - Frutexa (150m)
- `fumerola_100m.json` - Fumerola (100m)
- `fungoida_300m.json` - Fungoida (300m)
- `osseus_800m.json` - Osseus (800m)
- `recepta_150m.json` - Recepta (150m)
- `stratum_500m.json` - Stratum (500m)
- `tubus_800m.json` - Tubus (800m)
- `tussock_200m.json` - Tussock (200m)

## 📋 Complete JSON Data Schema

Each JSON file contains a root object where each key is a **unique variant ID**. Each variant contains the following data:

### Main Structure

```json
{
  "{variant_id}": {
    "name": "string",
    "fdevname": "string",
    "count": "integer",
    "reward": "integer",
    "atmosComposition": ["string"],
    "atmosphereType": ["string"],
    "bodies": ["string"],
    "histograms": {
      "atmos_types": {},
      "body_types": {},
      "gravity": [],
      "pressure": [],
      "temperature": [],
      "volcanic_body_types": {},
      "distance": [],
      "local_stars": {},
      "materials": {}
    }
  }
}
```

## 📊 Complete Data Values

### All Atmosphere Types (`atmos_types`)

Complete list of all atmosphere types found in the dataset:

- `No atmosphere`
- `Thin Ammonia`
- `Thin Argon`
- `Thin Carbon dioxide`
- `Thin Helium`
- `Thin Methane`
- `Thin Neon`
- `Thin Nitrogen`
- `Thin Oxygen`
- `Thin Sulphur dioxide`
- `Thin Water`
- `Thick Ammonia`
- `Thick Argon`
- `Thick Carbon dioxide`
- `Thick Helium`
- `Thick Methane`
- `Thick Neon`
- `Thick Nitrogen`
- `Thick Oxygen`
- `Thick Sulphur dioxide`
- `Thick Water`

### All Body Types (`body_types`)

Complete list of all celestial body types found in the dataset:

- `Rocky body`
- `High metal content world`
- `Rocky Ice world`
- `Icy body`
- `Water world`
- `Earth-like world`
- `Ammonia world`
- `Metal-rich body`
- `Gas giant with water-based life`
- `Gas giant with ammonia-based life`
- `Class I gas giant`
- `Class II gas giant`
- `Class III gas giant`
- `Class IV gas giant`
- `Class V gas giant`
- `Water giant`
- `Ice world`

### All Star Types (`local_stars`)

Complete list of all star types found in the dataset:

- `A (Blue-White) Star`
- `A (Blue-White super giant) Star`
- `B (Blue-White) Star`
- `F (White) Star`
- `G (White-Yellow) Star`
- `K (Yellow-Orange) Star`
- `K (Yellow-Orange giant) Star`
- `L (Brown dwarf) Star`
- `M (Red dwarf) Star`
- `M (Red giant) Star`
- `O (Blue-White) Star`
- `T (Brown dwarf) Star`
- `T Tauri Star`
- `Y (Brown dwarf) Star`
- `White Dwarf (D) Star`
- `White Dwarf (DA) Star`
- `White Dwarf (DAB) Star`
- `White Dwarf (DC) Star`
- `White Dwarf (DCV) Star`
- `White Dwarf (DQ) Star`
- `Black Hole`
- `Neutron Star`
- `null` (no primary star data)

### All Surface Materials (`materials`)

Complete list of all surface materials found in the dataset:

- `Antimony`
- `Arsenic`
- `Cadmium`
- `Carbon`
- `Chromium`
- `Germanium`
- `Iron`
- `Manganese`
- `Mercury`
- `Molybdenum`
- `Nickel`
- `Niobium`
- `Phosphorus`
- `Polonium`
- `Ruthenium`
- `Selenium`
- `Sulphur`
- `Technetium`
- `Tellurium`
- `Tin`
- `Tungsten`
- `Vanadium`
- `Yttrium`
- `Zinc`
- `Zirconium`

### All Volcanic Body Types (`volcanic_body_types`)

Complete list of all body type + volcanism combinations found in the dataset:

Format: `"{BodyType} - {VolcanismType}"`

**Rocky body combinations:**
- `Rocky body - No volcanism`
- `Rocky body - Silicate vapour geysers`
- `Rocky body - Water geysers`
- `Rocky body - Carbon dioxide geysers`
- `Rocky body - Ammonia geysers`
- `Rocky body - Methane geysers`
- `Rocky body - Nitrogen geysers`
- `Rocky body - Silicate magma`
- `Rocky body - Iron magma`
- `Rocky body - Rocky magma`
- `Rocky body - Major Silicate Magma`
- `Rocky body - Major Iron Magma`
- `Rocky body - Major Rocky Magma`

**High metal content world combinations:**
- `High metal content world - No volcanism`
- `High metal content world - Silicate vapour geysers`
- `High metal content world - Water geysers`
- `High metal content world - Carbon dioxide geysers`
- `High metal content world - Ammonia geysers`
- `High metal content world - Methane geysers`
- `High metal content world - Nitrogen geysers`
- `High metal content world - Silicate magma`
- `High metal content world - Iron magma`
- `High metal content world - Rocky magma`
- `High metal content world - Major Silicate Magma`
- `High metal content world - Major Iron Magma`
- `High metal content world - Major Rocky Magma`

**Rocky Ice world combinations:**
- `Rocky Ice world - No volcanism`
- `Rocky Ice world - Water geysers`
- `Rocky Ice world - Carbon dioxide geysers`
- `Rocky Ice world - Ammonia geysers`
- `Rocky Ice world - Methane geysers`
- `Rocky Ice world - Nitrogen geysers`
- `Rocky Ice world - Silicate magma`
- `Rocky Ice world - Iron magma`
- `Rocky Ice world - Rocky magma`

**Icy body combinations:**
- `Icy body - No volcanism`
- `Icy body - Water geysers`
- `Icy body - Carbon dioxide geysers`
- `Icy body - Ammonia geysers`
- `Icy body - Methane geysers`
- `Icy body - Nitrogen geysers`

**Other body types:**
- `Water world - No volcanism`
- `Ammonia world - No volcanism`
- `Metal-rich body - No volcanism`
- `Metal-rich body - Silicate magma`
- `Metal-rich body - Iron magma`
- `Metal-rich body - Rocky magma`

### All Galactic Regions (`regions`)

Complete list of all galactic regions found in the dataset:

- `0 Antimony`
- `Acheron`
- `Achilles's Altar`
- `Aquila's Halo`
- `Arcadian Stream`
- `Dryman's Point`
- `Empyrean Straits`
- `Errant Marches`
- `Elysian Shore`
- `Formidian Frontier`
- `Formidine Rift`
- `Galactic Centre`
- `Hawking's Gap`
- `Hieronymus Delta`
- `Inner Orion-Perseus Conflux`
- `Inner Orion Spur`
- `Inner Scutum-Centaurus Arm`
- `Izanami`
- `Kepler's Crest`
- `Lyra's Song`
- `Mare Somnia`
- `Newton's Vault`
- `Norma Arm`
- `Norma Expanse`
- `Odin's Hold`
- `Outer Arm`
- `Outer Orion-Perseus Conflux`
- `Outer Orion Spur`
- `Outer Scutum-Centaurus Arm`
- `Perseus Arm`
- `Ryker's Hope`
- `Sagittarius-Carina Arm`
- `Sanguineous Rim`
- `Temple`
- `Tenebrae`
- `The Abyss`
- `The Conduit`
- `The Veils`
- `The Void`
- `Trojan Belt`
- `Vulcan Gate`
- `Xibalba`

### Reward Values Range

Reward values found in the dataset range from:
- **Minimum**: ~500,000 Cr
- **Maximum**: ~15,000,000 Cr
- **Common values**: 1,000,000 Cr, 2,000,000 Cr, 4,000,000 Cr, 6,000,000 Cr, 8,000,000 Cr, 10,000,000 Cr, 12,000,000 Cr, 15,000,000 Cr

### Physical Parameter Ranges

#### Gravity (`gravity` bins)
- **Range**: Typically 0.04g to 0.5g
- **Most common range**: 0.13g to 0.25g
- **Bin count**: 10-20 bins per variant

#### Pressure (`pressure` bins)
- **Range**: Typically 0.001atm to 0.1atm
- **Most common range**: 0.02atm to 0.08atm
- **Bin count**: 10-20 bins per variant

#### Temperature (`temperature` bins)
- **Range**: Typically 180K to 350K
- **Most common range**: 200K to 280K
- **Bin count**: 10-30 bins per variant

#### Distance (`distance` bins)
- **Range**: Typically 100 LS to 1,000,000 LS
- **Most common range**: 1,000 LS to 100,000 LS
- **Bin count**: 10-15 bins per variant (may include empty bins)

### Field Details

#### Base Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `name` | `string` | Full variant name (Genus Species - Color) | `"Osseus Fractus - Lime"` |
| `fdevname` | `string` | Internal Frontier Developments name | `"$Codex_Ent_Osseus_01_A_Name;"` |
| `count` | `integer` | Total number of discoveries for this variant | `1610` |
| `reward` | `integer` | Base credit value for this variant | `4027800` |
| `atmosComposition` | `array<string>` | List of possible atmospheric compositions | `["Carbon dioxide"]` |
| `atmosphereType` | `array<string>` | List of possible atmosphere types | `["Thin Carbon dioxide"]` |
| `bodies` | `array<string>` | List of possible celestial body types | `["High metal content world", "Rocky body"]` |

#### Histogram Structures

##### `atmos_types` (Map)
- **Type**: `object<string, number>`
- **Description**: Distribution of atmosphere types where this variant was found
- **Key**: One of the atmosphere types listed above
- **Value**: Number of occurrences
- **Example**:
```json
"atmos_types": {
  "Thin Carbon dioxide": 1583,
  "Thick Carbon dioxide": 27
}
```

##### `body_types` (Map)
- **Type**: `object<string, number>`
- **Description**: Distribution of celestial body types where this variant was found
- **Key**: One of the body types listed above
- **Value**: Number of occurrences
- **Example**:
```json
"body_types": {
  "High metal content world": 258,
  "Rocky body": 1325
}
```

##### `gravity` (Array of Bins)
- **Type**: `array<Bin>`
- **Description**: Gravity distribution in g (value ranges)
- **Bin Structure**:
  - `min`: `number` - Minimum value of the range
  - `max`: `number` - Maximum value of the range
  - `value`: `number` - Number of occurrences in this range
- **Example**:
```json
"gravity": [
  {
    "max": 0.06076143740865367,
    "min": 0.041836545324768,
    "value": 33
  },
  {
    "max": 0.07968632949253933,
    "min": 0.06076143740865367,
    "value": 21
  }
]
```

##### `pressure` (Array of Bins)
- **Type**: `array<Bin>`
- **Description**: Atmospheric pressure distribution in atm (value ranges)
- **Structure**: Identical to `gravity`
- **Example**:
```json
"pressure": [
  {
    "max": 0.03186851046796611,
    "min": 0.0258166404539847,
    "value": 92
  }
]
```

##### `temperature` (Array of Bins)
- **Type**: `array<Bin>`
- **Description**: Temperature distribution in Kelvin (value ranges)
- **Structure**: Identical to `gravity`
- **Example**:
```json
"temperature": [
  {
    "max": 250.5,
    "min": 200.0,
    "value": 150
  }
]
```

##### `volcanic_body_types` (Map)
- **Type**: `object<string, number>`
- **Description**: Distribution of body type + volcanism combinations
- **Key**: Format `"{BodyType} - {VolcanismType}"` (see complete list above)
- **Value**: Number of occurrences
- **Example**:
```json
"volcanic_body_types": {
  "Rocky body - Silicate vapour geysers": 500,
  "High metal content world - No volcanism": 200
}
```

##### `distance` (Array of Bins)
- **Type**: `array<Bin>`
- **Description**: Distance from arrival distribution in LS (Light Seconds)
- **Structure**: Identical to `gravity`
- **Note**: May contain empty objects `{}` for ranges without data
- **Example**:
```json
"distance": [
  {
    "max": 44125.57897541666,
    "min": 1494.884849,
    "value": 1578
  },
  {},
  {
    "max": 129386.96722824997,
    "min": 86756.27310183331,
    "value": 2
  }
]
```

##### `local_stars` (Map)
- **Type**: `object<string, number>`
- **Description**: Distribution of star types in the system where this variant was found
- **Key**: One of the star types listed above
- **Value**: Number of occurrences
- **Example**:
```json
"local_stars": {
  "A (Blue-White) Star": 1394,
  "M (Red dwarf) Star": 6,
  "Y (Brown dwarf) Star": 168,
  "null": 10
}
```

##### `materials` (Map)
- **Type**: `object<string, number>`
- **Description**: Distribution of surface materials present on planets where this variant was found
- **Key**: One of the materials listed above
- **Value**: Number of occurrences
- **Example**:
```json
"materials": {
  "Carbon": 1577,
  "Iron": 1577,
  "Nickel": 1577,
  "Phosphorus": 1577,
  "Sulphur": 1577,
  "Tin": 502
}
```

## 🔍 Data Usage

This data is used by the `elite-warboard-missions` module to:

1. **Calculate appearance probabilities**: By comparing planet characteristics (type, atmosphere, volcanism, temperature, gravity, pressure) with histograms
2. **Filter possible species**: Keep only species that match the planet's conditions
3. **Predict variants**: Use surface materials and star types to identify probable variants
4. **Calculate values**: Use the `reward` field to estimate total system value

## 📐 Probability Calculation

The probability calculation uses:
- **Geometric mean** of individual probabilities (body type, atmosphere, volcanism, temperature, gravity, pressure)
- **Global rarity correction**: `(species count) / (total bodies in dataset)`
- **Filtering**: Species with probability < 1% are excluded

## 🔗 Data Source

Data comes from [Canonn Bioforge](https://bioforge.canonn.tech/), a community project that collects and analyzes exobiology discoveries in Elite Dangerous.
