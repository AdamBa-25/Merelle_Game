package model;

import boardifier.model.GameStageModel;
import boardifier.model.Model;
import boardifier.model.StageElementsFactory;
import boardifier.model.TextElement;

/**
 * Model for the Nine Men's Morris (Mérelle) stage.
 *
 * Contains the complete state of a game:
 * - the board (MerelleBoard)
 * - the 9 pawns for player 1 and 9 pawns for player 2
 * - the current phase (PLACEMENT or MOVEMENT)
 * - the number of pawns still in hand (not placed during phase 1)
 * - the millJustFormed flag: a capture is pending
 * - the history of the last moves for the draw rule
 * - a TextElement displaying the current player's name
 */
public class MerelleStageModel extends GameStageModel {

    /** Phase 1: placing the pawns */
    public static final int PHASE_PLACEMENT   = 0;
    /** Phase 2: moving the pawns */
    public static final int PHASE_MOVEMENT = 1;

    /**
     * History size: we store 4 snapshots to be able to compare
     * 2 positions of the SAME player (indices 0 and 2, or 1 and 3 — separated by a turn each).
     */
    private static final int DRAW_HISTORY_SIZE = 4;

    // --- Game elements ---
    private MerelleBoard board;
    private MerellePawn[] pawnsJ1; // 9 pawns for player 0
    private MerellePawn[] pawnsJ2; // 9 pawns for player 1
    private TextElement playerName; // text displayed above the board

    // --- Game state ---
    private int currentPhase;
    private int pawnsInHandJ1; // player 0 pawns not yet placed
    private int pawnsInHandJ2; // player 1 pawns not yet placed
    private boolean millJustFormed; // true if a capture is pending
    private String[] lastMoves;     // history of the last moves

    /**
     * Last mill formed by each player, encoded as a sorted "pos1-pos2-pos3" string.
     * Stored until the player forms ANOTHER mill (never cleared otherwise).
     * Rule: a player cannot reform their last mill until they have
     * formed another one in the meantime.
     * Index 0 = player 0, index 1 = player 1.
     */
    private String[] lastMillByPlayer;

    /**
     * Constructor called by StageFactory via reflection.
     *
     * @param name  stage name (must match the name registered in StageFactory)
     * @param model global model
     */
    public MerelleStageModel(String name, Model model) {
        super(name, model);
        currentPhase   = PHASE_PLACEMENT;
        pawnsInHandJ1  = 9;
        pawnsInHandJ2  = 9;
        millJustFormed = false;
        lastMoves      = new String[DRAW_HISTORY_SIZE];
        lastMillByPlayer = new String[2]; // null by default (no mill recorded)
    }

    // ===== Getters / Setters =====

    public MerelleBoard getBoard() { return board; }
    public void setBoard(MerelleBoard board) {
        this.board = board;
        addContainer(board);
    }

    public MerellePawn[] getPawnsJ1() { return pawnsJ1; }

    /**
     * Returns the color of player 0.
     * Fallback value if the pawns are not yet initialized (prevents an NPE).
     */
    public int getColorJ1() {
        if (pawnsJ1 == null || pawnsJ1.length == 0) return MerellePawn.PAWN_BLACK;
        return pawnsJ1[0].getColor();
    }

    public void setPawnsJ1(MerellePawn[] pawns) {
        this.pawnsJ1 = pawns;
        for (MerellePawn p : pawns) addElement(p);
    }

    public MerellePawn[] getPawnsJ2() { return pawnsJ2; }

    /**
     * Returns the color of player 1.
     * Fallback value if the pawns are not yet initialized (prevents an NPE).
     */
    public int getColorJ2() {
        if (pawnsJ2 == null || pawnsJ2.length == 0) return MerellePawn.PAWN_RED;
        return pawnsJ2[0].getColor();
    }
    public void setPawnsJ2(MerellePawn[] pawns) {
        this.pawnsJ2 = pawns;
        for (MerellePawn p : pawns) addElement(p);
    }

    public TextElement getPlayerName() { return playerName; }
    public void setPlayerName(TextElement playerName) {
        this.playerName = playerName;
        addElement(playerName);
    }

    public int getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(int phase) { this.currentPhase = phase; }

    public boolean isMillJustFormed() { return millJustFormed; }
    public void setMillJustFormed(boolean formed) { this.millJustFormed = formed; }

    /** Returns true if all pawns of both players have been placed. */
    public boolean allPawnsPlaced() {
        return pawnsInHandJ1 == 0 && pawnsInHandJ2 == 0;
    }

    /** Number of pawns still in hand for the given player. */
    public int getPawnsInHand(int playerId) {
        return (playerId == 0) ? pawnsInHandJ1 : pawnsInHandJ2;
    }

    /** Decrements the pawns in hand counter after a placement. */
    public void decreasePawnsInHand(int playerId) {
        if (playerId == 0 && pawnsInHandJ1 > 0) pawnsInHandJ1--;
        else if (playerId == 1 && pawnsInHandJ2 > 0) pawnsInHandJ2--;
    }

    /**
     * Checks if all pawns are placed and transitions to the moving phase.
     * @return true if the transition has just occurred
     */
    public boolean checkAndTransitionToMovePhase() {
        if (currentPhase == PHASE_PLACEMENT && allPawnsPlaced()) {
            currentPhase = PHASE_MOVEMENT;
            return true;
        }
        return false;
    }

    /**
     * Returns a copy of the last moves history.
     * Used by the AI decider to penalize repetitions.
     */
    public String[] getLastMoves() {
        return lastMoves.clone();
    }

    /**
     * Records the last move played for the draw by repetition rule.
     * The playerId is prefixed to the string so that snapshots from two different
     * players are never considered identical.
     *
     * @param playerId index of the current player (0 or 1)
     * @param move     move description (e.g., "place:4", "9->10", "capture:2")
     */
    public void recordMove(int playerId, String move) {
        String snapshot = playerId + ":" + move;
        for (int i = DRAW_HISTORY_SIZE - 1; i > 0; i--) {
            lastMoves[i] = lastMoves[i - 1];
        }
        lastMoves[0] = snapshot;
    }

    /**
     * Returns true if both players have repeated the exact same move twice each
     * (draw by repetition over 4 moves: lastMoves[0]==lastMoves[2] AND lastMoves[1]==lastMoves[3]).
     *
     * With an alternating history P0/P1/P0/P1:
     *   index 0 = last move P0
     *   index 1 = last move P1
     *   index 2 = second-to-last move P0
     *   index 3 = second-to-last move P1
     * If [0]==[2] AND [1]==[3], both players have played the same move twice in a row.
     */
    public boolean isDrawByRepetition() {
        if (lastMoves[0] == null || lastMoves[1] == null
                || lastMoves[2] == null || lastMoves[3] == null) return false;
        return lastMoves[0].equals(lastMoves[2]) && lastMoves[1].equals(lastMoves[3]);
    }

    // ===== Management of the "same mill forbidden two turns in a row" rule =====

    /**
     * Encodes a mill into a sorted "p1-p2-p3" key string.
     */
    private String millKey(int[] millPositions) {
        int[] s = millPositions.clone();
        if (s[0] > s[1]) { int t = s[0]; s[0] = s[1]; s[1] = t; }
        if (s[1] > s[2]) { int t = s[1]; s[1] = s[2]; s[2] = t; }
        if (s[0] > s[1]) { int t = s[0]; s[0] = s[1]; s[1] = t; }
        return s[0] + "-" + s[1] + "-" + s[2];
    }

    /**
     * Records the last mill formed by this player.
     * Overwrites the previous one: only the LAST mill is remembered.
     * Memory is never cleared otherwise — the player must form
     * another mill before they can reform this one.
     */
    public void recordLastMill(int playerId, int[] millPositions) {
        lastMillByPlayer[playerId] = millKey(millPositions);
    }

    /**
     * Does nothing — kept for backward compatibility, but the memory of the last
     * mill must NEVER be cleared by a move that does not form a mill.
     */
    public void clearLastMill(int playerId) {
        // Intentionally empty.
        // A move without a mill does NOT reset the memory: the player
        // must form ANOTHER mill before being able to reform the previous one.
    }

    /**
     * Returns true if the given mill is identical to the last recorded mill
     * for this player → reformation is forbidden until another mill is formed.
     */
    public boolean isSameMillAsLast(int playerId, int[] millPositions) {
        if (lastMillByPlayer[playerId] == null) return false;
        return millKey(millPositions).equals(lastMillByPlayer[playerId]);
    }

    @Override
    public StageElementsFactory getDefaultElementFactory() {
        return new MerelleStageFactory(this);
    }
}