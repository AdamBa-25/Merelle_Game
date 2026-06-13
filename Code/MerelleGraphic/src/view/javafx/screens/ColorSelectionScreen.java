package view.javafx.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import model.GameConfig;
import model.MerellePawn;
import view.javafx.SceneManager;
import view.javafx.board.FXBoardView;

/**
 * Color selection - straight edges, flat colors, no glow.
 * The "Help" checkbox appears here, alongside the color choice.
 */
public class ColorSelectionScreen extends VBox {

    private static final String[] COLOR_NAMES = {
        "Steel", "Red", "Blue", "Green", "Gold", "Purple", "Cyan"
    };

    private int step = 0;
    private int forbiddenColor = -1;
    private final GameConfig cfg;
    private final SceneManager manager;
    private TextField nameField;

    public ColorSelectionScreen(SceneManager manager, GameConfig cfg) {
        this.manager = manager;
        this.cfg = cfg;
        setAlignment(Pos.CENTER);
        setSpacing(32);
        setPadding(new Insets(50));
        getStyleClass().add("menu-root");
        buildUI();
    }

    private void buildUI() {
        getChildren().clear();

        Text title = new Text("Choose colors");
        title.getStyleClass().add("screen-title");

        Text prompt = new Text(getPromptForStep());
        prompt.getStyleClass().add("prompt-text");

        HBox colorRow = new HBox(16);
        colorRow.setAlignment(Pos.CENTER);

        for (int i = 0; i < MerellePawn.NB_COLORS; i++) {
            int colorIdx = i;
            boolean forbidden = (i == forbiddenColor);
            Color c = FXBoardView.fxColorOf(i);

            Circle circle = new Circle(28);
            circle.setFill(c);
            circle.setStroke(forbidden ? Color.web("#333333") : c.brighter());
            circle.setStrokeWidth(2);

            Text name = new Text(COLOR_NAMES[i]);
            name.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
            name.setFill(forbidden ? Color.web("#444444")
                                   : Color.web(GameConfig.darkMode ? "#90a8e0" : "#C8966A"));

            VBox slot = new VBox(8, circle, name);
            slot.setAlignment(Pos.CENTER);
            slot.setPadding(new Insets(10, 12, 10, 12));

            String baseStyle = "-fx-border-radius: 0;" + "-fx-background-radius: 0;" + "-fx-border-width: 2;";

            boolean dark = GameConfig.darkMode;
            String normalBorder = dark ? "#4060cc" : "#8B6914";
            String hoverBorder  = dark ? "#8090ff" : "#DAA520";
            String hoverBg = dark ? "rgba(80,112,221,0.18)" : "rgba(218,165,32,0.18)";

            if (forbidden) {
                slot.setStyle(baseStyle + "-fx-border-color: #222222;" + "-fx-background-color: transparent;");
                slot.setOpacity(0.30);
            } else {
                slot.setStyle(baseStyle +
                    "-fx-border-color: " + normalBorder + ";" +
                    "-fx-background-color: transparent;" +
                    "-fx-cursor: hand;");
                slot.setOnMouseEntered(e -> slot.setStyle(baseStyle +
                    "-fx-border-color: " + hoverBorder + ";" +
                    "-fx-background-color: " + hoverBg + ";" +
                    "-fx-cursor: hand;"));
                slot.setOnMouseExited(e -> slot.setStyle(baseStyle +
                    "-fx-border-color: " + normalBorder + ";" +
                    "-fx-background-color: transparent;" +
                    "-fx-cursor: hand;"));
                slot.setOnMouseClicked(e -> selectAndConfirm(colorIdx));
            }

            colorRow.getChildren().add(slot);
        }

        // Name field
        Text nameLbl = new Text(step == 0 ? "Your name (Player 1):" : "Your name (Player 2):");
        nameLbl.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        nameLbl.setFill(Color.web(GameConfig.darkMode ? "#90a8e0" : "#C8966A"));

        nameField = new TextField(step == 0 ? cfg.nameJ1 : cfg.nameJ2);
        nameField.setPromptText(step == 0 ? "Player 1" : "Player 2");
        nameField.setMaxWidth(220);
        nameField.setStyle(
            "-fx-background-color: " + (GameConfig.darkMode ? "#0d0d20" : "#2e1a08") + ";" +
            "-fx-text-fill: " + (GameConfig.darkMode ? "#c0d0ff" : "#F5DEB3") + ";" +
            "-fx-border-color: " + (GameConfig.darkMode ? "#4060cc" : "#8B6914") + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 0;" +
            "-fx-background-radius: 0;" +
            "-fx-font-size: 15px;" +
            "-fx-padding: 8 12;"
        );
        VBox nameBox = new VBox(6, nameLbl, nameField);
        nameBox.setAlignment(Pos.CENTER);

        Text hint = new Text("Click a color to select it");
        hint.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        hint.setFill(Color.web(GameConfig.darkMode ? "#5070aa" : "#8B6030"));

        VBox helpBox = new VBox(8);
        helpBox.setAlignment(Pos.CENTER);
        if (cfg.mode == 0) {
            String playerLabel = (step == 0) ? cfg.nameJ1 : cfg.nameJ2;
            boolean currentHelp = (step == 0) ? cfg.helpEnabledJ1 : cfg.helpEnabledJ2;

            CheckBox helpCheck = new CheckBox("Help for " + playerLabel + " (show possible moves)");
            helpCheck.setSelected(currentHelp);
            helpCheck.setStyle(
                "-fx-text-fill: " + (GameConfig.darkMode ? "#90a8e0" : "#F5DEB3") + ";" +
                "-fx-font-family: Arial;" +
                "-fx-font-size: 13px;"
            );
            helpCheck.setOnAction(e -> {
                if (step == 0) cfg.helpEnabledJ1 = helpCheck.isSelected();
                else           cfg.helpEnabledJ2 = helpCheck.isSelected();
            });
            helpBox.getChildren().add(helpCheck);
        } else if (cfg.mode == 1) {
            CheckBox helpCheck = new CheckBox("Help (show possible moves)");
            helpCheck.setSelected(cfg.helpEnabledJ1);
            helpCheck.setStyle(
                "-fx-text-fill: " + (GameConfig.darkMode ? "#90a8e0" : "#F5DEB3") + ";" +
                "-fx-font-family: Arial;" +
                "-fx-font-size: 13px;"
            );
            helpCheck.setOnAction(e -> cfg.helpEnabledJ1 = helpCheck.isSelected());
            helpBox.getChildren().add(helpCheck);
        }

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("btn", "btn-back");
        backBtn.setOnAction(e -> manager.showModeSelection());

        getChildren().addAll(title, prompt, nameBox, colorRow, hint, helpBox, backBtn);
    }

    private void selectAndConfirm(int colorIdx) {
        String name = (nameField != null && !nameField.getText().isBlank())
                      ? nameField.getText().trim() : null;
        if (cfg.mode == 0 && step == 0) {
            cfg.nameJ1 = (name != null) ? name : "Player 1";
            cfg.colorJ1 = colorIdx;
            step = 1;
            forbiddenColor = colorIdx;
            buildUI();
        } else if (cfg.mode == 0) {
            cfg.nameJ2  = (name != null) ? name : "Player 2";
            cfg.colorJ2 = colorIdx;
            manager.showFirstPlayer();
        } else {
            cfg.nameJ1 = (name != null) ? name : "Player 1";
            cfg.colorJ1 = colorIdx;
            cfg.colorJ2 = pickColorForAI(colorIdx);
            manager.showAISelection();
        }
    }

    private String getPromptForStep() {
        if (cfg.mode == 0)
            return step == 0
                ? "Player 1, choose your color:"
                : "Player 2, choose your color:";
        return "Player 1, choose your color:";
    }

    private int pickColorForAI(int humanColor) {
        for (int i = 0; i < MerellePawn.NB_COLORS; i++) if (i != humanColor) return i;
        return MerellePawn.PAWN_RED;
    }
}
