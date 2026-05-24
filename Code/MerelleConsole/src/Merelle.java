import boardifier.control.Logger;
import boardifier.control.StageFactory;
import boardifier.model.GameException;
import boardifier.model.Model;
import boardifier.view.View;
import control.MerelleController;
import control.MerelleDecider;
import model.MerellePawn;
import model.MerelleStageFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Merelle {

    /**
     * Contains the configuration chosen by the players before a game.
     * Grouping these values into an object allows them to be returned from
     * configureGame() and reused for each replay.
     */
    private static class GameConfig {
        int mode;
        int colorJ1;
        int colorJ2;
        int aiDifficulty1 = MerelleDecider.DIFFICULTY_MINIMAX;
        int aiDifficulty2 = MerelleDecider.DIFFICULTY_MINIMAX;
    }

    public static void main(String[] args) {
        Logger.setLevel(Logger.LOGGER_NONE);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("=== Nine Men's Morris ===");
        System.out.println();
        System.out.println("Coordinates: row A-G (top to bottom) + column 1-7 (left to right)");
        System.out.println("Valid positions: A1 A4 A7 | B2 B4 B6 | C3 C4 C5 | D1 D2 D3 D5 D6 D7 | etc.");
        System.out.println("Special commands: \"stop\" = quit the program | \"return\" = forfeit the current game");

        // Register the stage only once, before any replay loop
        StageFactory.registerModelAndView("merelle", "model.MerelleStageModel", "view.MerelleStageView");

        // Mode passed as a command-line argument (optional)
        Integer forcedMode = null;
        if (args.length >= 1) {
            int parsed = parseMode(args[0]);
            if (parsed < 0) {
                System.err.println("Invalid mode: '" + args[0] + "' (must be 0, 1 or 2)");
                System.err.println("0 = Human vs Human | 1 = Human vs AI | 2 = AI vs AI");
                System.exit(1);
            }
            forcedMode = parsed;
        }

        java.util.Random rng = new java.util.Random();
        boolean playAgain = true;

        while (playAgain) {
            System.out.println();

            // Complete configuration for each new game
            GameConfig cfg = configureGame(br, forcedMode);

            MerelleStageFactory.setColors(cfg.colorJ1, cfg.colorJ2);

            // Creates a new model and new players for each game
            Model model = new Model();
            if (cfg.mode == 0) {
                model.addHumanPlayer("Player 1");
                model.addHumanPlayer("Player 2");
            } else if (cfg.mode == 1) {
                model.addHumanPlayer("Player 1");
                model.addComputerPlayer("Computer");
            } else {
                model.addComputerPlayer("AI-1");
                model.addComputerPlayer("AI-2");
            }

            View view = new View(model);
            MerelleController control = new MerelleController(model, view);
            control.setFirstStageName("merelle");

            // Randomly select the starting player
            int startingPlayer = rng.nextInt(2);

            GameException error = startGame(control);
            if (error != null) {
                System.out.println("Unable to start the game: " + error.getMessage());
                System.exit(1);
            }

            model.setIdPlayer(startingPlayer);
            System.out.println();
            System.out.println("Coin toss: "
                    + model.getPlayers().get(startingPlayer).getName()
                    + " goes first!");
            System.out.println();

            control.stageLoop();

            // Ask to play again (except for AI vs AI or EOF)
            playAgain = askPlayAgain(br, cfg.mode);
        }

        System.out.println("Thanks for playing! See you soon.");
    }

    /**
     * Asks the players to configure a new game:
     * game mode, AI difficulty (if applicable), and colors.
     * If forcedMode != null, the mode selection is skipped.
     *
     * @param br         reader for the standard input
     * @param forcedMode mode imposed by CLI argument, or null to prompt for it
     * @return the chosen configuration
     */
    private static GameConfig configureGame(BufferedReader br, Integer forcedMode) {
        GameConfig cfg = new GameConfig();

        // Mode selection
        if (forcedMode != null) {
            cfg.mode = forcedMode;
        } else {
            System.out.println("Choose a game mode:");
            System.out.println("0 = Human vs Human");
            System.out.println("1 = Human vs AI");
            System.out.println("2 = AI vs AI");
            cfg.mode = readIntInRange(br, 0, 2);
        }

        // AI Difficulty
        if (cfg.mode == 1) {
            // Human vs AI: a single difficulty level
            cfg.aiDifficulty1 = chooseDifficulty(br, "the AI");
            cfg.aiDifficulty2 = cfg.aiDifficulty1;
            MerelleDecider.aiDifficultyPerPlayer = null;
            MerelleDecider.aiDifficulty = cfg.aiDifficulty1;
        } else if (cfg.mode == 2) {
            // AI vs AI: independent difficulty for each AI
            System.out.println("--- AI 1 Configuration (Black) ---");
            cfg.aiDifficulty1 = chooseDifficulty(br, "AI 1");
            System.out.println("--- AI 2 Configuration (Red) ---");
            cfg.aiDifficulty2 = chooseDifficulty(br, "AI 2");
            MerelleDecider.aiDifficultyPerPlayer = new int[]{ cfg.aiDifficulty1, cfg.aiDifficulty2 };
        }

        // Colors
        if (cfg.mode == 0) {
            System.out.println("Mode: Human vs Human");
            MerellePawn.printColorMenu();
            cfg.colorJ1 = chooseColor(br, "Player 1, choose your color: ", -1);
            cfg.colorJ2 = chooseColor(br, "Player 2, choose your color: ", cfg.colorJ1);
        } else if (cfg.mode == 1) {
            System.out.println("Mode: Human vs AI");
            MerellePawn.printColorMenu();
            cfg.colorJ1 = chooseColor(br, "Player 1, choose your color: ", -1);
            cfg.colorJ2 = pickColorForIA(cfg.colorJ1);
            System.out.println("The AI will play with the color: " + MerellePawn.getColorName(cfg.colorJ2));
        } else {
            System.out.println("Mode: AI vs AI");
            cfg.colorJ1 = MerellePawn.PAWN_BLACK;
            cfg.colorJ2 = MerellePawn.PAWN_RED;
            System.out.println("AI 1: " + MerellePawn.getColorName(cfg.colorJ1)
                    + " | AI 2: " + MerellePawn.getColorName(cfg.colorJ2));
        }

        return cfg;
    }

    /**
     * Asks if the players want to play another game.
     * In AI vs AI mode, directly returns false (no interaction).
     * In case of EOF, safely returns false.
     */
    private static boolean askPlayAgain(BufferedReader br, int mode) {
        if (mode == 2) return false;
        System.out.println();
        System.out.print("Play again? (y/n) > ");
        String line = readLine(br);
        if (line == null) return false;
        return line.trim().toLowerCase().startsWith("y") || line.trim().toLowerCase().startsWith("o");
    }

    /**
     * Reads an integer between min and max inclusive from the standard input.
     * Loops until a valid input is provided. EOF returns min.
     */
    private static int readIntInRange(BufferedReader br, int min, int max) {
        while (true) {
            System.out.print("> ");
            String line = readLine(br);
            if (line == null) return min; // EOF -> default value
            int val = parseIntOrMinus1(line.trim());
            if (val >= min && val <= max) return val;
            System.out.println("Invalid choice (enter a number between " + min + " and " + max + ").");
        }
    }

    /**
     * Parses the game mode from a string.
     * Returns -1 if invalid or out of the 0-2 range.
     */
    private static int parseMode(String s) {
        if (s == null || s.isEmpty()) return -1;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return -1;
        }
        int val = Integer.parseInt(s);
        if (val < 0 || val > 2) return -1;
        return val;
    }

    /**
     * Starts the game and returns the exception if it fails, null otherwise.
     */
    private static GameException startGame(MerelleController control) {
        try {
            control.startGame();
            return null;
        } catch (GameException e) {
            return e;
        }
    }

    /**
     * Prompts the user to choose a color.
     * Rejects the "forbidden" color (already taken by the other player).
     * In case of EOF, returns a default color.
     */
    private static int chooseColor(BufferedReader br, String prompt, int forbidden) {
        while (true) {
            System.out.print(prompt);
            String line = readLine(br);
            if (line == null) {
                return (forbidden == MerellePawn.PAWN_BLACK) ? MerellePawn.PAWN_RED : MerellePawn.PAWN_BLACK;
            }
            int choice = parseIntOrMinus1(line.trim());
            if (choice == -1) {
                System.out.println("Please enter a valid digit.");
                continue;
            }
            if (!MerellePawn.isValidColor(choice)) {
                System.out.println("Invalid choice (0 to " + (MerellePawn.NB_COLORS - 1) + ").");
                continue;
            }
            if (choice == forbidden) {
                System.out.println("This color is already taken, please choose another one.");
                continue;
            }
            return choice;
        }
    }

    /**
     * Reads a line from the BufferedReader.
     * Returns null in case of error or end of file.
     */
    private static String readLine(BufferedReader br) {
        try {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parses an integer from a string.
     * Returns -1 if invalid.
     */
    private static int parseIntOrMinus1(String s) {
        if (s == null || s.isEmpty()) return -1;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return -1;
        }
        return Integer.parseInt(s);
    }

    /**
     * Automatically picks a color for the AI,
     * ensuring it is different from the human player's color.
     */
    private static int pickColorForIA(int humanColor) {
        for (int i = 0; i < MerellePawn.NB_COLORS; i++) {
            if (i != humanColor) return i;
        }
        return MerellePawn.PAWN_RED;
    }

    /**
     * Displays the AI difficulty menu.
     */
    private static void printDifficultyMenu(String label) {
        System.out.println("Choose the algorithm for " + label + ":");
        System.out.println("1 - MiniMax");
        System.out.println("2 - Alpha-Beta");
        System.out.println("3 - MCTS (Monte Carlo Tree Search)");
    }

    /**
     * Prompts the user to choose the AI difficulty.
     */
    private static int chooseDifficulty(BufferedReader br, String label) {
        printDifficultyMenu(label);
        while (true) {
            System.out.print("> ");
            String line = readLine(br);
            if (line == null) return MerelleDecider.DIFFICULTY_MINIMAX; // EOF -> default
            int val = parseIntOrMinus1(line.trim());
            if (val >= 1 && val <= 3) {
                String[] names = { "", "MiniMax", "Alpha-Beta", "Monte Carlo" };
                System.out.println("Selected algorithm: " + names[val]);
                return val;
            }
            System.out.println("Invalid choice (enter 1, 2 or 3).");
        }
    }
}