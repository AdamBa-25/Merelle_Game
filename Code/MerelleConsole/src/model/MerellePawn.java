package model;

import boardifier.model.ElementTypes;
import boardifier.model.GameElement;
import boardifier.model.GameStageModel;
import boardifier.view.ConsoleColor;

/**
 * Represents a pawn in the Nine Men's Morris (Mérelle) game.
 *
 * Available colors (PAWN_* constants):
 * PAWN_BLACK  (0) → black background,   white letter 'B'
 * PAWN_RED    (1) → red background,     black letter 'R'
 * PAWN_BLUE   (2) → blue background,    white letter 'U'
 * PAWN_GREEN  (3) → green background,   black letter 'G'
 * PAWN_YELLOW (4) → yellow background,  black letter 'Y'
 * PAWN_PURPLE (5) → purple background,  white letter 'P'
 * PAWN_CYAN   (6) → cyan background,    black letter 'C'
 *
 */
public class MerellePawn extends GameElement {

    // ===== Color constants =====
    public static final int PAWN_BLACK = 0;
    public static final int PAWN_RED = 1;
    public static final int PAWN_BLUE = 2;
    public static final int PAWN_GREEN = 3;
    public static final int PAWN_YELLOW = 4;
    public static final int PAWN_PURPLE = 5;
    public static final int PAWN_CYAN = 6;

    /** Total number of available colors — useful for validation checks. */
    public static final int NB_COLORS = 7;

    private int color;

    /**
     * @param color One of the PAWN_* constants defined above.
     * @param gameStageModel The stage model owning this element.
     */
    public MerellePawn(int color, GameStageModel gameStageModel)
    {
        super(gameStageModel);
        ElementTypes.register("pawn", 50);
        type = ElementTypes.getType("pawn");
        this.color = color;
    }

    public int getColor() { return color; }

    /**
     * Letter representing this pawn in the console (without color formatting).
     * Used for instance in logs or user input prompt messages.
     */
    public char getSymbol()
    {
        switch (color)
        {
            case PAWN_BLACK: return 'B';
            case PAWN_RED: return 'R';
            case PAWN_BLUE: return 'U';
            case PAWN_GREEN: return 'G';
            case PAWN_YELLOW: return 'Y';
            case PAWN_PURPLE: return 'P';
            case PAWN_CYAN: return 'C';
            default: return '?';
        }
    }

    /**
     * Returns the human-readable name of a color from its ID.
     * Static so it can be called without an instance: MerellePawn.getColorName(colorJ2)
     *
     * @param color the color identifier (PAWN_* constant)
     */
    public static String getColorName(int color)
    {
        switch (color)
        {
            case PAWN_BLACK: return "Black";
            case PAWN_RED: return "Red";
            case PAWN_BLUE: return "Blue";
            case PAWN_GREEN: return "Green";
            case PAWN_YELLOW: return "Yellow";
            case PAWN_PURPLE: return "Purple";
            case PAWN_CYAN: return "Cyan";
            default: return "Unknown";
        }
    }

    /**
     * Returns the ANSI background string for this pawn.
     */
    public String getBackgroundColor()
    {
        switch (color)
        {
            case PAWN_BLACK: return ConsoleColor.BLACK_BACKGROUND;
            case PAWN_RED: return ConsoleColor.RED_BACKGROUND;
            case PAWN_BLUE: return ConsoleColor.BLUE_BACKGROUND;
            case PAWN_GREEN: return ConsoleColor.GREEN_BACKGROUND;
            case PAWN_YELLOW: return ConsoleColor.YELLOW_BACKGROUND;
            case PAWN_PURPLE: return ConsoleColor.PURPLE_BACKGROUND;
            case PAWN_CYAN: return ConsoleColor.CYAN_BACKGROUND;
            default: return ConsoleColor.WHITE_BACKGROUND;
        }
    }

    /**
     * Returns the ANSI text color for this pawn.
     * Dark backgrounds (black, blue, purple) receive white text.
     * Light backgrounds (red, green, yellow, cyan) receive black text.
     */
    public String getTextColor()
    {
        switch (color)
        {
            case PAWN_BLACK:
            case PAWN_BLUE:
            case PAWN_PURPLE:
                return ConsoleColor.WHITE;
            case PAWN_RED:
            case PAWN_GREEN:
            case PAWN_YELLOW:
            case PAWN_CYAN:
            default:
                return ConsoleColor.BLACK;
        }
    }

    /**
     * Checks if an integer corresponds to a valid color.
     *
     * @param colorId the integer entered by the user
     * @return true if colorId is between 0 and NB_COLORS - 1
     */
    public static boolean isValidColor(int colorId)
    {
        return colorId >= 0 && colorId < NB_COLORS;
    }

    /**
     * Prints the list of available colors with their indices in the console.
     * To be called in Merelle.java during the color selection stage.
     */
    public static void printColorMenu()
    {
        System.out.println("Available colors:");
        for (int i = 0; i < NB_COLORS; i++) {
            System.out.println("  " + i + " → " + getColorName(i));
        }
    }
}