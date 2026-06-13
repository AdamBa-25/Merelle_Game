package model;

import control.MerelleDecider;

/**
 * Configuration shared across all screens.
 */
public class GameConfig {
    public int mode = 0;
    public int colorJ1 = MerellePawn.PAWN_BLACK;
    public int colorJ2 = MerellePawn.PAWN_RED;
    public int aiDifficulty1 = MerelleDecider.DIFFICULTY_MINIMAX;
    public int aiDifficulty2 = MerelleDecider.DIFFICULTY_MINIMAX;

    /** Player names */
    public String nameJ1 = "Joueur 1";
    public String nameJ2 = "Joueur 2";

    /** Board theme — always 0 (Wood) */
    public int boardTheme = 0;

    /** Index of the first player (0 or 1), -1 = random */
    public int firstPlayer = -1;

    /** Dark mode — affects menus only, not the board. */
    public static boolean darkMode = false;

    /** Hint mode: shows possible moves — one flag per human player. */
    public boolean helpEnabledJ1 = false;
    public boolean helpEnabledJ2 = false;

    /** Win counter (persists for the duration of the session). */
    public static int winsJ1 = 0;
    public static int winsJ2 = 0;

    /** Last names used (to display them in the menu) */
    public static String lastNameJ1 = "";
    public static String lastNameJ2 = "";

    /** AI algorithm name based on the difficulty */
    public static String aiName(int difficulty) {
        switch (difficulty) {
            case MerelleDecider.DIFFICULTY_ALPHABETA:  return "Alpha-Beta";
            case MerelleDecider.DIFFICULTY_MONTECARLO: return "Monte Carlo";
            default:                                    return "MiniMax";
        }
    }
}
