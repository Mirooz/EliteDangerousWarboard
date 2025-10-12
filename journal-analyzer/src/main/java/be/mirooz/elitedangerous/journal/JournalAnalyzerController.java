package be.mirooz.elitedangerous.journal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Controller for the Journal Analyzer utility
 */
public class JournalAnalyzerController implements Initializable {

    @FXML private Button selectJournalButton;
    @FXML private Label statusLabel;
    @FXML private ListView<String> eventTypeFilterList;
    @FXML private ListView<JournalEventItem> eventsList;
    @FXML private TextArea jsonViewer;
    @FXML private VBox mainContainer;

    private ObservableList<JournalEventItem> allEvents;
    private FilteredList<JournalEventItem> filteredEvents;
    private Set<String> visibleEventTypes;
    private ObjectMapper objectMapper;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        objectMapper = new ObjectMapper();
        visibleEventTypes = new HashSet<>();
        
        setupEventTypeFilter();
        setupEventsList();
        setupJsonViewer();
    }

    private void setupEventTypeFilter() {
        eventTypeFilterList.setCellFactory(CheckBoxListCell.forListView(item -> {
            BooleanProperty visible = new SimpleBooleanProperty(visibleEventTypes.contains(item));
            visible.addListener((obs, wasVisible, isNowVisible) -> {
                if (isNowVisible) {
                    visibleEventTypes.add(item);
                } else {
                    visibleEventTypes.remove(item);
                }
                updateFilteredEvents();
            });
            return visible;
        }));
    }

    private void setupEventsList() {
        allEvents = FXCollections.observableArrayList();
        filteredEvents = new FilteredList<>(allEvents);
        
        eventsList.setItems(filteredEvents);
        eventsList.setCellFactory(listView -> new SimpleEventCell());
        
        // Handle selection to show JSON
        eventsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                jsonViewer.setText(newVal.getJsonContent());
            }
        });
    }

    private void setupJsonViewer() {
        jsonViewer.setEditable(false);
        jsonViewer.setWrapText(true);
    }

    @FXML
    private void selectJournalFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Journal File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Journal Files", "*.log")
        );
        
        // Set default directory to Elite Dangerous journal folder
        String userHome = System.getProperty("user.home");
        Path journalPath = Paths.get(userHome, "Saved Games", "Frontier Developments", "Elite Dangerous");
        if (Files.exists(journalPath)) {
            fileChooser.setInitialDirectory(journalPath.toFile());
        }
        
        Stage stage = (Stage) selectJournalButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            loadJournalFile(selectedFile);
        }
    }

    private void loadJournalFile(File file) {
        Platform.runLater(() -> {
            statusLabel.setText("Loading: " + file.getName());
        });
        
        new Thread(() -> {
            try {
                List<JournalEventItem> events = new ArrayList<>();
                Set<String> eventTypes = new HashSet<>();
                
                try (Stream<String> lines = Files.lines(file.toPath())) {
                    lines.forEach(line -> {
                        if (line.trim().startsWith("{")) {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(line);
                                String eventType = jsonNode.has("event") ? jsonNode.get("event").asText() : "Unknown";
                                String timestamp = jsonNode.has("timestamp") ? jsonNode.get("timestamp").asText() : "";
                                
                                events.add(new JournalEventItem(eventType, timestamp, jsonNode.toPrettyString()));
                                eventTypes.add(eventType);
                            } catch (Exception e) {
                                System.err.println("Error parsing line: " + line);
                            }
                        }
                    });
                }
                
                Platform.runLater(() -> {
                    allEvents.clear();
                    allEvents.addAll(events);
                    
                    // Update event type filter
                    ObservableList<String> eventTypeList = FXCollections.observableArrayList(eventTypes);
                    eventTypeList.sort(String::compareTo);
                    eventTypeFilterList.setItems(eventTypeList);
                    
                    // Make all event types visible by default
                    visibleEventTypes.addAll(eventTypes);
                    
                    // Update status
                    statusLabel.setText(String.format("%d events loaded (%d different types)", 
                        events.size(), eventTypes.size()));
                    
                    updateFilteredEvents();
                });
                
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void updateFilteredEvents() {
        filteredEvents.setPredicate(event -> visibleEventTypes.contains(event.getEventType()));
    }

    @FXML
    private void selectAllEventTypes() {
        for (String eventType : eventTypeFilterList.getItems()) {
            visibleEventTypes.add(eventType);
        }
        eventTypeFilterList.refresh();
        updateFilteredEvents();
    }

    @FXML
    private void deselectAllEventTypes() {
        visibleEventTypes.clear();
        eventTypeFilterList.refresh();
        updateFilteredEvents();
    }


    /**
     * Simple cell for displaying journal events (event name + date)
     */
    private static class SimpleEventCell extends ListCell<JournalEventItem> {
        private final HBox container;
        private final Label eventTypeLabel;
        private final Label timestampLabel;

        public SimpleEventCell() {
            container = new HBox();
            container.setSpacing(10);
            container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            eventTypeLabel = new Label();
            eventTypeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #FF6B00; -fx-min-width: 150;");
            
            timestampLabel = new Label();
            timestampLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #CCCCCC; -fx-min-width: 200;");
            
            container.getChildren().addAll(eventTypeLabel, timestampLabel);
        }

        @Override
        protected void updateItem(JournalEventItem item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
            } else {
                eventTypeLabel.setText(item.getEventType());
                timestampLabel.setText(item.getTimestamp());
                setGraphic(container);
            }
        }
    }
}
