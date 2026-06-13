package view.javafx;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.GameConfig;
import view.javafx.screens.*;

/**
 * Central navigation manager. Holds the primary Stage and switches scenes.
 */
public class SceneManager {

    private final Stage stage;
    private GameConfig currentConfig;

    public SceneManager(Stage stage) {
        this.stage = stage;
        this.currentConfig = new GameConfig();
    }

    public void showMenu() { setScene(new MainMenuScreen(this), 1080, 790); }
    public void showModeSelection() { setScene(new ModeSelectionScreen(this), 1080, 790); }
    public void showColorSelection() { setScene(new ColorSelectionScreen(this, currentConfig), 1080, 790); }
    public void showAISelection() { setScene(new AISelectionScreen(this, currentConfig),    1080, 790); }
    /** Theme selection removed - always wood. */
    public void showThemeSelection()  { showGame(); }

    /** Shows the "who starts" screen before launching the game */
    public void showFirstPlayer() {
        FirstPlayerScreen screen = new FirstPlayerScreen(this, currentConfig);
        setScene(screen, 1080, 790);
    }

    /** Launches the game while respecting cfg.firstPlayer */
    public void startGameWithFirstPlayer() {
        showGame(); // firstPlayer will be read in FXGameController
    }

    /** Returns from FirstPlayerScreen to the appropriate previous screen */
    public void showPreviousBeforeGame(GameConfig cfg) {
        if (cfg.mode == 2) showAISelection();
        else if (cfg.mode == 1) showAISelection();
        else showColorSelection();
    }
    public void showRules() { setScene(new RulesScreen(this), 1080, 790); }

    public void showGame() {
        currentConfig.boardTheme = 0;
        GameScreen gameScreen = new GameScreen(this, currentConfig);
        gameScreen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Scene scene = new Scene(gameScreen, 1080, 790);
        applyStylesheets(scene);
        stage.setScene(scene);
        stage.setMinWidth(650);
        stage.setMinHeight(520);
        stage.setResizable(true);
    }

    public void setConfig(GameConfig cfg)  { this.currentConfig = cfg; }
    public GameConfig getConfig() { return currentConfig; }

    private void setScene(javafx.scene.layout.Region root, double w, double h) {
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Scene scene = new Scene(root, w, h);
        applyStylesheets(scene);
        applySceneBackground(scene);
        stage.setScene(scene);
        stage.sizeToScene();
    }

    /** Scene background color (clears the default JavaFX white) */
    private void applySceneBackground(Scene scene) {
        if (GameConfig.darkMode) {
            scene.setFill(Color.web("#03030a"));
        } else {
            scene.setFill(Color.web("#160c03"));
        }
    }

    /** Loads the base CSS + the dark CSS if enabled. */
    private void applyStylesheets(Scene scene) {
        try {
            String base = getClass().getResource("/style/style.css").toExternalForm();
            scene.getStylesheets().add(base);
        } catch (Exception ignored) {}
        if (GameConfig.darkMode) {
            try {
                String dark = getClass().getResource("/style/style-dark.css").toExternalForm();
                scene.getStylesheets().add(dark);
            } catch (Exception ignored) {}
        }
    }
}
