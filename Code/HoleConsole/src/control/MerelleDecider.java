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

    /** Constantes de difficulté */
    public static final int DIFFICULTY_MINIMAX    = 1;
    public static final int DIFFICULTY_ALPHABETA  = 2;
    public static final int DIFFICULTY_MONTECARLO = 3;

    /** Profondeur de recherche pour MiniMax et Alpha-Beta. */
    private static final int MINIMAX_DEPTH   = 5;
    private static final int ALPHABETA_DEPTH = 7;

    /** Nombre de simulations par coup pour Monte Carlo. */
    private static final int MCTS_SIMULATIONS = 120;

    /** Difficulté active, à définir avant le lancement de la partie. */
    public static int aiDifficulty = DIFFICULTY_MINIMAX;

    /**
     * Difficultés par joueur (index 0 et 1).
     * Si null, on utilise aiDifficulty comme valeur commune.
     */
    public static int[] aiDifficultyPerPlayer = null;

    private static final Random random = new Random();

    /** Constructeur */
    public MerelleDecider(Model model, Controller control) {
        super(model, control);
    }

    @Override
    public ActionList decide() {
        return new ActionList();
    }

    /**
     * Calcule et retourne la décision de l'IA sous forme de chaîne,
     * dans le même format que la saisie clavier humaine.
     * Délègue à la stratégie choisie via aiDifficulty.
     *
     * @param stageModel le modèle du stage courant
     * @param playerId   index du joueur IA (0 ou 1)
     * @return la saisie simulée : "A1" (placement), "A1 B2" (déplacement), "XA1" (capture)
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
    // STRATÉGIE 1 : MINIMAX
    // ================================================================

    /**
     * Point d'entrée MiniMax.
     * Gère les 3 situations : capture, placement (phase 1), déplacement (phase 2).
     * Pour chaque coup possible, appelle minimax() et garde le meilleur score.
     *
     * @param stageModel le modèle du stage courant
     * @param playerId   index du joueur IA (0 ou 1)
     * @return la meilleure saisie trouvée par MiniMax
     */
    private String getDecisionMinimax(MerelleStageModel stageModel, int playerId) {
        MerelleBoard board = stageModel.getBoard();
        int phase = stageModel.getCurrentPhase();

        int colorAI  = (playerId == 0) ? stageModel.getColorJ1() : stageModel.getColorJ2();
        int colorOpp = (playerId == 0) ? stageModel.getColorJ2() : stageModel.getColorJ1();

        int[] snap = boardSnapshot(board, colorAI, colorOpp);

        // Dernier coup joué par CE joueur (2 entrées en arrière dans l'historique
        // partagé, car les deux joueurs alternent).
        // Format : "src->dest", ex. "9->10"
        String lastOwnMove = getLastOwnMove(stageModel, playerId);

        // --- CAS SPÉCIAL : capture ---
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

        // --- PHASE 1 : placement ---
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

        // --- PHASE 2 : déplacement ---
        List<int[]> moves = allMovesSnap(snap, colorAI);
        int bestScore = Integer.MIN_VALUE;
        List<int[]> bestMoves = new ArrayList<>();
        for (int[] mv : moves) {
            int[] next = snapCopy(snap);
            next[mv[1]] = next[mv[0]];
            next[mv[0]] = -1;
            int score = minimax(next, MINIMAX_DEPTH - 1, false, colorAI, colorOpp, phase);

            // Pénalise le coup inverse du dernier coup de CE joueur (ping-pong).
            String moveStr = mv[0] + "->" + mv[1];
            if (isPingPong(lastOwnMove, moveStr)) {
                score -= 500;
            }

            // Pénalise fortement la reformation du même moulin que le dernier formé.
            // L'IA pourra le reformer seulement après en avoir fait un autre.
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
     * Retourne le dernier coup joué par CE joueur (pas l'adversaire).
     * Dans un historique alterné [opp, me, opp], le coup du joueur courant
     * est à l'index 1 (2 coups en arrière).
     * Retourne null si pas encore joué.
     */
    private String getLastOwnMove(MerelleStageModel stageModel, int playerId) {
        String[] history = stageModel.getLastMoves();
        // Depuis la mise à jour du modèle, chaque entrée est préfixée par le joueur :
        // "0:9->10" ou "1:9->10". On cherche la dernière entrée appartenant à CE joueur.
        String prefix = playerId + ":";
        for (String entry : history) {
            if (entry != null && entry.startsWith(prefix)) {
                // On retire le préfixe "0:" ou "1:" pour ne garder que le coup brut
                return entry.substring(prefix.length());
            }
        }
        return null;
    }

    /**
     * Retourne true si moveStr est l'exact inverse de lastMove.
     * Ex : lastMove = "9->10", moveStr = "10->9" → true (ping-pong).
     * lastMove doit être sans préfixe joueur (déjà extrait par getLastOwnMove).
     */
    private boolean isPingPong(String lastMove, String moveStr) {
        if (lastMove == null || !lastMove.contains("->")) return false;
        String[] parts = lastMove.split("->");
        if (parts.length != 2) return false;
        String inverse = parts[1] + "->" + parts[0];
        return inverse.equals(moveStr);
    }

    /**
     * Algorithme MiniMax récursif travaillant sur un snapshot int[24].
     * Chaque case vaut : colorAI, colorOpp, ou -1 (libre).
     * Aucun objet boardifier n'est touché → pas d'événements, pas de débordement.
     *
     * @param snap         copie de l'état du plateau (int[24])
     * @param depth        profondeur restante
     * @param isMaximizing true = tour de l'IA, false = tour de l'adversaire
     * @param colorAI      constante couleur de l'IA
     * @param colorOpp     constante couleur de l'adversaire
     * @param phase        phase actuelle
     * @return score de l'état
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
                    // Si ce placement forme un moulin, simuler chaque capture possible
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
                    // Si ce déplacement forme un moulin, simuler chaque capture possible
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
                    // Si ce placement forme un moulin, simuler chaque capture possible
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
                    // Si ce déplacement forme un moulin, simuler chaque capture possible
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
     * Vérifie si placer/déplacer un pion en {@code pos} forme un moulin pour {@code color}
     * dans le snapshot donné (le pion est déjà posé en snap[pos] = color avant l'appel).
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
    // STRATÉGIE 2 : ALPHA-BETA
    // ================================================================

    /**
     * Point d'entrée de la stratégie Alpha-Bêta.
     * Sélectionne le meilleur coup possible en explorant l'arbre de jeu
     * avec l'algorithme Minimax optimisé par élagage Alpha-Bêta.
     *
     * Cette méthode est structurellement identique à MiniMax :
     * elle génère tous les coups possibles selon la phase de jeu
     * (placement, déplacement, capture), puis évalue chaque résultat
     * via la fonction alphabeta().
     *
     * Différence principale : les branches inutiles sont coupées dès que
     * alpha >= beta, ce qui réduit fortement le nombre de positions explorées.
     *
     * @param stageModel modèle du stage courant contenant l'état du jeu
     * @param playerId   identifiant du joueur IA (0 ou 1)
     * @return une chaîne représentant le coup choisi :
     *         - "A1" pour un placement
     *         - "A1 B2" pour un déplacement
     *         - "XA1" pour une capture
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

        // ===================== DEPLACEMENT =====================
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

            // Pénalise fortement la reformation du même moulin que le dernier formé.
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
     * Implémentation récursive de l'algorithme Alpha-Bêta.
     * Variante optimisée du Minimax qui réduit le nombre de nœuds explorés
     * grâce à l'élagage des branches inutiles.
     *
     * L'algorithme maintient deux bornes :
     * - alpha : meilleure valeur garantie pour le joueur maximisant
     * - beta  : meilleure valeur garantie pour le joueur minimisant
     *
     * Dès qu'une position satisfait la condition alpha >= beta,
     * l'exploration de la branche courante est interrompue car elle ne peut
     * pas influencer la décision finale.
     *
     * La fonction explore les trois cas de jeu :
     * - phase de placement
     * - phase de déplacement
     * - capture après formation d'un moulin
     *
     * @param snap        état actuel du plateau de jeu
     * @param depth        profondeur restante de recherche
     * @param alpha        meilleure valeur déjà trouvée pour le joueur MAX
     * @param beta         meilleure valeur déjà trouvée pour le joueur MIN
     * @param isMaximizing true si c'est le tour de l'IA, false sinon
     * @param colorAI     identifiant du joueur IA (0 ou 1)
     * @param phase        phase actuelle du jeu (placement ou déplacement)
     * @return score heuristique de la position évaluée
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
    // STRATÉGIE 3 : MONTE CARLO
    // ================================================================

    /**
     * Point d'entrée Monte Carlo.
     * Pour chaque coup possible, lance MCTS_SIMULATIONS parties aléatoires
     * depuis l'état résultant, et choisit le coup avec le meilleur taux de victoires.
     *
     * @param stageModel le modèle du stage courant
     * @param playerId   index du joueur IA (0 ou 1)
     * @return le coup avec le meilleur taux de victoires simulées
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
                    // Si ce placement forme un moulin, simuler une capture aléatoire avant
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

        // ===================== DEPLACEMENT =====================
        List<int[]> moves = allMovesSnap(snap, colorAI);
        int bestWins = -1;
        List<int[]> bestMoves = new ArrayList<>();

        for (int[] mv : moves) {
            int[] next = snapCopy(snap);
            next[mv[1]] = next[mv[0]];
            next[mv[0]] = -1;

            // Filtre anti-ping-pong et anti-reformation de moulin dès la racine
            String moveStr = mv[0] + "->" + mv[1];
            if (isPingPong(lastOwnMove, moveStr)) continue;
            if (wouldReformLastMillSnap(snap, mv[0], mv[1], colorAI, playerId, stageModel)) continue;

            int wins = 0;
            for (int i = 0; i < MCTS_SIMULATIONS; i++) {
                // Si ce déplacement forme un moulin, simuler une capture aléatoire avant
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

        // Repli si tous les coups étaient filtrés (ne devrait pas arriver)
        if (bestMoves.isEmpty()) {
            for (int[] mv : moves) bestMoves.add(mv);
        }

        int[] best = bestMoves.get(random.nextInt(bestMoves.size()));
        return posToCoord(best[0]) + " " + posToCoord(best[1]);
    }

    /**
     * Simule une partie complètement aléatoire depuis un snapshot int[24],
     * jusqu'à un état terminal ou une limite de tours.
     * À chaque tour, choisit un coup aléatoire parmi les coups valides,
     * en évitant le ping-pong (coup immédiatement inverse du précédent).
     *
     * @param snap          état initial du plateau (sera modifié en place)
     * @param currentPlayer index du joueur dont c'est le tour (0 ou 1)
     * @param phase         phase de départ (PHASE_PLACEMENT ou PHASE_DEPLACEMENT)
     * @param colorAI       couleur du joueur 0 (IA dans la simulation)
     * @param colorOpp      couleur du joueur 1
     * @return index du joueur gagnant (0 ou 1), ou -1 si nul / limite atteinte
     */
    private int simulateRandomGameSnap(int[] snap, int currentPlayer, int phase,
                                       int colorAI, int colorOpp) {
        // Limite de tours pour éviter les boucles infinies
        final int MAX_TURNS = 80;

        // Mémorise le dernier coup de chaque joueur pour éviter le ping-pong
        // Format : "src->dest", null si pas encore joué
        String[] lastMove = new String[2];

        for (int turn = 0; turn < MAX_TURNS; turn++) {
            int color    = (currentPlayer == 0) ? colorAI : colorOpp;
            int oppColor = (currentPlayer == 0) ? colorOpp : colorAI;

            // --- Vérification d'état terminal ---
            if (isTerminalSnap(snap, colorAI, colorOpp, phase)) {
                // Qui a perdu ?
                int pawnsAI  = 0, pawnsOpp = 0;
                for (int v : snap) {
                    if (v == colorAI)  pawnsAI++;
                    if (v == colorOpp) pawnsOpp++;
                }
                if (phase == MerelleStageModel.PHASE_DEPLACEMENT) {
                    if (pawnsOpp < 3 || allMovesSnap(snap, colorOpp).isEmpty()) return 0; // colorAI (joueur 0) gagne
                    if (pawnsAI  < 3 || allMovesSnap(snap, colorAI ).isEmpty()) return 1; // colorOpp (joueur 1) gagne
                }
                return -1; // nul (placement ou indéfini)
            }

            // --- Phase de placement ---
            if (phase == MerelleStageModel.PHASE_PLACEMENT) {
                List<Integer> placements = allPlacementsSnap(snap);
                if (placements.isEmpty()) {
                    // Plus de cases libres : passage en phase 2 (peut arriver en simulation)
                    phase = MerelleStageModel.PHASE_DEPLACEMENT;
                    continue;
                }
                int pos = placements.get(random.nextInt(placements.size()));
                snap[pos] = color;

                // Si moulin formé, capturer un pion adverse aléatoire (hors moulin si possible)
                if (formsMillSnap(snap, pos, color)) {
                    List<Integer> caps = allCapturesSnap(snap, oppColor);
                    if (!caps.isEmpty()) {
                        snap[caps.get(random.nextInt(caps.size()))] = -1;
                    }
                }

                // Transition de phase si tous les pions sont posés
                // (simplifié : on bascule après 18 coups de placement au total)
                int placed = 0;
                for (int v : snap) if (v != -1) placed++;
                if (placed >= 18) phase = MerelleStageModel.PHASE_DEPLACEMENT;

            } else {
                // --- Phase de déplacement ---
                List<int[]> moves = allMovesSnap(snap, color);
                if (moves.isEmpty()) {
                    // Ce joueur est bloqué → l'autre gagne
                    return 1 - currentPlayer;
                }

                // Filtrer le coup ping-pong
                String myLastMove = lastMove[currentPlayer];
                List<int[]> filtered = new ArrayList<>();
                for (int[] mv : moves) {
                    if (!isPingPong(myLastMove, mv[0] + "->" + mv[1])) {
                        filtered.add(mv);
                    }
                }
                // S'il ne reste qu'un seul coup et qu'il est ping-pong, on l'autorise quand même
                if (filtered.isEmpty()) filtered = moves;

                int[] mv = filtered.get(random.nextInt(filtered.size()));
                snap[mv[1]] = snap[mv[0]];
                snap[mv[0]] = -1;
                lastMove[currentPlayer] = mv[0] + "->" + mv[1];

                // Si moulin formé, capturer un pion adverse aléatoire
                if (formsMillSnap(snap, mv[1], color)) {
                    List<Integer> caps = allCapturesSnap(snap, oppColor);
                    if (!caps.isEmpty()) {
                        snap[caps.get(random.nextInt(caps.size()))] = -1;
                    }
                }
            }

            currentPlayer = 1 - currentPlayer;
        }

        // Limite de tours atteinte : évaluer par heuristique
        int score = evaluateSnap(snap, colorAI, colorOpp, phase);
        if (score > 0) return 0;
        if (score < 0) return 1;
        return -1;
    }

    /**
     * Simule une partie complètement aléatoire depuis l'état actuel du plateau
     * jusqu'à ce qu'un état terminal soit atteint (victoire ou blocage).
     * À chaque tour, choisit un coup aléatoire parmi les coups valides.
     * (Surcharge conservée pour compatibilité avec la signature d'origine.)
     *
     * @param board        état actuel du plateau (non utilisé directement — préférer simulateRandomGameSnap)
     * @param currentPlayer index du joueur dont c'est le tour au début de la simulation
     * @param phase        phase actuelle au début de la simulation
     * @param colorJ1      couleur du joueur 0
     * @param colorJ2      couleur du joueur 1
     * @return index du joueur gagnant (0 ou 1), ou -1 si match nul / limite de tours
     */
    private int simulateRandomGame(MerelleBoard board, int currentPlayer, int phase,
                                   int colorJ1, int colorJ2) {
        int[] snap = boardSnapshot(board, colorJ1, colorJ2);
        return simulateRandomGameSnap(snap, currentPlayer, phase, colorJ1, colorJ2);
    }

    // ================================================================
    // ÉVALUATION
    // ================================================================

    /**
     * Fonction d'évaluation heuristique d'un état du plateau.
     * Retourne un score du point de vue de playerId :
     *   score positif  -> position favorable à l'IA
     *   score négatif  -> position favorable à l'adversaire
     *   ±10000         -> victoire / défaite (état terminal)
     *
     * Critères pris en compte :
     *   - nombre de pions sur le plateau (×10)
     *   - nombre de moulins formés (×50)
     *   - mobilité : nombre de coups disponibles (×2)
     *
     * @param board    état du plateau à évaluer
     * @param colorAI  couleur du joueur IA (constante MerellePawn.PAWN_*)
     * @param colorOpp couleur de l'adversaire
     * @return score entier centré sur 0
     */
    private int evaluate(MerelleBoard board, int colorAI, int colorOpp) {
        int[] snap = boardSnapshot(board, colorAI, colorOpp);
        return evaluateSnap(snap, colorAI, colorOpp);
    }

    // ================================================================
    // SNAPSHOT — représentation légère du plateau en int[24]
    // ================================================================

    /**
     * Convertit le board réel en tableau int[24].
     * Chaque case vaut : colorAI, colorOpp, ou -1 (libre).
     * Aucun objet boardifier n'est créé ni modifié.
     */
    private int[] boardSnapshot(MerelleBoard board, int colorAI, int colorOpp) {
        int[] snap = new int[24];
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = board.getPawnAt(pos);
            if (pw == null)                    snap[pos] = -1;
            else if (pw.getColor() == colorAI) snap[pos] = colorAI;
            else                               snap[pos] = colorOpp;
        }
        return snap;
    }

    /** Copie un snapshot (pour ne pas modifier l'original lors de la simulation). */
    private int[] snapCopy(int[] snap) {
        return snap.clone();
    }

    /**
     * Évaluation heuristique sur un snapshot int[24].
     * Surcharge sans phase : suppose PHASE_DEPLACEMENT (appels legacy).
     */
    private int evaluateSnap(int[] snap, int colorAI, int colorOpp) {
        return evaluateSnap(snap, colorAI, colorOpp, MerelleStageModel.PHASE_DEPLACEMENT);
    }

    /**
     * Évaluation heuristique sur un snapshot int[24], avec conscience de la phase.
     * Le test "pions < 3 = fin de partie" n'est valide qu'en phase déplacement ;
     * en phase placement les joueurs ont naturellement peu de pions au début.
     */
    private int evaluateSnap(int[] snap, int colorAI, int colorOpp, int phase) {

        int pawnsAI = 0;
        int pawnsOpp = 0;

        int millsAI = 0;
        int millsOpp = 0;

        int threatsAI = 0;
        int threatsOpp = 0;

        int mobilityAI = allMovesSnap(snap, colorAI).size();
        int mobilityOpp = allMovesSnap(snap, colorOpp).size();

        // Comptage
        for (int v : snap) {
            if (v == colorAI) pawnsAI++;
            else if (v == colorOpp) pawnsOpp++;
        }

        // Fin de partie
        if (phase == MerelleStageModel.PHASE_DEPLACEMENT) {
            if (pawnsOpp < 3 || allMovesSnap(snap, colorOpp).isEmpty())
                return 100000;

            if (pawnsAI < 3 || allMovesSnap(snap, colorAI).isEmpty())
                return -100000;

            if (allMovesSnap(snap, colorOpp).isEmpty()) return 100000;
            if (allMovesSnap(snap, colorAI).isEmpty()) return -100000;
        }

        // Analyse des moulins
        for (int[] mill : MerelleBoard.MILLS) {

            int ai = 0;
            int opp = 0;
            int free = 0;

            for (int pos : mill) {
                if (snap[pos] == colorAI) ai++;
                else if (snap[pos] == colorOpp) opp++;
                else free++;
            }

            // Moulin complet
            if (ai == 3) millsAI++;
            if (opp == 3) millsOpp++;

            // Menace directe
            if (ai == 2 && free == 1) threatsAI++;
            if (opp == 2 && free == 1) threatsOpp++;
        }

        boolean deadPosition =
                millsAI == 0 &&
                        millsOpp == 0 &&
                        threatsAI == 0 &&
                        threatsOpp == 0 &&
                        pawnsAI == pawnsOpp;

        int deadPenalty = deadPosition ? -800 : 0;

        int riskyMillsAI = 0;
        int riskyMillsOpp = 0;

        for (int[] mill : MerelleBoard.MILLS) {

            int ai = 0, opp = 0, empty = 0;

            for (int p : mill) {
                if (snap[p] == colorAI) ai++;
                else if (snap[p] == colorOpp) opp++;
                else empty++;
            }

            if (ai == 3) {
                // check si ça ouvre une capture immédiate facile
                riskyMillsAI++;
            }

            if (opp == 3) {
                riskyMillsOpp++;
            }
        }

        int score = 0;



        // Très important
        score += (millsAI - millsOpp) * 5000;

        // IMPORTANT :
        // empêcher un moulin adverse
        score += (threatsAI - threatsOpp) * 2000;

        // Secondaire
        score += (pawnsAI - pawnsOpp) * 1000;

        score += deadPenalty;

        score += (mobilityAI - mobilityOpp) * 50;

        score -= riskyMillsAI * 200;
        score += riskyMillsOpp * 200;

        return score;
    }

    /** isTerminal sur un snapshot. */
    private boolean isTerminalSnap(int[] snap, int colorAI, int colorOpp, int phase) {
        int pawnsAI = 0, pawnsOpp = 0;
        for (int v : snap) {
            if (v == colorAI)  pawnsAI++;
            if (v == colorOpp) pawnsOpp++;
        }
        if (pawnsAI  < 3) return true;
        if (pawnsOpp < 3) return true;
        if (phase == MerelleStageModel.PHASE_DEPLACEMENT) {
            if (allMovesSnap(snap, colorAI).isEmpty())  return true;
            if (allMovesSnap(snap, colorOpp).isEmpty()) return true;
        }
        return false;
    }

    /** Positions libres sur un snapshot. */
    private List<Integer> allPlacementsSnap(int[] snap) {
        List<Integer> result = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++)
            if (snap[pos] == -1) result.add(pos);
        return result;
    }

    /** Déplacements valides [src, dest] pour une couleur sur un snapshot. */
    private List<int[]> allMovesSnap(int[] snap, int color) {
        List<int[]> moves = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++) {
            if (snap[pos] == color) {
                for (int adj : MerelleBoard.ADJACENCY[pos])
                    if (snap[adj] == -1) moves.add(new int[]{pos, adj});
            }
        }
        return moves;
    }

    /** Captures disponibles pour oppColor sur un snapshot. */
    private List<Integer> allCapturesSnap(int[] snap, int oppColor) {
        // Vérifie si tous les pions adverses sont en moulin
        boolean allInMills = true;
        for (int pos = 0; pos < 24; pos++) {
            if (snap[pos] == oppColor && !isInMillSnap(snap, pos, oppColor)) {
                allInMills = false;
                break;
            }
        }
        List<Integer> targets = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++) {
            if (snap[pos] == oppColor) {
                if (allInMills || !isInMillSnap(snap, pos, oppColor))
                    targets.add(pos);
            }
        }
        return targets;
    }

    /** Vérifie si une position est dans un moulin complet sur un snapshot. */
    private boolean isInMillSnap(int[] snap, int pos, int color) {
        for (int[] mill : MerelleBoard.MILLS) {
            boolean inMill = false;
            for (int p : mill) if (p == pos) { inMill = true; break; }
            if (!inMill) continue;
            boolean full = true;
            for (int p : mill) if (snap[p] != color) { full = false; break; }
            if (full) return true;
        }
        return false;
    }

    // ================================================================
    // UTILITAIRE — ANTI-REFORMATION DE MOULIN
    // ================================================================

    /**
     * Retourne true si déplacer le pion de src vers dest reformerait
     * le dernier moulin mémorisé pour ce joueur dans stageModel.
     * Travaille entièrement sur le snapshot (pas d'objet boardifier).
     *
     * Règle : l'IA peut reformer le même moulin, mais seulement après
     * en avoir formé un autre entre-temps. Ici on pénalise simplement
     * le coup si c'est exactement le dernier moulin mémorisé.
     *
     * @param snap      état du plateau avant le déplacement
     * @param src       position source du pion
     * @param dest      position destination du pion
     * @param color     couleur du joueur
     * @param playerId  index du joueur (0 ou 1)
     * @param stageModel modèle courant (pour récupérer le dernier moulin mémorisé)
     * @return true si le déplacement reformerait le même moulin qu'avant
     */
    private boolean wouldReformLastMillSnap(int[] snap, int src, int dest,
                                            int color, int playerId,
                                            MerelleStageModel stageModel) {
        for (int[] mill : MerelleBoard.MILLS) {
            // Le moulin doit contenir dest
            boolean containsDest = false;
            for (int p : mill) if (p == dest) { containsDest = true; break; }
            if (!containsDest) continue;

            // Vérifie si les 3 cases seraient toutes occupées par ce joueur après src→dest
            boolean wouldForm = true;
            for (int p : mill) {
                if (p == dest) continue; // sera occupé
                if (p == src)  { wouldForm = false; break; } // sera vide après
                if (snap[p] != color) { wouldForm = false; break; }
            }
            if (!wouldForm) continue;

            // Ce moulin se formerait : est-il le même que le dernier mémorisé ?
            if (stageModel.isSameMillAsLast(playerId, mill)) return true;
        }
        return false;
    }

    // ================================================================
    // UTILITAIRES — GÉNÉRATION DES COUPS
    // ================================================================

    /**
     * Retourne toutes les positions libres où un joueur peut placer un pion (phase 1).
     *
     * @param board état du plateau
     * @return liste des positions libres (0-23)
     */
    private List<Integer> allPlacements(MerelleBoard board) {
        List<Integer> result = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++)
            if (board.isFreeAt(pos)) result.add(pos);
        return result;
    }

    /**
     * Retourne tous les déplacements valides [src, dest] pour la couleur donnée (phase 2).
     * Un déplacement est valide si src contient un pion de la couleur et dest est libre et adjacent.
     *
     * @param board       état du plateau
     * @param playerColor couleur du joueur (constante MerellePawn.PAWN_*)
     * @return liste de tableaux [src, dest]
     */
    private List<int[]> allMoves(MerelleBoard board, int playerColor) {
        List<int[]> moves = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = board.getPawnAt(pos);
            if (pw != null && pw.getColor() == playerColor) {
                for (int adj : MerelleBoard.ADJACENCY[pos])
                    if (board.isFreeAt(adj)) moves.add(new int[]{pos, adj});
            }
        }
        return moves;
    }

    /**
     * Retourne toutes les positions adverses capturables.
     * Si tous les pions adverses sont en moulin, tous sont capturables (règle officielle).
     * Sinon, seuls les pions hors moulin sont capturables.
     *
     * @param board    état du plateau
     * @param oppColor couleur de l'adversaire (constante MerellePawn.PAWN_*)
     * @return liste des positions adverses capturables
     */
    private List<Integer> allCaptures(MerelleBoard board, int oppColor) {
        boolean allInMills = board.allPawnsInMills(oppColor);
        List<Integer> targets = new ArrayList<>();
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = board.getPawnAt(pos);
            if (pw != null && pw.getColor() == oppColor) {
                if (allInMills || !board.isInMill(pos, oppColor))
                    targets.add(pos);
            }
        }
        return targets;
    }

    // ================================================================
    // UTILITAIRES — ÉTAT TERMINAL
    // ================================================================

    /**
     * Retourne true si l'état du plateau est terminal (la partie est finie).
     * Conditions de fin : un joueur a moins de 3 pions sur le plateau,
     * ou un joueur ne peut plus bouger (bloqué) en phase 2.
     *
     * @param board    état du plateau
     * @param colorAI  couleur du joueur IA
     * @param colorOpp couleur de l'adversaire
     * @param phase    phase actuelle (PHASE_PLACEMENT ou PHASE_DEPLACEMENT)
     * @return true si la partie est terminée
     */
    private boolean isTerminal(MerelleBoard board, int colorAI, int colorOpp, int phase) {
        if (board.countPawns(colorAI)  < 3) return true;
        if (board.countPawns(colorOpp) < 3) return true;
        if (phase == MerelleStageModel.PHASE_DEPLACEMENT) {
            if (board.isBlocked(colorAI))  return true;
            if (board.isBlocked(colorOpp)) return true;
        }
        return false;
    }

    // ================================================================
    // UTILITAIRES — CONVERSION
    // ================================================================

    /**
     * Convertit une position logique (0-23) en coordonnée console (ex. "A1").
     * Lettre = ligne (A=0 à G=6), chiffre = colonne (1 à 7).
     *
     * @param pos position logique (0-23)
     * @return chaîne de 2 caractères, ex. "D4", ou "??" si pos invalide
     */
    public static String posToCoord(int pos) {
        if (pos < 0 || pos >= 24) return "??";
        int row = MerelleBoard.POS_TO_GRID[pos][0];
        int col = MerelleBoard.POS_TO_GRID[pos][1];
        return "" + (char)('A' + row) + (col + 1);
    }
}