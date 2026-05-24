package model;

import boardifier.model.GameElement;
import boardifier.model.GameStageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@DisplayName("MerelleBoard")
class MerelleBoardTest {

    @Mock
    private GameStageModel mockStage;

    private TestableBoard board;

    static class TestableBoard extends MerelleBoard {

        private final Map<Integer, MerellePawn> cells = new HashMap<>();

        TestableBoard(GameStageModel stage) {
            super(0, 0, stage);
        }

        public void addElement(GameElement element, int row, int col) {
            int pos = getLogicalPos(row, col);
            if (pos >= 0 && element instanceof MerellePawn) {
                cells.put(pos, (MerellePawn) element);
            }
        }

        public GameElement getElement(int row, int col) {
            int pos = getLogicalPos(row, col);
            return cells.getOrDefault(pos, null);
        }

        public void removeElement(GameElement element) {
            cells.values().remove(element);
        }

        public void clear() {
            cells.clear();
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        board = new TestableBoard(mockStage);
    }

    private MerellePawn pawn(int color) {
        return new MerellePawn(color, mockStage);
    }

    private void place(int color, int pos) {
        board.placePawnAt(pawn(color), pos);
    }


    @Nested
    @DisplayName("getPawnAt() / isFreeAt()")
    class PawnAccess {

        @Test
        @DisplayName("getPawnAt() returns null on an empty cell")
        void getPawnAt_emptyCell_returnsNull() {
            assertNull(board.getPawnAt(0));
        }

        @Test
        @DisplayName("getPawnAt() returns the pawn after placement")
        void getPawnAt_afterPlace_returnsPawn() {
            place(MerellePawn.PAWN_BLACK, 0);
            assertNotNull(board.getPawnAt(0));
            assertEquals(MerellePawn.PAWN_BLACK, board.getPawnAt(0).getColor());
        }

        @Test
        @DisplayName("getPawnAt() returns null for out-of-bounds position")
        void getPawnAt_outOfBounds_returnsNull() {
            assertNull(board.getPawnAt(-1));
            assertNull(board.getPawnAt(24));
        }

        @Test
        @DisplayName("isFreeAt() is true on empty cell, false after placement")
        void isFreeAt_emptyThenOccupied() {
            assertTrue(board.isFreeAt(5));
            place(MerellePawn.PAWN_RED, 5);
            assertFalse(board.isFreeAt(5));
        }

        @Test
        @DisplayName("removePawnAt() frees the cell")
        void removePawnAt_freesCell() {
            place(MerellePawn.PAWN_RED, 3);
            board.removePawnAt(3);
            assertTrue(board.isFreeAt(3));
        }
    }


    @Nested
    @DisplayName("isValidPos()")
    class ValidPos {

        @ParameterizedTest(name = "pos={0} → valid")
        @CsvSource({"0", "1", "12", "23"})
        @DisplayName("isValidPos() is true for 0 to 23")
        void isValidPos_valid(int pos) {
            assertTrue(board.isValidPos(pos));
        }

        @Test
        @DisplayName("isValidPos() is false for -1 and 24")
        void isValidPos_invalid() {
            assertFalse(board.isValidPos(-1));
            assertFalse(board.isValidPos(24));
        }
    }


    @Nested
    @DisplayName("getLogicalPos()")
    class LogicalPos {

        @Test
        @DisplayName("A1 = grid[0][0] → logical position 0")
        void logicalPos_A1() {
            assertEquals(0, board.getLogicalPos(0, 0));
        }

        @Test
        @DisplayName("G7 = grid[6][6] → logical position 23")
        void logicalPos_G7() {
            assertEquals(23, board.getLogicalPos(6, 6));
        }

        @Test
        @DisplayName("Invalid grid cell returns -1")
        void logicalPos_invalidGrid_returnsMinusOne() {
            assertEquals(-1, board.getLogicalPos(0, 1));
            assertEquals(-1, board.getLogicalPos(1, 0));
        }

        @Test
        @DisplayName("Out of bounds returns -1")
        void logicalPos_outOfBounds() {
            assertEquals(-1, board.getLogicalPos(-1, 0));
            assertEquals(-1, board.getLogicalPos(0, 7));
        }
    }


    @Nested
    @DisplayName("isAdjacent()")
    class Adjacency {

        @Test
        @DisplayName("A1(0) is adjacent to D1(1)")
        void adjacent_A1_D1() {
            assertTrue(board.isAdjacent(0, 1));
        }

        @Test
        @DisplayName("A1(0) is adjacent to A4(9)")
        void adjacent_A1_A4() {
            assertTrue(board.isAdjacent(0, 9));
        }

        @Test
        @DisplayName("Adjacency is symmetric")
        void adjacent_isSymmetric() {
            assertTrue(board.isAdjacent(0, 1));
            assertTrue(board.isAdjacent(1, 0));
        }

        @Test
        @DisplayName("A1(0) is not adjacent to G1(2)")
        void notAdjacent_A1_G1() {
            assertFalse(board.isAdjacent(0, 2));
        }

        @Test
        @DisplayName("A1(0) is not adjacent to itself")
        void notAdjacent_self() {
            assertFalse(board.isAdjacent(0, 0));
        }

        @Test
        @DisplayName("isAdjacent() is false for invalid positions")
        void notAdjacent_invalidPos() {
            assertFalse(board.isAdjacent(-1, 0));
            assertFalse(board.isAdjacent(0, 24));
        }

        @ParameterizedTest(name = "pos {0} → {1} neighbors")
        @CsvSource({
            "4, 4",
            "0, 2",
            "19, 4"
        })
        @DisplayName("Number of neighbors per position")
        void adjacencyCount(int pos, int expectedCount) {
            int count = 0;
            for (int other = 0; other < 24; other++) {
                if (board.isAdjacent(pos, other)) count++;
            }
            assertEquals(expectedCount, count);
        }
    }


    @Nested
    @DisplayName("isInMill()")
    class InMill {

        @Test
        @DisplayName("isInMill() is true when 3 same-color pawns form a mill")
        void isInMill_fullMill() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_BLACK, 1);
            place(MerellePawn.PAWN_BLACK, 2);
            assertTrue(board.isInMill(0, MerellePawn.PAWN_BLACK));
            assertTrue(board.isInMill(1, MerellePawn.PAWN_BLACK));
            assertTrue(board.isInMill(2, MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("isInMill() is false when the mill is incomplete (2 out of 3 pawns)")
        void isInMill_partialMill() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_BLACK, 1);
            assertFalse(board.isInMill(0, MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("isInMill() is false when colors are mixed on the mill")
        void isInMill_mixedColors() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_RED,   1);
            place(MerellePawn.PAWN_BLACK, 2);
            assertFalse(board.isInMill(0, MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("isInMill() is false for a position not in any mill")
        void isInMill_posNotInAnyMill() {
            assertFalse(board.isInMill(0, MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("isInMill() is false for wrong color even if pawns are physically aligned")
        void isInMill_wrongColor() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_BLACK, 1);
            place(MerellePawn.PAWN_BLACK, 2);
            assertFalse(board.isInMill(0, MerellePawn.PAWN_RED));
        }

        @Test
        @DisplayName("Verify mill B6-D6-F6 = {18,19,20}")
        void isInMill_anotherMill() {
            place(MerellePawn.PAWN_RED, 18);
            place(MerellePawn.PAWN_RED, 19);
            place(MerellePawn.PAWN_RED, 20);
            assertTrue(board.isInMill(19, MerellePawn.PAWN_RED));
        }
    }

    @Nested
    @DisplayName("countPawns()")
    class CountPawns {

        @Test
        @DisplayName("countPawns() is 0 on an empty board")
        void countPawns_emptyBoard() {
            assertEquals(0, board.countPawns(MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("countPawns() counts only pawns of the requested color")
        void countPawns_countsOnlyMatchingColor() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_BLACK, 1);
            place(MerellePawn.PAWN_RED,   2);
            assertEquals(2, board.countPawns(MerellePawn.PAWN_BLACK));
            assertEquals(1, board.countPawns(MerellePawn.PAWN_RED));
        }

        @Test
        @DisplayName("countPawns() after removing a pawn")
        void countPawns_afterRemoval() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_BLACK, 1);
            board.removePawnAt(0);
            assertEquals(1, board.countPawns(MerellePawn.PAWN_BLACK));
        }
    }

    @Nested
    @DisplayName("isBlocked()")
    class Blocked {

        @Test
        @DisplayName("isBlocked() is false on an empty board (0 pawns → not blocked)")
        void isBlocked_emptyBoard_false() {
            assertFalse(board.isBlocked(MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("isBlocked() is false if at least one pawn can move")
        void isBlocked_canMove_false() {
            place(MerellePawn.PAWN_BLACK, 0);
            assertFalse(board.isBlocked(MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("isBlocked() is true when all neighbors of all pawns are occupied")
        void isBlocked_allNeighborsOccupied_true() {
            place(MerellePawn.PAWN_BLACK, 15);
            place(MerellePawn.PAWN_RED,   11);
            place(MerellePawn.PAWN_RED,   16);
            assertTrue(board.isBlocked(MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("isBlocked() is false if one pawn can still move")
        void isBlocked_oneCanMove_false() {
            place(MerellePawn.PAWN_BLACK, 15);
            place(MerellePawn.PAWN_RED,   11);
            place(MerellePawn.PAWN_RED,   16);
            place(MerellePawn.PAWN_BLACK, 0);
            assertFalse(board.isBlocked(MerellePawn.PAWN_BLACK));
        }
    }

    @Nested
    @DisplayName("allPawnsInMills()")
    class AllInMills {

        @Test
        @DisplayName("allPawnsInMills() is false on an empty board")
        void allPawnsInMills_emptyBoard_false() {
            assertFalse(board.allPawnsInMills(MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("allPawnsInMills() is true when all pawns are in mills")
        void allPawnsInMills_allInMill_true() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_BLACK, 1);
            place(MerellePawn.PAWN_BLACK, 2);
            assertTrue(board.allPawnsInMills(MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("allPawnsInMills() is false if one pawn is outside a mill")
        void allPawnsInMills_onePawnOutside_false() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_BLACK, 1);
            place(MerellePawn.PAWN_BLACK, 2);
            place(MerellePawn.PAWN_BLACK, 9);
            assertFalse(board.allPawnsInMills(MerellePawn.PAWN_BLACK));
        }
    }
    @Nested
    @DisplayName("getMillContaining()")
    class GetMill {

        @Test
        @DisplayName("Returns null if no complete mill for this color")
        void getMillContaining_noMill_returnsNull() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_BLACK, 1);
            assertNull(board.getMillContaining(0, MerellePawn.PAWN_BLACK));
        }

        @Test
        @DisplayName("Returns mill {0,1,2} when it is complete")
        void getMillContaining_returnsMill() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_BLACK, 1);
            place(MerellePawn.PAWN_BLACK, 2);
            int[] mill = board.getMillContaining(0, MerellePawn.PAWN_BLACK);
            assertNotNull(mill);
            assertEquals(3, mill.length);
            boolean has0 = false, has1 = false, has2 = false;
            for (int p : mill) {
                if (p == 0) has0 = true;
                if (p == 1) has1 = true;
                if (p == 2) has2 = true;
            }
            assertTrue(has0 && has1 && has2);
        }

        @Test
        @DisplayName("Returns a copy (external modification has no effect)")
        void getMillContaining_returnsCopy() {
            place(MerellePawn.PAWN_BLACK, 0);
            place(MerellePawn.PAWN_BLACK, 1);
            place(MerellePawn.PAWN_BLACK, 2);
            int[] mill = board.getMillContaining(0, MerellePawn.PAWN_BLACK);
            assertNotNull(mill);
            mill[0] = 99;
            int[] mill2 = board.getMillContaining(0, MerellePawn.PAWN_BLACK);
            assertNotNull(mill2);
            assertNotEquals(99, mill2[0]);
        }
    }
}
