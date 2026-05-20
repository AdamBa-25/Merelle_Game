package model;

import boardifier.model.GameStageModel;
import boardifier.model.Model;
import boardifier.model.StageElementsFactory;
import boardifier.model.TextElement;

/**
 * Modèle du stage de la Mérelle.
 *
 * Contient l'état complet d'une partie :
 * - le plateau (MerelleBoard)
 * - les 9 pions joueur 1 et 9 pions joueur 2
 * - la phase courante (PLACEMENT ou DEPLACEMENT)
 * - le nombre de pions encore en main (non posés en phase 1)
 * - le flag millJustFormed : une capture est en attente
 * - l'historique des derniers coups pour la règle d'égalité
 * - un TextElement affichant le nom du joueur courant
 */
public class MerelleStageModel extends GameStageModel {

    /** Phase 1 : placement des pions */
    public static final int PHASE_PLACEMENT   = 0;
    /** Phase 2 : déplacement des pions */
    public static final int PHASE_DEPLACEMENT = 1;

    /**
     * Taille de l'historique : on stocke 5 snapshots pour pouvoir comparer
     * 3 positions du MÊME joueur (indices 0, 2, 4 — espacés d'un tour chacun).
     */
    private static final int DRAW_HISTORY_SIZE = 4;

    // --- Éléments du jeu ---
    private MerelleBoard board;
    private MerellePawn[] pawnsJ1; // 9 pions du joueur 0
    private MerellePawn[] pawnsJ2; // 9 pions du joueur 1
    private TextElement playerName; // texte affiché au-dessus du plateau

    // --- État de la partie ---
    private int currentPhase;
    private int pawnsInHandJ1; // pions joueur 0 pas encore posés
    private int pawnsInHandJ2; // pions joueur 1 pas encore posés
    private boolean millJustFormed; // true si une capture est en attente
    private String[] lastMoves;     // historique des derniers coups

    /**
     * Dernier moulin formé par chaque joueur, encodé comme "pos1-pos2-pos3" trié.
     * Null si le joueur n'a pas encore formé de moulin, ou si son dernier coup n'en a pas créé.
     * Utilisé pour appliquer la règle : un joueur ne peut pas casser et reformer
     * le même moulin deux tours de suite.
     * Index 0 = joueur 0, index 1 = joueur 1.
     */
    private String[] lastMillByPlayer;

    /**
     * Constructeur appelé par StageFactory via réflexion.
     *
     * @param name  nom du stage (doit correspondre au nom enregistré dans StageFactory)
     * @param model modèle global
     */
    public MerelleStageModel(String name, Model model) {
        super(name, model);
        currentPhase   = PHASE_PLACEMENT;
        pawnsInHandJ1  = 9;
        pawnsInHandJ2  = 9;
        millJustFormed = false;
        lastMoves      = new String[DRAW_HISTORY_SIZE];
        lastMillByPlayer = new String[2]; // null par défaut (aucun moulin mémorisé)
    }

    // ===== Getters / Setters =====

    public MerelleBoard getBoard() { return board; }
    public void setBoard(MerelleBoard board) {
        this.board = board;
        addContainer(board);
    }

    public MerellePawn[] getPawnsJ1() { return pawnsJ1; }

    /**
     * Retourne la couleur du joueur 0.
     * Valeur de secours si les pions ne sont pas encore initialisés (évite un NPE).
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
     * Retourne la couleur du joueur 1.
     * Valeur de secours si les pions ne sont pas encore initialisés (évite un NPE).
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

    /** Retourne true si tous les pions des deux joueurs sont posés. */
    public boolean allPawnsPlaced() {
        return pawnsInHandJ1 == 0 && pawnsInHandJ2 == 0;
    }

    /** Nombre de pions encore en main pour le joueur donné. */
    public int getPawnsInHand(int playerId) {
        return (playerId == 0) ? pawnsInHandJ1 : pawnsInHandJ2;
    }

    /** Décrémente le compteur de pions en main après un placement. */
    public void decreasePawnsInHand(int playerId) {
        if (playerId == 0 && pawnsInHandJ1 > 0) pawnsInHandJ1--;
        else if (playerId == 1 && pawnsInHandJ2 > 0) pawnsInHandJ2--;
    }

    /**
     * Vérifie si tous les pions sont posés et passe en phase de déplacement.
     * @return true si la transition vient de se produire
     */
    public boolean checkAndTransitionToMovePhase() {
        if (currentPhase == PHASE_PLACEMENT && allPawnsPlaced()) {
            currentPhase = PHASE_DEPLACEMENT;
            return true;
        }
        return false;
    }

    /**
     * Retourne une copie de l'historique des derniers coups.
     * Utilisé par le décideur IA pour pénaliser les répétitions.
     */
    public String[] getLastMoves() {
        return lastMoves.clone();
    }

    /**
     * Enregistre le dernier coup joué pour la règle d'égalité par répétition.
     * Le playerId est préfixé dans la chaîne pour que les snapshots de deux joueurs
     * différents ne soient jamais considérés comme identiques.
     *
     * @param playerId index du joueur courant (0 ou 1)
     * @param move     description du coup (ex. "place:4", "9->10", "capture:2")
     */
    public void recordMove(int playerId, String move) {
        String snapshot = playerId + ":" + move;
        for (int i = DRAW_HISTORY_SIZE - 1; i > 0; i--) {
            lastMoves[i] = lastMoves[i - 1];
        }
        lastMoves[0] = snapshot;
    }

    /**
     * Retourne true si les deux joueurs ont répété le même coup deux fois chacun
     * (égalité par répétition sur 4 coups : lastMoves[0]==lastMoves[2] ET lastMoves[1]==lastMoves[3]).
     *
     * Avec un historique alterné J0/J1/J0/J1 :
     *   index 0 = dernier coup J0
     *   index 1 = dernier coup J1
     *   index 2 = avant-dernier coup J0
     *   index 3 = avant-dernier coup J1
     * Si [0]==[2] ET [1]==[3], les deux joueurs ont joué le même coup 2 fois de suite.
     */
    public boolean isDrawByRepetition() {
        if (lastMoves[0] == null || lastMoves[1] == null
                || lastMoves[2] == null || lastMoves[3] == null) return false;
        return lastMoves[0].equals(lastMoves[2]) && lastMoves[1].equals(lastMoves[3]);
    }

    // ===== Gestion de la règle "même moulin interdit deux tours de suite" =====

    /**
     * Mémorise le moulin que le joueur playerId vient de former.
     * Le moulin est encodé en triant ses 3 positions pour garantir une clé unique
     * quelle que soit l'ordre dans lequel les positions sont passées.
     *
     * @param playerId     index du joueur (0 ou 1)
     * @param millPositions tableau de 3 positions logiques formant le moulin
     */
    public void recordLastMill(int playerId, int[] millPositions) {
        int[] s = millPositions.clone();
        // Tri à bulles sur 3 éléments
        if (s[0] > s[1]) { int t = s[0]; s[0] = s[1]; s[1] = t; }
        if (s[1] > s[2]) { int t = s[1]; s[1] = s[2]; s[2] = t; }
        if (s[0] > s[1]) { int t = s[0]; s[0] = s[1]; s[1] = t; }
        lastMillByPlayer[playerId] = s[0] + "-" + s[1] + "-" + s[2];
    }

    /**
     * Efface le moulin mémorisé pour le joueur playerId.
     * À appeler quand le joueur joue un coup qui ne forme pas de moulin.
     *
     * @param playerId index du joueur (0 ou 1)
     */
    public void clearLastMill(int playerId) {
        lastMillByPlayer[playerId] = null;
    }

    /**
     * Retourne true si le moulin à vérifier est identique au dernier moulin
     * mémorisé pour ce joueur (règle : même moulin interdit 2 tours de suite).
     *
     * @param playerId     index du joueur (0 ou 1)
     * @param millPositions tableau de 3 positions logiques à vérifier
     */
    public boolean isSameMillAsLast(int playerId, int[] millPositions) {
        if (lastMillByPlayer[playerId] == null) return false;
        int[] s = millPositions.clone();
        if (s[0] > s[1]) { int t = s[0]; s[0] = s[1]; s[1] = t; }
        if (s[1] > s[2]) { int t = s[1]; s[1] = s[2]; s[2] = t; }
        if (s[0] > s[1]) { int t = s[0]; s[0] = s[1]; s[1] = t; }
        String key = s[0] + "-" + s[1] + "-" + s[2];
        return key.equals(lastMillByPlayer[playerId]);
    }

    @Override
    public StageElementsFactory getDefaultElementFactory() {
        return new MerelleStageFactory(this);
    }
}
