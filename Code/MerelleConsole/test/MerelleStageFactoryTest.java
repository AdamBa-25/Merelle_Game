package model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("MerelleStageFactory")
class MerelleStageFactoryTest {

    @BeforeEach
    void setUp() {
        MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED);
    }

    @AfterEach
    void tearDown() {
        MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED);
    }

    @Nested
    @DisplayName("setColors() — valid color combinations (no exception expected)")
    class SetColorsValidInputs {


        @Test
        @DisplayName("Default colors (BLACK / RED) are accepted without exception")
        void setColors_defaultColors_noException() {
            assertDoesNotThrow(
                () -> MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED),
                "setColors(BLACK, RED) must not throw — these are the factory defaults"
            );
        }

        @Test
        @DisplayName("Non-default colors (BLUE / GREEN) are accepted without exception")
        void setColors_blueGreen_noException() {
            assertDoesNotThrow(
                () -> MerelleStageFactory.setColors(MerellePawn.PAWN_BLUE, MerellePawn.PAWN_GREEN),
                "setColors(BLUE, GREEN) must not throw"
            );
        }

        @Test
        @DisplayName("Every valid (c1 != c2) pair is accepted — exhaustive check")
        void setColors_allValidCombinations_noException() {
            for (int c1 = 0; c1 < MerellePawn.NB_COLORS; c1++) {
                for (int c2 = 0; c2 < MerellePawn.NB_COLORS; c2++) {
                    if (c1 != c2) {
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

        @Test
        @DisplayName("setColors() can be called multiple times consecutively without throwing")
        void setColors_calledTwice_noException() {
            assertDoesNotThrow(() -> {
                MerelleStageFactory.setColors(MerellePawn.PAWN_BLUE, MerellePawn.PAWN_GREEN);
                MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED);
            }, "Two consecutive setColors() calls must not throw");
        }
    }

    @Nested
    @DisplayName("setColors() — same color for both players (edge case)")
    class SetColorsSameColor {

        @Test
        @DisplayName("setColors(BLACK, BLACK) does not throw — correction is deferred to setup()")
        void sameColors_black_noException() {
            assertDoesNotThrow(
                () -> MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_BLACK),
                "setColors() must not throw even for identical colors — correction happens in setup()"
            );
        }


        @Test
        @DisplayName("Every same-color pair (c, c) does not throw in setColors()")
        void allSameColorPairs_noException() {
            for (int c = 0; c < MerellePawn.NB_COLORS; c++) {
                final int color = c;
                assertDoesNotThrow(
                    () -> MerelleStageFactory.setColors(color, color),
                    "setColors(" + c + ", " + c + ") must not throw"
                );
                MerelleStageFactory.setColors(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED);
            }
        }
    }


    @Nested
    @DisplayName("MerellePawn color constants relied on by MerelleStageFactory")
    class ColorConstants {

        @Test
        @DisplayName("PAWN_BLACK == 0 (default J1 color and base index)")
        void pawnBlack_isZero() {
            assertEquals(0, MerellePawn.PAWN_BLACK,
                "PAWN_BLACK must equal 0 — used as base index in MerelleStageFactory");
        }

        @Test
        @DisplayName("PAWN_RED == 1 (default J2 color)")
        void pawnRed_isOne() {
            assertEquals(1, MerellePawn.PAWN_RED,
                "PAWN_RED must equal 1 — used as default J2 color in MerelleStageFactory");
        }

        @Test
        @DisplayName("PAWN_BLACK and PAWN_RED are different (required for two-player assignment)")
        void pawnBlackAndRed_areDifferent() {
            assertNotEquals(MerellePawn.PAWN_BLACK, MerellePawn.PAWN_RED,
                "Default colors for J1 and J2 must be distinct");
        }


        @Test
        @DisplayName("NB_COLORS >= 2 (conflict-correction requires at least 2 distinct colors)")
        void nbColors_atLeastTwo() {
            assertTrue(MerellePawn.NB_COLORS >= 2,
                "NB_COLORS must be >= 2 — the factory needs at least two colors to assign to two players");
        }


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

        @Test
        @DisplayName("isValidColor() returns true for valid indices, false for invalid ones")
        void isValidColor_correctBehavior() {
            for (int i = 0; i < MerellePawn.NB_COLORS; i++) {
                assertTrue(MerellePawn.isValidColor(i),
                    "isValidColor(" + i + ") should return true");
            }
            assertFalse(MerellePawn.isValidColor(-1),
                "isValidColor(-1) should return false");
            assertFalse(MerellePawn.isValidColor(MerellePawn.NB_COLORS),
                "isValidColor(NB_COLORS) should return false — index is one past the end");
        }
    }
}
