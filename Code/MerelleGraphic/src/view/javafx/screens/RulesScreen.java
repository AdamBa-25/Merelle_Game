package view.javafx.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import model.GameConfig;
import view.javafx.SceneManager;

public class RulesScreen extends VBox {

    /** Full-screen usage from the main menu: closing returns to the menu. */
    public RulesScreen(SceneManager manager) {
        this(manager::showMenu, "Back to menu");
    }

    /** Overlay usage during a game: closing just hides the overlay. */
    public RulesScreen(Runnable onClose) {
        this(onClose, "Close");
    }

    private RulesScreen(Runnable onClose, String closeLabel) {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(30, 40, 30, 40));
        getStyleClass().add("menu-root");

        Text title = new Text("Nine Men's Morris Rules");
        title.getStyleClass().add("screen-title");

        VBox rulesContent = new VBox(18);
        rulesContent.setMaxWidth(740);
        rulesContent.setFillWidth(true);

        rulesContent.getChildren().addAll(
            ruleSection("Objective",
                "Reduce the opponent to 2 pawns, or block them so they can no longer move."),

            ruleSection("The board",
                "24 intersections spread across 3 concentric squares connected by midlines. " +
                "Each player has 9 pawns. Coordinates use a letter (A-G, row) " +
                "and a number (1-7, column). Example: A1, D4, G7."),

            ruleSection("Phase 1 - Placement",
                "Players take turns placing their 9 pawns on free intersections. " +
                "As soon as 3 aligned pawns form a mill, the player immediately captures an " +
                "opponent's pawn. A pawn in a mill cannot be captured, unless all of the " +
                "opponent's pawns are in mills."),

            ruleSection("Phase 2 - Movement",
                "Once all 18 pawns are placed, each player moves one of their pawns to a " +
                "free adjacent intersection. Forming a mill always allows capturing an " +
                "opponent's pawn. A player cannot reform exactly the same mill on two " +
                "consecutive turns."),

            ruleSection("Phase 3 - Flying (when 3 pawns remain)",
                "When a player has only 3 pawns left, they may move their pawns to any " +
                "free intersection (not only adjacent squares). This is called \"flying\"."),

            ruleSection("End of game",
                "A player loses if they have only 2 pawns left, or if they can no longer " +
                "make any move. A draw is declared in case of position repetition (the same " +
                "configuration occurring 3 times in a row)."),

            ruleSection("Mills",
                "There are 16 possible mills: 4 lines on each side of the 3 squares, " +
                "plus 4 lines connecting the squares to the center. An active mill is shown in gold on the board. " +
                "The selected pawn is shown in cyan, and capturable pawns in red."),

            ruleSection("Commands (console mode)",
                "- Placement: enter the target coordinate (e.g., D4)\n" +
                "- Movement: enter source then destination (e.g., A1 A4)\n" +
                "- Capture: prefix the coordinate with X (e.g., XB2)\n" +
                "- Input is case-insensitive.")
        );

        ScrollPane scroll = new ScrollPane(rulesContent);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(460);
        scroll.getStyleClass().add("rules-scroll");
        scroll.setPadding(new Insets(0, 8, 0, 8));

        Button closeBtn = new Button(closeLabel);
        closeBtn.getStyleClass().addAll("btn", "btn-back");
        closeBtn.setOnAction(e -> onClose.run());

        getChildren().addAll(title, scroll, closeBtn);
    }

    private VBox ruleSection(String heading, String body) {
        Text h = new Text(heading);
        h.getStyleClass().add("rules-heading");

        Text b = new Text(body);
        b.getStyleClass().add("rules-body");
        b.setWrappingWidth(700);

        // Thin separator line
        Rectangle sep = new Rectangle(700, 1);
        boolean dark = GameConfig.darkMode;
        sep.setFill(Color.web(dark ? "#3050aa30" : "#8B691430"));

        VBox section = new VBox(6, h, b, sep);
        section.setPadding(new Insets(4, 0, 4, 0));
        return section;
    }
}
