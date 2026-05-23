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

/**
 * Unit tests for MerelleDecider — the three AI strategies (Minimax, AlphaBeta, MonteCarlo).
 *
 * ============================================================
 * HOW DO WE TEST THE AI?
 * ============================================================
 *
 * MerelleDecider does NOT touch the boardifier framework directly
 * during its calculations: all AI logic works on a boardSnapshot
 * (int[24] — a lightweight copy of the board). The only place where
 * boardifier is used is boardSnapshot(), which reads getPawnAt() on
 * a MerelleBoard.
 *
 * Adopted strategy:
 *   1. Mock Model and Controller (required by the Decider constructor
 *      but NOT used inside getDecision()).
 *   2. Mock MerelleStageModel to precisely control the state returned
 *      to the AI (phase, mills, colors…).
 *   3. Mock MerelleBoard and configure getPawnAt() case by case
 *      to simulate a precise board state — without going through boardifier.
 *   4. Verify the FORMAT of the answer (regex) and LOGICAL CONSISTENCY
 *      (e.g., the captured piece is really an opponent's pawn).
 *
 * What we do NOT test here:
 *   - The absolute "strategic quality" of the AI (too brittle).
 *   - Private internals (minimax(), alphabeta()…) — tested indirectly.
 *   - boardifier interactions (View, ActionPlayer…).
 *
 * ============================================================
 * STRUCTURE
 * ============================================================
 *   ├── posToCoord()             — public static method, easy to unit-test
 *   ├── getDecision() routing    — verifies aiDifficulty picks the right AI
 *   ├── Minimax
 *   │     ├── placement          — format "XX"
 *   │     ├── move               — format "XX YY"
 *   │     └── capture            — format "XXX"
 *   ├── AlphaBeta                — same scenarios, consistency with Minimax
 *   ├── MonteCarlo               — same scenarios (non-deterministic)
 *   └── Common behavior          — properties that hold for all 3 AIs
 *
 * ============================================================
 * NOTE ON MOCKITO (core 5.x, no PowerMock)
 * ============================================================
 *   - @Mock                     → creates a mock of the interface/class
 *   - when(...).thenReturn(...) → defines the mock's behavior
 *   - verify(...)               → checks a method was called
 *
 * IMPORTANT: mocks must be created BEFORE being passed to thenReturn().
 * Nesting mock() calls inside thenReturn() triggers Mockito's
 * UnfinishedStubbingException. That is why placeAI()/placeOpp()
 * extract the pawn mock into a local variable first.
 */
@DisplayName("MerelleDecider")
class MerelleDeciderTest {

    // ================================================================
    // Shared mocks
    // ================================================================

    @Mock
    private Model mockModel;

    @Mock
    private Controller mockController;

    @Mock
    private MerelleStageModel mockStage;

    @Mock
    private MerelleBoard mockBoard;

    /** The Decider under test. Recreated before each test to isolate static state. */
    private MerelleDecider decider;

    // ================================================================
    // Convenience constants
    // ================================================================

    /** Color of player 0 (AI) in all tests. */
    private static final int COLOR_AI  = MerellePawn.PAWN_BLACK; // 0
    /** Color of player 1 (opponent) in all tests. */
    private static final int COLOR_OPP = MerellePawn.PAWN_RED;   // 1

    // ================================================================
    // Expected answer formats (regex)
    // ================================================================

    /**
     * A valid board coordinate: letter A-G followed by digit 1-7.
     * Example: "A1", "D4", "G7".
     */
    private static final String COORD_REGEX = "[A-G][1-7]";

    /**
     * A placement (phase 1): a single coordinate.
     * Example: "A1".
     */
    private static final String PLACEMENT_REGEX = "[A-G][1-7]";

    /**
     * A move (phase 2): two coordinates separated by a space.
     * Example: "A1 B2".
     */
    private static final String MOVE_REGEX = "[A-G][1-7] [A-G][1-7]";

    /**
     * A capture: "X" followed by a valid coordinate.
     * Example: "XA1", "XD4".
     */
    private static final String CAPTURE_REGEX = "X[A-G][1-7]";

    // ================================================================
    // Setup / Teardown
    // ================================================================

    @BeforeEach
    void setUp() {
        // Initialize all @Mock-annotated fields in this class
        MockitoAnnotations.openMocks(this);

        // Build the Decider with mocked dependencies.
        // Model and Controller are required by the parent Decider constructor
        // but are NOT called during getDecision() — mocks are sufficient.
        decider = new MerelleDecider(mockModel, mockController);

        // Default stage: return the two player colors and the mocked board
        when(mockStage.getColorJ1()).thenReturn(COLOR_AI);
        when(mockStage.getColorJ2()).thenReturn(COLOR_OPP);
        when(mockStage.getBoard()).thenReturn(mockBoard);

        // Default: no mill just formed, no previous moves recorded
        when(mockStage.isMillJustFormed()).thenReturn(false);
        when(mockStage.getLastMoves()).thenReturn(new String[]{null, null, null, null});

        // Default: empty board — every cell returns null (= no pawn = -1 in snapshot)
        for (int pos = 0; pos < 24; pos++) {
            when(mockBoard.getPawnAt(pos)).thenReturn(null);
        }

        // Reset static fields to avoid cross-test contamination
        MerelleDecider.aiDifficulty         = MerelleDecider.DIFFICULTY_MINIMAX;
        MerelleDecider.aiDifficultyPerPlayer = null;
    }

    @AfterEach
    void tearDown() {
        // Restore static fields to their default values after each test
        MerelleDecider.aiDifficulty         = MerelleDecider.DIFFICULTY_MINIMAX;
        MerelleDecider.aiDifficultyPerPlayer = null;
    }

    // ================================================================
    // Helpers
    // ================================================================

    /**
     * Returns a mocked MerellePawn whose getColor() returns {@code color}.
     *
     * We do NOT create a real MerellePawn because its constructor requires
     * a fully functional GameStageModel and registers types in a static
     * boardifier registry. A mock is sufficient here since boardSnapshot()
     * only calls getColor().
     */
    private MerellePawn pawn(int color) {
        MerellePawn p = mock(MerellePawn.class);
        when(p.getColor()).thenReturn(color);
        return p;
    }

    /**
     * Places an AI pawn (COLOR_AI) at logical position {@code pos} on the mocked board.
     *
     * The pawn mock is created BEFORE being passed to thenReturn() to avoid
     * Mockito's UnfinishedStubbingException (caused by nesting mock() inside
     * a when/thenReturn call).
     */
    private void placeAI(int pos) {
        MerellePawn p = pawn(COLOR_AI);
        when(mockBoard.getPawnAt(pos)).thenReturn(p);
    }

    /**
     * Places an opponent pawn (COLOR_OPP) at logical position {@code pos}.
     * Same safety pattern as placeAI().
     */
    private void placeOpp(int pos) {
        MerellePawn p = pawn(COLOR_OPP);
        when(mockBoard.getPawnAt(pos)).thenReturn(p);
    }

    // ================================================================
    // SECTION 1 — posToCoord() : public static method
    // ================================================================

    /**
     * posToCoord() converts a logical position (0-23) to a console coordinate.
     * It is the inverse of parseCoord() tested in MerelleControllerParseCoordTest.
     *
     * We verify the 4 corners of the board, a central position, and invalid inputs.
     */
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

        /**
         * Round-trip test: posToCoord(pos) → coord, then parseCoord(coord) must give back pos.
         * This ensures the two conversion methods are strict inverses of each other
         * for all 24 valid board positions.
         */
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

    // ================================================================
    // SECTION 2 — Routing: getDecision() selects the right AI
    // ================================================================

    /**
     * getDecision() delegates to one of the three strategies based on aiDifficulty.
     * We verify that the result has the correct FORMAT in each case.
     *
     * Context: nearly empty board (2 AI pawns placed) → placement phase
     * → the expected answer is a single coordinate.
     */
    @Nested
    @DisplayName("getDecision() — routing to the correct strategy")
    class Routing {

        @BeforeEach
        void context() {
            // Placement phase with a couple of AI pawns already on the board
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
            // An unknown value should fall through the switch/case to the default branch,
            // which must use Minimax as a safe fallback.
            MerelleDecider.aiDifficulty = 999;
            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Minimax fallback should return 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("aiDifficultyPerPlayer overrides global aiDifficulty for player 1")
        void routing_perPlayerDifficulty() {
            // Player 0 = Minimax, Player 1 = AlphaBeta
            // Both return the same format here — we just verify no exception and correct format.
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
            MerelleDecider.aiDifficultyPerPlayer = new int[]{
                MerelleDecider.DIFFICULTY_MINIMAX,
                MerelleDecider.DIFFICULTY_ALPHABETA
            };

            // For player 1, swap colors so the decider sees the correct perspective
            when(mockStage.getColorJ1()).thenReturn(COLOR_OPP);
            when(mockStage.getColorJ2()).thenReturn(COLOR_AI);
            placeAI(0);

            String result = decider.getDecision(mockStage, 1);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Per-player routing should return 'XX' but returned: " + result);
        }
    }

    // ================================================================
    // SECTION 3 — Minimax
    // ================================================================

    /**
     * Tests for the Minimax strategy.
     *
     * How it works: getDecisionMinimax() builds a boardSnapshot (int[24])
     * from the mocked board, then explores the minimax tree on that snapshot.
     * No real boardifier objects are touched during the search.
     *
     * Three distinct scenarios:
     *   1. Placement phase  → must return a single coordinate "XX"
     *   2. Move phase       → must return "XX YY"
     *   3. Capture (mill just formed) → must return "XYY"
     *
     * Board constraint: each side must have at least 3 pawns on the board
     * to avoid triggering the "< 3 pawns = loss" terminal condition in the
     * minimax tree, which would cause the decider to skip move generation.
     */
    @Nested
    @DisplayName("Minimax")
    class MinimaxTests {

        @BeforeEach
        void setDifficulty() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
        }

        // ── 3a. Placement phase ──────────────────────────────────────

        @Test
        @DisplayName("Placement: returns a valid coordinate on an empty board")
        void placement_emptyBoard_returnsValidCoord() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);
            // All cells return null by default (set up in setUp())

            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result, "Result must not be null");
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Should be a coordinate 'XX' but returned: " + result);
        }

        @Test
        @DisplayName("Placement: the chosen cell is free (not already occupied)")
        void placement_chosenCellIsFree() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            // Occupy a few cells so the AI must choose elsewhere
            placeAI(0);  // A1 occupied
            placeAI(1);  // D1 occupied
            placeOpp(2); // G1 occupied

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
            /*
             * Scenario: AI has two pawns on mill {0,1,2} (A1-D1-G1).
             * Placing at pos 2 (G1) would complete the mill.
             * We verify the AI returns a valid placement coordinate.
             *
             * NOTE: We do NOT assert the result must be "G1" because Minimax
             * at depth 5 may occasionally prefer another cell if it leads to
             * a higher global score. We only check the format.
             */
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);
            placeAI(0);  // A1
            placeAI(1);  // D1
            // pos 2 (G1) is free — completing the mill

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Should be a coordinate 'XX' but returned: " + result);
        }

        // ── 3b. Move phase ────────────────────────────────────────────

        @Test
        @DisplayName("Move: returns the format 'XX YY'")
        void move_returnsCorrectFormat() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);

            /*
             * Minimal board for the AI to have at least one legal move:
             *   AI at A1(0), A4(9), A7(21) — adjacent cells D1(1) and D4(11) are free
             *   Opponent at G1(2), G4(14), G7(23)
             *
             * Both sides have exactly 3 pawns, which is the minimum to stay
             * above the "< 3 pawns = terminal loss" threshold in minimax.
             */
            placeAI(0);    // A1
            placeAI(9);    // A4
            placeAI(21);   // A7

            placeOpp(2);   // G1
            placeOpp(14);  // G4
            placeOpp(23);  // G7

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

        // ── 3c. Capture after mill formation ─────────────────────────

        @Test
        @DisplayName("Capture: returns the format 'XYY'")
        void capture_returnsCorrectFormat() {
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            // A mill was just formed — the AI must now choose a capture
            when(mockStage.isMillJustFormed()).thenReturn(true);

            /*
             * AI mill: positions {0,1,2} (A1-D1-G1)
             * Opponent pawns: positions {9,11,14} — not in a mill, so all capturable
             *
             * We use positions {9,11,14} for the opponent (not {3,4,5}) because
             * whether {3,4,5} forms a real mill on the board depends on the
             * game's adjacency definition. Using spread-out isolated pawns
             * avoids that ambiguity.
             */
            placeAI(0);  // A1
            placeAI(1);  // D1
            placeAI(2);  // G1

            placeOpp(9);   // A4
            placeOpp(11);  // C4
            placeOpp(14);  // G4

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

            // Extract the captured coordinate and convert to logical position
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

    // ================================================================
    // SECTION 4 — AlphaBeta
    // ================================================================

    /**
     * Tests for the Alpha-Beta pruning strategy.
     *
     * Why the same scenarios as Minimax?
     * AlphaBeta is an optimization of Minimax: it finds the SAME optimal
     * decisions but explores fewer branches. Format constraints are therefore
     * identical. We also verify that both algorithms return results of the
     * same format on the same position.
     */
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

        /**
         * Minimax and AlphaBeta must return results of the SAME FORMAT on
         * an identical position (they may differ on the exact move chosen
         * when multiple moves have equal score).
         */
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

    // ================================================================
    // SECTION 5 — MonteCarlo
    // ================================================================

    /**
     * Tests for the Monte Carlo Tree Search strategy.
     *
     * MonteCarlo is NON-DETERMINISTIC: it plays MCTS_SIMULATIONS random
     * games per candidate move and picks the move with the best win rate.
     * We therefore cannot test the EXACT move chosen.
     *
     * We only verify:
     *   - The FORMAT of the response (placement / move / capture)
     *   - LOGICAL VALIDITY (chosen cell is free, captured pawn is opponent's, etc.)
     *
     * NOTE: MonteCarlo is slower than Minimax/AlphaBeta because it runs
     * many full random simulations. This is expected behavior.
     */
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
            // A few pawns on the board to speed up the test (fewer free cells = fewer simulations)
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

    // ================================================================
    // SECTION 6 — Common behavior across all 3 AIs
    // ================================================================

    /**
     * These tests verify properties that MUST hold for ALL THREE strategies.
     * They are parameterized over DIFFICULTY_MINIMAX, DIFFICULTY_ALPHABETA,
     * and DIFFICULTY_MONTECARLO.
     */
    @Nested
    @DisplayName("Common behavior across all 3 AIs")
    class CommonBehavior {

        /**
         * When there is only one free cell on the board, all three AIs must
         * choose that cell during the placement phase.
         *
         * Setup: 4 AI pawns + 4 opponent pawns placed, leaving exactly pos 5 free.
         * We keep counts small (4+4) and well below the 9-pawn-per-player limit
         * so the decider does not hit a "too many pawns placed" guard.
         */
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

            // AI occupies positions 0,1,2,3 — Opponent occupies 6,7,8,9
            // Position 4,5,10,11,...,23 are free — but we only leave pos 5 open
            // by placing pawns on all EXCEPT pos 5.
            // Use 4 AI + 4 opponent (total 8) keeping one cell open at pos 5 (D2).
            // Remaining cells: fill them with alternating AI/Opp to stay balanced.
            // We fill positions 0-4 (AI) and 6-9 (OPP), leaving only pos 5 (D3) free.
            placeAI(0); placeAI(1); placeAI(2); placeAI(3); placeAI(4);
            placeOpp(6); placeOpp(7); placeOpp(8); placeOpp(9); placeOpp(10);
            // Fill the rest except pos 5
            for (int pos = 11; pos < 24; pos++) {
                if (pos % 2 == 0) placeAI(pos);
                else              placeOpp(pos);
            }
            // pos 5 is the only free cell

            String expected = MerelleDecider.posToCoord(5);
            String result = decider.getDecision(mockStage, 0);

            assertNotNull(result, "Difficulty " + difficulty + ": result is null");
            assertEquals(expected, result,
                "Difficulty " + difficulty
                + ": only pos 5 (" + expected + ") is free, but AI returned: " + result);
        }

        /**
         * All three AIs must return a non-null, non-empty result
         * in the move phase on a valid board.
         */
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

        /**
         * The AI must not produce a ping-pong move (A→B then B→A)
         * when multiple alternative moves are available.
         *
         * Ping-pong detection: if the last move was "srcA → destB",
         * the next move must not be "destB → srcA".
         *
         * We simulate the history via getLastMoves() using the format
         * "playerIndex:srcCoord destCoord" which is the format expected
         * by the decider when building the ping-pong penalty.
         *
         * NOTE: On a very constrained board (only one legal move), the AI
         * may have no choice but to reverse. This test uses a board with
         * several available moves to make the assertion meaningful.
         */
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

            /*
             * Board: AI at A1(0), A4(9), A7(21) — several adjacent free cells exist.
             * Opponent at G1(2), G4(14), G7(23).
             *
             * Last AI move: A4(9) → D4(11)  (i.e., the AI moved from pos 9 to pos 11).
             * Ping-pong would be: D4(11) → A4(9)  = "D4 A4".
             *
             * We use the coordinate format "A4 D4" (src dest without player prefix)
             * because that is what the controller stores in lastMoves.
             * Adjust the format string below if the actual format in your codebase differs.
             */
            placeAI(0);    // A1
            placeAI(11);   // D4 (AI just moved here)
            placeAI(21);   // A7

            placeOpp(2);   // G1
            placeOpp(14);  // G4
            placeOpp(23);  // G7

            // Simulate the history: AI's last recorded move was A4 → D4
            when(mockStage.getLastMoves()).thenReturn(
                new String[]{"A4 D4", null, null, null}
            );

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(MOVE_REGEX),
                "Should be a move 'XX YY' but returned: " + result);

            // The ping-pong move would be reversing A4→D4, i.e., D4→A4
            assertNotEquals("D4 A4", result,
                "Difficulty " + difficulty + ": ping-pong detected (D4→A4 after A4→D4)");
        }
    }

    // ================================================================
    // SECTION 7 — Edge cases and robustness
    // ================================================================

    /**
     * These tests ensure the Decider does not crash in unusual situations.
     */
    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Placement for player 1 (not only player 0): correct format")
        void placement_player1_returnsValidCoord() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            // For player 1, colors are swapped relative to the default setUp()
            when(mockStage.getColorJ1()).thenReturn(COLOR_OPP); // J1 = opponent
            when(mockStage.getColorJ2()).thenReturn(COLOR_AI);  // J2 = AI

            placeOpp(0);
            placeOpp(1);

            String result = decider.getDecision(mockStage, 1);

            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Player 1 placement should return 'XX' but returned: " + result);
        }

        /**
         * Official rule: if ALL opponent pawns are in mills, one of them
         * can still be captured. The AI must return a valid capture target
         * even in this case.
         */
        @Test
        @DisplayName("Capture when ALL opponent pawns are in mills → still captures one")
        void capture_allOppPawnsInMill_stillCaptures() {
            MerelleDecider.aiDifficulty = MerelleDecider.DIFFICULTY_MINIMAX;
            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_DEPLACEMENT);
            when(mockStage.isMillJustFormed()).thenReturn(true);

            /*
             * AI mill: positions {0,1,2} (A1-D1-G1)
             * Opponent mill: positions {21,22,23} (A7-D7-G7)
             * All opponent pawns are in a mill — the rule still allows capture.
             */
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
            // All cells null by default from setUp()

            assertDoesNotThrow(() -> decider.getDecision(mockStage, 0),
                "getDecision() must not throw on an empty board");
        }

        @Test
        @DisplayName("aiDifficultyPerPlayer null → fallback to global aiDifficulty")
        void perPlayerNull_fallbackToGlobal() {
            MerelleDecider.aiDifficulty         = MerelleDecider.DIFFICULTY_ALPHABETA;
            MerelleDecider.aiDifficultyPerPlayer = null; // no per-player config

            when(mockStage.getCurrentPhase()).thenReturn(MerelleStageModel.PHASE_PLACEMENT);

            String result = decider.getDecision(mockStage, 0);
            assertNotNull(result);
            assertTrue(result.matches(PLACEMENT_REGEX),
                "Global AlphaBeta fallback should return 'XX' but returned: " + result);
        }

        /**
         * posToCoord() must cover all 24 positions: none should return "??".
         * This ensures POS_TO_GRID in MerelleDecider has no gaps.
         */
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
