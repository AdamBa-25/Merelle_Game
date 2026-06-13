package view.javafx.screens;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import model.GameConfig;
import view.javafx.SceneManager;

/**
 * Main screen - board preview, colored buttons, flat background, straight edges.
 */
public class MainMenuScreen extends VBox {

    public MainMenuScreen(SceneManager manager) {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(0);
        getStyleClass().add("menu-root");

        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("top-menubar");
        menuBar.setMaxWidth(Double.MAX_VALUE);
        Menu optionsMenu = new Menu("Options");

        CheckMenuItem darkModeItem = new CheckMenuItem("Dark mode");
        darkModeItem.setSelected(GameConfig.darkMode);
        darkModeItem.setOnAction(e -> {
            GameConfig.darkMode = darkModeItem.isSelected();
            manager.showMenu();
        });

        MenuItem rulesItem = new MenuItem("Game rules");
        rulesItem.setOnAction(e -> manager.showRules());
        MenuItem quitItem = new MenuItem("Quit");
        quitItem.setOnAction(e -> Platform.exit());

        optionsMenu.getItems().addAll(darkModeItem, rulesItem, new SeparatorMenuItem(), quitItem);
        menuBar.getMenus().add(optionsMenu);

        Region topDecor = makeDecorLine();

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(30, 50, 0, 50));
        VBox.setVgrow(center, Priority.ALWAYS);

        Text title = new Text("Nine Men's Morris");
        title.getStyleClass().add("title");

        Text subtitle = new Text("— Nine Men's Morris —");
        subtitle.getStyleClass().add("subtitle");

        Canvas boardCanvas = makeBoardCanvas(280);

        Button playBtn  = bigBtn("Play",  "btn-play",   220);
        Button rulesBtn = bigBtn("Rules", "btn-rules",  220);
        Button quitBtn  = bigBtn("Quit",  "btn-quit",   220);

        playBtn.setOnAction(e -> manager.showModeSelection());
        rulesBtn.setOnAction(e -> manager.showRules());
        quitBtn.setOnAction(e -> Platform.exit());

        HBox scoreRow = buildScoreRow();

        VBox btnBox = new VBox(18, playBtn, rulesBtn, quitBtn);
        btnBox.setAlignment(Pos.CENTER);

        center.getChildren().addAll(
            title, spacer(6), subtitle, spacer(22),
            boardCanvas, spacer(18),
            scoreRow, spacer(10),
            btnBox
        );

        getChildren().addAll(menuBar, topDecor, center, spacer(30));
    }

    private Canvas makeBoardCanvas(double sz) {
        Canvas c = new Canvas(sz, sz);
        drawBoardPreview(c.getGraphicsContext2D(), sz);
        return c;
    }

    private void drawBoardPreview(GraphicsContext gc, double sz) {
        boolean dark = GameConfig.darkMode;

        gc.clearRect(0, 0, sz, sz);

        Color boardBg;
        Color lineColor;
        Color nodeColor;
        Color nodeFill;
        Color strokeBorder;

        if (dark) {
            boardBg = Color.web("#08080f");
            lineColor = Color.web("#3858a8");   // blue - same as the rest of dark mode
            nodeColor = Color.web("#5070c0");
            nodeFill = Color.web("#0a0c22");
            strokeBorder = Color.web("#3858a8");   // blue outline in dark mode
        } else {
            boardBg = Color.web("#6B3D14");   // flat wood
            lineColor = Color.web("#8B5E1A");
            nodeColor = Color.web("#DAA520");
            nodeFill = Color.web("#4A2808");
            strokeBorder = Color.web("#5A3010");   // dark wood outline
        }

        gc.setFill(boardBg);
        gc.fillRect(4, 4, sz - 8, sz - 8);
        gc.setStroke(strokeBorder);
        gc.setLineWidth(2);
        gc.strokeRect(4, 4, sz - 8, sz - 8);

        double margin = sz * 0.10;
        double boardW  = sz - 2 * margin;
        double cell = boardW / 6.0;
        double cx = sz / 2;
        double[] offsets = {margin, margin + cell, margin + cell * 2};

        for (double off : offsets) {
            double bsz = sz - 2 * off;
            gc.setStroke(lineColor);
            gc.setLineWidth(1.5);
            gc.strokeRect(off, off, bsz, bsz);
        }

        double innerTop = offsets[2];
        double innerBottom = sz - offsets[2];
        double innerLeft = offsets[2];
        double innerRight  = sz - offsets[2];
        double outerTop = offsets[0];
        double outerBottom = sz - offsets[0];
        double outerLeft = offsets[0];
        double outerRight = sz - offsets[0];

        gc.setStroke(lineColor);
        gc.setLineWidth(1.5);
        // Top
        gc.strokeLine(cx, outerTop,    cx, innerTop);
        // Bottom
        gc.strokeLine(cx, innerBottom, cx, outerBottom);
        // Left
        gc.strokeLine(outerLeft,  cx, innerLeft,  cx);
        // Right
        gc.strokeLine(innerRight, cx, outerRight, cx);

        int[][] pts = {
            {0,0},{0,3},{0,6},{3,0},{3,6},{6,0},{6,3},{6,6},
            {1,1},{1,3},{1,5},{3,1},{3,5},{5,1},{5,3},{5,5},
            {2,2},{2,3},{2,4},{3,2},{3,4},{4,2},{4,3},{4,4}
        };
        for (int[] pt : pts) {
            double nx = margin + pt[1] * cell;
            double ny = margin + pt[0] * cell;
            gc.setFill(nodeColor);
            gc.fillOval(nx - 5, ny - 5, 10, 10);
            gc.setStroke(nodeFill.brighter());
            gc.setLineWidth(1);
            gc.strokeOval(nx - 5, ny - 5, 10, 10);
        }
    }

    private HBox buildScoreRow() {
        HBox row = new HBox(32);
        row.setAlignment(Pos.CENTER);
        int w1 = GameConfig.winsJ1, w2 = GameConfig.winsJ2;
        if (w1 == 0 && w2 == 0) return row;

        boolean dark = GameConfig.darkMode;
        String nameJ1 = GameConfig.lastNameJ1.isEmpty() ? "Player 1" : GameConfig.lastNameJ1;
        String nameJ2 = GameConfig.lastNameJ2.isEmpty() ? "Player 2" : GameConfig.lastNameJ2;

        row.getChildren().addAll(
            scoreChip(nameJ1, w1, dark),
            scoreSep(dark),
            scoreChip(nameJ2, w2, dark)
        );
        return row;
    }

    private VBox scoreChip(String name, int wins, boolean dark) {
        Text nameTxt = new Text(name);
        nameTxt.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        nameTxt.setFill(Color.web(dark ? "#90a8e0" : "#DAA520"));

        Text winsTxt = new Text(wins + " win" + (wins > 1 ? "s" : ""));
        winsTxt.setFont(Font.font("Arial", 13));
        winsTxt.setFill(Color.web(dark ? "#7090c8" : "#B8860B"));

        VBox chip = new VBox(3, nameTxt, winsTxt);
        chip.setAlignment(Pos.CENTER);
        chip.setPadding(new Insets(8, 18, 8, 18));
        chip.setStyle(
            "-fx-border-radius: 0;" +
            "-fx-background-radius: 0;" +
            "-fx-border-width: 1;" +
            "-fx-border-color: " + (dark ? "#3050bb55" : "#DAA52055") + ";" +
            "-fx-background-color: " + (dark ? "#10102855" : "#2e1a0844") + ";"
        );
        return chip;
    }

    private Region scoreSep(boolean dark) {
        Region r = new Region();
        r.setPrefWidth(2);
        r.setPrefHeight(36);
        r.setStyle("-fx-background-color: " + (dark ? "#4060cc44" : "#DAA52044") + ";");
        return r;
    }

    private Button bigBtn(String text, String styleClass, double width) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", styleClass);
        b.setPrefWidth(width);
        return b;
    }

    private Region makeDecorLine() {
        Region r = new Region();
        r.setPrefHeight(2);
        r.setMaxWidth(Double.MAX_VALUE);
        // Solid line, no gradient
        r.setStyle(GameConfig.darkMode
            ? "-fx-background-color: #3858a8;"
            : "-fx-background-color: #7A5010;");
        return r;
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setPrefHeight(h);
        return r;
    }
}
