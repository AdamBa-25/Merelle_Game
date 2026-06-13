package view.javafx.screens;

import control.MerelleDecider;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import model.GameConfig;
import model.MerellePawn;
import view.javafx.SceneManager;
import view.javafx.board.FXBoardView;

/**
 * AI selection - same circles as ColorSelectionScreen.
 * In AI vs AI mode, AI2 is rebuilt if AI1's color changes
 * to prevent both from choosing the same color.
 */
public class AISelectionScreen extends VBox {

    private static final String[] COLOR_NAMES = {
        "Steel", "Red", "Blue", "Green", "Gold", "Purple", "Cyan"
    };

    private final VBox ai2Container = new VBox();

    public AISelectionScreen(SceneManager manager, GameConfig cfg) {
        setAlignment(Pos.CENTER);
        setSpacing(24);
        setPadding(new Insets(40));
        getStyleClass().add("menu-root");

        Text title = new Text("Choose the AI algorithm");
        title.getStyleClass().add("screen-title");
        getChildren().add(title);

        if (cfg.mode == 2) {
            // AI 1 - when its color changes, we rebuild AI 2
            VBox ai1Box = buildAISelector("AI 1",
                val -> cfg.aiDifficulty1 = val, cfg.aiDifficulty1,
                cfg.colorJ1,
                idx -> {
                    cfg.colorJ1 = idx;
                    // Make sure AI2 does not have the same color
                    if (cfg.colorJ2 == idx) {
                        cfg.colorJ2 = pickOther(idx);
                    }
                    rebuildAI2(cfg);
                },
                -1);
            getChildren().add(ai1Box);

            // AI 2 - in a reloadable container
            ai2Container.setAlignment(Pos.CENTER);
            rebuildAI2(cfg);
            getChildren().add(ai2Container);

        } else {
            getChildren().add(buildAISelector("Computer (opponent)", val -> {
                cfg.aiDifficulty1 = val;
                cfg.aiDifficulty2 = val;
            }, cfg.aiDifficulty1, cfg.colorJ2, idx -> cfg.colorJ2 = idx, cfg.colorJ1));
        }

        Button playBtn = new Button("Play");
        playBtn.getStyleClass().addAll("btn", "btn-play");
        playBtn.setPrefWidth(220);
        playBtn.setOnAction(e -> {
            if (cfg.mode == 2) {
                MerelleDecider.aiDifficultyPerPlayer = new int[]{cfg.aiDifficulty1, cfg.aiDifficulty2};
                // AI names are assigned in FXGameController, not here
            } else {
                MerelleDecider.aiDifficultyPerPlayer = null;
                MerelleDecider.aiDifficulty = cfg.aiDifficulty1;
            }
            manager.showFirstPlayer();
        });

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("btn", "btn-back");
        backBtn.setOnAction(e -> {
            if (cfg.mode == 2) manager.showModeSelection();
            else manager.showColorSelection();
        });

        getChildren().addAll(playBtn, backBtn);
    }

    private void rebuildAI2(GameConfig cfg) {
        ai2Container.getChildren().clear();
        VBox ai2 = buildAISelector("AI 2",
            val -> cfg.aiDifficulty2 = val, cfg.aiDifficulty2,
            cfg.colorJ2,
            idx -> cfg.colorJ2 = idx,
            cfg.colorJ1);   // AI1's current color is forbidden for AI2
        ai2Container.getChildren().add(ai2);
    }

    private int pickOther(int forbidden) {
        for (int i = 0; i < MerellePawn.NB_COLORS; i++) {
            if (i != forbidden) return i;
        }
        return 1;
    }

    private VBox buildAISelector(String label, java.util.function.IntConsumer onAlgo, int initialAlgo, int initialColor, java.util.function.IntConsumer onColor, int forbiddenColor) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);

        Text lbl = new Text(label);
        lbl.getStyleClass().add("prompt-text");

        // ── Algorithm ──
        ToggleGroup group = new ToggleGroup();
        RadioButton minimax = new RadioButton("MiniMax");
        RadioButton alphaBeta = new RadioButton("Alpha-Beta");
        RadioButton mcts = new RadioButton("Monte Carlo");
        for (RadioButton rb : new RadioButton[]{minimax, alphaBeta, mcts}) {
            rb.setToggleGroup(group);
            rb.getStyleClass().add("radio-option");
        }
        switch (initialAlgo) {
            case MerelleDecider.DIFFICULTY_ALPHABETA:  alphaBeta.setSelected(true); break;
            case MerelleDecider.DIFFICULTY_MONTECARLO: mcts.setSelected(true);      break;
            default:                                    minimax.setSelected(true);   break;
        }
        minimax.setOnAction(e -> onAlgo.accept(MerelleDecider.DIFFICULTY_MINIMAX));
        alphaBeta.setOnAction(e -> onAlgo.accept(MerelleDecider.DIFFICULTY_ALPHABETA));
        mcts.setOnAction(e -> onAlgo.accept(MerelleDecider.DIFFICULTY_MONTECARLO));
        HBox algoRow = new HBox(24, minimax, alphaBeta, mcts);
        algoRow.setAlignment(Pos.CENTER);

        Text colorLbl = new Text("Color:");
        colorLbl.getStyleClass().add("prompt-text");

        int[] selectedColor = {initialColor};
        HBox colorRow = new HBox(16);
        colorRow.setAlignment(Pos.CENTER);
        VBox[] slots = new VBox[MerellePawn.NB_COLORS];

        boolean dark = GameConfig.darkMode;
        String normalBorder = dark ? "#4060cc" : "#8B6914";
        String hoverBorder  = dark ? "#8090ff" : "#DAA520";
        String hoverBg = dark ? "rgba(80,112,221,0.18)" : "rgba(218,165,32,0.18)";
        String baseStyle = "-fx-border-radius: 0; -fx-background-radius: 0; -fx-border-width: 2;";

        for (int i = 0; i < MerellePawn.NB_COLORS; i++) {
            final int idx = i;
            Color c = FXBoardView.fxColorOf(i);

            Circle circle = new Circle(28);
            circle.setFill(c);
            circle.setStroke(c.brighter());
            circle.setStrokeWidth(2);

            Text cname = new Text(COLOR_NAMES[i]);
            cname.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
            cname.setFill(Color.web(dark ? "#90a8e0" : "#C8966A"));

            VBox slot = new VBox(8, circle, cname);
            slot.setAlignment(Pos.CENTER);
            slot.setPadding(new Insets(10, 12, 10, 12));
            slots[i] = slot;

            // Grayed out only if the color is taken by the opponent
            boolean forbidden = (i == forbiddenColor);

            if (forbidden) {
                slot.setStyle(baseStyle +
                    "-fx-border-color: #222222;" +
                    "-fx-background-color: transparent;");
                slot.setOpacity(0.30);
            } else {
                updateSlotStyle(slot, i == selectedColor[0], dark, baseStyle, normalBorder, hoverBorder, hoverBg);
                slot.setStyle(slot.getStyle() + "-fx-cursor: hand;");

                slot.setOnMouseEntered(e -> {
                    if (idx != selectedColor[0])
                        slot.setStyle(baseStyle +
                            "-fx-border-color: " + hoverBorder + ";" +
                            "-fx-background-color: " + hoverBg + ";" +
                            "-fx-cursor: hand;");
                });
                slot.setOnMouseExited(e -> {
                    if (idx != selectedColor[0])
                        slot.setStyle(baseStyle +
                            "-fx-border-color: " + normalBorder + ";" +
                            "-fx-background-color: transparent;" +
                            "-fx-cursor: hand;");
                });
                slot.setOnMouseClicked(e -> {
                    updateSlotStyle(slots[selectedColor[0]], false, dark, baseStyle, normalBorder, hoverBorder, hoverBg);
                    selectedColor[0] = idx;
                    onColor.accept(idx);
                    updateSlotStyle(slot, true, dark, baseStyle, normalBorder, hoverBorder, hoverBg);
                });
            }

            colorRow.getChildren().add(slot);
        }

        box.getChildren().addAll(lbl, algoRow, colorLbl, colorRow);
        return box;
    }

    private void updateSlotStyle(VBox slot, boolean selected, boolean dark, String base, String normalBorder, String hoverBorder, String hoverBg) {
        if (selected) {
            slot.setStyle(base +
                "-fx-border-color: " + hoverBorder + ";" +
                "-fx-background-color: " + hoverBg + ";" +
                "-fx-cursor: hand;");
        } else {
            slot.setStyle(base +
                "-fx-border-color: " + normalBorder + ";" +
                "-fx-background-color: transparent;" +
                "-fx-cursor: hand;");
        }
    }
}
