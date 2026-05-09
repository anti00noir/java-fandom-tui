package com.fandomtui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main TUI application class that manages screens and navigation.
 * Uses Lanterna for terminal rendering and user input handling.
 */
public class TuiApp {
  private Terminal terminal;
  private Screen screen;
  private MultiWindowTextGUI gui;
  private FandomApiClient apiClient;
  private ArticleViewer articleViewer;

  // UI Components for search screen
  private TextBox searchBox;
  private ActionListBox actionListBox;
  private Label statusLabel;
  private BasicWindow searchWindow;

  private boolean running = true;

  /**
   * Initializes the terminal UI and all components.
   */
  public TuiApp() throws IOException {
    // Initialize terminal with default settings
    DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
    terminal = terminalFactory.createTerminal();
    screen = new TerminalScreen(terminal);
    screen.startScreen();

    // Create GUI
    gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(),
        new EmptySpace(TextColor.ANSI.BLACK));

    // Initialize API client
    apiClient = new FandomApiClient();

    // Initialize article viewer
    articleViewer = new ArticleViewer(apiClient, gui);
  }

  /**
   * Starts the application flow.
   */
  public void start() throws IOException {
    // First, prompt for the wiki URL
    String wikiUrl = promptWikiUrl();
    if (wikiUrl == null) {
      exit();
      return;
    }

    apiClient.setWikiBaseUrl(wikiUrl);

    // Create and show the main search screen
    createSearchScreen();

    // Start the GUI event loop
    gui.addWindow(searchWindow);
    gui.waitForWindowToClose(searchWindow);

    exit();
  }

  /**
   * Prompts the user to enter a Fandom wiki URL.
   */
  private String promptWikiUrl() {
    String url = TextInputDialog.showDialog(
        gui,
        "Fandom Wiki TUI",
        "Enter the Fandom wiki URL or name:",
        "runescape.fandom.com");

    return url;
  }

  /**
   * Creates the main search screen with search bar, results list, and status bar.
   */
  private void createSearchScreen() {
    searchWindow = new BasicWindow("Fandom Wiki Browser - " + apiClient.getWikiBaseUrl());

    // Main panel with border layout
    Panel mainPanel = new Panel(new BorderLayout());

    // Top panel: Search bar
    Panel searchPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
    searchPanel.addComponent(new Label("Search: "));

    searchBox = new TextBox(new TerminalSize(40, 1));
    searchPanel.addComponent(searchBox);

    Button searchButton = new Button("Search", () -> performSearch());
    searchPanel.addComponent(searchButton);

    mainPanel.addComponent(searchPanel, BorderLayout.Location.TOP);

    // Center panel: Results list
    actionListBox = new ActionListBox(new TerminalSize(80, 20));
    mainPanel.addComponent(actionListBox, BorderLayout.Location.CENTER);

    // Bottom panel: Status bar
    statusLabel = new Label("Type a search query and press Enter or click Search | " +
        "Arrow keys to navigate | q: Quit | ESC: Quit | " +
        "Wiki: " + apiClient.getWikiBaseUrl());
    mainPanel.addComponent(statusLabel, BorderLayout.Location.BOTTOM);

    searchWindow.setComponent(mainPanel);

    // Handle keyboard events
    searchWindow.addWindowListener(new WindowListenerAdapter() {
      @Override
      public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
        handleKeyStroke(keyStroke);
      }
    });

    // Set window hints
    searchWindow.setHints(Arrays.asList(
        Window.Hint.FULL_SCREEN,
        Window.Hint.NO_DECORATIONS));
  }

  /**
   * Handles keyboard input for navigation and actions.
   */
  private void handleKeyStroke(KeyStroke keyStroke) {
    if (keyStroke.getKeyType() == KeyType.Character) {
      Character c = keyStroke.getCharacter();

      if (c == 'q' || c == 'Q') {
        searchWindow.close();
      } else if (c == 's' || c == 'S') {
        // Focus the search box
        searchBox.takeFocus();
      } else if (c == 'b' || c == 'B') {
        // Already on search screen, do nothing
      }
    } else if (keyStroke.getKeyType() == KeyType.Escape) {
      searchWindow.close();
    } else if (keyStroke.getKeyType() == KeyType.Enter) {
      // If search box is focused, perform search
      performSearch();
    } else if (keyStroke.getKeyType() == KeyType.ArrowUp) {
      // Move selection up in results list
      actionListBox.handleInput(keyStroke);
    } else if (keyStroke.getKeyType() == KeyType.ArrowDown) {
      // Move selection down in results list
      actionListBox.handleInput(keyStroke);
    }
  }

  /**
   * Performs a search using the API and updates the results list.
   */
  private void performSearch() {
    String query = searchBox.getText().trim();
    if (query.isEmpty()) {
      statusLabel.setText("Please enter a search query");
      return;
    }

    statusLabel.setText("Searching for '" + query + "'...");

    try {
      // Execute search via API
      List<FandomApiClient.SearchResult> results = apiClient.search(query);

      // Clear existing results
      actionListBox.clearItems();

      if (results.isEmpty()) {
        statusLabel.setText("No results found for '" + query + "'");

        // Add a placeholder for empty results
        actionListBox.addItem("No results found", () -> {
        });
      } else {
        statusLabel.setText("Found " + results.size() + " results for '" + query +
            "' | Use arrows to select, Enter to view article");

        // Add results to the list
        for (FandomApiClient.SearchResult result : results) {
          String displayText = result.title();
          if (result.snippet() != null && !result.snippet().isEmpty()) {
            displayText += " - " + result.snippet();
          }

          // Make the display text safe (truncate if too long)
          if (displayText.length() > 100) {
            displayText = displayText.substring(0, 97) + "...";
          }

          final String title = result.title();
          actionListBox.addItem(displayText, () -> {
            // When a result is selected, show the article
            articleViewer.showArticle(title);
          });
        }
      }
    } catch (IOException e) {
      statusLabel.setText("Error: " + e.getMessage());
      showError("Search failed", "Could not complete search: " + e.getMessage());
    }
  }

  /**
   * Shows an error dialog.
   */
  private void showError(String title, String message) {
    MessageDialog.showMessageDialog(gui, title, message);
  }

  /**
   * Cleanly exits the application.
   */
  private void exit() {
    try {
      if (screen != null) {
        screen.stopScreen();
      }
      if (terminal != null) {
        terminal.close();
      }
    } catch (IOException e) {
      System.err.println("Error shutting down: " + e.getMessage());
    }
  }
}
