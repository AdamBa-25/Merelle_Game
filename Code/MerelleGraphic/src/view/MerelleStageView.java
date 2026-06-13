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
 * Console view of the Nine Men's Morris game.
 *
 * Contains the inner class BoardLook which draws the 3 concentric
 * squares using double-line Unicode characters.
 *
 * BoardLook extends ContainerLook (mandatory because MerelleBoard is
 * a ContainerElement). The shape is 14 rows x 28 columns,
 * which corresponds to rowHeight=2 and colWidth=4 on a 7x7 grid
 * (7*2=14, 7*4=28). The board is drawn at the following positions:
 * ROW_MAP = { 0, 2, 4, 6, 8, 10, 12 }
 * COL_MAP = { 0, 4, 8, 12, 16, 20, 24 }
 */
public class MerelleStageView extends GameStageView {

    public MerelleStageView(String name, GameStageModel gameStageModel) {

        super(name, gameStageModel);
    }

    @Override
    public void createLooks() throws GameException {

        MerelleStageModel model = (MerelleStageModel) gameStageModel;

        // 1. Current player's name
        addLook(new TextLook(model.getPlayerName()));

        // 2. Board: 3 concentric squares (inner class below)
        addLook(new BoardLook(model.getBoard()));

        // 3. Player 1 pawns
        for (MerellePawn pawn : model.getPawnsJ1()) {
            addLook(new MerellePawnLook(pawn));
        }

        // 4. Player 2 pawns
        for (MerellePawn pawn : model.getPawnsJ2()) {
            addLook(new MerellePawnLook(pawn));
        }
    }

    /**
     * Draws the 3 concentric squares of Nine Men's Morris in a shape of
     * 14 rows x 28 columns, then overlays the pawns via renderInners().
     *
     * The size of the shape is enforced by boardifier:
     * height = nbRows * rowHeight = 7 * 2 = 14
     * width  = nbCols * colWidth  = 7 * 4 = 28
     *
     * The board nodes (corners / intersections) are placed at the positions:
     * ROW_MAP[r] = r * 2   (r from 0 to 6)
     * COL_MAP[c] = c * 4   (c from 0 to 6)
     */
    private static class BoardLook extends ContainerLook {

        private static final int[] ROW_MAP = { 0, 2, 4, 6, 8, 10, 12 };

        private static final int[] COL_MAP = { 0, 4, 8, 12, 16, 20, 24 };

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

            // rowHeight=2, colWidth=4: standard boardifier cell size.
            // depth=-1: rendered in the background, underneath other elements.
            // innersTop=1, innersLeft=3: padding offset to make room
            //   for row labels (letters A–G) and column labels (1–7).
            super(element, 2, 4, -1, 1, 3);
        }


        /**
         * Calls setSize with the real dimensions calculated by ContainerLook
         * (14x28), draws Unicode lines and nodes, then lets renderInners()
         * place the pawns inside the cells.
         */
        @Override
        protected void render() {

            // The enlarged shape: +1 row at the top (numbers), +3 columns on the left (letters)
            // Base dimensions: 7*2=14 rows, 7*4=28 columns
            // After enlargement: 15 rows, 31 columns
            setSize(getWidth(), getHeight());
            clearShape();

            drawLines();
            drawNodes();
            drawLabels();

            // Lets ContainerLook place the pawns inside the cells
            renderInners();
        }


        private void drawLabels() {

            // Numbers 1 to 7 at the top (row 0 of the shape), aligned with each column node
            for (int c = 0; c < 7; c++) {
                int sc = COL_MAP[c] + innersLeft;
                shape[0][sc] = String.valueOf(c + 1);
            }

            // Letters A to G on the left (column 0 of the shape), aligned with each row node
            for (int r = 0; r < 7; r++) {
                int sr = ROW_MAP[r] + innersTop;
                shape[sr][0] = String.valueOf((char) ('A' + r));
            }
        }

        private void drawLines() {

            // Outer square – top and bottom
            drawH(0, 0, 0, 3);   drawH(0, 3, 0, 6);
            drawH(6, 0, 6, 3);   drawH(6, 3, 6, 6);

            // Outer square – left and right
            drawV(0, 0, 3, 0);   drawV(3, 0, 6, 0);
            drawV(0, 6, 3, 6);   drawV(3, 6, 6, 6);

            // Middle square – top and bottom
            drawH(1, 1, 1, 3);   drawH(1, 3, 1, 5);
            drawH(5, 1, 5, 3);   drawH(5, 3, 5, 5);

            // Middle square – left and right
            drawV(1, 1, 3, 1);   drawV(3, 1, 5, 1);
            drawV(1, 5, 3, 5);   drawV(3, 5, 5, 5);

            // Inner square – top and bottom
            drawH(2, 2, 2, 3);   drawH(2, 3, 2, 4);
            drawH(4, 2, 4, 3);   drawH(4, 3, 4, 4);

            // Inner square – left and right
            drawV(2, 2, 3, 2);   drawV(3, 2, 4, 2);
            drawV(2, 4, 3, 4);   drawV(3, 4, 4, 4);

            // Vertical connections column D
            drawV(0, 3, 1, 3);   drawV(1, 3, 2, 3);
            drawV(4, 3, 5, 3);   drawV(5, 3, 6, 3);

            // Horizontal connections row 4
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

        private void drawNodes() {

            // { grid_row, grid_column, top, bottom, left, right }
            int[][] nodes = {

                    // Outer square
                    { 0, 0, 0, 1, 0, 1 },   // A1 ╔
                    { 0, 3, 0, 1, 1, 1 },   // D1 ╦
                    { 0, 6, 0, 1, 1, 0 },   // G1 ╗
                    { 3, 0, 1, 1, 0, 1 },   // A4 ╠
                    { 3, 6, 1, 1, 1, 0 },   // G4 ╣
                    { 6, 0, 1, 0, 0, 1 },   // A7 ╚
                    { 6, 3, 1, 0, 1, 1 },   // D7 ╩
                    { 6, 6, 1, 0, 1, 0 },   // G7 ╝

                    // Middle square
                    { 1, 1, 0, 1, 0, 1 },   // B2 ╔
                    { 1, 3, 1, 1, 1, 1 },   // D2 ╦
                    { 1, 5, 0, 1, 1, 0 },   // F2 ╗
                    { 3, 1, 1, 1, 1, 1 },   // B4 ╠
                    { 3, 5, 1, 1, 1, 1 },   // F4 ╣
                    { 5, 1, 1, 0, 0, 1 },   // B6 ╚
                    { 5, 3, 1, 1, 1, 1 },   // D6 ╩
                    { 5, 5, 1, 0, 1, 0 },   // F6 ╝

                    // Inner square
                    { 2, 2, 0, 1, 0, 1 },   // C3 ╔
                    { 2, 3, 1, 0, 1, 1 },   // D3 ╦
                    { 2, 4, 0, 1, 1, 0 },   // E3 ╗
                    { 3, 2, 1, 1, 0, 1 },   // C4 ╠
                    { 3, 4, 1, 1, 1, 0 },   // E4 ╣
                    { 4, 2, 1, 0, 0, 1 },   // C5 ╚
                    { 4, 3, 0, 1, 1, 1 },   // D5 ╩
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