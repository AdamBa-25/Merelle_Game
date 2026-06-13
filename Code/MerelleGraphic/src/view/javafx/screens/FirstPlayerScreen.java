package view.javafx.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import model.GameConfig;
import view.javafx.SceneManager;
import view.javafx.board.FXBoardView;

/**
 * Who starts? - large, readable buttons.
 * Help is chosen earlier (ColorSelectionScreen / AISelectionScreen).
 */
public class FirstPlayerScreen extends VBox {

    public FirstPlayerScreen(SceneManager manager, GameConfig cfg) {
        setAlignment(Pos.CENTER);
        setSpacing(28);
        setPadding(new Insets(50));
        getStyleClass().add("menu-root");
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Text title = new Text("Who starts?");
        title.getStyleClass().add("screen-title");

        Text sub = new Text("Choose which player will go first");
        sub.getStyleClass().add("prompt-text");

        String nameP1, nameP2;
        if (cfg.mode == 2) {
            nameP1 = GameConfig.aiName(cfg.aiDifficulty1);
            nameP2 = GameConfig.aiName(cfg.aiDifficulty2);
        } else if (cfg.mode == 1) {
            nameP1 = cfg.nameJ1;
            nameP2 = GameConfig.aiName(cfg.aiDifficulty1);
        } else {
            nameP1 = cfg.nameJ1;
            nameP2 = cfg.nameJ2;
        }

        HBox j1Row = makePlayerBtn(nameP1, cfg.colorJ1, () -> {
            cfg.firstPlayer = 0;
            manager.startGameWithFirstPlayer();
        });

        HBox j2Row = makePlayerBtn(nameP2, cfg.colorJ2, () -> {
            cfg.firstPlayer = 1;
            manager.startGameWithFirstPlayer();
        });

        Button randBtn = new Button("Random");
        randBtn.getStyleClass().addAll("btn", "btn-secondary");
        randBtn.setPrefWidth(270);
        randBtn.setMaxWidth(270);
        randBtn.setOnAction(e -> {
            cfg.firstPlayer = new java.util.Random().nextInt(2);
            manager.startGameWithFirstPlayer();
        });

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("btn", "btn-back");
        backBtn.setOnAction(e -> manager.showPreviousBeforeGame(cfg));

        VBox btnBox = new VBox(16, j1Row, j2Row, randBtn);
        btnBox.setAlignment(Pos.CENTER);

        getChildren().addAll(title, sub, btnBox, backBtn);
    }

    private HBox makePlayerBtn(String name, int color, Runnable action) {
        Color fxColor = FXBoardView.fxColorOf(color);

        Circle dot = new Circle(14);
        dot.setFill(fxColor);
        dot.setStroke(fxColor.brighter());
        dot.setStrokeWidth(2);

        Text nameTxt = new Text("  " + name);
        nameTxt.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        nameTxt.setFill(Color.web(GameConfig.darkMode ? "#c8d8ff" : "#F5DEB3"));

        HBox row = new HBox(10, dot, nameTxt);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefWidth(270);
        row.setMaxWidth(270);
        row.setPrefHeight(40);
        row.setPadding(new Insets(0, 20, 0, 20));
        row.setStyle(
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-border-width: 1;" +
            "-fx-border-color: " + toHex(fxColor.darker()) + ";" +
            "-fx-background-color: " + toHex(fxColor.deriveColor(0, 1, 0.12, 0.7)) + ";" +
            "-fx-cursor: hand;"
        );
        row.setOnMouseClicked(e -> action.run());
        return row;
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }
}
