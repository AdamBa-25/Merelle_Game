package view;

import boardifier.model.GameElement;
import boardifier.view.ConsoleColor;
import boardifier.view.ElementLook;
import model.MerellePawn;

/**
 * Look console d'un pion de la Mérelle.

 * Affiche un seul caractère coloré dans shape[0][0] :
 *   couleur texte + couleur fond + lettre du pion + RESET

 * Ce look sert pour les pions en réserve (pas encore posés sur le plateau).
 * Les pions posés sont dessinés directement par BoardLook dans MerelleStageView.
 */
public class MerellePawnLook extends ElementLook {

    public MerellePawnLook(GameElement element) {

        super(element, 1, 1);
    }

    @Override
    protected void render() {

        MerellePawn pawn = (MerellePawn) element;

        shape[0][0] = pawn.getTextColor()
                + pawn.getBackgroundColor()
                + pawn.getSymbol()
                + ConsoleColor.RESET;
    }
}
