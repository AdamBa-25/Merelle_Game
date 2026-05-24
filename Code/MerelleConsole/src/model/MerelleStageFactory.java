package model;

import boardifier.model.GameStageModel;
import boardifier.model.StageElementsFactory;
import boardifier.model.TextElement;

/**
 * Factory for the Nine Men's Morris (Mérelle) stage.
 *
 * Creates and registers all game elements within the stage:
 * - the board (MerelleBoard) at position (0,1) in the virtual space
 * - a TextElement for the current player's name at (0,0)
 * - 9 pawns for player 0 and 9 pawns for player 1 with the chosen colors
 *
 * Colors are transmitted via colorJ1 / colorJ2 before launching.
 * Pawns are not placed on the board upon creation.
 */
public class MerelleStageFactory extends StageElementsFactory {

    /**
     * Colors chosen by the players.
     * Private to prevent any uncontrolled modification from the outside.
     * To be initialized via setColors() from Merelle.java before launching.
     * Default values: Black for P1, Red for P2.
     */
    private static int colorJ1 = MerellePawn.PAWN_BLACK;
    private static int colorJ2 = MerellePawn.PAWN_RED;

    /**
     * Initializes the colors of both players before the stage starts.
     * To be called from Merelle.java after the colors have been chosen.
     *
     * @param c1 color of player 0 (MerellePawn.PAWN_* constant)
     * @param c2 color of player 1 (MerellePawn.PAWN_* constant)
     */
    public static void setColors(int c1, int c2) {
        colorJ1 = c1;
        colorJ2 = c2;
    }

    private MerelleStageModel stageModel;

    /**
     * @param gameStageModel the stage to initialize (cast to MerelleStageModel)
     */
    public MerelleStageFactory(GameStageModel gameStageModel) {
        super(gameStageModel);
        stageModel = (MerelleStageModel) gameStageModel;
    }

    /**
     * Creates all game elements and assigns them to the stage model.
     * This method is called automatically by GameStageModel.createElements().
     */
    @Override
    public void setup() {

        // Safety check: invalid colors → fallback to default values
        if (!MerellePawn.isValidColor(colorJ1)) colorJ1 = MerellePawn.PAWN_BLACK;
        if (!MerellePawn.isValidColor(colorJ2)) colorJ2 = MerellePawn.PAWN_RED;

        // Safety check: identical colors → search for the first different color for P2
        if (colorJ1 == colorJ2) {
            for (int i = 0; i < MerellePawn.NB_COLORS; i++) {
                if (i != colorJ1) {
                    colorJ2 = i;
                    break;
                }
            }
        }

        // --- TextElement : current player's name ---
        TextElement text = new TextElement(stageModel.getCurrentPlayerName(), stageModel);
        text.setLocation(0, 0);
        stageModel.setPlayerName(text);

        // --- Game board at position (0,1) in the virtual space ---
        MerelleBoard board = new MerelleBoard(0, 1, stageModel);
        stageModel.setBoard(board);

        // --- 9 pawns for player 0 ---
        // The pawns are set to invisible in the reserve so they do not
        // overwrite the current player name display (position (0,0)).
        MerellePawn[] pawnsJ1 = new MerellePawn[9];
        for (int i = 0; i < 9; i++) {
            pawnsJ1[i] = new MerellePawn(colorJ1, stageModel);
            pawnsJ1[i].setVisible(false);
        }
        stageModel.setPawnsJ1(pawnsJ1);

        // --- 9 pawns for player 1 ---
        MerellePawn[] pawnsJ2 = new MerellePawn[9];
        for (int i = 0; i < 9; i++) {
            pawnsJ2[i] = new MerellePawn(colorJ2, stageModel);
            pawnsJ2[i].setVisible(false);
        }
        stageModel.setPawnsJ2(pawnsJ2);
    }
}