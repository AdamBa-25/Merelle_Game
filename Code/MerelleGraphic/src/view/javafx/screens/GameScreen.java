package view.javafx.screens;

import control.FXGameController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.GameConfig;
import view.javafx.SceneManager;
import view.javafx.board.FXBoardView;
import view.javafx.components.InfoPanel;
import view.javafx.components.ReservePanel;

import java.util.Optional;

/**
 * Game screen — Pause button in the info bar (AI vs AI),
 * Help option chosen before the game from FirstPlayerScreen.
 *
 * The rules are displayed as an overlay on top of this screen so that
 * showing them does not interrupt or reset the ongoing game.
 */
public class GameScreen extends StackPane {

    public GameScreen(SceneManager manager, GameConfig cfg) {
        boolean dark = GameConfig.darkMode;

        String bgColor = dark ? "#03030a" : "#1A0E05";
        String infoColor = dark ? "#06061a" : "#2C1A0E";
        String borderColor = dark ? "#4060cc" : "#8B5E1A";

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + bgColor + ";");

        InfoPanel    infoPanel    = new InfoPanel();
        infoPanel.setColors(cfg.colorJ1, cfg.colorJ2);
        ReservePanel reservePanel = new ReservePanel();

        FXGameController[] ctrlHolder = new FXGameController[1];

        FXBoardView board = new FXBoardView(pos -> {
            if (ctrlHolder[0] != null) ctrlHolder[0].onBoardClick(pos);
        });

        FXGameController ctrl = new FXGameController(cfg, board, infoPanel, reservePanel);
        ctrlHolder[0] = ctrl;
        ctrl.setOnGameEnd(() -> showEndDialog(manager, ctrl, cfg));

        StackPane rulesOverlay = new StackPane();
        rulesOverlay.setVisible(false);
        rulesOverlay.setManaged(false);
        rulesOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");

        RulesScreen rulesScreen = new RulesScreen(() -> hideOverlay(rulesOverlay));
        rulesScreen.setMaxWidth(820);
        rulesScreen.setMaxHeight(620);
        StackPane.setAlignment(rulesScreen, Pos.CENTER);
        rulesOverlay.getChildren().add(rulesScreen);

        MenuBar menuBar = buildMenuBar(manager, cfg, ctrl, rulesOverlay);

        HBox infoBar = buildInfoBar(infoPanel, reservePanel, cfg, infoColor, borderColor);


        if (cfg.mode == 2) {
            Button pauseBtn = new Button("Pause");
            pauseBtn.getStyleClass().addAll("btn", "btn-pause");
            pauseBtn.setOnAction(e -> {
                ctrl.togglePause();
                pauseBtn.setText(ctrl.isPaused() ? "Resume" : "Pause");
            });
            ctrl.setOnPauseStateChanged(() ->
                pauseBtn.setText(ctrl.isPaused() ? "Resume" : "Pause")
            );
            infoBar.getChildren().add(pauseBtn);
            HBox.setMargin(pauseBtn, new Insets(0, 8, 0, 8));
        }

        board.setStyle("-fx-background-color: transparent;");
        board.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        BorderPane centerPane = new BorderPane();
        centerPane.setTop(infoBar);
        centerPane.setCenter(board);
        centerPane.setStyle("-fx-background-color: " + bgColor + ";");
        BorderPane.setAlignment(board, Pos.CENTER);

        root.setTop(menuBar);
        root.setCenter(centerPane);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        getChildren().addAll(root, rulesOverlay);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    private void showOverlay(StackPane overlay) {
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    private void hideOverlay(StackPane overlay) {
        overlay.setVisible(false);
        overlay.setManaged(false);
    }

    private MenuBar buildMenuBar(SceneManager manager, GameConfig cfg, FXGameController ctrl, StackPane rulesOverlay) {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("top-menubar");
        menuBar.setMaxWidth(Double.MAX_VALUE);

        Menu optionMenu = new Menu("Options");

        MenuItem newGame = new MenuItem("New game");
        newGame.setOnAction(e -> manager.showGame());

        MenuItem rules = new MenuItem("Game rules");
        rules.setOnAction(e -> showOverlay(rulesOverlay));

        MenuItem mainMenu = new MenuItem("Main menu");
        mainMenu.setOnAction(e -> confirmReturnToMenu(manager));

        optionMenu.getItems().addAll(newGame, rules, new SeparatorMenuItem(), mainMenu);

        Menu quitMenu = new Menu("Quit");
        MenuItem quitItem = new MenuItem("Close the game");
        quitItem.setOnAction(e -> javafx.application.Platform.exit());
        quitMenu.getItems().add(quitItem);

        menuBar.getMenus().addAll(optionMenu, quitMenu);
        return menuBar;
    }

    private HBox buildInfoBar(InfoPanel infoPanel, ReservePanel reservePanel,
                               GameConfig cfg, String bg, String border) {
        HBox infoArea = infoPanel.asTopBar();
        HBox.setHgrow(infoArea, Priority.ALWAYS);
        HBox reserveArea = reservePanel.asTopBar(cfg.colorJ1, cfg.colorJ2);

        HBox bar = new HBox(0, infoArea, reserveArea);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-width: 0 0 2 0;" +
            "-fx-border-radius: 0;" +
            "-fx-background-radius: 0;" +
            "-fx-padding: 8 10 8 10;"
        );
        bar.setPrefHeight(48);
        bar.setMinHeight(48);
        bar.setMaxHeight(48);
        return bar;
    }

    private void confirmReturnToMenu(SceneManager manager) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Quit game");
        confirm.setHeaderText("Return to the main menu?");
        confirm.setContentText("The current game will be lost.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.YES) manager.showMenu();
    }

    private void showEndDialog(SceneManager manager, FXGameController ctrl, GameConfig cfg) {
        boardifier.model.Model model = ctrl.getModel();
        String msg;
        if (model.getIdWinner() >= 0) {
            String winner = model.getPlayers().get(model.getIdWinner()).getName();
            msg = winner + " wins the game!";
            if (model.getIdWinner() == 0) GameConfig.winsJ1++;
            else                          GameConfig.winsJ2++;
        } else {
            msg = "Draw (position repetition)!";
        }

        String name1 = GameConfig.lastNameJ1.isEmpty() ? cfg.nameJ1 : GameConfig.lastNameJ1;
        String name2 = GameConfig.lastNameJ2.isEmpty() ? cfg.nameJ2 : GameConfig.lastNameJ2;
        String scoreMsg = name1 + " : " + GameConfig.winsJ1
                        + "   —   " + name2 + " : " + GameConfig.winsJ2;

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Game over");
        alert.setHeaderText(msg);
        alert.setContentText(scoreMsg);

        ButtonType replay  = new ButtonType("Play again");
        ButtonType menuBtn = new ButtonType("Main menu");
        ButtonType quit    = new ButtonType("Quit");
        alert.getButtonTypes().setAll(replay, menuBtn, quit);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == replay)       manager.showGame();
            else if (result.get() == menuBtn) manager.showMenu();
            else javafx.application.Platform.exit();
        }
    }
}
