# Elite Dangerous Warboard

## Description

**Elite Dangerous Warboard** is a specialized dashboard designed to optimize the Elite Dangerous gaming experience, specifically focused on massacre and conflict missions. The application analyzes game journal files in real-time to provide a comprehensive overview of your missions, combat statistics, and mission opportunities.

![exempledashboard.png](elite-warboard-missions%2Fsrc%2Fmain%2Fresources%2Fimages%2Fexempledashboard.png)

## 🎯 Main Features

### 📊 **Massacre Mission Dashboard**
- **Mission stacking** : Visualize all your active massacre missions with real-time progression
- **Conflict missions** : Track civil war and conflict missions
- **Smart filtering** : Filter by type (Massacre/Conflict) and status (Active/Completed/Failed) with styled ComboBoxes
- **Wing missions** : Automatic detection of wing missions with distinctive icon
- **Visual progression** : Progress bars to track your kills toward objectives

### 🔍 **Combat System Search**
- **Massacre search** : Find systems conducive to massacre missions via EdTools API
- **Conflict search** : Locate civil war and conflict zones via Inara API
- **Advanced filters** : Maximum distance, minimum sources, large pads only
- **Faction filtering** : Filter results by Federation, Empire, Alliance, or Independent
- **Bilingual interface** : English and French with explanatory descriptions

### 💰 **Credits and Bounty Management**
- **Pending credits** : Track credits from completed but unredeemed missions
- **Potential credits** : Estimate total earnings from active missions
- **Bounty journal** : History of destroyed ships with bounties and timestamps
- **Automatic reset** : Automatic reset when redeeming bounties

### 🚀 **Advanced Features**
- **New commander detection** : Automatic popup and journal re-reading when switching commanders
- **Real-time reading** : Automatic monitoring of new journal files
- **Elite Dangerous interface** : Visual theme consistent with the game universe
- **Network error handling** : Translated error popups in case of connection issues
[seachsystem](elite-warboard-missions%2Fsrc%2Fmain%2Fresources%2Fimages%2Fseachsystem)
## 🛠️ Installation

### **Simple Installation (Recommended)**
1. Download `EliteWarboard-Setup.exe` from releases
2. Run the installer
3. Launch the application from Start Menu or desktop

**✅ No external downloads required** : JDK and JavaFX are embedded in the installer. No Java installation or additional dependencies needed.

### **Configuration**
1. On first launch, configure your Elite Dangerous journal folder
2. Default location: `C:/Users/[YourName]/Saved Games/Frontier Developments/Elite Dangerous`
3. Select your language (English/French)
4. The application automatically starts analyzing your journals

## 🎮 Usage

### **Main Interface**
- **Header** : Overview of active missions, credits and statistics
- **Left panel** : Destroyed ships journal and bounties
- **Center panel** : Mission list with ComboBox filters
- **Right panel** : Detailed faction statistics
- **Footer** : Commander information and current system

### **System Search**
1. Click "SEARCH COMBAT SYSTEMS" in the header
2. Choose "MASSACRE" or "CONFLICT" tab
3. Configure your search criteria
4. Click "SEARCH" to get results
5. Click on a system to copy it to clipboard

### **Mission Filtering**
- **Type** : All, Massacre, Conflict
- **Status** : All, Active, Completed, Failed
- **Faction** : Click on a row in statistics to filter


![seachsystem.png](elite-warboard-missions%2Fsrc%2Fmain%2Fresources%2Fimages%2Fseachsystem.png)
## 🌐 Language Support

The application is available in **English and French** with complete interface translation including:
- All menus and buttons
- Error messages and notifications
- System search descriptions
- Mission types and statuses

## 🔧 Technologies Used

- **JavaFX 17** : Modern user interface
- **Maven** : Project management and automated build
- **Jackson** : JSON journal file analysis
- **Lombok** : Boilerplate code reduction
- **EdTools API** : Massacre system search
- **Inara API** : Conflict zone search
- **jpackage** : Installer creation with embedded runtime

## 📁 Project Structure

```
elite-warboard-missions/
├── src/main/java/be/mirooz/elitedangerous/dashboard/
│   ├── controller/          # JavaFX controllers
│   ├── handlers/            # Journal event handlers
│   ├── model/               # Data models and enums
│   ├── service/             # Business services (EdTools, Inara, Journal)
│   └── ui/                  # Interface components
├── src/main/resources/
│   ├── css/                 # Elite Dangerous theme
│   ├── fxml/               # User interfaces
│   ├── images/             # Icons and images
│   └── messages_*.properties # Translations
├── executable/             # Final build with embedded runtime
└── installer.iss           # Inno Setup installation script
```

## 📋 Supported Journal Events

The application automatically processes these Elite Dangerous events:

- `MissionAccepted` : Adds a new mission
- `MissionCompleted` : Marks a mission as completed
- `MissionFailed` : Marks a mission as failed
- `MissionRedirected` : Marks massacre missions as completed
- `Bounty` : Updates kill counters and bounties
- `RedeemVoucher` : Resets bounty statistics
- `Commander` : Detects commander changes
- `LoadGame` : Updates ship information
- `Location` : Updates current position
- `Docked`/`Undocked` : Tracks docking status

## 🌐 External APIs

- **EdTools PvE** : Massacre system search
- **Inara** : Conflict zone and civil war search
- **Error handling** : Translated network error popups

## 🎨 User Interface

- **Elite Dangerous theme** : Characteristic orange/cyan colors
- **Styled ComboBoxes** : Consistent filter styling
- **Contextual popups** : Error messages and notifications
- **Responsive** : Adaptive interface with optimized padding and spacing
- **Bilingual** : Complete English/French support

## 🚀 Build and Development

### **Local Build**
```bash
mvn clean install
```

### **Installer Creation**
```bash
mvn clean install
# Automatically generates EliteWarboard-Setup.exe with embedded runtime
```

### **Development**
```bash
mvn exec:java
```

## 📝 Changelog

### Version 1.0.0
- ✅ Automatic new commander detection
- ✅ Notification popup with automatic journal re-reading
- ✅ Immediate reading of new journal files
- ✅ Network error handling with translated popups
- ✅ Styled ComboBoxes for mission filters
- ✅ Explanatory descriptions for search tabs
- ✅ Complete conflict mission support
- ✅ Bilingual English/French interface
- ✅ Real-time massacre mission dashboard
- ✅ System search via EdTools and Inara
- ✅ Destroyed ships journal
- ✅ Faction bounty statistics
- ✅ Interactive mission filtering
- ✅ Elite Dangerous user interface
- ✅ Wing mission support
- ✅ Automatic bounty reset

## 🤝 Contributing

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## 🆘 Support

If you encounter issues:

1. Verify your Elite Dangerous journal folder is correctly configured
2. Ensure Elite Dangerous is generating journal files
3. Check your internet connection for system searches
4. Open a GitHub issue with your problem details

## 🎯 Project Goal

**Elite Dangerous Warboard** is specifically designed to optimize the experience for players focused on massacre and conflict missions. It enables you to:

- **Efficiently stack** massacre missions to maximize earnings
- **Quickly find** the best systems for combat missions
- **Track in real-time** your mission progression
- **Optimize** your credit and reputation farming strategy

The application is completely self-contained and requires no external dependency installation.