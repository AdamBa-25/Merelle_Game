package control;

import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.model.Model;
import boardifier.model.action.ActionList;
import model.MerelleBoard;
import model.MerellePawn;
import model.MerelleStageModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MerelleDecider extends Decider {

    /** Difficulty constants */
    public static final int DIFFICULTY_MINIMAX    = 1;
    public static final int DIFFICULTY_ALPHABETA  = 2;
    public static final int DIFFICULTY_MONTECARLO = 3;

    /** Search depth for MiniMax and Alpha-Beta. */
    private static final int MINIMAX_DEPTH   = 4;
    private static final int ALPHABETA_DEPTH = 6;

    /** Number of simulations per move for Monte Carlo. */
    private static final int MCTS_SIMULATIONS = 80;

    /** Active difficulty, to be set before starting the game. */
    public static int aiDifficulty = DIFFICULTY_MINIMAX;

    /**
     * Difficulties per player (index 0 and 1).
     * If null, aiDifficulty is used as the common value.
     */
    public static int[] aiDifficultyPerPlayer = null;

    private static final Random random = new Random();

    /** Constructor */
    public MerelleDecider(Model model, Controller control) {
        super(model, control);
    }

    @Override
    public ActionList decide() {
        return new ActionList();
    }

    /**
     * Computes and returns the AI's decision as a string,
     * using the same format as human keyboard input.
     * Delegates to the chosen strategy via aiDifficulty.
     *
     * @param stageModel the current stage model
     * @param playerId   AI player index (0 or 1)
     * @return the simulated input: "A1" (placement), "A1 B2" (movement), "XA1" (capture)
     */
    public String getDecision(MerelleStageModel stageModel, int playerId) {
        int diff = (aiDifficultyPerPlayer != null && playerId >= 0 && playerId < aiDifficultyPerPlayer.length)
                ? aiDifficultyPerPlayer[playerId]
                : aiDifficulty;
        switch (diff) {
            case DIFFICULTY_ALPHABETA:  return getDecisionAlphaBeta(stageModel, playerId);
            case DIFFICULTY_MONTECARLO: return getDecisionMonteCarlo(stageModel, playerId);
            default:                    return getDecisionMinimax(stageModel, playerId);
        }
    }

    // ================================================================
    // STRATEGY 1: MINIMAX
    // ================================================================

    /**
     * MiniMax entry point.
     * Handles the 3 situations: capture, placement (phase 1), movement (phase 2).
     * For each possible move, calls minimax() and keeps the best score.
     *
     * @param stageModel the current stage model
     * @param playerId   AI player index (0 or 1)
     * @return the best input string found by MiniMax
     */
    private String getDecisionMinimax(MerelleStageModel stageModel, int playerId) {
        MerelleBoard board = stageModel.getBoard();
        int phase = stageModel.getCurrentPhase();

        int colorAI  = (playerId == 0) ? stageModel.getColorJ1() : stageModel.getColorJ2();
        int colorOpp = (playerId == 0) ? stageModel.getColorJ2() : stageModel.getColorJ1();

        int[] snap = boardSnapshot(board, colorAI, colorOpp);

        // Last move played by THIS player (2 entries back in the shared history,
        // since both players alternate).
        // Format: "src->dest", e.g., "9->10"
        String lastOwnMove = getLastOwnMove(stageModel, playerId);

        // --- SPECIAL CASE: capture ---
        if (stageModel.isMillJustFormed()) {
            List<Integer> captures = allCapturesSnap(snap, colorOpp);
            int bestScore = Integer.MIN_VALUE;
            List<Integer> bestCaptures = new ArrayList<>();
            for (int pos : captures) {
                int[] next = snapCopy(snap);
                next[pos] = -1;
                int score = minimax(next, MINIMAX_DEPTH - 1, false, colorAI, colorOpp, phase);
                if (score > bestScore) {
                    bestScore = score;
                    bestCaptures.clear();
                    bestCaptures.add(pos);
                } else if (score == bestScore) {
                    bestCaptures.add(pos);
                }
            }
            int bestPos = bestCaptures.get(random.nextInt(bestCaptures.size()));
            return "X" + posToCoord(bestPos);
        }

        // --- PHASE 1: placement ---
        if (phase == MerelleStageModel.PHASE_PLACEMENT) {
            List<Integer> placements = allPlacementsSnap(snap);
            int bestScore = Integer.MIN_VALUE;
            List<Integer> bestPlacements = new ArrayList<>();
            for (int pos : placements) {
                int[] next = snapCopy(snap);
                next[pos] = colorAI;
                int score = minimax(next, MINIMAX_DEPTH - 1, false, colorAI, colorOpp, phase);
                if (score > bestScore) {
                    bestScore = score;
                    bestPlacements.clear();
                    bestPlacements.add(pos);
                } else if (score == bestScore) {
                    bestPlacements.add(pos);
                }
            }
            int bestPos = bestPlacements.get(random.nextInt(bestPlacements.size()));
            return posToCoord(bestPos);
        }

        // --- PHASE 2: movement ---
        List<int[]> moves = allMovesSnap(snap, colorAI);
        int bestScore = Integer.MIN_VALUE;
        List<int[]> bestMoves = new ArrayList<>();
        for (int[] mv : moves) {
            int[] next = snapCopy(snap);
            next[mv[1]] = next[mv[0]];
            next[mv[0]] = -1;
            int score = minimax(next, MINIMAX_DEPTH - 1, false, colorAI, colorOpp, phase);

            // Penalizes the reverse move of THIS player's last move (ping-pong).
            String moveStr = mv[0] + "->" + mv[1];
            if (isPingPong(lastOwnMove, moveStr)) {
                score -= 500;
            }

            // Heavily penalizes reforming the exact same mill as the last one formed.
            // The AI will only be allowed to reform it after making a different one.
            if (wouldReformLastMillSnap(snap, mv[0], mv[1], colorAI, playerId, stageModel)) {
                score -= 100000;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(mv);
            } else if (score == bestScore) {
                bestMoves.add(mv);
            }
        }
        int[] bestMove = bestMoves.get(random.nextInt(bestMoves.size()));
        return posToCoord(bestMove[0]) + " " + posToCoord(bestMove[1]);
    }

    /**
     * Returns the last move played by THIS player (not the opponent).
     * In an alternating history [opp, me, opp], the current player's move
     * is at index 1 (2 moves back).
     * Returns null if not played yet.
     */
    private String getLastOwnMove(MerelleStageModel stageModel, int playerId) {
        String[] history = stageModel.getLastMoves();
        // Since the model update, each entry is prefixed by the player:
        // "0:9->10" or "1:9->10". We search for the last entry belonging to THIS player.
        String prefix = playerId + ":";
        for (String entry : history) {
            if (entry != null && entry.startsWith(prefix)) {
                // We remove the "0:" or "1:" prefix to only keep the raw move
                return entry.substring(prefix.length());
            }
        }
        return null;
    }

    /**
     * Returns true if moveStr is the exact reverse of lastMove.
     * E.g.: lastMove = "9->10", moveStr = "10->9" → true (ping-pong).
     * lastMove must be without player prefix (already extracted by getLastOwnMove).
     */
    private boolean isPingPong(String lastMove, String moveStr) {
        if (lastMove == null || !lastMove.contains("->")) return false;
        String[] parts = lastMove.split("->");
        if (parts.length != 2) return false;
        String inverse = parts[1] + "->" + parts[0];
        return inverse.equals(moveStr);
    }

    /**
     * Recursive MiniMax algorithm working on an int[24] snapshot.
     * Each cell is: colorAI, colorOpp, or -1 (free).
     * No boardifier objects are mutated → no events, no stack overflow.
     *
     * @param snap         copy of the board state (int[24])
     * @param depth        remaining depth
     * @param isMaximizing true = AI's turn, false = opponent's turn
     * @param colorAI      AI color constant
     * @param colorOpp     opponent color constant
     * @param phase        current phase
     * @return state score
     */
    private int minimax(int[] snap, int depth, boolean isMaximizing,
                        int colorAI, int colorOpp, int phase) {

        if (depth == 0 || isTerminalSnap(snap, colorAI, colorOpp, phase)) {
            return evaluateSnap(snap, colorAI, colorOpp, phase);
        }

        int currentColor  = isMaximizing ? colorAI  : colorOpp;
        int opponentColor = isMaximizing ? colorOpp : colorAI;

        if (isMaximizing) {
            int best = Integer.MIN_VALUE;
            if (phase == MerelleStageModel.PHASE_PLACEMENT) {
                for (int pos : allPlacementsSnap(snap)) {
                    int[] next = snapCopy(snap);
                    next[pos] = currentColor;
                    // If this placement forms a mill, simulate every possible capture
                    if (formsMillSnap(next, pos, currentColor)) {
                        List<Integer> captures = allCapturesSnap(next, opponentColor);
                        for (int capPos : captures) {
                            int[] afterCap = snapCopy(next);
                            afterCap[capPos] = -1;
                            best = Math.max(best, minimax(afterCap, depth - 1, false, colorAI, colorOpp, phase));
                        }
                    } else {
                        best = Math.max(best, minimax(next, depth - 1, false, colorAI, colorOpp, phase));
                    }
                }
            } else {
                for (int[] mv : allMovesSnap(snap, currentColor)) {
                    int[] next = snapCopy(snap);
                    next[mv[1]] = next[mv[0]];
                    next[mv[0]] = -1;
                    // If this move forms a mill, simulate every possible capture
                    if (formsMillSnap(next, mv[1], currentColor)) {
                        List<Integer> captures = allCapturesSnap(next, opponentColor);
                        for (int capPos : captures) {
                            int[] afterCap = snapCopy(next);
                            afterCap[capPos] = -1;
                            best = Math.max(best, minimax(afterCap, depth - 1, false, colorAI, colorOpp, phase));
                        }
                    } else {
                        best = Math.max(best, minimax(next, depth - 1, false, colorAI, colorOpp, phase));
                    }
                }
            }
            return best == Integer.MIN_VALUE ? evaluateSnap(snap, colorAI, colorOpp, phase) : best;

        } else {
            int best = Integer.MAX_VALUE;
            if (phase == MerelleStageModel.PHASE_PLACEMENT) {
                for (int pos : allPlacementsSnap(snap)) {
                    int[] next = snapCopy(snap);
                    next[pos] = currentColor;
                    // If this placement forms a mill, simulate every possible capture
                    if (formsMillSnap(next, pos, currentColor)) {
                        List<Integer> captures = allCapturesSnap(next, opponentColor);
                        for (int capPos : captures) {
                            int[] afterCap = snapCopy(next);
                            afterCap[capPos] = -1;
                            best = Math.min(best, minimax(afterCap, depth - 1, true, colorAI, colorOpp, phase));
                        }
                    } else {
                        best = Math.min(best, minimax(next, depth - 1, true, colorAI, colorOpp, phase));
                    }
                }
            } else {
                for (int[] mv : allMovesSnap(snap, currentColor)) {
                    int[] next = snapCopy(snap);
                    next[mv[1]] = next[mv[0]];
                    next[mv[0]] = -1;
                    // If this move forms a mill, simulate every possible capture
                    if (formsMillSnap(next, mv[1], currentColor)) {
                        List<Integer> captures = allCapturesSnap(next, opponentColor);
                        for (int capPos : captures) {
                            int[] afterCap = snapCopy(next);
                            afterCap[capPos] = -1;
                            best = Math.min(best, minimax(afterCap, depth - 1, true, colorAI, colorOpp, phase));
                        }
                    } else {
                        best = Math.min(best, minimax(next, depth - 1, true, colorAI, colorOpp, phase));
                    }
                }
            }
            return best == Integer.MAX_VALUE ? evaluateSnap(snap, colorAI, colorOpp, phase) : best;
        }
    }

    /**
     * Checks if placing/moving a pawn at {@code pos} forms a mill for {@code color}
     * in the given snapshot (the pawn is already set at snap[pos] = color before the call).
     */
    private boolean formsMillSnap(int[] snap, int pos, int color) {
        for (int[] mill : MerelleBoard.MILLS) {
            boolean containsPos = false;
            for (int p : mill) if (p == pos) { containsPos = true; break; }
            if (!containsPos) continue;
            boolean full = true;
            for (int p : mill) if (snap[p] != color) { full = false; break; }
            if (full) return true;
        }
        return false;
    }

    // ================================================================
    // STRATEGY 2: ALPHA-BETA
    // ================================================================

    /**
     * Alpha-Beta strategy entry point.
     * Selects the best possible move by exploring the game tree
     * with the Minimax algorithm optimized by Alpha-Beta pruning.
     *
     * This method is structurally identical to MiniMax:
     * it generates all possible moves depending on the game phase
     * (placement, movement, capture), then evaluates each outcome
     * via the alphabeta() function.
     *
     * Main difference: useless branches are cut off as soon as
     * alpha >= beta, which significantly reduces the number of explored positions.
     *
     * @param stageModel current stage model containing the game state
     * @param playerId   AI player identifier (0 or 1)
     * @return a string representing the chosen move:
     * - "A1" for a placement
     * - "A1 B2" for a movement
     * - "XA1" for a capture
     */
    private String getDecisionAlphaBeta(MerelleStageModel stageModel, int playerId) {
        MerelleBoard board = stageModel.getBoard();
        int phase = stageModel.getCurrentPhase();

        int colorAI  = (playerId == 0) ? stageModel.getColorJ1() : stageModel.getColorJ2();
        int colorOpp = (playerId == 0) ? stageModel.getColorJ2() : stageModel.getColorJ1();

        int[] snap = boardSnapshot(board, colorAI, colorOpp);

        String lastOwnMove = getLastOwnMove(stageModel, playerId);

        // ===================== CAPTURE =====================
        if (stageModel.isMillJustFormed()) {
            List<Integer> captures = allCapturesSnap(snap, colorOpp);

            int bestScore = Integer.MIN_VALUE;
            List<Integer> best = new ArrayList<>();

            for (int pos : captures) {
                int[] next = snapCopy(snap);
                next[pos] = -1;

                int score = alphabeta(next, ALPHABETA_DEPTH - 1,
                        Integer.MIN_VALUE, Integer.MAX_VALUE,
                        false, colorAI, colorOpp, phase);

                if (score > bestScore) {
                    bestScore = score;
                    best.clear();
                    best.add(pos);
                } else if (score == bestScore) {
                    best.add(pos);
                }
            }

            return "X" + posToCoord(best.get(random.nextInt(best.size())));
        }

        // ===================== PLACEMENT =====================
        if (phase == MerelleStageModel.PHASE_PLACEMENT) {
            List<Integer> placements = allPlacementsSnap(snap);

            int bestScore = Integer.MIN_VALUE;
            List<Integer> best = new ArrayList<>();

            for (int pos : placements) {
                int[] next = snapCopy(snap);
                next[pos] = colorAI;

                int score = alphabeta(next, ALPHABETA_DEPTH - 1,
                        Integer.MIN_VALUE, Integer.MAX_VALUE,
                        false, colorAI, colorOpp, phase);

                if (score > bestScore) {
                    bestScore = score;
                    best.clear();
                    best.add(pos);
                } else if (score == bestScore) {
                    best.add(pos);
                }
            }

            return posToCoord(best.get(random.nextInt(best.size())));
        }

        // ===================== MOVEMENT =====================
        List<int[]> moves = allMovesSnap(snap, colorAI);

        int bestScore = Integer.MIN_VALUE;
        List<int[]> bestMoves = new ArrayList<>();

        for (int[] mv : moves) {
            int[] next = snapCopy(snap);
            next[mv[1]] = next[mv[0]];
            next[mv[0]] = -1;

            int score = alphabeta(next, ALPHABETA_DEPTH - 1,
                    Integer.MIN_VALUE, Integer.MAX_VALUE,
                    false, colorAI, colorOpp, phase);

            String moveStr = mv[0] + "->" + mv[1];
            if (isPingPong(lastOwnMove, moveStr)) {
                score -= 500;
            }

            // Heavily penalizes reforming the exact same mill as the last one formed.
            if (wouldReformLastMillSnap(snap, mv[0], mv[1], colorAI, playerId, stageModel)) {
                score -= 100000;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(mv);
            } else if (score == bestScore) {
                bestMoves.add(mv);
            }
        }

        int[] best = bestMoves.get(random.nextInt(bestMoves.size()));
        return posToCoord(best[0]) + " " + posToCoord(best[1]);
    }

    /**
     * Recursive implementation of the Alpha-Beta algorithm.
     * Optimized variant of Minimax that reduces the number of explored nodes
     * by pruning irrelevant branches.
     *
     * The algorithm maintains two bounds:
     * - alpha : best guaranteed value for the maximizing player
     * - beta  : best guaranteed value for the minimizing player
     *
     * As soon as a position satisfies the condition alpha >= beta,
     * the exploration of the current branch is cut off because it cannot
     * influence the final decision.
     *
     * The function explores the three game cases:
     * - placement phase
     * - movement phase
     * - capture after forming a mill
     *
     * @param snap         current game board state
     * @param depth        remaining search depth
     * @param alpha        best value found so far for the MAX player
     * @param beta         best value found so far for the MIN player
     * @param isMaximizing true if it's the AI's turn, false otherwise
     * @param colorAI      AI player identifier (0 or 1)
     * @param phase        current game phase (placement or movement)
     * @return heuristic score of the evaluated position
     */
    private int alphabeta(int[] snap, int depth, int alpha, int beta,
                          boolean isMaximizing,
                          int colorAI, int colorOpp, int phase) {

        if (depth == 0 || isTerminalSnap(snap, colorAI, colorOpp, phase)) {
            return evaluateSnap(snap, colorAI, colorOpp, phase);
        }

        int currentColor = isMaximizing ? colorAI : colorOpp;

        if (isMaximizing) {
            int best = Integer.MIN_VALUE;

            if (phase == MerelleStageModel.PHASE_PLACEMENT) {

                for (int pos : allPlacementsSnap(snap)) {
                    int[] next = snapCopy(snap);
                    next[pos] = currentColor;

                    if (formsMillSnap(next, pos, currentColor)) {
                        for (int cap : allCapturesSnap(next, colorOpp)) {
                            int[] after = snapCopy(next);
                            after[cap] = -1;

                            best = Math.max(best, alphabeta(after, depth - 1,
                                    alpha, beta, false,
                                    colorAI, colorOpp, phase));

                            alpha = Math.max(alpha, best);
                            if (beta <= alpha) return best; // PRUNING
                        }
                    } else {
                        best = Math.max(best, alphabeta(next, depth - 1,
                                alpha, beta, false,
                                colorAI, colorOpp, phase));

                        alpha = Math.max(alpha, best);
                        if (beta <= alpha) return best;
                    }
                }

            } else {

                for (int[] mv : allMovesSnap(snap, currentColor)) {
                    int[] next = snapCopy(snap);
                    next[mv[1]] = next[mv[0]];
                    next[mv[0]] = -1;

                    if (formsMillSnap(next, mv[1], currentColor)) {
                        for (int cap : allCapturesSnap(next, colorOpp)) {
                            int[] after = snapCopy(next);
                            after[cap] = -1;

                            best = Math.max(best, alphabeta(after, depth - 1,
                                    alpha, beta, false,
                                    colorAI, colorOpp, phase));

                            alpha = Math.max(alpha, best);
                            if (beta <= alpha) return best;
                        }
                    } else {
                        best = Math.max(best, alphabeta(next, depth - 1,
                                alpha, beta, false,
                                colorAI, colorOpp, phase));

                        alpha = Math.max(alpha, best);
                        if (beta <= alpha) return best;
                    }
                }
            }

            return best;

        } else {
            int best = Integer.MAX_VALUE;

            if (phase == MerelleStageModel.PHASE_PLACEMENT) {

                for (int pos : allPlacementsSnap(snap)) {
                    int[] next = snapCopy(snap);
                    next[pos] = currentColor;

                    if (formsMillSnap(next, pos, currentColor)) {
                        for (int cap : allCapturesSnap(next, colorAI)) {
                            int[] after = snapCopy(next);
                            after[cap] = -1;

                            best = Math.min(best, alphabeta(after, depth - 1,
                                    alpha, beta, true,
                                    colorAI, colorOpp, phase));

                            beta = Math.min(beta, best);
                            if (beta <= alpha) return best;
                        }
                    } else {
                        best = Math.min(best, alphabeta(next, depth - 1,
                                alpha, beta, true,
                                colorAI, colorOpp, phase));

                        beta = Math.min(beta, best);
                        if (beta <= alpha) return best;
                    }
                }

            } else {

                for (int[] mv : allMovesSnap(snap, currentColor)) {
                    int[] next = snapCopy(snap);
                    next[mv[1]] = next[mv[0]];
                    next[mv[0]] = -1;

                    if (formsMillSnap(next, mv[1], currentColor)) {
                        for (int cap : allCapturesSnap(next, colorAI)) {
                            int[] after = snapCopy(next);
                            after[cap] = -1;

                            best = Math.min(best, alphabeta(after, depth - 1,
                                    alpha, beta, true,
                                    colorAI, colorOpp, phase));

                            beta = Math.min(beta, best);
                            if (beta <= alpha) return best;
                        }
                    } else {
                        best = Math.min(best, alphabeta(next, depth - 1,
                                alpha, beta, true,
                                colorAI, colorOpp, phase));

                        beta = Math.min(beta, best);
                        if (beta <= alpha) return best;
                    }
                }
            }

            return best;
        }
    }

    // ================================================================
    // STRATEGY 3: MONTE CARLO
    // ================================================================

    /**
     * Monte Carlo entry point.
     * For each possible move, launches MCTS_SIMULATIONS random games
     * from the resulting state, and chooses the move with the best win rate.
     *
     * @param stageModel the current stage model
     * @param playerId   AI player index (0 or 1)
     * @return the move with the best simulated win rate
     */
    private String getDecisionMonteCarlo(MerelleStageModel stageModel, int playerId) {
        MerelleBoard board = stageModel.getBoard();
        int phase = stageModel.getCurrentPhase();

        int colorAI  = (playerId == 0) ? stageModel.getColorJ1() : stageModel.getColorJ2();
        int colorOpp = (playerId == 0) ? stageModel.getColorJ2() : stageModel.getColorJ1();

        int[] snap = boardSnapshot(board, colorAI, colorOpp);

        String lastOwnMove = getLastOwnMove(stageModel, playerId);

        // ===================== CAPTURE =====================
        if (stageModel.isMillJustFormed()) {
            List<Integer> captures = allCapturesSnap(snap, colorOpp);
            int bestWins = -1;
            List<Integer> bestCaptures = new ArrayList<>();

            for (int pos : captures) {
                int[] next = snapCopy(snap);
                next[pos] = -1;
                int wins = 0;
                for (int i = 0; i < MCTS_SIMULATIONS; i++) {
                    int winner = simulateRandomGameSnap(snapCopy(next), 1 - playerId, phase, colorAI, colorOpp);
                    if (winner == playerId) wins++;
                }
                if (wins > bestWins) {
                    bestWins = wins;
                    bestCaptures.clear();
                    bestCaptures.add(pos);
                } else if (wins == bestWins) {
                    bestCaptures.add(pos);
                }
            }
            return "X" + posToCoord(bestCaptures.get(random.nextInt(bestCaptures.size())));
        }

        // ===================== PLACEMENT =====================
        if (phase == MerelleStageModel.PHASE_PLACEMENT) {
            List<Integer> placements = allPlacementsSnap(snap);
            int bestWins = -1;
            List<Integer> bestPlacements = new ArrayList<>();

            for (int pos : placements) {
                int[] next = snapCopy(snap);
                next[pos] = colorAI;
                int wins = 0;
                for (int i = 0; i < MCTS_SIMULATIONS; i++) {
                    // If this placement forms a mill, simulate a random capture beforehand
                    int[] afterCap = next;
                    if (formsMillSnap(next, pos, colorAI)) {
                        List<Integer> caps = allCapturesSnap(next, colorOpp);
                        if (!caps.isEmpty()) {
                            afterCap = snapCopy(next);
                            afterCap[caps.get(random.nextInt(caps.size()))] = -1;
                        }
                    }
                    int winner = simulateRandomGameSnap(snapCopy(afterCap), 1 - playerId, phase, colorAI, colorOpp);
                    if (winner == playerId) wins++;
                }
                if (wins > bestWins) {
                    bestWins = wins;
                    bestPlacements.clear();
                    bestPlacements.add(pos);
                } else if (wins == bestWins) {
                    bestPlacements.add(pos);
                }
            }
            return posToCoord(bestPlacements.get(random.nextInt(bestPlacements.size())));
        }

        // ===================== MOVEMENT =====================
        List<int[]> moves = allMovesSnap(snap, colorAI);
        int bestWins = -1;
        List<int[]> bestMoves = new ArrayList<>();

        for (int[] mv : moves) {
            int[] next = snapCopy(snap);
            next[mv[1]] = next[mv[0]];
            next[mv[0]] = -1;

            // Anti-ping-pong and anti-mill reformation filtering right from the root
            String moveStr = mv[0] + "->" + mv[1];
            if (isPingPong(lastOwnMove, moveStr)) continue;
            if (wouldReformLastMillSnap(snap, mv[0], mv[1], colorAI, playerId, stageModel)) continue;

            int wins = 0;
            for (int i = 0; i < MCTS_SIMULATIONS; i++) {
                // If this move forms a mill, simulate a random capture beforehand
                int[] afterCap = next;
                if (formsMillSnap(next, mv[1], colorAI)) {
                    List<Integer> caps = allCapturesSnap(next, colorOpp);
                    if (!caps.isEmpty()) {
                        afterCap = snapCopy(next);
                        afterCap[caps.get(random.nextInt(caps.size()))] = -1;
                    }
                }
                int winner = simulateRandomGameSnap(snapCopy(afterCap), 1 - playerId, phase, colorAI, colorOpp);
                if (winner == playerId) wins++;
            }
            if (wins > bestWins) {
                bestWins = wins;
                bestMoves.clear();
                bestMoves.add(mv);
            } else if (wins == bestWins) {
                bestMoves.add(mv);
            }
        }

        // Fallback if all moves were filtered out (should not happen)
        if (bestMoves.isEmpty()) {
            for (int[] mv : moves) bestMoves.add(mv);
        }

        int[] best = bestMoves.get(random.nextInt(bestMoves.size()));
        return posToCoord(best[0]) + " " + posToCoord(best[1]);
    }

    /**
     * Simulates a completely random game from an int[24] snapshot,
     * until a terminal state or a turn limit is reached.
     * At each turn, chooses a random move among valid moves,
     * while avoiding ping-pong (move immediately reverse of the previous one).
     *
     * @param snap          initial board state (will be modified in place)
     * @param currentPlayer index of the player whose turn it is (0 or 1)
     * @param phase         starting phase (PHASE_PLACEMENT or PHASE_MOVEMENT)
     * @param colorAI       color of player 0 (AI in the simulation)
     * @param colorOpp      color of player 1
     * @return index of the winning player (0 or 1), or -1 if draw / limit reached
     */
    private int simulateRandomGameSnap(int[] snap, int currentPlayer, int phase,
                                       int colorAI, int colorOpp) {
        int maxTurns = 200; // Loop safeguard to avoid infinite simulations
        int turnCount = 0;
        int activeColor = (currentPlayer == 0) ? colorAI : colorOpp;
        int oppColor    = (currentPlayer == 0) ? colorOpp : colorAI;

        // Keep track of the last simulated move to limit endless ping-ponging in MCTS
        int lastSrc = -1;
        int lastDest = -1;

        while (turnCount < maxTurns) {
            if (isTerminalSnap(snap, colorAI, colorOpp, phase)) {
                return getWinnerFromSnap(snap, colorAI, colorOpp, phase);
            }

            if (phase == MerelleStageModel.PHASE_PLACEMENT) {
                List<Integer> placements = allPlacementsSnap(snap);
                if (placements.isEmpty()) {
                    // Transition to movement phase if no placements are left
                    phase = MerelleStageModel.PHASE_MOVEMENT;
                    continue;
                }
                int choose = placements.get(random.nextInt(placements.size()));
                snap[choose] = activeColor;

                if (formsMillSnap(snap, choose, activeColor)) {
                    List<Integer> caps = allCapturesSnap(snap, oppColor);
                    if (!caps.isEmpty()) {
                        snap[caps.get(random.nextInt(caps.size()))] = -1;
                    }
                }
            } else {
                // Movement phase
                List<int[]> moves = allMovesSnap(snap, activeColor);
                if (moves.isEmpty()) {
                    // Current player is blocked and loses
                    return 1 - currentPlayer;
                }

                // Filter out immediate ping-pong moves if alternatives exist
                List<int[]> filteredMoves = new ArrayList<>();
                for (int[] mv : moves) {
                    if (mv[0] != lastDest || mv[1] != lastSrc) {
                        filteredMoves.add(mv);
                    }
                }
                int[] chosenMove = filteredMoves.isEmpty()
                        ? moves.get(random.nextInt(moves.size()))
                        : filteredMoves.get(random.nextInt(filteredMoves.size()));

                lastSrc = chosenMove[0];
                lastDest = chosenMove[1];

                snap[chosenMove[1]] = snap[chosenMove[0]];
                snap[chosenMove[0]] = -1;

                if (formsMillSnap(snap, chosenMove[1], activeColor)) {
                    List<Integer> caps = allCapturesSnap(snap, oppColor);
                    if (!caps.isEmpty()) {
                        snap[caps.get(random.nextInt(caps.size()))] = -1;
                    }
                }
            }

            // Alternate players
            currentPlayer = 1 - currentPlayer;
            activeColor = (currentPlayer == 0) ? colorAI : colorOpp;
            oppColor    = (currentPlayer == 0) ? colorOpp : colorAI;
            turnCount++;
        }

        return -1; // Draw if turn limit is reached
    }

    // ================================================================
    // UTILITY METHODS WORKING ON INT[24] SNAPSHOTS
    // ================================================================

    /**
     * Extracts a lightweight integer array representing the 24 cells of the board.
     */
    private int[] boardSnapshot(MerelleBoard board, int colorAI, int colorOpp) {
        int[] snap = new int[24];
        for (int i = 0; i < 24; i++) {
            MerellePawn p = board.getPawnAt(i);
            if (p == null) {
                snap[i] = -1;
            } else {
                snap[i] = p.getColor();
            }
        }
        return snap;
    }

    /**
     * Creates a deep copy of an int[24] snapshot.
     */
    private int[] snapCopy(int[] snap) {
        int[] copy = new int[24];
        System.arraycopy(snap, 0, copy, 0, 24);
        return copy;
    }

    /**
     * Gathers all free board positions available for placement.
     */
    private List<Integer> allPlacementsSnap(int[] snap) {
        List<Integer> placements = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            if (snap[i] == -1) placements.add(i);
        }
        return placements;
    }

    /**
     * Gathers all legal moves (src -> dest adjacent pairs) for a specific color.
     */
    private List<int[]> allMovesSnap(int[] snap, int color) {
        List<int[]> moves = new ArrayList<>();
        for (int src = 0; src < 24; src++) {
            if (snap[src] == color) {
                // ◄ Loop specifically over the structural neighbors of 'src'
                for (int dest : MerelleBoard.ADJACENCY[src]) {
                    if (snap[dest] == -1) {
                        moves.add(new int[]{src, dest});
                    }
                }
            }
        }
        return moves;
    }

    /**
     * Gathers all capturable opponent pawns.
     * Respects the rule: a pawn inside a mill cannot be captured unless ALL opponent pawns are in mills.
     */
    private List<Integer> allCapturesSnap(int[] snap, int oppColor) {
        List<Integer> captures = new ArrayList<>();
        List<Integer> inMill = new ArrayList<>();
        List<Integer> outOfMill = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            if (snap[i] == oppColor) {
                if (formsMillSnap(snap, i, oppColor)) {
                    inMill.add(i);
                } else {
                    outOfMill.add(i);
                }
            }
        }

        // If all opponent pawns are safe inside mills, any of them can be captured
        if (outOfMill.isEmpty()) {
            return inMill;
        }
        return outOfMill;
    }

    /**
     * Evaluates whether a snapshot state is terminal (victory/loss during movement phase).
     */
    private boolean isTerminalSnap(int[] snap, int colorAI, int colorOpp, int phase) {
        if (phase == MerelleStageModel.PHASE_PLACEMENT) return false;

        int countAI = 0, countOpp = 0;
        for (int i = 0; i < 24; i++) {
            if (snap[i] == colorAI) countAI++;
            if (snap[i] == colorOpp) countOpp++;
        }

        if (countAI < 3 || countOpp < 3) return true;
        if (allMovesSnap(snap, colorAI).isEmpty() || allMovesSnap(snap, colorOpp).isEmpty()) return true;

        return false;
    }

    /**
     * Identifies the winner index (0 for AI, 1 for Opponent) from a terminal snapshot.
     */
    private int getWinnerFromSnap(int[] snap, int colorAI, int colorOpp, int phase) {
        if (phase == MerelleStageModel.PHASE_PLACEMENT) return -1;

        int countAI = 0, countOpp = 0;
        for (int i = 0; i < 24; i++) {
            if (snap[i] == colorAI) countAI++;
            if (snap[i] == colorOpp) countOpp++;
        }

        if (countAI < 3 || allMovesSnap(snap, colorAI).isEmpty()) return 1; // Opponent wins
        if (countOpp < 3 || allMovesSnap(snap, colorOpp).isEmpty()) return 0; // AI wins

        return -1;
    }

    /**
     * Heuristic valuation function to score a given snapshot board state.
     */
    private int evaluateSnap(int[] snap, int colorAI, int colorOpp, int phase) {
        int score = 0;
        int countAI = 0;
        int countOpp = 0;
        int millsAI = 0;
        int millsOpp = 0;

        // Count pawns and detect mills
        for (int i = 0; i < 24; i++) {
            if (snap[i] == colorAI) countAI++;
            if (snap[i] == colorOpp) countOpp++;
        }

        // Count complete mills across the board
        for (int[] mill : MerelleBoard.MILLS) {
            if (snap[mill[0]] == colorAI && snap[mill[1]] == colorAI && snap[mill[2]] == colorAI) millsAI++;
            if (snap[mill[0]] == colorOpp && snap[mill[1]] == colorOpp && snap[mill[2]] == colorOpp) millsOpp++;
        }

        if (phase == MerelleStageModel.PHASE_PLACEMENT) {
            // Placement phase criteria: core piece advantage and positional potential (mills)
            score += (countAI - countOpp) * 200;
            score += (millsAI - millsOpp) * 150;
        } else {
            // Movement phase criteria: heavy penalties for losing pieces below threshold
            if (countAI < 3) return -1000000;
            if (countOpp < 3) return 1000000;

            int mobilityAI = allMovesSnap(snap, colorAI).size();
            int mobilityOpp = allMovesSnap(snap, colorOpp).size();

            if (mobilityAI == 0) return -1000000;
            if (mobilityOpp == 0) return 1000000;

            score += (countAI - countOpp) * 1000;
            score += (millsAI - millsOpp) * 400;
            score += (mobilityAI - mobilityOpp) * 10;
        }

        return score;
    }

    /**
     * Safety checks to see if moving src -> dest illegally repeats the exact last formed mill.
     */
    private boolean wouldReformLastMillSnap(int[] snap, int src, int dest, int color, int playerId, MerelleStageModel stageModel) {
        for (int[] mill : MerelleBoard.MILLS) {
            boolean containsDest = false;
            for (int p : mill) if (p == dest) { containsDest = true; break; }
            if (!containsDest) continue;

            boolean wouldForm = true;
            for (int p : mill) {
                if (p == dest) continue;
                if (p == src) { wouldForm = false; break; }
                if (snap[p] != color) { wouldForm = false; break; }
            }
            if (!wouldForm) continue;

            if (stageModel.isSameMillAsLast(playerId, mill)) return true;
        }
        return false;
    }

    /**
     * Translates a logical board array coordinate integer index (0-23) into its human readable alphanumeric counterpart.
     */
    private String posToCoord(int pos) {
        if (pos < 0 || pos >= 24) return "??";
        int row = MerelleBoard.POS_TO_GRID[pos][0];
        int col = MerelleBoard.POS_TO_GRID[pos][1];
        char rowLetter = (char) ('A' + row);
        int colNumber = col + 1;
        return "" + rowLetter + colNumber;
    }
}