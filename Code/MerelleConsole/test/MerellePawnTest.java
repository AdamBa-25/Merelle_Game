package model;

import boardifier.model.GameStageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@DisplayName("MerellePawn")
class MerellePawnTest {

    @Mock
    private GameStageModel mockStage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("getColor() returns the color passed to the constructor")
    void getColor_retournsConstructorColor() {
        MerellePawn pawn = new MerellePawn(MerellePawn.PAWN_RED, mockStage);
        assertEquals(MerellePawn.PAWN_RED, pawn.getColor());
    }

    @ParameterizedTest(name = "color {0} → getColor() = {0}")
    @CsvSource({"0", "1", "2", "3", "4", "5", "6"})
    @DisplayName("getColor() returns the correct constant for each color")
    void getColor_allColors(int color) {
        MerellePawn pawn = new MerellePawn(color, mockStage);
        assertEquals(color, pawn.getColor());
    }

    @ParameterizedTest(name = "PAWN_{0} → symbol '{1}'")
    @CsvSource({
        "0, N",   // BLACK
        "1, R",   // RED
        "2, B",   // BLUE
        "3, V",   // GREEN
        "4, J",   // YELLOW
        "5, M",   // PURPLE
        "6, C"    // CYAN
    })
    @DisplayName("getSymbol() returns the correct letter for each color")
    void getSymbol_allColors(int color, char expectedSymbol) {
        MerellePawn pawn = new MerellePawn(color, mockStage);
        assertEquals(expectedSymbol, pawn.getSymbol());
    }

    @Test
    @DisplayName("getSymbol() returns '?' for an unknown color")
    void getSymbol_unknownColor_returnsQuestionMark() {
        MerellePawn pawn = new MerellePawn(99, mockStage);
        assertEquals('?', pawn.getSymbol());
    }



    @ParameterizedTest(name = "color {0} → name '{1}'")
    @CsvSource({
        "0, Black",
        "1, Red",
        "2, Blue",
        "3, Green",
        "4, Yellow",
        "5, Magenta",
        "6, Cyan"
    })
    @DisplayName("getColorName() returns the correct name for each color")
    void getColorName_allColors(int color, String expectedName) {
        assertEquals(expectedName, MerellePawn.getColorName(color));
    }

    @Test
    @DisplayName("getColorName() returns 'Unknown' for an invalid color")
    void getColorName_invalid_returnsInconnu() {
        assertEquals("Unkown", MerellePawn.getColorName(-1));
        assertEquals("Unkown", MerellePawn.getColorName(99));
    }


    @ParameterizedTest(name = "color {0} → valid")
    @CsvSource({"0", "1", "2", "3", "4", "5", "6"})
    @DisplayName("isValidColor() returns true for valid colors (0 to NB_COLORS-1)")
    void isValidColor_valid(int color) {
        assertTrue(MerellePawn.isValidColor(color));
    }

    @Test
    @DisplayName("isValidColor() returns false for invalid colors")
    void isValidColor_invalid() {
        assertFalse(MerellePawn.isValidColor(-1));
        assertFalse(MerellePawn.isValidColor(MerellePawn.NB_COLORS));
        assertFalse(MerellePawn.isValidColor(100));
    }

    @Test
    @DisplayName("NB_COLORS equals 7")
    void nbColors_is7() {
        assertEquals(7, MerellePawn.NB_COLORS);
    }

    @ParameterizedTest(name = "PAWN_{0} → non-null backgroundColor")
    @CsvSource({"0", "1", "2", "3", "4", "5", "6"})
    @DisplayName("getBackgroundColor() returns a non-null ANSI string for each color")
    void getBackgroundColor_notNullNotEmpty(int color) {
        MerellePawn pawn = new MerellePawn(color, mockStage);
        String bg = pawn.getBackgroundColor();
        assertNotNull(bg);
        assertFalse(bg.isEmpty(), "getBackgroundColor() ne doit pas retourner une chaîne vide");
    }

    @Test
    @DisplayName("getTextColor() returns white for dark backgrounds (BLACK, BLUE, PURPLE)")
    void getTextColor_darkBackgrounds_returnWhite() {
        String white = new MerellePawn(MerellePawn.PAWN_BLACK, mockStage).getTextColor();

        assertEquals(white, new MerellePawn(MerellePawn.PAWN_BLUE,   mockStage).getTextColor());
        assertEquals(white, new MerellePawn(MerellePawn.PAWN_PURPLE, mockStage).getTextColor());
    }

    @Test
    @DisplayName("getTextColor() returns black for light backgrounds (RED, GREEN, YELLOW, CYAN)")
    void getTextColor_lightBackgrounds_returnBlack() {
        String black = new MerellePawn(MerellePawn.PAWN_RED, mockStage).getTextColor();

        assertEquals(black, new MerellePawn(MerellePawn.PAWN_GREEN,  mockStage).getTextColor());
        assertEquals(black, new MerellePawn(MerellePawn.PAWN_YELLOW, mockStage).getTextColor());
        assertEquals(black, new MerellePawn(MerellePawn.PAWN_CYAN,   mockStage).getTextColor());
    }

    @Test
    @DisplayName("getTextColor() returns different values for dark vs light backgrounds")
    void getTextColor_darkAndLight_areDifferent() {
        String dark  = new MerellePawn(MerellePawn.PAWN_BLACK, mockStage).getTextColor();
        String light = new MerellePawn(MerellePawn.PAWN_RED,   mockStage).getTextColor();
        assertNotEquals(dark, light);
    }


    @Test
    @DisplayName("printColorMenu() runs without throwing an exception")
    void printColorMenu_doesNotThrow() {
        assertDoesNotThrow(MerellePawn::printColorMenu);
    }
}
