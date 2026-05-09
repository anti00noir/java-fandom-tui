# Fandom TUI

A terminal-based user interface for browsing Fandom wikis, built with Java and Lanterna.

## Description

Fandom TUI is a command-line application that allows you to search and read articles from any Fandom wiki directly in your terminal. It provides a keyboard-driven interface for quick, distraction-free access to wiki content without needing a web browser.

## Features

- Connect to any Fandom wiki by URL or domain name
- Search for articles using the MediaWiki API
- Read article content with basic formatting (bold, italic, headings)
- Full keyboard navigation (arrows, Enter, shortcuts)
- Scrollable article view
- Error handling for network issues and invalid pages

## Requirements

- Java 17 or later
- Maven 3.6 or later

## Installation

1. Clone the repository:
   `git clone git@github.com:anti00noir/java-fandom-tui.git`

2. Build with Maven:
   `mvn clean package`
3. Run the application:
  `java -jar target/fandom-tui-1.0-SNAPSHOT.jar`




## Usage

1. Launch the application
2. Enter a Fandom wiki URL when prompted (e.g., `runescape.fandom.com`)
3. Type a search query and press Enter
4. Navigate results with arrow keys
5. Press Enter on a result to view the article
6. Use arrow keys to scroll through the article

### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| Arrow Up/Down | Navigate search results or scroll article |
| Enter | Select highlighted result |
| s | Focus search bar |
| q | Go back to search from article view |
| b | Go back to search from article view |
| Esc | Quit application |

## How It Works

The application uses the standard MediaWiki API at `/api.php` to interact with Fandom wikis. It makes two types of API calls:

- **Search**: Uses `action=opensearch` to find articles matching a query
- **Content**: Uses `action=parse` to retrieve the raw wikitext of a page

A simple wikitext parser strips markup and converts formatting to ANSI escape codes for terminal display.

## Project Structure

src/main/java/com/fandomtui/
Main.java - Application entry point
TuiApp.java - Terminal UI logic and screen management
FandomApiClient.java - MediaWiki API communication
WikitextParser.java - Wikitext to plain text conversion
ArticleViewer.java - Article content display window

## Dependencies

- Lanterna 3.2.5 - Terminal UI framework
- OkHttp 4.11.0 - HTTP client
- Jackson 2.15.2 - JSON parsing

## Limitations

- Basic wikitext parsing only (templates are stripped, not rendered)
- No support for images or tables(*yet*)
- Single page viewing at a time
- Network requests are synchronous

## License

MIT
