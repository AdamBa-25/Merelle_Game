package view;

import boardifier.model.ContainerElement;
import boardifier.model.GameException;
import boardifier.model.GameStageModel;
import boardifier.view.ConsoleColor;
import boardifier.view.ContainerLook;
import boardifier.view.GameStageView;
import boardifier.view.TextLook;
import model.MerelleBoard;
import model.MerellePawn;
import model.MerelleStageModel;

/**
 * Vue console du jeu de la Mérelle.
 *
 * Contient la classe interne BoardLook qui dessine les 3 carrés
 * concentriques avec des caractères Unicode double-trait.
 *
 * BoardLook étend ContainerLook (obligatoire car MerelleBoard est
 * un ContainerElement). La shape fait 14 lignes x 28 colonnes,
 * ce qui correspond à rowHeight=2 et colWidth=4 sur une grille 7x7
 * (7*2=14, 7*4=28). Le plateau est dessiné aux positions :
 *   ROW_MAP = { 0, 2, 4, 6, 8, 10, 12 }
 *   COL_MAP = { 0, 4, 8, 12, 16, 20, 24 }
 */
public class MerelleStageView extends GameStageView {

    public MerelleStageView(String name, GameStageModel gameStageModel) {

        super(name, gameStageModel);
    }

    @Override
    public void createLooks() throws GameException {

        MerelleStageModel model = (MerelleStageModel) gameStageModel;

        // 1. Nom du joueur courant
        addLook(new TextLook(model.getPlayerName()));

        // 2. Plateau : 3 carrés concentriques (classe interne ci-dessous)
        addLook(new BoardLook(model.getBoard()));

        // 3. Pions joueur 1
        for (MerellePawn pawn : model.getPawnsJ1()) {
            addLook(new MerellePawnLook(pawn));
        }

        // 4. Pions joueur 2
        for (MerellePawn pawn : model.getPawnsJ2()) {
            addLook(new MerellePawnLook(pawn));
        }
    }

    // =========================================================================
    //  Classe interne : look du plateau
    // =========================================================================

    /**
     * Dessine les 3 carrés concentriques de la Mérelle dans une shape de
     * 14 lignes x 28 colonnes, puis superpose les pions via renderInners().
     *
     * La taille de la shape est imposée par boardifier :
     *   height = nbRows * rowHeight = 7 * 2 = 14
     *   width  = nbCols * colWidth  = 7 * 4 = 28
     *
     * Les nœuds du plateau (coins / intersections) sont placés aux positions :
     *   ROW_MAP[r] = r * 2   (r de 0 à 6)
     *   COL_MAP[c] = c * 4   (c de 0 à 6)
     */
    private static class BoardLook extends ContainerLook {

        // ── Tables de correspondance grille 7x7 → shape 14x28 ─────────────────

        private static final int[] ROW_MAP = { 0, 2, 4, 6, 8, 10, 12 };

        private static final int[] COL_MAP = { 0, 4, 8, 12, 16, 20, 24 };

        // ── Caractères Unicode double-trait ───────────────────────────────────

        private static final String H  = "\u2550"; // ═
        private static final String V  = "\u2551"; // ║
        private static final String TL = "\u2554"; // ╔
        private static final String TR = "\u2557"; // ╗
        private static final String BL = "\u255A"; // ╚
        private static final String BR = "\u255D"; // ╝
        private static final String TX = "\u2566"; // ╦
        private static final String BX = "\u2569"; // ╩
        private static final String LX = "\u2560"; // ╠
        private static final String RX = "\u2563"; // ╣
        private static final String XX = "\u256C"; // ╬

        private BoardLook(ContainerElement element) {

            // rowHeight=2, colWidth=4 : taille de cellule standard boardifier.
            // depth=-1 : rendu en fond, sous les autres éléments.
            // innersTop=1, innersLeft=3 : décalage pour laisser de la place
            //   aux étiquettes de lignes (lettres A–G) et de colonnes (1–7).
            super(element, 2, 4, -1, 1, 3);
        }

        // ── Rendu ─────────────────────────────────────────────────────────────

        /**
         * Appelle setSize avec les dimensions réelles calculées par ContainerLook
         * (14x28), dessine les lignes et nœuds Unicode, puis laisse renderInners()
         * placer les pions dans les cellules.
         */
        @Override
        protected void render() {

            // La shape agrandie : +1 ligne en haut (chiffres), +3 colonnes à gauche (lettres)
            // Dimensions de base : 7*2=14 lignes, 7*4=28 colonnes
            // Après agrandissement : 15 lignes, 31 colonnes
            setSize(getWidth(), getHeight());
            clearShape();

            drawLines();
            drawNodes();
            drawLabels();

            // Laisse ContainerLook placer les pions dans les cellules
            renderInners();
        }

        // ── Étiquettes de colonnes (1–7) et de lignes (A–G) ─────────────────

        private void drawLabels() {

            // Chiffres 1 à 7 en haut (ligne 0 de la shape), alignés sur chaque nœud de colonne
            for (int c = 0; c < 7; c++) {
                int sc = COL_MAP[c] + innersLeft;
                shape[0][sc] = String.valueOf(c + 1);
            }

            // Lettres A à G à gauche (colonne 0 de la shape), alignées sur chaque nœud de ligne
            for (int r = 0; r < 7; r++) {
                int sr = ROW_MAP[r] + innersTop;
                shape[sr][0] = String.valueOf((char) ('A' + r));
            }
        }

        // ── Lignes de connexion ───────────────────────────────────────────────

        private void drawLines() {

            // Carré extérieur – haut et bas
            drawH(0, 0, 0, 3);   drawH(0, 3, 0, 6);
            drawH(6, 0, 6, 3);   drawH(6, 3, 6, 6);

            // Carré extérieur – gauche et droite
            drawV(0, 0, 3, 0);   drawV(3, 0, 6, 0);
            drawV(0, 6, 3, 6);   drawV(3, 6, 6, 6);

            // Carré moyen – haut et bas
            drawH(1, 1, 1, 3);   drawH(1, 3, 1, 5);
            drawH(5, 1, 5, 3);   drawH(5, 3, 5, 5);

            // Carré moyen – gauche et droite
            drawV(1, 1, 3, 1);   drawV(3, 1, 5, 1);
            drawV(1, 5, 3, 5);   drawV(3, 5, 5, 5);

            // Carré intérieur – haut et bas
            drawH(2, 2, 2, 3);   drawH(2, 3, 2, 4);
            drawH(4, 2, 4, 3);   drawH(4, 3, 4, 4);

            // Carré intérieur – gauche et droite
            drawV(2, 2, 3, 2);   drawV(3, 2, 4, 2);
            drawV(2, 4, 3, 4);   drawV(3, 4, 4, 4);

            // Liaisons verticales colonne D
            drawV(0, 3, 1, 3);   drawV(1, 3, 2, 3);
            drawV(4, 3, 5, 3);   drawV(5, 3, 6, 3);

            // Liaisons horizontales ligne 4
            drawH(3, 0, 3, 1);   drawH(3, 1, 3, 2);
            drawH(3, 4, 3, 5);   drawH(3, 5, 3, 6);
        }

        private void drawH(int gr, int gc1, int gr2, int gc2) {

            int sr  = ROW_MAP[gr]  + innersTop;
            int sc1 = COL_MAP[gc1] + innersLeft;
            int sc2 = COL_MAP[gc2] + innersLeft;

            for (int c = sc1 + 1; c < sc2; c++) {
                shape[sr][c] = H;
            }
        }

        private void drawV(int gr1, int gc, int gr2, int gc2) {

            int sc  = COL_MAP[gc]  + innersLeft;
            int sr1 = ROW_MAP[gr1] + innersTop;
            int sr2 = ROW_MAP[gr2] + innersTop;

            for (int r = sr1 + 1; r < sr2; r++) {
                shape[r][sc] = V;
            }
        }

        // ── Nœuds (coins et intersections) ───────────────────────────────────

        private void drawNodes() {

            // { rangée_grille, colonne_grille, haut, bas, gauche, droite }
            int[][] nodes = {

                    // Carré extérieur
                    { 0, 0, 0, 1, 0, 1 },   // A1 ╔
                    { 0, 3, 0, 1, 1, 1 },   // D1 ╦
                    { 0, 6, 0, 1, 1, 0 },   // G1 ╗
                    { 3, 0, 1, 1, 0, 1 },   // A4 ╠
                    { 3, 6, 1, 1, 1, 0 },   // G4 ╣
                    { 6, 0, 1, 0, 0, 1 },   // A7 ╚
                    { 6, 3, 1, 0, 1, 1 },   // D7 ╩
                    { 6, 6, 1, 0, 1, 0 },   // G7 ╝

                    // Carré moyen
                    { 1, 1, 0, 1, 0, 1 },   // B2 ╔
                    { 1, 3, 0, 1, 1, 1 },   // D2 ╦
                    { 1, 5, 0, 1, 1, 0 },   // F2 ╗
                    { 3, 1, 1, 1, 0, 1 },   // B4 ╠
                    { 3, 5, 1, 1, 1, 0 },   // F4 ╣
                    { 5, 1, 1, 0, 0, 1 },   // B6 ╚
                    { 5, 3, 1, 0, 1, 1 },   // D6 ╩
                    { 5, 5, 1, 0, 1, 0 },   // F6 ╝

                    // Carré intérieur
                    { 2, 2, 0, 1, 0, 1 },   // C3 ╔
                    { 2, 3, 0, 1, 1, 1 },   // D3 ╦
                    { 2, 4, 0, 1, 1, 0 },   // E3 ╗
                    { 3, 2, 1, 1, 0, 1 },   // C4 ╠
                    { 3, 4, 1, 1, 1, 0 },   // E4 ╣
                    { 4, 2, 1, 0, 0, 1 },   // C5 ╚
                    { 4, 3, 1, 0, 1, 1 },   // D5 ╩
                    { 4, 4, 1, 0, 1, 0 },   // E5 ╝
            };

            for (int[] n : nodes) {

                int sr = ROW_MAP[n[0]] + innersTop;
                int sc = COL_MAP[n[1]] + innersLeft;

                shape[sr][sc] = nodeChar(n[2], n[3], n[4], n[5]);
            }
        }

        private String nodeChar(int h, int b, int g, int d) {

            if (h==1 && b==1 && g==1 && d==1) return XX;
            if (h==0 && b==1 && g==1 && d==1) return TX;
            if (h==1 && b==0 && g==1 && d==1) return BX;
            if (h==1 && b==1 && g==0 && d==1) return LX;
            if (h==1 && b==1 && g==1 && d==0) return RX;
            if (h==0 && b==1 && g==0 && d==1) return TL;
            if (h==0 && b==1 && g==1 && d==0) return TR;
            if (h==1 && b==0 && g==0 && d==1) return BL;
            if (h==1 && b==0 && g==1 && d==0) return BR;
            return " ";
        }
    }
}
