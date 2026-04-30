package be.mirooz.elitedangerous.dashboard.tools;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Interface graphique simple pour simuler des événements de journal.
 * <p>
 * Permet de choisir un type d'événement, saisir des options {@code key=value},
 * puis d'écrire dans le dernier fichier {@code Journal.*.log}.
 */
public final class ExplorationJournalEventSimulatorGui {

    private static final List<String> EVENTS = List.of(
            "fsdjump",
            "scan",
            "scanorganic",
            "docked",
            "fssbodysignals",
            "saasignalsfound"
    );

    private final ExplorationJournalEventSimulatorTool tool = new ExplorationJournalEventSimulatorTool();
    private final JTextField journalDirField = new JTextField();
    private final JComboBox<String> eventCombo = new JComboBox<>(EVENTS.toArray(String[]::new));
    private final JTextArea optionsArea = new JTextArea(8, 90);
    private final JTextArea outputArea = new JTextArea(12, 90);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExplorationJournalEventSimulatorGui().show());
    }

    private void show() {
        JFrame frame = new JFrame("Elite Journal Event Simulator");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        main.add(buildJournalDirRow(frame));
        main.add(buildEventRow());
        main.add(buildOptionsPanel());
        main.add(buildActionsRow());
        main.add(buildOutputPanel());

        frame.add(main, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel buildJournalDirRow(JFrame frame) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        row.add(new JLabel("Dossier journal"), BorderLayout.WEST);

        Path detected = detectDefaultJournalDir();
        journalDirField.setText(detected == null ? "" : detected.toString());
        row.add(journalDirField, BorderLayout.CENTER);

        JButton browse = new JButton("Parcourir...");
        browse.addActionListener(unused -> {
            JFileChooser chooser = new JFileChooser(journalDirField.getText().trim());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                journalDirField.setText(chooser.getSelectedFile().toPath().toString());
            }
        });
        row.add(browse, BorderLayout.EAST);
        return row;
    }

    private JPanel buildEventRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        row.add(new JLabel("Événement"));
        eventCombo.setSelectedItem("scan");
        row.add(eventCombo);
        return row;
    }

    private JPanel buildOptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(new JLabel("Options (une ligne par option: key=value)"), BorderLayout.NORTH);
        optionsArea.setText("""
                selected-body-id=12
                signal-type=biological
                signal-count=3
                """);
        panel.add(new JScrollPane(optionsArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JButton simulateButton = new JButton("Simuler et ajouter");
        simulateButton.addActionListener(unused -> onSimulate());
        row.add(simulateButton);

        JButton clearOutputButton = new JButton("Vider le log");
        clearOutputButton.addActionListener(unused -> outputArea.setText(""));
        row.add(clearOutputButton);
        return row;
    }

    private JPanel buildOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(new JLabel("Sortie"), BorderLayout.NORTH);
        outputArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setPreferredSize(new Dimension(900, 260));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void onSimulate() {
        try {
            String event = String.valueOf(eventCombo.getSelectedItem()).toLowerCase(Locale.ROOT);
            String dirText = journalDirField.getText() == null ? "" : journalDirField.getText().trim();
            if (dirText.isBlank()) {
                throw new IllegalArgumentException("Le dossier journal est obligatoire.");
            }
            Path journalDir = Path.of(dirText);
            Map<String, String> options = parseOptions(optionsArea.getText());

            Path file = tool.simulateAndAppend(event, journalDir, options);
            String lastLine = readLastJsonLine(file);
            log("[" + LocalDateTime.now() + "] OK " + event + " -> " + file.getFileName());
            if (lastLine != null) {
                log(lastLine);
            }
            log("");
        } catch (Exception e) {
            log("ERREUR: " + e.getMessage());
            JOptionPane.showMessageDialog(
                    null,
                    e.getMessage(),
                    "Simulation impossible",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static Map<String, String> parseOptions(String text) {
        Map<String, String> options = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return options;
        }
        String[] lines = text.split("\\R");
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("--")) {
                line = line.substring(2);
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                throw new IllegalArgumentException("Option invalide: " + raw + " (attendu: key=value)");
            }
            String key = line.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(idx + 1).trim();
            if (key.isBlank()) {
                throw new IllegalArgumentException("Option invalide: clé vide");
            }
            options.put(key, value);
        }
        return options;
    }

    private static Path detectDefaultJournalDir() {
        List<Path> candidates = List.of(
                Path.of("elite-journal-simulator/src/main/resources/exemple"),
                Path.of("src/main/resources/exemple"),
                Path.of(System.getProperty("user.home"), "Saved Games", "Frontier Developments", "Elite Dangerous")
        );
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return Path.of("").toAbsolutePath().normalize();
    }

    private static String readLastJsonLine(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);
                if (line != null && !line.isBlank()) {
                    return line;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private void log(String text) {
        outputArea.append(text);
        outputArea.append(System.lineSeparator());
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
}
