package view.javafx.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import model.MerelleStageModel;
import model.MerellePawn;
import boardifier.model.Model;
import boardifier.model.Player;
import view.javafx.board.FXBoardView;

public class InfoPanel extends VBox {

    private Text barPlayerText;
    private Text barPhaseText;
    private Text barMessageText;
    private Circle barPlayerDot;  // dot showing the current player's color

    // Player colors (stored to display the correct colored dot)
    private int colorJ1 = MerellePawn.PAWN_BLACK;
    private int colorJ2 = MerellePawn.PAWN_RED;

    public InfoPanel() {
        setVisible(false);
        setManaged(false);
    }

    public void setColors(int c1, int c2) { colorJ1 = c1; colorJ2 = c2; }

    /** Returns an HBox suitable for embedding in the top bar. */
    public HBox asTopBar() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 20, 0, 16));

        barPlayerDot  = new Circle(8);
        barPlayerDot.setFill(FXBoardView.fxColorOf(colorJ1));
        barPlayerDot.setStroke(Color.web("#888888"));
        barPlayerDot.setStrokeWidth(1.5);

        barPlayerText  = styledText("Player 1", "#e0e0e0", 14, true);
        barPhaseText   = styledText("Placement", "#aaaaaa", 13, false);
        barMessageText = styledText("", "#FFD700", 13, true);

        HBox playerBlock = new HBox(8, barPlayerDot, barPlayerText);
        playerBlock.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(playerBlock, sep(), barPhaseText, sep(), barMessageText);
        return box;
    }

    private Text styledText(String s, String color, int size, boolean bold) {
        Text t = new Text(s);
        t.setFill(Color.web(color));
        t.setFont(Font.font("Arial", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return t;
    }

    private Rectangle sep() {
        Rectangle r = new Rectangle(1, 20);
        r.setFill(Color.web("#444444"));
        return r;
    }

    public void update(Model model, MerelleStageModel stage) {
        Player current = model.getCurrentPlayer();
        int idPlayer = model.getIdPlayer();
        String playerName = current.getName();
        String phase = stage.getCurrentPhase() == MerelleStageModel.PHASE_PLACEMENT
                ? "Placement" : "Movement";

        // Current player color
        int playerColor = (idPlayer == 0) ? colorJ1 : colorJ2;
        Color fxColor = FXBoardView.fxColorOf(playerColor);

        if (barPlayerText != null) barPlayerText.setText(playerName);
        if (barPlayerDot  != null) {
            barPlayerDot.setFill(fxColor);
            // Also adjust the text color
            barPlayerText.setFill(fxColor.brighter());
        }
        if (barPhaseText  != null) barPhaseText.setText("Phase: " + phase);

        if (stage.isMillJustFormed()) {
            showMessage("MILL! Capture an opponent's pawn.");
        } else {
            showMessage("");
        }
    }

    public void setStatus(String msg) {
        if (barMessageText != null) {
            barMessageText.setText(msg);
            barMessageText.setFill(Color.web("#aaddff"));
        }
    }

    public void showError(String msg) {
        if (barMessageText != null) {
            barMessageText.setText("Warning: " + msg);
            barMessageText.setFill(Color.web("#ff6b6b"));
        }
    }

    public void showMessage(String msg) {
        if (barMessageText != null) {
            barMessageText.setText(msg);
            barMessageText.setFill(Color.web("#FFD700"));
        }
    }
}
