package control;

import boardifier.control.ActionFactory;
import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.model.Model;
import boardifier.model.Player;
import boardifier.model.action.ActionList;
import boardifier.view.View;
import model.MerelleBoard;
import model.MerellePawn;
import model.MerelleStageModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Main controller for the Nine Men's Morris game.
 *
 * Manages the game loop, keyboard (or file) input, move validation,
 * and rule enforcement. Inherits from Controller like HoleController.
 *
 * Game command syntax:
 *  - Phase 1 (placement) : "A1"    → places a pawn at row A, column 1
 *  - Phase 2 (movement)  : "A1 B1" → moves from A1 to B1
 *  - Capture after mill  : "XA1"   → captures the opponent's pawn at A1
 *  - Immediate stop      : any input containing "stop"
 *
 * Coordinate system: letter A-G (row, from top to bottom),
 *                    digit 1-7 (column, from left to right).
 *
 * Modeled after HoleController.java from the HoleConsole tutorial.
 */
public class MerelleController extends Controller {

    /**
     * Exception thrown when the player types "return".
     * Interrupts the current game without quitting the program,
     * to fall back to the "Play again?" menu.
     */
    public static class ReturnToMenuException extends RuntimeException {
        public ReturnToMenuException() { super("Retour au menu demande."); }
    }

    private BufferedReader consoleIn;

    public MerelleController(Model model, View view) {
        super(model, view);
    }

    /**
     * Main loop of a game.
     * Displays, reads moves, and alternates players until the end of the game.
     */
    @Override
    public void stageLoop() {
        consoleIn = new BufferedReader(new InputStreamReader(System.in));
        // First display
        update();
        try {
            while (!model.isEndStage()) {
                playTurn();
                endOfTurn();
                update();
            }
            endGame();
        } catch (ReturnToMenuException e) {
            // The player typed "return": we abandon the game without displaying the end screen
            System.out.println();
            System.out.println("Game abandoned.");
        }
    }

    /**
     * Plays a turn for the current player (human or AI).
     */
    private void playTurn() {
        Player p = model.getCurrentPlayer();
        MerelleStageModel stageModel = (MerelleStageModel) model.getGameStage();

        if (p.getType() == Player.COMPUTER) {
            // First check if someone typed "stop" in the terminal
            try {
                if (consoleIn.ready()) {
                    String line = consoleIn.readLine();
                    if (line != null) {
                        String cmd = line.trim().toLowerCase();
                        if (cmd.contains("stop")) {
                            System.out.println("Stop requested. Exiting game.");
                            System.exit(0);
                        }
                        if (cmd.contains("return")) {
                            throw new ReturnToMenuException();
                        }
                    }
                }
            } catch (IOException ignored) {}

            // AI Player: request decision from the decider
            System.out.println(p.getName() + " (AI) is thinking...");
            MerelleDecider decider = new MerelleDecider(model, this);
            String decision = decider.getDecision(stageModel, model.getIdPlayer());
            System.out.println(p.getName() + " plays: " + decision);
            boolean ok = analyseAndPlay(decision);
            if (!ok) {
                // Should not happen with a correct AI
                System.out.println("AI ERROR: invalid move generated (" + decision + ")");
            }
        } else {
            // Human player: loop until a valid move is entered
            boolean ok = false;
            while (!ok) {
                System.out.print(p.getName() + " > ");
                try {
                    String line = consoleIn.readLine();
                    // End of file (EOF) → clean exit
                    if (line == null) { System.exit(0); }
                    line = line.trim();
                    // Immediate stop if "stop" detected
                    if (line.toLowerCase().contains("stop")) {
                        System.out.println("Stop requested. Exiting game.");
                        System.exit(0);
                    }
                    // Return to menu if "return" detected
                    if (line.toLowerCase().contains("return")) {
                        throw new ReturnToMenuException();
                    }
                    ok = analyseAndPlay(line);
                    if (!ok) {
                        System.out.println("Invalid command, try again!");
                    }
                } catch (IOException e) {
                    System.err.println("Read error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Switches to the next player and updates the displayed name.
     */
    @Override
    public void endOfTurn() {
        MerelleStageModel stageModel = (MerelleStageModel) model.getGameStage();

        // If a capture is pending, the current player plays again to capture
        if (stageModel.isMillJustFormed()) return;

        model.setNextPlayer();
        stageModel.getPlayerName().setText(model.getCurrentPlayerName());

        // Check transition from phase 1 → phase 2
        if (stageModel.checkAndTransitionToMovePhase()) {
            System.out.println(">>> All pawns have been placed! Beginning the movement phase. <<<");
        }

        // Check end conditions after changing the player
        checkEndConditions(stageModel);
    }

    /**
     * Analyzes input and executes the move if valid.
     *
     * Three cases:
     *  1. Capture (starts with 'X'): removes an opponent's pawn
     *  2. Phase 1 – one coordinate: places a pawn
     *  3. Phase 2 – two coordinates separated by space: moves a pawn
     *
     * Displays a precise error message in case of invalid input.
     *
     * @param input  raw player input
     * @return true if the move is valid and was executed
     */
    public boolean analyseAndPlay(String input) {
        if (input == null || input.isEmpty()) {
            System.out.println("ERROR [SYNTAX]: empty input.");
            return false;
        }

        MerelleStageModel stageModel = (MerelleStageModel) model.getGameStage();
        MerelleBoard board = stageModel.getBoard();
        int playerId = model.getIdPlayer();
        // playerColor is the actual color of the pawn (constant MerellePawn.PAWN_*)
        // It can differ from playerId if players choose non-standard colors.
        int playerColor  = (playerId == 0) ? stageModel.getColorJ1() : stageModel.getColorJ2();
        int oppColor     = (playerId == 0) ? stageModel.getColorJ2() : stageModel.getColorJ1();

        // ============================================================
        // CASE 1: Mandatory capture after forming a mill
        // ============================================================
        if (stageModel.isMillJustFormed()) {
            if (!input.toUpperCase().startsWith("X")) {
                System.out.println("ERROR [RULES]: you must capture an opponent's pawn!");
                System.out.println("  Format: X followed by the coordinate (e.g., XA1)");
                return false;
            }
            String coord = input.substring(1).trim();
            int pos = parseCoord(coord);
            if (pos < 0) {
                System.out.println("ERROR [SYNTAX]: invalid coordinate '" + coord + "' (e.g., XA1, XD4).");
                return false;
            }
            MerellePawn target = board.getPawnAt(pos);
            if (target == null) {
                System.out.println("ERROR [RULES]: no pawn to capture at " + coord.toUpperCase() + ".");
                return false;
            }
            if (target.getColor() == playerColor) {
                System.out.println("ERROR [RULES]: you cannot capture your own pawn!");
                return false;
            }
            // A pawn in a mill is protected, UNLESS all opponent pawns are in mills
            boolean allInMill = board.allPawnsInMills(oppColor);
            if (board.isInMill(pos, oppColor) && !allInMill) {
                System.out.println("ERROR [RULES]: this pawn is protected by a mill!");
                System.out.println("  Choose a pawn outside of a mill.");
                return false;
            }
            // Informative message if capture within a mill is forced (exceptional case)
            if (allInMill) {
                System.out.println("  (Info) All opponent pawns are in mills: you can capture any of them.");
            }
            // Execute the capture via ActionFactory
            // generateRemoveFromStage: removes the pawn from both the container AND the stage → disappears from the board
            ActionList actions = ActionFactory.generateRemoveFromStage(model, target);
            ActionPlayer play = new ActionPlayer(model, this, actions);
            play.start();
            stageModel.setMillJustFormed(false);
            System.out.println("  → " + model.getCurrentPlayerName() + " captures the pawn at " + coord.toUpperCase() + "!");
            // Check if the opponent lost
            checkEndConditions(stageModel);
            return true;
        }

        // ============================================================
        // CASE 2: Placement phase (single coordinate)
        // ============================================================
        if (stageModel.getCurrentPhase() == MerelleStageModel.PHASE_PLACEMENT) {
            if (input.contains(" ")) {
                System.out.println("ERROR [SYNTAX]: during the placement phase, enter a single coordinate (e.g., A1).");
                return false;
            }
            int pos = parseCoord(input);
            if (pos < 0) {
                System.out.println("ERROR [SYNTAX]: invalid coordinate '" + input + "' (letter A-G + number 1-7, e.g., A1).");
                return false;
            }
            if (!board.isFreeAt(pos)) {
                System.out.println("ERROR [RULES]: slot " + input.toUpperCase() + " is already occupied!");
                return false;
            }
            if (stageModel.getPawnsInHand(playerId) <= 0) {
                System.out.println("ERROR [RULES]: you have no more pawns left to place!");
                return false;
            }

            // Retrieve the first pawn still out of the board
            MerellePawn pawn = getNextPawnInHand(stageModel, playerId);
            if (pawn == null) {
                System.out.println("INTERNAL ERROR: no pawn available.");
                return false;
            }

            // Make the pawn visible before placing it (it was invisible in reserve)
            pawn.setVisible(true);

            // Place the pawn via ActionFactory
            ActionList actions = ActionFactory.generatePutInContainer(
                    model, pawn, "merelleboard",
                    MerelleBoard.POS_TO_GRID[pos][0],
                    MerelleBoard.POS_TO_GRID[pos][1]);
            ActionPlayer play = new ActionPlayer(model, this, actions);
            play.start();

            stageModel.decreasePawnsInHand(playerId);
            stageModel.recordMove(playerId, "place:" + pos);

            // Display remaining pawns in hand for both players
            int remaining0 = stageModel.getPawnsInHand(0);
            int remaining1 = stageModel.getPawnsInHand(1);
            System.out.println("  → " + model.getCurrentPlayerName()
                    + " places a pawn at " + input.toUpperCase()
                    + " | Pawns in hand: "
                    + model.getPlayers().get(0).getName() + " " + remaining0
                    + "  /  "
                    + model.getPlayers().get(1).getName() + " " + remaining1);

            // Detect mill formation
            if (board.isInMill(pos, playerColor)) {
                // Memorize this mill for the "same mill forbidden 2 turns in a row" rule.
                // NOTE: we do NOT check isSameMillAsLast() here because in the placement phase
                // we place pawns without moving them — it is therefore impossible to "break"
                // an existing mill to reform it. The PDF rule only applies to
                // the movement phase (case 3 below). We still memorize the
                // mill so that the rule becomes active from the very first turn of phase 2.
                int[] mill = board.getMillContaining(pos, playerColor);
                if (mill != null) stageModel.recordLastMill(playerId, mill);
                stageModel.setMillJustFormed(true);
                System.out.println("  >>> MILL formed! Capture an opponent's pawn (e.g., XA1) <<<");
            } else {
                // No mill: clear memory of the previous mill
                stageModel.clearLastMill(playerId);
            }
            return true;
        }

        // ============================================================
        // CASE 3: Movement phase (two coordinates)
        // ============================================================
        String[] parts = input.trim().split("\\s+");
        if (parts.length != 2) {
            System.out.println("ERROR [SYNTAX]: during the movement phase, enter source and destination coordinates (e.g., A1 B1).");
            return false;
        }
        int src  = parseCoord(parts[0]);
        int dest = parseCoord(parts[1]);
        if (src < 0) {
            System.out.println("ERROR [SYNTAX]: invalid source coordinate '" + parts[0] + "'.");
            return false;
        }
        if (dest < 0) {
            System.out.println("ERROR [SYNTAX]: invalid destination coordinate '" + parts[1] + "'.");
            return false;
        }
        MerellePawn pawn = board.getPawnAt(src);
        if (pawn == null) {
            System.out.println("ERROR [RULES]: no pawn found at " + parts[0].toUpperCase() + ".");
            return false;
        }
        if (pawn.getColor() != playerColor) {
            System.out.println("ERROR [RULES]: this pawn does not belong to you!");
            return false;
        }
        if (!board.isFreeAt(dest)) {
            System.out.println("ERROR [RULES]: slot " + parts[1].toUpperCase() + " is occupied!");
            return false;
        }
        if (!board.isAdjacent(src, dest)) {
            System.out.println("ERROR [RULES]: " + parts[0].toUpperCase() + " and " + parts[1].toUpperCase()
                    + " are not adjacent on the board!");
            return false;
        }

        // Rule: a player cannot break a mill and reform it on the next move.
        // Simulate the move on a copy to detect the resulting mill,
        // BEFORE applying anything to the real board.
        if (wouldReformSameMill(board, src, dest, playerColor, playerId, stageModel)) {
            System.out.println("ERROR [RULES]: you cannot reform the exact same mill two turns in a row!");
            System.out.println("  Move another pawn or choose a different destination.");
            return false;
        }

        // Move the pawn via ActionFactory
        ActionList actions = ActionFactory.generateMoveWithinContainer(
                model, pawn,
                MerelleBoard.POS_TO_GRID[dest][0],
                MerelleBoard.POS_TO_GRID[dest][1]);
        ActionPlayer play = new ActionPlayer(model, this, actions);
        play.start();

        stageModel.recordMove(playerId, src + "->" + dest);
        System.out.println("  → " + model.getCurrentPlayerName()
                + " moves " + parts[0].toUpperCase() + " → " + parts[1].toUpperCase());

        // Detect mill formation after the move
        if (board.isInMill(dest, playerColor)) {
            int[] mill = board.getMillContaining(dest, playerColor);
            if (mill != null) stageModel.recordLastMill(playerId, mill);
            stageModel.setMillJustFormed(true);
            System.out.println("  >>> MILL formed! Capture an opponent's pawn (e.g., XA1) <<<");
        } else {
            stageModel.clearLastMill(playerId);
        }
        return true;
    }

    /**
     * Checks endgame conditions and applies them to the model.
     * Called after each move and after each player switch.
     *
     * Victory conditions (phase 2 only):
     *  - A player has fewer than 3 pawns on the board
     *  - A player is blocked (no legal moves available)
     *
     * Draw condition:
     *  - Repetition of the identical last 3 moves
     */
    private void checkEndConditions(MerelleStageModel stageModel) {
        // No checks during phase 1 or if a capture is pending
        if (stageModel.getCurrentPhase() == MerelleStageModel.PHASE_PLACEMENT) return;
        if (stageModel.isMillJustFormed()) return;

        MerelleBoard board = stageModel.getBoard();
        // The actual colors of the two players (can differ from 0/1)
        int[] colors = { stageModel.getColorJ1(), stageModel.getColorJ2() };

        for (int pid = 0; pid < 2; pid++) {
            int color = colors[pid];
            int count = board.countPawns(color);
            // Defeat: fewer than 3 pawns
            if (count < 3) {
                System.out.println(model.getPlayers().get(pid).getName()
                        + " only has " + count + " pawn(s) left!");
                model.setIdWinner(1 - pid);
                model.stopStage();
                return;
            }
            // Defeat: blocked
            if (board.isBlocked(color)) {
                System.out.println(model.getPlayers().get(pid).getName()
                        + " is blocked, no legal moves available!");
                model.setIdWinner(1 - pid);
                model.stopStage();
                return;
            }
        }
        // Draw by repetition
        if (stageModel.isDrawByRepetition()) {
            System.out.println("Draw game due to repetition of moves!");
            model.setIdWinner(-1);
            model.stopStage();
        }
    }

    /**
     * Displays the result of the game.
     */
    @Override
    public void endGame() {
        System.out.println();
        System.out.println("=== GAME OVER ===");
        if (model.getIdWinner() >= 0) {
            System.out.println(">>> " + model.getPlayers().get(model.getIdWinner()).getName() + " wins! <<<");
        } else {
            System.out.println(">>> Tie game! <<<");
        }
    }

    /**
     * Converts a text coordinate into a logical position (0-23).
     * Format: letter A-G (row) + digit 1-7 (column), e.g., "A1", "d4".
     *
     * @param coord the string to analyze
     * @return logical position (0-23), or -1 if invalid
     */
    public static int parseCoord(String coord) {
        if (coord == null || coord.length() < 2) return -1;
        coord = coord.toUpperCase().trim();
        char rowChar = coord.charAt(0);
        if (rowChar < 'A' || rowChar > 'G') return -1;
        int row = rowChar - 'A';
        int col;
        try {
            col = Integer.parseInt(coord.substring(1)) - 1; // "1"→0, "7"→6
        } catch (NumberFormatException e) { return -1; }
        if (col < 0 || col > 6) return -1;
        // Search for the logical position matching (row, col)
        for (int pos = 0; pos < 24; pos++) {
            if (MerelleBoard.POS_TO_GRID[pos][0] == row
                    && MerelleBoard.POS_TO_GRID[pos][1] == col)
                return pos;
        }
        return -1; // grid cell but not a valid board position
    }

    /**
     * Returns the first pawn in hand (not yet placed on the board)
     * for the given player. A pawn "in hand" has no container.
     */
    private MerellePawn getNextPawnInHand(MerelleStageModel stageModel, int playerId) {
        MerellePawn[] pawns = (playerId == 0)
                ? stageModel.getPawnsJ1()
                : stageModel.getPawnsJ2();
        for (MerellePawn pawn : pawns)
            if (pawn.getContainer() == null) return pawn;
        return null;
    }

    /**
     * Checks if moving src→dest would reform a mill identical to the last
     * memorized mill for this player (rule: same mill forbidden 2 turns in a row).
     * The check is done WITHOUT modifying the actual board.
     *
     * Made public static so it can be reused by FXGameController (e.g. for hint filtering).
     *
     * @param board      current board
     * @param src        source position
     * @param dest       destination position
     * @param color      player color
     * @param playerId   player index (0 or 1)
     * @param stageModel current stage model
     * @return true if the move would reform the same mill
     */
    public static boolean wouldReformSameMill(MerelleBoard board, int src, int dest, int color, int playerId, MerelleStageModel stageModel) {
        // The mill history is permanent: any mill already formed
        // by this player (at any time in the game) is forbidden from being reformed.
        // We look to see if the move src→dest would form a mill already in history.
        for (int[] mill : MerelleBoard.MILLS) {
            // The mill must contain dest
            boolean containsDest = false;
            for (int p : mill) if (p == dest) { containsDest = true; break; }
            if (!containsDest) continue;

            // Check if all 3 slots would be occupied by this player after src→dest
            boolean wouldForm = true;
            for (int p : mill) {
                if (p == dest) continue; // will be occupied by the moved pawn
                if (p == src)  { wouldForm = false; break; } // src will be empty afterwards
                MerellePawn pw = board.getPawnAt(p);
                if (pw == null || pw.getColor() != color) { wouldForm = false; break; }
            }
            if (!wouldForm) continue;

            // This mill would form: is it already in the history?
            if (stageModel.isSameMillAsLast(playerId, mill)) return true;
        }
        return false;
    }
}