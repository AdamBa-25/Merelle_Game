package model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MerelleStageFactory.
 *
 * ============================================================
 * WHAT IS TESTED HERE
 * ============================================================
 *
 * MerelleStageFactory exposes only ONE public method beyond its
 * constructor: setColors(int c1, int c2).
 *
 * The fields colorJ1 / colorJ2 are private static — there are no
 * public getters to read them back. Therefore the only observable
 * contract we can test WITHOUT modifying the source is:
 *
 *   "Does setColors(c1, c2) accept the call without throwing?"
 *
 * We test this exhaustively:
 *   1. Every valid pair (c1 ≠ c2, both in [0, NB_COLORS)) → no exception.
 *   2. Same color for both players → no exception
 *      (the factory silently corrects the conflict in setup(), not in setColors()).
 *
 * ============================================================
 * WHY WE DO NOT TEST PERSISTENCE VIA GETTERS
 * ============================================================
 *
 * getColorJ1() / getColorJ2() do not exist in MerelleStageFactory.
 * The fields are private static. Reading them back would require
 * either: (a) adding public getters, or (b) Java reflection.
 *
 * We do NOT use reflection because it couples tests to implementation
 * details, breaks with obfuscation, and is fragile. Instead we test
 * the public contract: "setColors() must not throw for valid input."
 *
 * The actual color correction logic (identical colors, invalid values)
 * only runs inside setup(), which is an integration concern tested
 * separately (requires a full boardifier Model + Stage).
 *
 * ============================================================
 * WHAT IS NOT TESTED HERE
 * ============================================================
 *
 * setup() calls boardifier internals (addElement, TextElement…) that
 * require a fully initialised Model and Stage. That is an integration
 * test — it belongs in a separate integration test class.
 *
 * ============================================================
 * ISOLATION STRATEGY
 * ============================================================
 *
 * MerelleStageFactory.colorJ1 / colorJ2 are static fields.
 * setUp() and tearDown() reset them to known defaults via setColors()
 * so each test starts from a clean, predictable state.
 *
 * No Mockito needed: the testable surface of MerelleStageFactory
 * is pure Java (a static method call — no dependency injection).
 */
@DisplayName("MerelleStageFactory")
class MerelleStageFactoryTest {

    // =========================================================================
    // Setup / Teardown — isolate static state between tests
    // =========================================================================

    @BeforeEach
    void setUp() {
        // Reset static color fields to safe defaults before every test.
        // This prevents one test from affecting the next (isolation).
        MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED);
    }

    @AfterEach
    void tearDown() {
        // Mirror of setUp() — ensures the static fields are clean after the test
        // even if the test threw or left them in an unexpected state.
        MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED);
    }

    // =========================================================================
    // setColors() — valid inputs must not throw
    // =========================================================================

    @Nested
    @DisplayName("setColors() — valid color combinations (no exception expected)")
    class SetColorsValidInputs {

        /**
         * Happy-path with well-known different colors.
         * The simplest proof that the method works at all.
         */
        @Test
        @DisplayName("Default colors (BLACK / RED) are accepted without exception")
        void setColors_defaultColors_noException() {
            assertDoesNotThrow(
                () -> MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED),
                "setColors(BLACK, RED) must not throw — these are the factory defaults"
            );
        }

        /**
         * A second valid pair to confirm the method is not hard-coded
         * to accept only the default combination.
         */
        @Test
        @DisplayName("Non-default colors (BLUE / GREEN) are accepted without exception")
        void setColors_blueGreen_noException() {
            assertDoesNotThrow(
                () -> MerelleStageFactory.setColors(MerellePawn.PAWN_BLUE, MerellePawn.PAWN_GREEN),
                "setColors(BLUE, GREEN) must not throw"
            );
        }

        /**
         * Exhaustive check: every ordered pair (c1, c2) where c1 ≠ c2 and both
         * are valid indices [0, NB_COLORS) must be accepted without exception.
         *
         * This covers NB_COLORS * (NB_COLORS - 1) pairs — the full valid domain.
         *
         * Loop variables are copied into effectively-final locals BEFORE being
         * captured by the lambda. Java requires variables used inside a lambda to
         * be final or effectively final; a loop variable that changes each iteration
         * is neither, so the copy is mandatory.
         */
        @Test
        @DisplayName("Every valid (c1 != c2) pair is accepted — exhaustive check")
        void setColors_allValidCombinations_noException() {
            for (int c1 = 0; c1 < MerellePawn.NB_COLORS; c1++) {
                for (int c2 = 0; c2 < MerellePawn.NB_COLORS; c2++) {
                    if (c1 != c2) {
                        // Effectively-final copies required by Java lambda rules
                        final int color1 = c1;
                        final int color2 = c2;
                        assertDoesNotThrow(
                            () -> MerelleStageFactory.setColors(color1, color2),
                            "setColors(" + c1 + ", " + c2 + ") should not throw"
                        );
                    }
                }
            }
        }

        /**
         * Verify that setColors() can be called multiple times in a row
         * without accumulating state or throwing on the second call.
         * (Ensures no internal guard blocks repeated calls.)
         */
        @Test
        @DisplayName("setColors() can be called multiple times consecutively without throwing")
        void setColors_calledTwice_noException() {
            assertDoesNotThrow(() -> {
                MerelleStageFactory.setColors(MerellePawn.PAWN_BLUE, MerellePawn.PAWN_GREEN);
                // Second call with different values — must not throw either
                MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED);
            }, "Two consecutive setColors() calls must not throw");
        }
    }

    // =========================================================================
    // setColors() — same-color inputs (edge case)
    // =========================================================================

    @Nested
    @DisplayName("setColors() — same color for both players (edge case)")
    class SetColorsSameColor {

        /**
         * Passing the same color for both players is a degenerate input.
         * The correction logic lives in setup() (not in setColors()), so
         * setColors() itself must NOT throw — it just stores the (invalid)
         * values and lets setup() fix them later.
         *
         * We therefore only assert that no exception is thrown here.
         */
        @Test
        @DisplayName("setColors(BLACK, BLACK) does not throw — correction is deferred to setup()")
        void sameColors_black_noException() {
            assertDoesNotThrow(
                () -> MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_BLACK),
                "setColors() must not throw even for identical colors — correction happens in setup()"
            );
        }

        /**
         * Exhaustive: every same-color pair (c, c) must be stored without exception.
         * setup() is responsible for detecting and fixing the conflict.
         */
        @Test
        @DisplayName("Every same-color pair (c, c) does not throw in setColors()")
        void allSameColorPairs_noException() {
            for (int c = 0; c < MerellePawn.NB_COLORS; c++) {
                final int color = c;
                assertDoesNotThrow(
                    () -> MerelleStageFactory.setColors(color, color),
                    "setColors(" + c + ", " + c + ") must not throw"
                );
                // Reset to valid state for the next iteration
                MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED);
            }
        }
    }

    // =========================================================================
    // MerellePawn color constants — the factory depends on these exact values
    // =========================================================================

    @Nested
    @DisplayName("MerellePawn color constants relied on by MerelleStageFactory")
    class ColorConstants {

        /**
         * PAWN_BLACK must equal 0.
         * MerelleStageFactory uses it as the default J1 color AND as an
         * array/loop base index. If this value changes, the factory breaks silently.
         */
        @Test
        @DisplayName("PAWN_BLACK == 0 (default J1 color and base index)")
        void pawnBlack_isZero() {
            assertEquals(0, MerellePawn.PAWN_BLACK,
                "PAWN_BLACK must equal 0 — used as base index in MerelleStageFactory");
        }

        /**
         * PAWN_RED must equal 1.
         * It is the default J2 color assigned when no other color is chosen.
         */
        @Test
        @DisplayName("PAWN_RED == 1 (default J2 color)")
        void pawnRed_isOne() {
            assertEquals(1, MerellePawn.PAWN_RED,
                "PAWN_RED must equal 1 — used as default J2 color in MerelleStageFactory");
        }

        /**
         * The two default colors must differ.
         * The factory assigns one to each player at startup; identical defaults
         * would immediately trigger the conflict-correction code in setup().
         */
        @Test
        @DisplayName("PAWN_BLACK and PAWN_RED are different (required for two-player assignment)")
        void pawnBlackAndRed_areDifferent() {
            assertNotEquals(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED,
                "Default colors for J1 and J2 must be distinct");
        }

        /**
         * NB_COLORS must be at least 2.
         * The factory's conflict-correction loop in setup() searches for a color
         * ≠ c1. If NB_COLORS < 2 this loop never finds one and the game cannot start.
         */
        @Test
        @DisplayName("NB_COLORS >= 2 (conflict-correction requires at least 2 distinct colors)")
        void nbColors_atLeastTwo() {
            assertTrue(MerellePawn.NB_COLORS >= 2,
                "NB_COLORS must be >= 2 — the factory needs at least two colors to assign to two players");
        }

        /**
         * PAWN_BLACK and PAWN_RED must be valid indices in [0, NB_COLORS).
         * The factory passes these to new MerellePawn(color, …) — an out-of-range
         * value would cause an ArrayIndexOutOfBoundsException at runtime.
         */
        @Test
        @DisplayName("Default color indices are within [0, NB_COLORS)")
        void defaultColors_withinValidRange() {
            assertTrue(
                MerellePawn.PAWN_BLACK >= 0 && MerellePawn.PAWN_BLACK < MerellePawn.NB_COLORS,
                "PAWN_BLACK must be a valid color index (in range [0, NB_COLORS))"
            );
            assertTrue(
                MerellePawn.PAWN_RED >= 0 && MerellePawn.PAWN_RED < MerellePawn.NB_COLORS,
                "PAWN_RED must be a valid color index (in range [0, NB_COLORS))"
            );
        }

        /**
         * isValidColor() is called by setup() to sanitise user-provided colors.
         * It must return true for every index in [0, NB_COLORS) and false for
         * out-of-range values (-1, NB_COLORS).
         */
        @Test
        @DisplayName("isValidColor() returns true for valid indices, false for invalid ones")
        void isValidColor_correctBehavior() {
            // All indices in [0, NB_COLORS) must be valid
            for (int i = 0; i < MerellePawn.NB_COLORS; i++) {
                assertTrue(MerellePawn.isValidColor(i),
                    "isValidColor(" + i + ") should return true");
            }
            // Boundary values outside the range must be invalid
            assertFalse(MerellePawn.isValidColor(-1),
                "isValidColor(-1) should return false");
            assertFalse(MerellePawn.isValidColor(MerellePawn.NB_COLORS),
                "isValidColor(NB_COLORS) should return false — index is one past the end");
        }
    }
}
