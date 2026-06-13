package control;

import boardifier.control.StageFactory;
import boardifier.model.GameException;
import boardifier.model.Model;
import boardifier.model.Player;
import boardifier.view.View;
import javafx.application.Platform;
import javafx.concurrent.Task;
import model.*;
import view.javafx.board.FXBoardView;
import view.javafx.components.InfoPanel;
import view.javafx.components.ReservePanel;

import java.util.Collections;

/**
 * Event-driven game controller for the JavaFX version.
 * Replaces the blocking stageLoop() / stdin with click handlers + background Tasks for AI.
 */
public class FXGameController {

    private final Model model;
    private final MerelleController controller;
    private final FXBoardView boardView;
    private final InfoPanel infoPanel;
    private final ReservePanel reservePanel;
    private final GameConfig cfg;

    private Integer selectedPos = null; // first click in movement phase
    private Runnable onGameEnd;
    private boolean paused = false;
    private javafx.animation.PauseTransition pendingAIDelay = null;

    /** Returns whether help is enabled for the player with the given index. */
    private boolean isHelpEnabledFor(int playerIndex) {
        if (playerIndex == 0) return cfg.helpEnabledJ1;
        if (playerIndex == 1) return cfg.helpEnabledJ2;
        return false;
    }

    public FXGameController(GameConfig cfg, FXBoardView boardView, InfoPanel infoPanel, ReservePanel reservePanel) {
        this.cfg = cfg;
        this.boardView = boardView;
        this.infoPanel = infoPanel;
        this.reservePanel = reservePanel;

        // Register stage model + (console) view so StageFactory can instantiate via reflection
        StageFactory.registerModelAndView("merelle", "model.MerelleStageModel", "view.MerelleStageView");

        model = new Model();
        switch (cfg.mode) {
            case 0:
                model.addHumanPlayer(cfg.nameJ1);
                model.addHumanPlayer(cfg.nameJ2);
                break;
            case 1:
                model.addHumanPlayer(cfg.nameJ1);
                model.addComputerPlayer(GameConfig.aiName(cfg.aiDifficulty1));
                break;
            default:
                model.addComputerPlayer(GameConfig.aiName(cfg.aiDifficulty1));
                model.addComputerPlayer(GameConfig.aiName(cfg.aiDifficulty2));
                break;
        }

        MerelleStageFactory.setColors(cfg.colorJ1, cfg.colorJ2);
        infoPanel.setColors(cfg.colorJ1, cfg.colorJ2);

        // Configure AI difficulties
        if (cfg.mode == 2) {
            MerelleDecider.aiDifficultyPerPlayer = new int[]{cfg.aiDifficulty1, cfg.aiDifficulty2};
        } else {
            MerelleDecider.aiDifficultyPerPlayer = null;
            MerelleDecider.aiDifficulty = cfg.aiDifficulty1;
        }

        View view = new View(model);
        controller = new MerelleController(model, view);
        controller.setFirstStageName("merelle");

        // Remember the names for display in the menu
        GameConfig.lastNameJ1 = cfg.nameJ1;
        GameConfig.lastNameJ2 = cfg.nameJ2;

        try {
            controller.startGame();
        } catch (GameException e) {
            infoPanel.showError("Initialization error: " + e.getMessage());
            return;
        }

        // First player according to the user's choice (or random if -1)
        int startPlayer = (cfg.firstPlayer >= 0 && cfg.firstPlayer < model.getPlayers().size()) ? cfg.firstPlayer : new java.util.Random().nextInt(model.getPlayers().size());
        model.setIdPlayer(startPlayer);

        refreshUI();
        // Clearly announce who starts
        String starterName = model.getCurrentPlayerName();
        infoPanel.showMessage(starterName + " starts!");
        playNextTurnIfAI();
    }

    public void setOnGameEnd(Runnable r) { this.onGameEnd = r; }

    private Runnable onPauseStateChanged;
    public void setOnPauseStateChanged(Runnable r) { this.onPauseStateChanged = r; }
    public boolean isPaused() { return paused; }

    /** Toggle pause. Only meaningful in AI vs AI mode. */
    public void togglePause() {
        if (cfg.mode != 2) return;
        paused = !paused;
        if (onPauseStateChanged != null) onPauseStateChanged.run();
        if (!paused) {
            // Resume: re-trigger AI turn
            infoPanel.setStatus(model.getCurrentPlayer().getName() + " is thinking...");
            playNextTurnIfAI();
        } else {
            // Pause: cancel any pending delay
            if (pendingAIDelay != null) {
                pendingAIDelay.stop();
                pendingAIDelay = null;
            }
            infoPanel.showMessage("Game paused");
        }
    }

    /** Called by FXBoardView when the user clicks a node. */
    public void onBoardClick(int pos) {
        if (model.isEndStage()) return;

        Player current = model.getCurrentPlayer();
        if (current.getType() == Player.COMPUTER) return;

        MerelleStageModel stage = getStage();
        boolean ok = false;

        if (stage.isMillJustFormed()) {
            ok = controller.analyseAndPlay("X" + posToCoord(pos));
            if (ok) lastPlayedPos = -1; // capture, no new mill
        } else if (stage.getCurrentPhase() == MerelleStageModel.PHASE_PLACEMENT) {
            ok = controller.analyseAndPlay(posToCoord(pos));
            if (ok) lastPlayedPos = pos;
        } else {
            // Movement: 2-click protocol
            if (selectedPos == null) {
                MerelleBoard board = stage.getBoard();
                MerellePawn pawn = board.getPawnAt(pos);
                int myColor = (model.getIdPlayer() == 0) ? stage.getColorJ1() : stage.getColorJ2();
                if (pawn == null || pawn.getColor() != myColor) {
                    infoPanel.showError("Select one of your pawns.");
                    return;
                }
                selectedPos = pos;
                boardView.highlightSelected(pos);
                infoPanel.setStatus("Choose the destination.");
                // If help is enabled for this player, show the possible destinations
                if (isHelpEnabledFor(model.getIdPlayer())) showPlayableHints(stage);
                return;
            } else {
                int destPos = pos;
                String move = posToCoord(selectedPos) + " " + posToCoord(pos);
                ok = controller.analyseAndPlay(move);
                if (ok) lastPlayedPos = destPos;
                selectedPos = null;
                boardView.clearHighlight();
            }
        }

        if (ok) {
            afterMove();
        } else {
            infoPanel.showError("Invalid move, try again.");
            selectedPos = null;
            if (stage.isMillJustFormed()) {
                // Keep the capturable-pawn highlight visible until a valid
                // capture is made, instead of clearing it on a misclick.
                int playerId = model.getIdPlayer();
                int oppColor = (playerId == 0) ? stage.getColorJ2() : stage.getColorJ1();
                boardView.highlightCapturable(stage.getBoard(), oppColor);
            } else {
                boardView.clearHighlight();
            }
        }
    }

    // Position of the last move played - used to find the right mill to highlight
    private int lastPlayedPos = -1;

    private void afterMove() {
        controller.endOfTurn();
        refreshUI();

        if (model.isEndStage()) {
            boardView.clearHighlight();
            if (onGameEnd != null) Platform.runLater(onGameEnd);
            return;
        }

        MerelleStageModel stage = getStage();
        if (stage.isMillJustFormed()) {
            int playerId = model.getIdPlayer();
            int playerColor = (playerId == 0) ? stage.getColorJ1() : stage.getColorJ2();
            int oppColor = (playerId == 0) ? stage.getColorJ2() : stage.getColorJ1();

            boardView.clearHighlight();
            // Highlight ONLY the mill that contains the position of the last move
            if (lastPlayedPos >= 0) {
                int[] mill = stage.getBoard().getMillContaining(lastPlayedPos, playerColor);
                if (mill != null) boardView.highlightMill(mill);
            }
            boardView.highlightCapturable(stage.getBoard(), oppColor);
            infoPanel.showMessage(">>> MILL! Capture an opponent's pawn (click).");
        } else {
            boardView.clearHighlight();
        }

        playNextTurnIfAI();
    }

    private void playNextTurnIfAI() {
        if (model.isEndStage()) return;
        if (paused) return;
        Player current = model.getCurrentPlayer();
        if (current.getType() != Player.COMPUTER) return;

        infoPanel.setStatus(current.getName() + " is thinking...");

        // Visible delay before the AI plays (800ms for Player vs AI, 1000ms for AI vs AI)
        int delayMs = (cfg.mode == 2) ? 1000 : 800;
        pendingAIDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(delayMs));
        pendingAIDelay.setOnFinished(ev -> {
            pendingAIDelay = null;
            if (!paused) launchAITask();
        });
        pendingAIDelay.play();
    }

    private void launchAITask() {
        if (model.isEndStage()) return;
        if (paused) return;
        Player current = model.getCurrentPlayer();

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                MerelleDecider decider = new MerelleDecider(model, controller);
                return decider.getDecision(getStage(), model.getIdPlayer());
            }
        };

        task.setOnSucceeded(e -> {
            String move = task.getValue();
            boolean ok = controller.analyseAndPlay(move);
            if (!ok) {
                Platform.runLater(() -> infoPanel.showError("AI error: invalid move (" + move + ")"));
                return;
            }
            // Extract the destination position from the AI's decision
            Platform.runLater(() -> {
                lastPlayedPos = extractDestPos(move);
                afterMove();
                // For AI vs AI, the loop continues via playNextTurnIfAI() (which already adds the delay)
            });
        });

        task.setOnFailed(e -> Platform.runLater(() ->
                infoPanel.showError("AI error: " + task.getException().getMessage())));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void refreshUI() {
        MerelleStageModel stage = getStage();
        boardView.refresh(stage.getBoard());
        infoPanel.update(model, stage);
        reservePanel.update(stage, cfg.colorJ1, cfg.colorJ2);
        showPlayableHints(stage);
    }

    /** Highlights possible destinations (help) - movement phase only. */
    private void showPlayableHints(MerelleStageModel stage) {
        if (!isHelpEnabledFor(model.getIdPlayer())) {
            boardView.highlightPlayable(Collections.emptyList());
            return;
        }
        if (model.isEndStage()) return;
        Player current = model.getCurrentPlayer();
        if (current.getType() == Player.COMPUTER) {
            boardView.highlightPlayable(Collections.emptyList());
            return;
        }
        if (stage.isMillJustFormed()) {
            boardView.highlightPlayable(Collections.emptyList());
            return;
        }

        if (stage.getCurrentPhase() == MerelleStageModel.PHASE_PLACEMENT) {
            boardView.highlightPlayable(Collections.emptyList());
            return;
        }

        // Movement phase: only show destinations if a pawn is selected
        java.util.Set<Integer> playable = new java.util.HashSet<>();
        if (selectedPos != null) {
            MerelleBoard board = stage.getBoard();
            int playerId = model.getIdPlayer();
            int myColor = (playerId == 0) ? stage.getColorJ1() : stage.getColorJ2();
            int pawns = board.countPawns(myColor);
            boolean canFly = (pawns <= 3);
            for (int dest = 0; dest < 24; dest++) {
                if (board.getPawnAt(dest) == null) {
                    if (canFly || board.isAdjacent(selectedPos, dest)) {
                        // Filter out moves that would reform the same mill
                        // (they would be rejected by analyseAndPlay anyway)
                        if (!MerelleController.wouldReformSameMill(board, selectedPos, dest, myColor, playerId, stage)) {
                            playable.add(dest);
                        }
                    }
                }
            }
        }
        boardView.highlightPlayable(playable);
    }

    /**
     * Extracts the destination position from a game decision.
     * Placement: "A1"     -> position of A1
     * Movement: "A1 D1"   -> position of D1 (destination)
     * Capture: "XA1"      -> -1 (no new mill formed)
     */
    private int extractDestPos(String move) {
        if (move == null) return -1;
        move = move.trim();
        if (move.toUpperCase().startsWith("X")) return -1; // capture
        String[] parts = move.split("\\s+");
        // The last token is the destination (or the only position in phase 1)
        String coord = parts[parts.length - 1];
        return MerelleController.parseCoord(coord);
    }

    public Model getModel() { return model; }

    private MerelleStageModel getStage() {
        return (MerelleStageModel) model.getGameStage();
    }

    /** Converts a logical board position (0-23) to a coordinate string like "A1". */
    private String posToCoord(int pos) {
        int row = MerelleBoard.POS_TO_GRID[pos][0];
        int col = MerelleBoard.POS_TO_GRID[pos][1];
        return String.valueOf((char) ('A' + row)) + (col + 1);
    }
}
