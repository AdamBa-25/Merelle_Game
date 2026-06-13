package view.javafx.board;

import javafx.scene.paint.Color;

/**
 * Defines a visual theme for the Nine Men's Morris board.
 * Each theme specifies colors for: background, wood/stone surface,
 * lines, empty nodes, node stroke, and the panel/root background.
 */
public class BoardTheme {

    public final String name;
    public final String displayName;
    public final String emoji;

    // Board surface (inner rectangle)
    public final Color boardFill;
    public final Color boardStroke;

    // Lines on the board
    public final Color lineColor;
    public final double lineWidth;

    // Empty node fill / stroke
    public final Color emptyNodeFill;
    public final Color emptyNodeStroke;

    // Root background gradient (CSS string for -fx-background-color)
    public final String rootBackground;

    // Panel background (info + reserve)
    public final Color panelFill;
    public final Color panelStroke;

    public BoardTheme(String name, String displayName, String emoji, Color boardFill, Color boardStroke, Color lineColor, double lineWidth, Color emptyNodeFill, Color emptyNodeStroke, String rootBackground, Color panelFill, Color panelStroke) {
        this.name = name;
        this.displayName = displayName;
        this.emoji = emoji;
        this.boardFill = boardFill;
        this.boardStroke = boardStroke;
        this.lineColor = lineColor;
        this.lineWidth = lineWidth;
        this.emptyNodeFill = emptyNodeFill;
        this.emptyNodeStroke = emptyNodeStroke;
        this.rootBackground = rootBackground;
        this.panelFill = panelFill;
        this.panelStroke = panelStroke;
    }

    public static final BoardTheme[] ALL = {
        // 0 – Classic Wood
        new BoardTheme(
            "wood", "Classic Wood", "",
            Color.web("#C68642"), Color.web("#7B4A1A"),
            Color.web("#5C2E00"), 3.5,
            Color.web("#F5DEB3"), Color.web("#7B4A1A"),
            "linear-gradient(to bottom, #1a0f05, #3b2008)",
            Color.web("#3b2008CC"), Color.web("#8B6914")
        ),
        // 1 – Night
        new BoardTheme(
            "night", "Mystic Night", "",
            Color.web("#0D1B3E"), Color.web("#1E3A8A"),
            Color.web("#4A90D9"), 3.0,
            Color.web("#1A3A6E"), Color.web("#4A90D9"),
            "linear-gradient(to bottom, #020917, #0D1B3E)",
            Color.web("#0A1628CC"), Color.web("#4A90D9")
        ),
    };
}
