package view;

import boardifier.model.GameElement;
import boardifier.view.ConsoleColor;
import boardifier.view.ElementLook;
import model.MerellePawn;

/**
 * Console look of a Nine Men's Morris pawn.
 *
 * Displays a single colored character in shape[0][0]:
 * text color + background color + pawn letter + RESET
 *
 * This look is used for pawns in the reserve (not yet placed on the board).
 * Placed pawns are drawn directly by BoardLook in MerelleStageView.
 */
public class MerellePawnLook extends ElementLook {

    public MerellePawnLook(GameElement element) {

        super(element, 1, 1);
    }

    @Override
    protected void render() {

        MerellePawn pawn = (MerellePawn) element;

        shape[0][0] = pawn.getTextColor() + pawn.getBackgroundColor() + pawn.getSymbol() + ConsoleColor.RESET;
    }
}