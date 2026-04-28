package com.fandomtui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple wikitext parser that strips most markup for terminal display.
 * Handles common MediaWiki formatting elements.
 */
public class WikitextParser {
    
    // Patterns for different markup elements
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[(?:[^|\\]]+?\\|)?([^\\]]+?)\\]\\]");
    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile("\\[https?://[^\\s\\]]+(?:\\s+([^\\]]+))?\\]");
    private static final Pattern BOLD_ITALIC_PATTERN = Pattern.compile("'''''(.+?)'''''");
    private static final Pattern BOLD_PATTERN = Pattern.compile("'''(.+?)'''");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("''(.+?)''");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern REF_PATTERN = Pattern.compile("<ref[^>]*>.*?</ref>|<ref[^>]*/>");
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{[^}]+\\}\\}");
    private static final Pattern HEADING_PATTERN = Pattern.compile("={2,}(.+?)={2,}");
    private static final Pattern LIST_PATTERN = Pattern.compile("^[*#]+", Pattern.MULTILINE);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    
    /**
     * Parses wikitext into plain text with minimal formatting.
     * Uses ANSI escape codes for bold and italic in terminal.
     */
    public static String parseToPlainText(String wikitext) {
        if (wikitext == null || wikitext.isEmpty()) {
            return "";
        }
        
        String text = wikitext;
        
        // Remove comments first
        text = COMMENT_PATTERN.matcher(text).replaceAll("");
        
        // Remove reference tags and their content
        text = REF_PATTERN.matcher(text).replaceAll("");
        
        // Remove templates (simplified - won't handle nested templates well)
        text = TEMPLATE_PATTERN.matcher(text).replaceAll("");
        
        // Convert headings - add newlines and ANSI bold
        text = HEADING_PATTERN.matcher(text).replaceAll(match -> {
            String title = match.group(1).trim();
            return "\n\u001B[1m" + title + "\u001B[0m\n";
        });
        
        // Handle bold+italic
        text = BOLD_ITALIC_PATTERN.matcher(text).replaceAll(match -> 
            "\u001B[1;3m" + match.group(1) + "\u001B[0m");
        
        // Handle bold
        text = BOLD_PATTERN.matcher(text).replaceAll(match -> 
            "\u001B[1m" + match.group(1) + "\u001B[0m");
        
        // Handle italic
        text = ITALIC_PATTERN.matcher(text).replaceAll(match -> 
            "\u001B[3m" + match.group(1) + "\u001B[0m");
        
        // Handle internal wiki links - keep only display text
        text = LINK_PATTERN.matcher(text).replaceAll(match -> {
            return match.group(1);
        });
        
        // Handle external links - keep display text or URL
        text = EXTERNAL_LINK_PATTERN.matcher(text).replaceAll(match -> {
            String displayText = match.group(1);
            return displayText != null ? displayText : "[External Link]";
        });
        
        // Remove remaining HTML tags
        text = HTML_TAG_PATTERN.matcher(text).replaceAll("");
        
        // Handle list markers - convert to simple indented text
        text = LIST_PATTERN.matcher(text).replaceAll(match -> {
            String marker = match.group();
            return "  ".repeat(marker.length());
        });
        
        // Clean up extra whitespace
        text = text.replaceAll("\\n{3,}", "\n\n");  // Reduce multiple blank lines
        text = text.replaceAll("^\\s+\\n", "");      // Remove leading whitespace
        
        return text.trim();
    }
    
    /**
     * Extracts the first meaningful paragraph(s) from wikitext.
     * Useful for showing a preview or intro.
     */
    public static String extractIntro(String wikitext, int maxLines) {
        String plainText = parseToPlainText(wikitext);
        String[] lines = plainText.split("\\n");
        StringBuilder intro = new StringBuilder();
        
        int count = 0;
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("=")) {
                intro.append(line).append("\n");
                count++;
                if (count >= maxLines) break;
            }
        }
        
        return intro.toString().trim();
    }
}