package view.javafx.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import model.MerelleStageModel;
import view.javafx.board.FXBoardView;

public class ReservePanel extends VBox {

    private static final int INITIAL_PAWNS = 9;

    private HBox barJ1Row;
    private HBox barJ2Row;
    private Text barJ1Label;
    private Text barJ2Label;
    private int savedColorJ1;
    private int savedColorJ2;

    public ReservePanel() {
        setVisible(false);
        setManaged(false);
    }

    /**
     * Returns an HBox for the top bar.
     * Pre-filled with 9 pawns from the start: the width never changes,
     * the canvas does not move.
     */
    public HBox asTopBar(int colorJ1, int colorJ2) {
        savedColorJ1 = colorJ1;
        savedColorJ2 = colorJ2;

        HBox box = new HBox(16);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(0, 16, 0, 0));

        barJ1Label = label("P1: " + INITIAL_PAWNS);
        barJ1Row   = dotRow(colorJ1, INITIAL_PAWNS);
        barJ2Label = label("P2: " + INITIAL_PAWNS);
        barJ2Row   = dotRow(colorJ2, INITIAL_PAWNS);

        HBox j1Block = new HBox(6, barJ1Label, barJ1Row);
        j1Block.setAlignment(Pos.CENTER);
        HBox j2Block = new HBox(6, barJ2Label, barJ2Row);
        j2Block.setAlignment(Pos.CENTER);

        box.getChildren().addAll(sep(), j1Block, sep(), j2Block);
        return box;
    }

    public void update(MerelleStageModel stage, int colorJ1, int colorJ2) {
        if (barJ1Row == null) return;
        int h1 = stage.getPawnsInHand(0);
        int h2 = stage.getPawnsInHand(1);

        barJ1Label.setText("P1: " + h1);
        barJ2Label.setText("P2: " + h2);

        // Replace the pawns without changing the overall row size:
        // we always keep 9 slots - placed pawns become transparent
        rebuildDotRow(barJ1Row, colorJ1, h1);
        rebuildDotRow(barJ2Row, colorJ2, h2);
    }

    /** Rebuilds the row with exactly INITIAL_PAWNS slots.
     *  Pawns still in hand are colored, the others are invisible. */
    private void rebuildDotRow(HBox row, int color, int inHand) {
        row.getChildren().clear();
        for (int i = 0; i < INITIAL_PAWNS; i++) {
            Circle c = new Circle(6);
            if (i < inHand) {
                c.setFill(FXBoardView.fxColorOf(color));
                c.setStroke(Color.web("#888888"));
                c.setStrokeWidth(1);
            } else {
                // Placed pawn: ghost circle (same size, invisible)
                c.setFill(Color.TRANSPARENT);
                c.setStroke(Color.TRANSPARENT);
            }
            row.getChildren().add(c);
        }
    }

    private Text label(String s) {
        Text t = new Text(s);
        t.setFill(Color.web("#cccccc"));
        t.setStyle("-fx-font-size: 13px; -fx-font-family: 'Arial'; -fx-font-weight: bold;");
        return t;
    }

    /** Creates a row pre-filled with n colored pawns. */
    private HBox dotRow(int color, int n) {
        HBox row = new HBox(3);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinHeight(16);
        row.setPrefHeight(16);
        row.setMaxHeight(16);
        for (int i = 0; i < n; i++) {
            Circle c = new Circle(6);
            c.setFill(FXBoardView.fxColorOf(color));
            c.setStroke(Color.web("#888888"));
            c.setStrokeWidth(1);
            row.getChildren().add(c);
        }
        return row;
    }

    private Rectangle sep() {
        Rectangle r = new Rectangle(1, 20);
        r.setFill(Color.web("#444444"));
        return r;
    }
}
