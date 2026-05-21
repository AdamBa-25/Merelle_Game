package model;

import boardifier.model.GameStageModel;
import boardifier.model.StageElementsFactory;
import boardifier.model.TextElement;

/**
 * Factory du stage de la Mérelle.
 *
 * Crée et enregistre dans le stage tous les éléments du jeu :
 * - le plateau (MerelleBoard) en position (0,1) dans l'espace virtuel
 * - un TextElement pour le nom du joueur courant en (0,0)
 * - 9 pions joueur 0 et 9 pions joueur 1 avec les couleurs choisies
 *
 * Les couleurs sont transmises via colorJ1 / colorJ2 avant le lancement.
 * Les pions ne sont pas placés sur le plateau à la création.
 */
public class MerelleStageFactory extends StageElementsFactory {

    /**
     * Couleurs choisies par les joueurs.
     * Privées pour éviter toute modification non contrôlée depuis l'extérieur.
     * À initialiser via setColors() depuis Merelle.java avant le lancement.
     * Valeurs par défaut : Noir pour J1, Rouge pour J2.
     */
    private static int colorJ1 = MerellePawn.PAWN_BLACK;
    private static int colorJ2 = MerellePawn.PAWN_RED;

    /**
     * Initialise les couleurs des deux joueurs avant le démarrage du stage.
     * À appeler depuis Merelle.java après le choix des couleurs.
     *
     * @param c1 couleur du joueur 0 (constante MerellePawn.PAWN_*)
     * @param c2 couleur du joueur 1 (constante MerellePawn.PAWN_*)
     */
    public static void setColors(int c1, int c2) {
        colorJ1 = c1;
        colorJ2 = c2;
    }

    private MerelleStageModel stageModel;

    /**
     * @param gameStageModel le stage à initialiser (cast en MerelleStageModel)
     */
    public MerelleStageFactory(GameStageModel gameStageModel) {
        super(gameStageModel);
        stageModel = (MerelleStageModel) gameStageModel;
    }

    /**
     * Crée tous les éléments du jeu et les assigne au stage model.
     * Cette méthode est appelée automatiquement par GameStageModel.createElements().
     */
    @Override
    public void setup() {

        // Sécurité : couleurs invalides → valeurs par défaut
        if (!MerellePawn.isValidColor(colorJ1)) colorJ1 = MerellePawn.PAWN_BLACK;
        if (!MerellePawn.isValidColor(colorJ2)) colorJ2 = MerellePawn.PAWN_RED;

        // Sécurité : couleurs identiques → cherche la première couleur différente pour J2
        if (colorJ1 == colorJ2) {
            for (int i = 0; i < MerellePawn.NB_COLORS; i++) {
                if (i != colorJ1) {
                    colorJ2 = i;
                    break;
                }
            }
        }

        // --- TextElement : nom du joueur courant ---
        TextElement text = new TextElement(stageModel.getCurrentPlayerName(), stageModel);
        text.setLocation(0, 0);
        stageModel.setPlayerName(text);

        // --- Plateau de jeu en position (0,1) dans l'espace virtuel ---
        MerelleBoard board = new MerelleBoard(0, 1, stageModel);
        stageModel.setBoard(board);

        // --- 9 pions joueur 0 ---
        // Les pions sont rendus invisibles en réserve pour ne pas
        // écraser l'affichage du nom du joueur courant (position (0,0)).
        MerellePawn[] pawnsJ1 = new MerellePawn[9];
        for (int i = 0; i < 9; i++) {
            pawnsJ1[i] = new MerellePawn(colorJ1, stageModel);
            pawnsJ1[i].setVisible(false);
        }
        stageModel.setPawnsJ1(pawnsJ1);

        // --- 9 pions joueur 1 ---
        MerellePawn[] pawnsJ2 = new MerellePawn[9];
        for (int i = 0; i < 9; i++) {
            pawnsJ2[i] = new MerellePawn(colorJ2, stageModel);
            pawnsJ2[i].setVisible(false);
        }
        stageModel.setPawnsJ2(pawnsJ2);
    }
}