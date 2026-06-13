package view.javafx.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import model.GameConfig;
import view.javafx.SceneManager;

public class ModeSelectionScreen extends VBox {

    public ModeSelectionScreen(SceneManager manager) {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(50));
        getStyleClass().add("menu-root");

        Text title = new Text("Game mode");
        title.getStyleClass().add("screen-title");

        Text sub = new Text("Choose how you want to play");
        sub.getStyleClass().add("prompt-text");

        Region gap = new Region(); gap.setPrefHeight(16);

        Button hvhBtn  = makeBtn("Player vs Player", "btn btn-primary", 220);
        Button hvaiBtn = makeBtn("Player vs AI", "btn btn-primary", 220);
        Button aiaiBtn = makeBtn("AI vs AI", "btn btn-secondary", 220);
        Button backBtn = makeBtn("Back", "btn btn-back", 110);

        hvhBtn.setOnAction(e -> { manager.getConfig().mode = 0; manager.showColorSelection(); });
        hvaiBtn.setOnAction(e -> { manager.getConfig().mode = 1; manager.showColorSelection(); });
        aiaiBtn.setOnAction(e -> { manager.getConfig().mode = 2; manager.showAISelection(); });
        backBtn.setOnAction(e -> manager.showMenu());

        VBox btnBox = new VBox(14, hvhBtn, hvaiBtn, aiaiBtn);
        btnBox.setAlignment(Pos.CENTER);

        getChildren().addAll(title, sub, gap, btnBox, backBtn);
    }

    private Button makeBtn(String text, String classes, double width) {
        Button b = new Button(text);
        for (String c : classes.split(" ")) b.getStyleClass().add(c);
        b.setPrefWidth(width);
        return b;
    }
}
