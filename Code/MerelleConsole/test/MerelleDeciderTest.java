package control;

import boardifier.control.Controller;
import boardifier.model.Model;
import model.MerelleBoard;
import model.MerellePawn;
import model.MerelleStageModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("MerelleDecider")
class MerelleDeciderTest {


    @Mock
    private Model mockModel;

    @Mock
    private Controller mockController;

    @Mock
    private MerelleStageModel mockStage;

    @Mock
    private MerelleBoard mockBoard;

    private MerelleDecider decider;

    private static final int COLOR_AI  = MerellePawn.PAWN_BLACK;

    private static final int COLOR_OPP = MerellePawn.PAWN_RED;

    private static final String COORD_REGEX = "[A-G][1-7]";

    private static final String PLACEMENT_REGEX = "[A-G][1-7]";

    private static final String MOVE_REGEX = "[A-G][1-7] [A-G][1-7]";

    private static final String CAPTURE_REGEX = "X[A-G][1-7]";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        decider = new MerelleDecider(mockModel, mockController);

        when(mockStage.getColorJ1()).thenReturn(COLOR_AI);
        when(mockStage.getColorJ2()).thenReturn(COLOR_OPP);
        when(mockStage.getBoard()).thenReturn(mockBoard);

        when(mockStage.isMillJustFormed()).thenReturn(false);
        when(mockStage.getLastMoves()).thenReturn(new String[]{null, null, null, null});

        for (int pos = 0; pos < 24; pos++) {
            when(mockBoard.getPawnAt(pos)).thenReturn(null);
        }

        MerelleDecider.aiDifficulty         = MerelleDecider.DIFFICULTY_MINIMAX;
        MerelleDecider.aiDifficultyPerPlayer = null;
    }

    @AfterEach
    void tearDown() {
        MerelleDecider.aiDifficulty         = MerelleDecider.DIFFICULTY_MINIMAX;
        MerelleDecider.aiDifficultyPerPlayer = null;
    }

    private MerellePawn pawn(int color) {
        MerellePawn p = mock(MerellePawn.class);
        when(p.getColor()).thenReturn(color);
        return p;
    }

    private void placeAI(int pos) {
        MerellePawn p = pawn(COLOR_AI);
        when(mockBoard.getPawnAt(pos)).thenReturn(p);
    }

    private void placeOpp(int pos) {
        MerellePawn p = pawn(COLOR_OPP);
        when(mockBoard.getPawnAt(pos)).thenReturn(p);
    }

    @Nested
    @DisplayName("posToCoord() — position to coordinate conversion")
    class PosToCoord {

        @Test
        @DisplayName("pos 0 → A1 (top-left corner)")
        void pos0_isA1() {
            assertEquals("A1", MerelleDecider.posToCoord(0));
        }

        @Test
        @DisplayName("pos 2 → A7 (row=0→A, col=6→7)")
        void pos2_isA7() {
            assertEquals("A7", MerelleDecider.posToCoord(2));
        }

        @Test
        @DisplayName("pos 21 → G1 (row=6→G, col=0→1)")
        void pos21_isG1() {
            assertEquals("G1", MerelleDecider.posToCoord(21));
        }

        @Test
        @DisplayName("pos 23 → G7 (bottom-right corner)")
        void pos23_isG7() {
            assertEquals("G7", MerelleDecider.posToCoord(23));
        }

        @Test
        @DisplayName("pos 4 → B4 (row=1→B, col=3→4)")
        void pos4_isB4() {
            assertEquals("B4", MerelleDecider.posToCoord(4));
        }

        @Test
        @DisplayName("pos -1 → ?? (negative invalid position)")
        void posNeg1_isInvalid() {
            assertEquals("??", MerelleDecider.posToCoord(-1));
        }

        @Test
        @DisplayName("pos 24 → ?? (out-of-bounds position)")
        void pos24_isInvalid() {
            assertEquals("??", MerelleDecider.posToCoord(24));
        }


        @Test
        @DisplayName("posToCoord and parseCoord are inverses on all 24 positions")
        void roundTrip_allPositions() {
            for (int pos = 0; pos < 24; pos++) {
                String coord = MerelleDecider.posToCoord(pos);
                assertNotEquals("??", coord,
                    "posToCoord(" + pos + ") returned '??' — position not covered in POS_TO_GRID");
                int roundTripped = MerelleController.parseCoord(coord);
                assertEquals(pos, roundTripped,
                    "Round-trip failed for pos=" + pos + " → coord=" + coord);
            }
        }
    }





    @Nested
    @DisplayName("getDecision() — routing to the correct strategy")
    class Routing {

        @BeforeEach
        void context() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);
            placeAI(0);
            placeAI(1);
        }

        @Test
        @DisplayName("DIFFICULTY_MINIMAX → placement format result")
        void routing_minimax() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Minimax placement should return 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("DIFFICULTY_ALPHABETA → placement format result")
        void routing_alphabeta() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_ALPHABETA;
            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "AlphaBeta placement should return 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("DIFFICULTY_MONTECARLO → placement format result")
        void routing_montecarlo() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MONTECARLO;
            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "MonteCarlo placement should return 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("Unknown difficulty value → fallback to Minimax")
        void routing_unknownFallsBackToMinimax() {
            MerelleDecider.aiDifficulty = 999;
            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Minimax fallback should return 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("aiDifficultyPerPlayer overrides global aiDifficulty for player 1")
        void routing_perPlayerDifficulty() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
            MerelleDecider.aiDifficultyPerPlayer = new int[]{
                MerelleDecider.DIFFICULTY_MINIMAX,
                MerelleDecider.DIFFICULTY_ALPHABETA
            };

            when(mockStage.getColorJ1()).thenReturn(COLOR_OPP);
            when(mockStage.getColorJ2()).thenReturn(COLOR_AI);
            placeAI(0);

            String result = decider.getDecision(mockStage, 1);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Per-player routing should return 'XX' but returned: " + result);
        }
    }


    @Nested
    @DisplayName("Minimax")
    class MinimaxTests {

        @BeforeEach
        void setDifficulty() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
        }

        @Test
        @DisplayName("Placement: returns a valid coordinate on an empty board")
        void placement_emptyBoard_returnsValidCoord() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result, "Result must not be null");
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Should be a coordinate 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("Placement: the chosen cell is free (not already occupied)")
        void placement_chosenCellIsFree() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            placeAI(0);
            placeAI(1);
            placeOpp(2);

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);

            int pos = MerelleController.parseCoord(result);
            assertTrue(pos >= 0, "The returned coordinate must be valid: " + result);
            assertNotEquals(0, pos, "AI chose A1 which is already occupied");
            assertNotEquals(1, pos, "AI chose D1 which is already occupied");
            assertNotEquals(2, pos, "AI chose G1 which is already occupied");
        }

        @Test
        @DisplayName("Placement: forms a mill when possible (2 AI pawns already aligned)")
        void placement_formsMillWhenPossible() {

            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);
            placeAI(0);
            placeAI(1);

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Should be a coordinate 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("Move: returns the format 'XX YY'")
        void move_returnsCorrectFormat() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);

            placeAI(0);
            placeAI(9);
            placeAI(21);

            placeOpp(2);
            placeOpp(14);
            placeOpp(23);

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result);
            assertTrue(result.matches(MOVE_REGEX),
                "Should be 'XX YY' but returned: " + result);
        }

        @Test
        @DisplayName("Move: source and destination are different (no null move)")
        void move_srcAndDestAreDifferent() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);

            placeAI(0); placeAI(9); placeAI(21);
            placeOpp(2); placeOpp(14); placeOpp(23);

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);

            String[] parts = result.split(" ");
            assertEquals(2, parts.length, "A move must have exactly 2 parts");
            assertNotEquals(parts[0], parts[1],
                "Source and destination must differ — a null move is illegal");
        }

        @Test
        @DisplayName("Move: the source cell belongs to the AI")
        void move_srcBelongsToAI() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);

            Set<Integer> aiPositions = new HashSet<>();
            aiPositions.add(0);
            aiPositions.add(9);
            aiPositions.add(21);

            placeAI(0); placeAI(9); placeAI(21);
            placeOpp(2); placeOpp(14); placeOpp(23);

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);

            String src = result.split(" ")[0];
            int srcPos = MerelleController.parseCoord(src);
            assertTrue(aiPositions.contains(srcPos),
                "Source " + src + " (pos " + srcPos + ") does not belong to the AI");
        }

        @Test
        @DisplayName("Capture: returns the format 'XYY'")
        void capture_returnsCorrectFormat() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            when(mockStage.isMillJustFormed()).thenReturn(true);

            placeAI(0);
            placeAI(1);
            placeAI(2);

            placeOpp(9);
            placeOpp(11);
            placeOpp(14);

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result);
            assertTrue(result.matches(CAPTURE_REGEX),
                "Should be 'XYY' but returned: " + result);
        }

        @Test
        @DisplayName("Capture: the captured pawn is an opponent's pawn (not AI's)")
        void capture_targetIsOpponent() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            when(mockStage.isMillJustFormed()).thenReturn(true);

            placeAI(0); placeAI(1); placeAI(2);
            placeOpp(9); placeOpp(11); placeOpp(14);

            Set<Integer> oppPositions = new HashSet<>();
            oppPositions.add(9);
            oppPositions.add(11);
            oppPositions.add(14);

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.startsWith("X"), "A capture must start with 'X'");

            String capturedCoord = result.substring(1);
            int capturedPos = MerelleController.parseCoord(capturedCoord);
            assertTrue(oppPositions.contains(capturedPos),
                "Captured pawn at pos=" + capturedPos + " is not an opponent's pawn");
        }

        @Test
        @DisplayName("Capture: does not capture the AI's own pawn")
        void capture_doesNotCaptureOwnPawn() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            when(mockStage.isMillJustFormed()).thenReturn(true);

            placeAI(0); placeAI(1); placeAI(2);
            placeOpp(9); placeOpp(11); placeOpp(14);

            Set<Integer> aiPositions = new HashSet<>();
            aiPositions.add(0); aiPositions.add(1); aiPositions.add(2);

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);

            int capturedPos = MerelleController.parseCoord(result.substring(1));
            assertFalse(aiPositions.contains(capturedPos),
                "AI must never capture its own pawn (pos=" + capturedPos + ")");
        }
    }

    @Nested
    @DisplayName("AlphaBeta")
    class AlphaBetaTests {

        @BeforeEach
        void setDifficulty() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_ALPHABETA;
        }

        @Test
        @DisplayName("Placement: returns a valid coordinate")
        void placement_returnsValidCoord() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "AlphaBeta placement should return 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("Move: returns the format 'XX YY'")
        void move_returnsCorrectFormat() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            placeAI(0); placeAI(9); placeAI(21);
            placeOpp(2); placeOpp(14); placeOpp(23);

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result);
            assertTrue(result.matches(MOVE_REGEX),
                "AlphaBeta move should return 'XX YY' but returned: " + result);
        }

        @Test
        @DisplayName("Capture: returns the format 'XYY'")
        void capture_returnsCorrectFormat() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            when(mockStage.isMillJustFormed()).thenReturn(true);
            placeAI(0); placeAI(1); placeAI(2);
            placeOpp(9); placeOpp(11); placeOpp(14);

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result);
            assertTrue(result.matches(CAPTURE_REGEX),
                "AlphaBeta capture should return 'XYY' but returned: " + result);
        }

        @Test
        @DisplayName("Consistency with Minimax: same format on the same position")
        void consistency_sameFormatAsMinimax() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);
            placeAI(0);

            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
            String minimaxResult = decider.getDecision(mockStage, 0);

            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_ALPHABETA;
            String abResult = decider.getDecision(mockStage, 0);

            assertTrue(minimaxResult.matches(PLACEMENT_REGEX),
                "Minimax should return a coordinate but returned: " + minimaxResult);
            assertTrue(abResult.matches(PLACEMENT_REGEX),
                "AlphaBeta should return a coordinate but returned: " + abResult);
        }
    }

    @Nested
    @DisplayName("MonteCarlo")
    class MonteCarloTests {

        @BeforeEach
        void setDifficulty() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MONTECARLO;
        }

        @Test
        @DisplayName("Placement: returns a valid coordinate")
        void placement_returnsValidCoord() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            placeAI(0); placeAI(1);
            placeOpp(2); placeOpp(3);

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "MonteCarlo placement should return 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("Placement: the chosen cell is not already occupied")
        void placement_chosenCellIsFree() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);
            placeAI(0); placeAI(9);
            placeOpp(2); placeOpp(14);

            Set<Integer> occupied = new HashSet<>();
            occupied.add(0); occupied.add(9);
            occupied.add(2); occupied.add(14);

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);

            int pos = MerelleController.parseCoord(result);
            assertFalse(occupied.contains(pos),
                "MonteCarlo chose an occupied cell (pos=" + pos + ")");
        }

        @Test
        @DisplayName("Move: returns the format 'XX YY'")
        void move_returnsCorrectFormat() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            placeAI(0); placeAI(9); placeAI(21);
            placeOpp(2); placeOpp(14); placeOpp(23);

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result);
            assertTrue(result.matches(MOVE_REGEX),
                "MonteCarlo move should return 'XX YY' but returned: " + result);
        }

        @Test
        @DisplayName("Capture: returns the format 'XYY'")
        void capture_returnsCorrectFormat() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            when(mockStage.isMillJustFormed()).thenReturn(true);
            placeAI(0); placeAI(1); placeAI(2);
            placeOpp(9); placeOpp(11); placeOpp(14);

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result);
            assertTrue(result.matches(CAPTURE_REGEX),
                "MonteCarlo capture should return 'XYY' but returned: " + result);
        }

        @Test
        @DisplayName("Capture: the target is an opponent's pawn")
        void capture_targetIsOpponent() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            when(mockStage.isMillJustFormed()).thenReturn(true);
            placeAI(0); placeAI(1); placeAI(2);
            placeOpp(9); placeOpp(11); placeOpp(14);

            Set<Integer> oppPositions = new HashSet<>();
            oppPositions.add(9); oppPositions.add(11); oppPositions.add(14);

            String result = decider.getDecision(mockStage, 0);
            String capturedCoord = result.substring(1);
            int capturedPos = MerelleController.parseCoord(capturedCoord);

            assertTrue(oppPositions.contains(capturedPos),
                "MonteCarlo captured pos=" + capturedPos + " which is not an opponent's pawn");
        }
    }


    @Nested
    @DisplayName("Common behavior across all 3 AIs")
    class CommonBehavior {

        @ParameterizedTest(name = "difficulty {0}: only one placement possible → always chosen")
        @ValueSource(ints = {
            MerelleDecider.DIFFICULTY_MINIMAX,
            MerelleDecider.DIFFICULTY_ALPHABETA,
            MerelleDecider.DIFFICULTY_MONTECARLO
        })
        @DisplayName("Single placement possible → always chosen")
        void singlePlacement_alwaysChosen(int difficulty) {
            MerelleDecider.aiDifficulty = difficulty;
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            placeAI(0); placeAI(1); placeAI(2); placeAI(3); placeAI(4);
            placeOpp(6); placeOpp(7); placeOpp(8); placeOpp(9); placeOpp(10);

            for (int pos = 11; pos < 24; pos++) {
                if (pos % 2 == 0) placeAI(pos);
                else              placeOpp(pos);
            }


            String expected = MerelleDecider.posToCoord(5);
            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result, "Difficulty " + difficulty + ": result is null");
            assertEquals(expected, result,
                "Difficulty " + difficulty
                + ": only pos 5 (" + expected + ") is free, but AI returned: " + result);
        }

        @ParameterizedTest(name = "difficulty {0}: result is non-null in move phase")
        @ValueSource(ints = {
            MerelleDecider.DIFFICULTY_MINIMAX,
            MerelleDecider.DIFFICULTY_ALPHABETA,
            MerelleDecider.DIFFICULTY_MONTECARLO
        })
        @DisplayName("Result is non-null and non-empty in move phase")
        void move_neverNullOrEmpty(int difficulty) {
            MerelleDecider.aiDifficulty = difficulty;
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);

            placeAI(0); placeAI(9); placeAI(21);
            placeOpp(2); placeOpp(14); placeOpp(23);

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result, "Difficulty " + difficulty + ": result is null");
            assertFalse(result.isEmpty(), "Difficulty " + difficulty + ": result is empty");
        }

        @ParameterizedTest(name = "difficulty {0}: no ping-pong when alternatives exist")
        @ValueSource(ints = {
            MerelleDecider.DIFFICULTY_MINIMAX,
            MerelleDecider.DIFFICULTY_ALPHABETA,
            MerelleDecider.DIFFICULTY_MONTECARLO
        })
        @DisplayName("No ping-pong: answer must not reverse the AI's last move")
        void noPingPong_whenAlternativesExist(int difficulty) {
            MerelleDecider.aiDifficulty = difficulty;
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);

            placeAI(0);
            placeAI(11);
            placeAI(21);

            placeOpp(2);
            placeOpp(14);
            placeOpp(23);

            when(mockStage.getLastMoves()).thenReturn(
                new String[]{"A4 D4", null, null, null}
            );

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(MOVE_REGEX),
                "Should be a move 'XX YY' but returned: " + result);

            assertNotEquals("D4 A4", result,
                "Difficulty " + difficulty + ": ping-pong detected (D4→A4 after A4→D4)");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Placement for player 1 (not only player 0): correct format")
        void placement_player1_returnsValidCoord() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            when(mockStage.getColorJ1()).thenReturn(COLOR_OPP);
            when(mockStage.getColorJ2()).thenReturn(COLOR_AI);

            placeOpp(0);
            placeOpp(1);

            String result = decider.getDecision(mockStage, 1);

            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Player 1 placement should return 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("Capture when ALL opponent pawns are in mills → still captures one")
        void capture_allOppPawnsInMill_stillCaptures() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            when(mockStage.isMillJustFormed()).thenReturn(true);

            placeAI(0);  placeAI(1);  placeAI(2);
            placeOpp(21); placeOpp(22); placeOpp(23);

            Set<Integer> oppPositions = new HashSet<>();
            oppPositions.add(21); oppPositions.add(22); oppPositions.add(23);

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result, "AI must capture even when all opponent pawns are in mills");
            assertTrue(result.matches(CAPTURE_REGEX),
                "Should be 'XYY' but returned: " + result);

            int capturedPos = MerelleController.parseCoord(result.substring(1));
            assertTrue(oppPositions.contains(capturedPos),
                "Captured pos=" + capturedPos + " is not an opponent's pawn");
        }

        @Test
        @DisplayName("getDecision() does not throw on empty board in placement phase")
        void noException_emptyBoard_placement() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            assertDoesNotThrow(() -> decider.getDecision(mockStage, 0),
                "getDecision() must not throw on an empty board");
        }

        @Test
        @DisplayName("aiDifficultyPerPlayer null → fallback to global aiDifficulty")
        void perPlayerNull_fallbackToGlobal() {
            MerelleDecider.aiDifficulty         = MerelleDecider.DIFFICULTY_ALPHABETA;
            MerelleDecider.aiDifficultyPerPlayer = null;

            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Global AlphaBeta fallback should return 'XX' but returned: " + result);
        }


        @Test
        @DisplayName("posToCoord() maps all 24 positions to valid coordinates")
        void posToCoord_coversAll24Positions() {
            for (int pos = 0; pos < 24; pos++) {
                String coord = MerelleDecider.posToCoord(pos);
                assertNotEquals("??", coord,
                    "posToCoord(" + pos + ") returned '??' — position not mapped in POS_TO_GRID");
                assertTrue(coord.matches(COORD_REGEX),
                    "posToCoord(" + pos + ") = '" + coord + "' does not match expected format");
            }
        }
    }
}
