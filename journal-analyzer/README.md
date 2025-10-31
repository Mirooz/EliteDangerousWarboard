# Elite Dangerous Journal Analyzer

## Description

**Elite Dangerous Journal Analyzer** is a standalone utility designed to analyze Elite Dangerous journal files. It provides a comprehensive interface to explore all journal events with filtering capabilities and JSON visualization.

## Features

### 📊 **Event Analysis**
- **Complete Event List**: View all events from journal files
- **Event Filtering**: Filter by event type with checkboxes
- **JSON Visualization**: See complete JSON for each event
- **Expandable Events**: Expand/collapse individual events
- **Bulk Operations**: Select all/deselect all event types

### 🔍 **File Selection**
- **Journal File Support**: Load any `.log` journal file
- **Default Directory**: Automatically opens Elite Dangerous journal folder
- **Real-time Loading**: Asynchronous file processing

### 🎨 **Elite Dangerous Theme**
- **Consistent Styling**: Orange/cyan color scheme
- **Professional Interface**: Clean, modern design
- **Responsive Layout**: Three-panel layout

## Installation

### **Prerequisites**
- Java 17 or higher
- Maven 3.6 or higher

### **Build and Run**
```bash
# Clone or download the project
cd journal-analyzer

# Build the project
mvn clean compile

# Run the application
mvn exec:java

# Or use the batch file (Windows)
run.bat
```

### **Create Standalone JAR**
```bash
# Create executable JAR with all dependencies
mvn clean package

# Run the standalone JAR
java -jar target/journal-analyzer.jar
```

## Usage

### **Basic Workflow**
1. **Launch Application**: Run the application using Maven or the batch file
2. **Select Journal File**: Click "Select Journal File" and choose a `.log` file
3. **Filter Events**: Use checkboxes to show/hide specific event types
4. **Explore Events**: Click on events to view their JSON content
5. **Expand Events**: Use ▼/▲ buttons to expand/collapse individual events

### **Interface Layout**
- **Left Panel**: Event type filters with checkboxes
- **Center Panel**: List of journal events
- **Right Panel**: JSON viewer for selected events

### **Controls**
- **Select All**: Show all event types
- **Deselect All**: Hide all event types
- **Expand All**: Expand all events
- **Collapse All**: Collapse all events

## Technical Details

### **Technologies Used**
- **JavaFX 17**: Modern user interface
- **Jackson**: JSON parsing and formatting
- **Maven**: Project management and build
- **Maven Shade Plugin**: Standalone JAR creation

### **Project Structure**
```
journal-analyzer/
├── src/main/java/be/mirooz/elitedangerous/journal/
│   ├── JournalAnalyzerApp.java          # Main application class
│   ├── JournalAnalyzerController.java    # UI controller
│   └── JournalEventItem.java            # Data model
├── src/main/resources/
│   ├── fxml/journal-analyzer.fxml       # UI layout
│   ├── css/elite-theme.css              # Styling
│   └── images/elite-icon.png            # Application icon
├── pom.xml                              # Maven configuration
├── run.bat                              # Windows launcher
└── README.md                            # This file
```

### **Dependencies**
- JavaFX Controls (17.0.1)
- JavaFX FXML (17.0.1)
- Jackson Databind (2.15.2)
- Jackson Core (2.15.2)
- Jackson Annotations (2.15.2)

## Features in Detail

### **Event Filtering**
- **Checkbox Interface**: Easy selection/deselection of event types
- **Real-time Filtering**: Instant updates when changing selections
- **Bulk Operations**: Select all or deselect all with one click

### **Event Display**
- **Event Type**: Bold orange text showing the event name
- **Timestamp**: Gray text showing when the event occurred
- **Expandable Content**: Click ▼/▲ to show/hide JSON content
- **JSON Formatting**: Pretty-printed JSON with syntax highlighting

### **File Processing**
- **Asynchronous Loading**: Non-blocking file processing
- **Progress Indication**: Status updates during loading
- **Error Handling**: Graceful handling of malformed JSON lines

## Use Cases

### **Development**
- **Debug Journal Events**: Understand what events are generated
- **Event Analysis**: Study event structure and content
- **Testing**: Verify journal file integrity

### **Research**
- **Event Statistics**: Count different types of events
- **Pattern Analysis**: Identify recurring event patterns
- **Data Mining**: Extract specific information from journals

### **Troubleshooting**
- **Issue Diagnosis**: Find problematic events
- **Event Validation**: Verify event structure
- **Log Analysis**: Understand game behavior

## Contributing

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Support

If you encounter issues:

1. Ensure Java 17+ is installed
2. Verify Maven is properly configured
3. Check that journal files are valid JSON format
4. Open a GitHub issue with problem details

## Changelog

### Version 1.0.0
- ✅ Initial release
- ✅ Journal file analysis
- ✅ Event filtering system
- ✅ JSON visualization
- ✅ Elite Dangerous theme
- ✅ Standalone JAR support



