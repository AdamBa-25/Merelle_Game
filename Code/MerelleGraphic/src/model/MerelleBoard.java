package model;

import boardifier.model.ContainerElement;
import boardifier.model.GameElement;
import boardifier.model.GameStageModel;

/**
 * Nine Men's Morris (Mérelle) board.
 *
 * The real board is a graph of 24 nodes (positions where a pawn can be placed),
 * but boardifier only handles rectangular grids. We map the 24 positions
 * onto an internal 7x7 grid: the remaining 25 cells are left empty.
 *
 * User coordinate system: letter A-G (row) + number 1-7 (column).
 */
public class MerelleBoard extends ContainerElement {

    /**
     * Main table: logical position (0-23) -> [row, col] in the 7x7 grid.
     *
     * Each row = a valid board position with its grid coordinates.
     * Used to read/write into the boardifier grid and to convert
     * a user coordinate into a grid cell.
     *
     * Full equivalences:
     * pos  0 = A1 = grid[0][0]    pos  1 = D1 = grid[0][3]    pos  2 = G1 = grid[0][6]
     * pos  3 = B2 = grid[1][1]    pos  4 = D2 = grid[1][3]    pos  5 = F2 = grid[1][5]
     * pos  6 = C3 = grid[2][2]    pos  7 = D3 = grid[2][3]    pos  8 = E3 = grid[2][4]
     * pos  9 = A4 = grid[3][0]    pos 10 = B4 = grid[3][1]    pos 11 = C4 = grid[3][2]
     * pos 12 = E4 = grid[3][4]    pos 13 = F4 = grid[3][5]    pos 14 = G4 = grid[3][6]
     * pos 15 = C5 = grid[4][2]    pos 16 = D5 = grid[4][3]    pos 17 = E5 = grid[4][4]
     * pos 18 = B6 = grid[5][1]    pos 19 = D6 = grid[5][3]    pos 20 = F6 = grid[5][5]
     * pos 21 = A7 = grid[6][0]    pos 22 = D7 = grid[6][3]    pos 23 = G7 = grid[6][6]
     */
    public static final int[][] POS_TO_GRID = {
            {0,0},{0,3},{0,6},   // pos  0= A1,  1= D1,  2= G1  (outer square top)
            {1,1},{1,3},{1,5},   // pos  3= B2,  4= D2,  5= F2  (middle square top)
            {2,2},{2,3},{2,4},   // pos  6= C3,  7= D3,  8= E3  (inner square top)
            {3,0},{3,1},{3,2},   // pos  9= A4, 10= B4, 11= C4  (middle left of the 3 squares)
            {3,4},{3,5},{3,6},   // pos 12= E4, 13= F4, 14= G4  (middle right of the 3 squares)
            {4,2},{4,3},{4,4},   // pos 15= C5, 16= D5, 17= E5  (inner square bottom)
            {5,1},{5,3},{5,5},   // pos 18= B6, 19= D6, 20= F6  (middle square bottom)
            {6,0},{6,3},{6,6}    // pos 21= A7, 22= D7, 23= G7  (outer square bottom)
    };

    /**
     * The 16 possible mills, each defined by 3 logical positions.
     * A mill is formed when all 3 positions belong to the same player.
     *
     * Outer square (4 sides):
     * {0,1,2}   = A1-D1-G1  (top side)
     * {0,9,21}  = A1-A4-A7  (left side)
     * {2,14,23} = G1-G4-G7  (right side)
     * {21,22,23}= A7-D7-G7  (bottom side)
     * Middle square (4 sides):
     * {3,4,5}   = B2-D2-F2  (top side)
     * {3,10,18} = B2-B4-B6  (left side)
     * {5,13,20} = F2-F4-F6  (right side)
     * {18,19,20}= B6-D6-F6  (bottom side)
     * Inner square (4 sides):
     * {6,7,8}   = C3-D3-E3  (top side)
     * {6,11,15} = C3-C4-C5  (left side)
     * {8,12,17} = E3-E4-E5  (right side)
     * {15,16,17}= C5-D5-E5  (bottom side)
     * Vertical links between squares (4 central lines):
     * {1,4,7}   = D1-D2-D3  (top center)
     * {9,10,11} = A4-B4-C4  (left center)
     * {12,13,14}= E4-F4-G4  (right center)
     * {16,19,22}= D5-D6-D7  (bottom center)
     */
    public static final int[][] MILLS = {
            {0,1,2}, {0,9,21}, {2,14,23}, {21,22,23},   // outer square
            {3,4,5}, {3,10,18}, {5,13,20}, {18,19,20},   // middle square
            {6,7,8}, {6,11,15}, {8,12,17}, {15,16,17},   // inner square
            {1,4,7}, {9,10,11}, {12,13,14}, {16,19,22}   // links between squares
    };

    /**
     * Adjacencies: for each logical position, the positions directly
     * accessible by a move (connected by a line on the board).
     * Used to validate moves in phase 2 and to detect blockages.
     *
     * pos  0 (A1) : D1(1), A4(9)
     * pos  1 (D1) : A1(0), G1(2), D2(4)
     * pos  2 (G1) : D1(1), G4(14)
     * pos  3 (B2) : D2(4), B4(10)
     * pos  4 (D2) : D1(1), B2(3), F2(5), D3(7)
     * pos  5 (F2) : D2(4), F4(13)
     * pos  6 (C3) : D3(7), C4(11)
     * pos  7 (D3) : D2(4), C3(6), E3(8)
     * pos  8 (E3) : D3(7), E4(12)
     * pos  9 (A4) : A1(0), B4(10), A7(21)
     * pos 10 (B4) : B2(3), A4(9), C4(11), B6(18)
     * pos 11 (C4) : C3(6), B4(10), C5(15)
     * pos 12 (E4) : E3(8), F4(13), E5(17)
     * pos 13 (F4) : F2(5), E4(12), G4(14), F6(20)
     * pos 14 (G4) : G1(2), F4(13), G7(23)
     * pos 15 (C5) : C4(11), D5(16)
     * pos 16 (D5) : C5(15), E5(17), D6(19)
     * pos 17 (E5) : E4(12), D5(16)
     * pos 18 (B6) : B4(10), D6(19)
     * pos 19 (D6) : D5(16), B6(18), F6(20), D7(22)
     * pos 20 (F6) : F4(13), D6(19)
     * pos 21 (A7) : A4(9), D7(22)
     * pos 22 (D7) : D6(19), A7(21), G7(23)
     * pos 23 (G7) : G4(14), D7(22)
     */
    public static final int[][] ADJACENCY = {
            {1,9},         // 0  = A1
            {0,2,4},       // 1  = D1
            {1,14},        // 2  = G1
            {4,10},        // 3  = B2
            {1,3,5,7},     // 4  = D2
            {4,13},        // 5  = F2
            {7,11},        // 6  = C3
            {4,6,8},       // 7  = D3
            {7,12},        // 8  = E3
            {0,10,21},     // 9  = A4
            {3,9,11,18},   // 10 = B4
            {6,10,15},     // 11 = C4
            {8,13,17},     // 12 = E4
            {5,12,14,20},  // 13 = F4
            {2,13,23},     // 14 = G4
            {11,16},       // 15 = C5
            {15,17,19},    // 16 = D5
            {12,16},       // 17 = E5
            {10,19},       // 18 = B6
            {16,18,20,22}, // 19 = D6
            {13,19},       // 20 = F6
            {9,22},        // 21 = A7
            {19,21,23},    // 22 = D7
            {14,22}        // 23 = G7
    };

    /**
     * Inverse table: gridToPos[row][col] -> logical position (-1 if invalid cell).
     * Constructed once in buildGridToPos() from POS_TO_GRID.
     * Used by getLogicalPos() to convert grid coordinates into a logical position.
     */
    private int[][] gridToPos;

    /**
     * Creates the Nine Men's Morris board.
     * Calls the ContainerElement constructor with a 7x7 grid,
     * then constructs the gridToPos inverse table.
     *
     * @param x              x position of the board in the boardifier virtual space
     * @param y              y position of the board in the boardifier virtual space
     * @param gameStageModel the stage owning this element
     */
    public MerelleBoard(int x, int y, GameStageModel gameStageModel) {
        super("merelleboard", x, y, 7, 7, gameStageModel);
        buildGridToPos();
    }

    /**
     * Constructs the inverse table: initializes everything to -1, then for each
     * logical position (0-23) writes its index into the corresponding grid cell.
     */
    private void buildGridToPos() {
        gridToPos = new int[7][7];
        for (int r = 0; r < 7; r++)
            for (int c = 0; c < 7; c++)
                gridToPos[r][c] = -1; // invalid cell by default
        for (int pos = 0; pos < 24; pos++)
            gridToPos[POS_TO_GRID[pos][0]][POS_TO_GRID[pos][1]] = pos;
    }

    /**
     * Returns the logical position (0-23) for a grid cell,
     * or -1 if the cell is not a valid board position.
     * E.g.: getLogicalPos(0,0) = 0 (A1), getLogicalPos(0,1) = -1 (empty cell).
     */
    public int getLogicalPos(int row, int col) {
        if (row < 0 || row >= 7 || col < 0 || col >= 7) return -1;
        return gridToPos[row][col];
    }

    /**
     * Returns the pawn at the given logical position, or null if empty.
     * Reads the grid cell via POS_TO_GRID then casts the GameElement into a MerellePawn.
     */
    public MerellePawn getPawnAt(int pos) {
        if (pos < 0 || pos >= 24) return null;
        GameElement el = getElement(POS_TO_GRID[pos][0], POS_TO_GRID[pos][1]);
        return (el instanceof MerellePawn) ? (MerellePawn) el : null;
    }

    /**
     * Places a pawn at the given logical position.
     * Translates the position into grid [row,col] and calls boardifier's addElement().
     */
    public void placePawnAt(MerellePawn pawn, int pos) {
        addElement(pawn, POS_TO_GRID[pos][0], POS_TO_GRID[pos][1]);
    }

    /**
     * Removes the pawn at the given logical position.
     * The pawn is removed from the grid (container = null) but remains in the stage.
     */
    public void removePawnAt(int pos) {
        MerellePawn pawn = getPawnAt(pos);
        if (pawn != null) removeElement(pawn);
    }

    /** Returns true if no pawn occupies the given logical position. */
    public boolean isFreeAt(int pos) {
        return getPawnAt(pos) == null;
    }

    /** Returns true if pos is a valid position index (0 to 23). */
    public boolean isValidPos(int pos) {
        return pos >= 0 && pos < 24;
    }

    /**
     * Returns true if src and dest are connected by a line on the board.
     * Iterates through ADJACENCY[src] and searches for dest inside.
     * Used to validate a move in phase 2.
     */
    public boolean isAdjacent(int src, int dest) {
        if (!isValidPos(src) || !isValidPos(dest)) return false;
        for (int adj : ADJACENCY[src])
            if (adj == dest) return true;
        return false;
    }

    /**
     * Returns true if the position pos is part of a complete mill of the color playerColor.
     *
     * IMPORTANT: playerColor is a pawn color (MerellePawn.PAWN_BLACK, PAWN_RED, etc.),
     * not a player index (0 or 1). The two coincide when playing Black/Red by default,
     * but diverge as soon as players choose other colors.
     *
     * @param pos         logical position (0-23) to check
     * @param playerColor player color (MerellePawn.PAWN_* constant)
     */
    public boolean isInMill(int pos, int playerColor) {
        for (int[] mill : MILLS) {
            // Checks if pos belongs to this mill
            boolean posInMill = false;
            for (int p : mill) if (p == pos) { posInMill = true; break; }
            if (!posInMill) continue;
            // Checks if all 3 cells are occupied by the same color
            boolean allSame = true;
            for (int p : mill) {
                MerellePawn pw = getPawnAt(p);
                if (pw == null || pw.getColor() != playerColor) { allSame = false; break; }
            }
            if (allSame) return true;
        }
        return false;
    }

    // checkMillFormed() removed: it was an exact alias of isInMill().
    // Use isInMill(pos, playerColor) directly in the controller instead.

    /**
     * Counts and returns the number of pawns of color playerColor on the board.
     * Used to detect the loss condition (fewer than 3 pawns).
     *
     * @param playerColor player color (MerellePawn.PAWN_* constant)
     */
    public int countPawns(int playerColor) {
        int count = 0;
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = getPawnAt(pos);
            if (pw != null && pw.getColor() == playerColor) count++;
        }
        return count;
    }

    /**
     * Returns true if the player of color playerColor can no longer move any pawn.
     * For each pawn of this color, checks if at least one adjacent cell is free.
     * As soon as a free cell is found, returns false (not blocked).
     * Returns false if the player has no pawns: game over is then handled
     * by the "fewer than 3 pawns" condition, not by blockage.
     *
     * @param playerColor player color (MerellePawn.PAWN_* constant)
     */
    public boolean isBlocked(int playerColor) {
        boolean hasPawn = false;
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = getPawnAt(pos);
            if (pw != null && pw.getColor() == playerColor) {
                hasPawn = true;
                for (int adj : ADJACENCY[pos])
                    if (isFreeAt(adj)) return false; // this pawn can move → not blocked
            }
        }
        // No pawns on the board → defeat by "fewer than 3 pawns", not by blockage
        if (!hasPawn) return false;
        return true; // pawns exist but none can move
    }

    /**
     * Returns true if all pawns of color playerColor are in mills.
     * Special case: if true, the rules allow capturing a pawn inside a mill.
     * Returns false if the player has no pawns (no pawns = not "all in mills").
     *
     * @param playerColor player color (MerellePawn.PAWN_* constant)
     */
    public boolean allPawnsInMills(int playerColor) {
        boolean found = false;
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = getPawnAt(pos);
            if (pw != null && pw.getColor() == playerColor) {
                found = true;
                // If a pawn of this color is outside a mill → not all in mills
                if (!isInMill(pos, playerColor)) return false;
            }
        }
        // If no pawns found, return false (degenerate case, should not happen in normal gameplay)
        return found;
    }

    /**
     * Returns the first complete mill of playerColor containing position pos,
     * as an array of 3 logical positions.
     * Returns null if pos is not part of any complete mill of this color.
     * Used by the controller to remember the formed mill ("same mill forbidden for 2 turns" rule).
     *
     * @param pos         logical position (0-23) to check
     * @param playerColor player color (MerellePawn.PAWN_* constant)
     * @return array of 3 positions or null
     */
    public int[] getMillContaining(int pos, int playerColor) {
        for (int[] mill : MILLS) {
            boolean posInMill = false;
            for (int p : mill) if (p == pos) { posInMill = true; break; }
            if (!posInMill) continue;
            boolean allSame = true;
            for (int p : mill) {
                MerellePawn pw = getPawnAt(p);
                if (pw == null || pw.getColor() != playerColor) { allSame = false; break; }
            }
            if (allSame) return mill.clone();
        }
        return null;
    }
}