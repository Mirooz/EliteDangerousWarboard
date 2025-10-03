package be.mirooz.elitedangerous.dashboard.ui.manager;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class CopyClipboardManager {
    private static CopyClipboardManager instance;

    private CopyClipboardManager() {}

    public static CopyClipboardManager getInstance() {
        if (instance == null) {
            instance = new CopyClipboardManager();
        }
        return instance;
    }
    public void copyToClipboard(String value) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        clipboard.setContent(content);
    }
}
