package model;

import boardifier.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@DisplayName("MerelleStageModel")
class MerelleStageModelTest {

    @Mock
    private Model mockModel;

    private MerelleStageModel stageModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stageModel = new MerelleStageModel("merelle", mockModel);
    }

    @Nested
    @DisplayName("Initial state")
    class InitialState {

        @Test
        @DisplayName("Initial phase is PHASE_PLACEMENT")
        void initialPhase_isPlacement() {
            assertEquals(MerelleStageModel.PHASE_PLACEMENT, stageModel.getCurrentPhase());
        }

        @Test
        @DisplayName("Each player has 9 pawns in hand at start")
        void initialPawnsInHand_is9() {
            assertEquals(9, stageModel.getPawnsInHand(0));
            assertEquals(9, stageModel.getPawnsInHand(1));
        }

        @Test
        @DisplayName("millJustFormed is false at start")
        void initialMillJustFormed_isFalse() {
            assertFalse(stageModel.isMillJustFormed());
        }

        @Test
        @DisplayName("allPawnsPlaced() is false at start (pawns in hand)")
        void initialAllPawnsPlaced_isFalse() {
            assertFalse(stageModel.allPawnsPlaced());
        }
    }

    @Nested
    @DisplayName("getColorJ1 / getColorJ2 — null guards")
    class ColorGuards {

        @Test
        @DisplayName("getColorJ1() returns PAWN_BLACK when pawnsJ1 is not initialized")
        void getColorJ1_nullPawns_returnsDefault() {
            assertEquals(MerellePawn.PAWN_BLACK, stageModel.getColorJ1());
        }

        @Test
        @DisplayName("getColorJ2() returns PAWN_RED when pawnsJ2 is not initialized")
        void getColorJ2_nullPawns_returnsDefault() {
            assertEquals(MerellePawn.PAWN_RED, stageModel.getColorJ2());
        }

        @Test
        @DisplayName("getColorJ1() returns the first pawn's color after setPawnsJ1()")
        void getColorJ1_withPawns_returnsFirstPawnColor() {
            MerellePawn[] pawns = new MerellePawn[]{
                new MerellePawn(MerellePawn.PAWN_BLUE, stageModel)
            };
            stageModel.setPawnsJ1(pawns);
            assertEquals(MerellePawn.PAWN_BLUE, stageModel.getColorJ1());
        }

        @Test
        @DisplayName("getColorJ2() returns the first pawn's color after setPawnsJ2()")
        void getColorJ2_withPawns_returnsFirstPawnColor() {
            MerellePawn[] pawns = new MerellePawn[]{
                new MerellePawn(MerellePawn.PAWN_GREEN, stageModel)
            };
            stageModel.setPawnsJ2(pawns);
            assertEquals(MerellePawn.PAWN_GREEN, stageModel.getColorJ2());
        }
    }



    @Nested
    @DisplayName("decreasePawnsInHand()")
    class DecreasePawns {

        @Test
        @DisplayName("Decrements player 0 counter")
        void decreasePawnsInHand_player0() {
            stageModel.decreasePawnsInHand(0);
            assertEquals(8, stageModel.getPawnsInHand(0));
            assertEquals(9, stageModel.getPawnsInHand(1));
        }

        @Test
        @DisplayName("Decrements player 1 counter")
        void decreasePawnsInHand_player1() {
            stageModel.decreasePawnsInHand(1);
            assertEquals(9, stageModel.getPawnsInHand(0));
            assertEquals(8, stageModel.getPawnsInHand(1));
        }

        @Test
        @DisplayName("Does not go below 0 for player 0")
        void decreasePawnsInHand_player0_doesNotGoBelowZero() {
            for (int i = 0; i < 15; i++) stageModel.decreasePawnsInHand(0);
            assertEquals(0, stageModel.getPawnsInHand(0));
        }

        @Test
        @DisplayName("Does not go below 0 for player 1")
        void decreasePawnsInHand_player1_doesNotGoBelowZero() {
            for (int i = 0; i < 15; i++) stageModel.decreasePawnsInHand(1);
            assertEquals(0, stageModel.getPawnsInHand(1));
        }

        @Test
        @DisplayName("allPawnsPlaced() is true when both counters reach 0")
        void allPawnsPlaced_trueWhenBothZero() {
            for (int i = 0; i < 9; i++) {
                stageModel.decreasePawnsInHand(0);
                stageModel.decreasePawnsInHand(1);
            }
            assertTrue(stageModel.allPawnsPlaced());
        }

        @Test
        @DisplayName("allPawnsPlaced() is false if only one player has placed all pawns")
        void allPawnsPlaced_falseIfOnlyOneEmpty() {
            for (int i = 0; i < 9; i++) stageModel.decreasePawnsInHand(0);
            assertFalse(stageModel.allPawnsPlaced());
        }
    }

    @Nested
    @DisplayName("checkAndTransitionToMovePhase()")
    class PhaseTransition {

        @Test
        @DisplayName("No transition if pawns remain in hand")
        void noTransition_whenPawnsInHand() {
            boolean transitioned = stageModel.checkAndTransitionToMovePhase();
            assertFalse(transitioned);
            assertEquals(MerelleStageModel.PHASE_PLACEMENT, stageModel.getCurrentPhase());
        }

        @Test
        @DisplayName("Transitions to PHASE_DEPLACEMENT when all pawns are placed")
        void transition_whenAllPawnsPlaced() {
            for (int i = 0; i < 9; i++) {
                stageModel.decreasePawnsInHand(0);
                stageModel.decreasePawnsInHand(1);
            }
            boolean transitioned = stageModel.checkAndTransitionToMovePhase();
            assertTrue(transitioned);
            assertEquals(MerelleStageModel.PHASE_DEPLACEMENT, stageModel.getCurrentPhase());
        }

        @Test
        @DisplayName("checkAndTransitionToMovePhase() returns false if already in PHASE_DEPLACEMENT")
        void noTransition_ifAlreadyInMovePhase() {
            stageModel.setCurrentPhase(MerelleStageModel.PHASE_DEPLACEMENT);
            boolean transitioned = stageModel.checkAndTransitionToMovePhase();
            assertFalse(transitioned);
        }
    }

    @Nested
    @DisplayName("recordMove() / isDrawByRepetition()")
    class DrawByRepetition {

        @Test
        @DisplayName("isDrawByRepetition() is false with an empty history")
        void draw_falseWithEmptyHistory() {
            assertFalse(stageModel.isDrawByRepetition());
        }

        @Test
        @DisplayName("isDrawByRepetition() is false with fewer than 4 moves")
        void draw_falseWithLessThan4Moves() {
            stageModel.recordMove(0, "9->10");
            stageModel.recordMove(1, "3->4");
            stageModel.recordMove(0, "9->10");
            assertFalse(stageModel.isDrawByRepetition());
        }

        @Test
        @DisplayName("isDrawByRepetition() is true when both players repeat the same move")
        void draw_trueWhenBothPlayersRepeat() {
            stageModel.recordMove(0, "9->10");
            stageModel.recordMove(1, "3->4");
            stageModel.recordMove(0, "9->10");
            stageModel.recordMove(1, "3->4");
            assertTrue(stageModel.isDrawByRepetition());
        }

        @Test
        @DisplayName("isDrawByRepetition() is false if only J0 repeats (J1 changes)")
        void draw_falseWhenOnlyJ0Repeats() {
            stageModel.recordMove(0, "9->10");
            stageModel.recordMove(1, "3->4");
            stageModel.recordMove(0, "9->10");
            stageModel.recordMove(1, "5->13");
            assertFalse(stageModel.isDrawByRepetition());
        }

        @Test
        @DisplayName("isDrawByRepetition() is false if only J1 repeats (J0 changes)")
        void draw_falseWhenOnlyJ1Repeats() {
            stageModel.recordMove(0, "9->10");
            stageModel.recordMove(1, "3->4");
            stageModel.recordMove(0, "10->11");
            stageModel.recordMove(1, "3->4");
            assertFalse(stageModel.isDrawByRepetition());
        }

        @Test
        @DisplayName("J0 and J1 snapshots do not mix (playerId prefix)")
        void draw_playerIdPrefixPreventsConfusion() {
            stageModel.recordMove(0, "same_move");
            stageModel.recordMove(1, "same_move");
            stageModel.recordMove(0, "same_move");
            stageModel.recordMove(1, "same_move");
            assertTrue(stageModel.isDrawByRepetition());
        }

        @Test
        @DisplayName("getLastMoves() returns a copy (external modification has no effect)")
        void getLastMoves_returnsCopy() {
            stageModel.recordMove(0, "9->10");
            String[] copy = stageModel.getLastMoves();
            copy[0] = "tampered";
            assertEquals("0:9->10", stageModel.getLastMoves()[0]);
        }
    }

    @Nested
    @DisplayName("Mill rule (recordLastMill / isSameMillAsLast / clearLastMill)")
    class MillMemory {

        @Test
        @DisplayName("isSameMillAsLast() is false if no mill has been recorded")
        void isSameMillAsLast_falseIfNone() {
            assertFalse(stageModel.isSameMillAsLast(0, new int[]{0, 1, 2}));
        }

        @Test
        @DisplayName("isSameMillAsLast() is true after recordLastMill() with the same mill")
        void isSameMillAsLast_trueAfterRecord() {
            stageModel.recordLastMill(0, new int[]{0, 1, 2});
            assertTrue(stageModel.isSameMillAsLast(0, new int[]{0, 1, 2}));
        }

        @Test
        @DisplayName("isSameMillAsLast() is order-independent")
        void isSameMillAsLast_orderIndependent() {
            stageModel.recordLastMill(0, new int[]{2, 0, 1});
            assertTrue(stageModel.isSameMillAsLast(0, new int[]{0, 1, 2}));
            assertTrue(stageModel.isSameMillAsLast(0, new int[]{1, 0, 2}));
            assertTrue(stageModel.isSameMillAsLast(0, new int[]{2, 1, 0}));
        }

        @Test
        @DisplayName("isSameMillAsLast() is false for a different mill")
        void isSameMillAsLast_falseForDifferentMill() {
            stageModel.recordLastMill(0, new int[]{0, 1, 2});
            assertFalse(stageModel.isSameMillAsLast(0, new int[]{3, 4, 5}));
        }

        @Test
        @DisplayName("clearLastMill() does NOT reset memory (intentional behavior)")
        void clearLastMill_doesNotClearMemory() {
            stageModel.recordLastMill(0, new int[]{0, 1, 2});
            stageModel.clearLastMill(0);
            assertTrue(stageModel.isSameMillAsLast(0, new int[]{0, 1, 2}));
        }

        @Test
        @DisplayName("Each player's mill memory is independent")
        void mills_areIndependentPerPlayer() {
            stageModel.recordLastMill(0, new int[]{0, 1, 2});
            stageModel.recordLastMill(1, new int[]{3, 4, 5});

            assertTrue(stageModel.isSameMillAsLast(0, new int[]{0, 1, 2}));
            assertFalse(stageModel.isSameMillAsLast(0, new int[]{3, 4, 5}));

            assertTrue(stageModel.isSameMillAsLast(1, new int[]{3, 4, 5}));
            assertFalse(stageModel.isSameMillAsLast(1, new int[]{0, 1, 2}));
        }

        @Test
        @DisplayName("recordLastMill() overwrites the previous mill")
        void recordLastMill_overwritesPrevious() {
            stageModel.recordLastMill(0, new int[]{0, 1, 2});
            stageModel.recordLastMill(0, new int[]{9, 10, 11});
            assertFalse(stageModel.isSameMillAsLast(0, new int[]{0, 1, 2}));
            assertTrue(stageModel.isSameMillAsLast(0, new int[]{9, 10, 11}));
        }
    }

    @Nested
    @DisplayName("millJustFormed")
    class MillJustFormed {

        @Test
        @DisplayName("setMillJustFormed(true) then isMillJustFormed() is true")
        void setMillJustFormed_true() {
            stageModel.setMillJustFormed(true);
            assertTrue(stageModel.isMillJustFormed());
        }

        @Test
        @DisplayName("setMillJustFormed(false) resets the flag")
        void setMillJustFormed_false() {
            stageModel.setMillJustFormed(true);
            stageModel.setMillJustFormed(false);
            assertFalse(stageModel.isMillJustFormed());
        }
    }
}
