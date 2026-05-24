package control;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MerelleController.parseCoord()")
class MerelleControllerParseCoordTest {


    @ParameterizedTest(name = "{0} → position {1}")
    @CsvSource({
        "A1,  0", "A4,  1", "A7,  2",
        "B2,  3", "B4,  4", "B6,  5",
        "C3,  6", "C4,  7", "C5,  8",
        "D1,  9", "D2, 10", "D3, 11",
        "D5, 12", "D6, 13", "D7, 14",
        "E3, 15", "E4, 16", "E5, 17",
        "F2, 18", "F4, 19", "F6, 20",
        "G1, 21", "G4, 22", "G7, 23"
    })
    @DisplayName("All 24 valid coordinates in uppercase")
    void parseCoord_allValidPositions(String coord, int expectedPos) {
        assertEquals(expectedPos, MerelleController.parseCoord(coord));
    }

    @ParameterizedTest(name = "'{0}' minuscule → position {1}")
    @CsvSource({
        "a1,  0", "a4,  1", "g7, 23", "b4,  4", "d5, 12"
    })
    @DisplayName("Lowercase coordinates are accepted (case insensitive)")
    void parseCoord_lowercase(String coord, int expectedPos) {
        assertEquals(expectedPos, MerelleController.parseCoord(coord));
    }

    @ParameterizedTest(name = "'{0}' mixte → position {1}")
    @CsvSource({
        "A1,  0", "d4, -1", "G7, 23"
    })
    @DisplayName("Mixed case accepted")
    void parseCoord_mixedCase(String coord, int expectedPos) {
        assertEquals(expectedPos, MerelleController.parseCoord(coord));
    }

    @Nested
    @DisplayName("Letters outside A-G → -1")
    class InvalidLetters {

        @Test
        @DisplayName("Letter H → invalid")
        void letter_H() {
            assertEquals(-1, MerelleController.parseCoord("H1"));
        }

        @Test
        @DisplayName("Letter Z → invalid")
        void letter_Z() {
            assertEquals(-1, MerelleController.parseCoord("Z4"));
        }

        @Test
        @DisplayName("Digit instead of letter → invalid")
        void digit_asLetter() {
            assertEquals(-1, MerelleController.parseCoord("14"));
        }
    }

    @Nested
    @DisplayName("Digits outside 1-7 → -1")
    class InvalidDigits {

        @Test
        @DisplayName("Column 0 → invalid")
        void col_0() {
            assertEquals(-1, MerelleController.parseCoord("A0"));
        }

        @Test
        @DisplayName("Column 8 → invalid")
        void col_8() {
            assertEquals(-1, MerelleController.parseCoord("A8"));
        }

        @Test
        @DisplayName("Negative column → invalid")
        void col_negative() {
            assertEquals(-1, MerelleController.parseCoord("A-1"));
        }
    }

    @Nested
    @DisplayName("Grid cells not on the board → -1")
    class InvalidGridCells {

        @Test
        @DisplayName("A2 is a grid cell but not a board position")
        void A2_notOnBoard() {
            assertEquals(-1, MerelleController.parseCoord("A2"));
        }

        @Test
        @DisplayName("A3 is a grid cell but not a board position")
        void A3_notOnBoard() {
            assertEquals(-1, MerelleController.parseCoord("A3"));
        }

        @Test
        @DisplayName("D4 is the grid center but not a board position")
        void D4_notOnBoard() {
            assertEquals(-1, MerelleController.parseCoord("D4"));
        }

        @Test
        @DisplayName("B1 is not on the board")
        void B1_notOnBoard() {
            assertEquals(-1, MerelleController.parseCoord("B1"));
        }
    }


    @Nested
    @DisplayName("Malformed inputs → -1")
    class MalformedInput {

        @Test
        @DisplayName("null → -1")
        void nullInput() {
            assertEquals(-1, MerelleController.parseCoord(null));
        }

        @Test
        @DisplayName("Empty string → -1")
        void emptyString() {
            assertEquals(-1, MerelleController.parseCoord(""));
        }

        @Test
        @DisplayName("Single character → -1")
        void singleChar() {
            assertEquals(-1, MerelleController.parseCoord("A"));
        }

        @Test
        @DisplayName("Input with spaces → handled after trim (valid if coordinate is correct)")
        void withSpaces_trimmed() {
            assertEquals(0, MerelleController.parseCoord("  A1  "));
        }

        @Test
        @DisplayName("Coordinate with special characters → -1")
        void specialChars() {
            assertEquals(-1, MerelleController.parseCoord("A!"));
            assertEquals(-1, MerelleController.parseCoord("A@1"));
        }

        @Test
        @DisplayName("Too many characters → -1 if invalid cell")
        void tooLong_invalidCell() {
            assertEquals(-1, MerelleController.parseCoord("A11"));
        }
    }
}
