package com.fandomtui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

/**
 * Displays article content in a scrollable text view.
 * Provides keyboard navigation for viewing article content.
 */
public class ArticleViewer {
    private final FandomApiClient apiClient;
    private final MultiWindowTextGUI gui;
    private Window currentWindow;
    
    public ArticleViewer(FandomApiClient apiClient, MultiWindowTextGUI gui) {
        this.apiClient = apiClient;
        this.gui = gui;
    }
    
    /**
     * Shows the article content for a given page title.
     * Creates a scrollable window with the parsed article content.
     */
    public void showArticle(String pageTitle) {
        try {
            // Fetch article content from API
            FandomApiClient.ArticleContent content = apiClient.getArticleContent(pageTitle);
            
            // Parse wikitext to plain text
            String parsedText = WikitextParser.parseToPlainText(content.wikitext());
            
            // Split into lines for the text box
            List<String> lines = Arrays.asList(parsedText.split("\n", -1));
            
            // Create the article window
            BasicWindow articleWindow = createArticleWindow(content.title(), lines);
            
            // Remove current window and add the article window
            if (currentWindow != null) {
                gui.removeWindow(currentWindow);
            }
            currentWindow = articleWindow;
            gui.addWindow(articleWindow);
            
        } catch (IOException e) {
            showError("Failed to load article: " + e.getMessage());
        }
    }
    
    /**
     * Creates a window displaying article content with scroll capabilities.
     */
    private BasicWindow createArticleWindow(String title, List<String> contentLines) {
        BasicWindow window = new BasicWindow("Article: " + title);
        
        Panel mainPanel = new Panel(new BorderLayout());
        
        // Create text box for article content
        TextBox textBox = new TextBox(
                new TerminalSize(80, 20),
                TextBox.Style.MULTI_LINE
        );
        textBox.setReadOnly(true);
        
        // Add content line by line
        for (String line : contentLines) {
            textBox.addLine(line);
        }
        
        // Status bar with controls
        Panel statusPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        statusPanel.addComponent(new Label(" ↑↓: Scroll | q: Back to search | ESC: Quit | " + 
                                          "Wiki: " + apiClient.getWikiBaseUrl()));
        
        mainPanel.addComponent(textBox, BorderLayout.Location.CENTER);
        mainPanel.addComponent(statusPanel, BorderLayout.Location.BOTTOM);
        
        window.setComponent(mainPanel);
        
        // Handle keyboard input for scrolling and navigation
        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                if (keyStroke.getKeyType() == KeyType.Character) {
                    Character c = keyStroke.getCharacter();
                    
                    if (c == 'q' || c == 'Q') {
                        // Go back to search - close this window
                        gui.removeWindow(window);
                        currentWindow = null;
                    } else if (c == 'b' || c == 'B') {
                        // Go back to search
                        gui.removeWindow(window);
                        currentWindow = null;
                    }
                } else if (keyStroke.getKeyType() == KeyType.Escape) {
                    // Quit application
                    System.exit(0);
                }
                // Arrow keys are handled automatically by TextBox for scrolling
            }
        });
        
        // Set window to fill screen
        window.setHints(Arrays.asList(Window.Hint.FULL_SCREEN));
        
        return window;
    }
    
    /**
     * Shows an error dialog to the user.
     */
    private void showError(String message) {
        MessageDialog.showMessageDialog(gui, "Error", message);
    }
    
    /**
     * Cleans up resources.
     */
    public void close() {
        if (currentWindow != null) {
            gui.removeWindow(currentWindow);
            currentWindow = null;
        }
    }
}