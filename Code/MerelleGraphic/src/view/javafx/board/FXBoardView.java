package view.javafx.board;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.MerelleBoard;
import model.MerellePawn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Board canvas - flat wood theme, straight edges, no effects.
 */
public class FXBoardView extends Pane {

    /** Adjacency list for the 24 nodes */
    private static final int[][] ADJACENCY = {
        {1,9},          // 0  A1
        {0,2,4},        // 1  D1
        {1,14},         // 2  G1
        {4,10},         // 3  B2
        {1,3,5,7},      // 4  D2
        {4,13},         // 5  F2
        {7,11},         // 6  C3
        {4,6,8},        // 7  D3
        {7,12},         // 8  E3
        {0,10,21},      // 9  A4
        {3,9,11,18},    // 10 B4
        {6,10,15},      // 11 C4
        {8,13,17},      // 12 E4
        {5,12,14,20},   // 13 F4
        {2,13,23},      // 14 G4
        {11,16},        // 15 C5
        {15,17,19},     // 16 D5
        {12,16},        // 17 E5
        {10,19},        // 18 B6
        {16,18,20,22},  // 19 D6
        {13,19},        // 20 F6
        {9,22},         // 21 A7
        {19,21,23},     // 22 D7
        {14,22}         // 23 G7
    };

    private final Canvas canvas;
    private final Map<Integer, double[]> nodePos = new HashMap<>();
    private double nodeR = 14;

    private Integer selectedPos = null;
    private final Set<Integer> millPositions       = new HashSet<>();
    private final Set<Integer> capturablePositions = new HashSet<>();
    private final Set<Integer> playablePositions   = new HashSet<>();

    private MerelleBoard currentBoard = null;
    private final Consumer<Integer> clickHandler;

    public FXBoardView(Consumer<Integer> onNodeClick) {
        this.clickHandler = onNodeClick;
        canvas = new Canvas();
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        canvas.widthProperty().addListener((o, ov, nv) -> redraw());
        canvas.heightProperty().addListener((o, ov, nv) -> redraw());
        canvas.setOnMouseClicked(e -> handleClick(e.getX(), e.getY()));
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public FXBoardView(Consumer<Integer> onNodeClick, BoardTheme unused) {
        this(onNodeClick);
    }

    private void computeLayout(double w, double h) {
        nodePos.clear();
        double size   = Math.min(w, h) - 10;
        double margin = size * 0.09;
        double cell   = (size - 2 * margin) / 6.0;
        nodeR = Math.max(12, cell * 0.22);
        double ox = (w - size) / 2.0 + margin;
        double oy = (h - size) / 2.0 + margin;
        for (int pos = 0; pos < 24; pos++) {
            double x = ox + MerelleBoard.POS_TO_GRID[pos][1] * cell;
            double y = oy + MerelleBoard.POS_TO_GRID[pos][0] * cell;
            nodePos.put(pos, new double[]{x, y});
        }
    }

    private void redraw() {
        double w = canvas.getWidth(), h = canvas.getHeight();
        if (w < 100 || h < 100) return;
        computeLayout(w, h);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // Dark outer background
        gc.setFill(Color.web("#0f0b07"));
        gc.fillRect(0, 0, w, h);

        drawBoardSurface(gc, w, h);
        drawLines(gc);
        drawLabels(gc);
        drawNodes(gc);
    }

    private void drawBoardSurface(GraphicsContext gc, double w, double h) {
        double size   = Math.min(w, h) - 10;
        double margin = size * 0.09;
        double cell   = (size - 2 * margin) / 6.0;
        double ox = (w - size) / 2.0 + margin;
        double oy = (h - size) / 2.0 + margin;

        double bx = ox - cell * 0.38;
        double by = oy - cell * 0.38;
        double bw = 6 * cell + cell * 0.76;
        double bh = 6 * cell + cell * 0.76;

        // Flat wood background - no gradient
        gc.setFill(Color.web("#6B3D14"));
        gc.fillRect(bx, by, bw, bh);

        // Contour droit
        gc.setStroke(Color.web("#5A3010"));
        gc.setLineWidth(2);
        gc.strokeRect(bx, by, bw, bh);
    }

    private void drawLines(GraphicsContext gc) {
        Color lineColor = Color.web("#C8882A");  // lignes bien visibles, ambre chaud
        for (int i = 0; i < 24; i++) {
            for (int j : ADJACENCY[i]) {
                if (j > i) {
                    double[] a = nodePos.get(i), b = nodePos.get(j);
                    gc.setStroke(lineColor);
                    gc.setLineWidth(2.5);
                    gc.strokeLine(a[0], a[1], b[0], b[1]);
                }
            }
        }
    }

    private void drawLabels(GraphicsContext gc) {
        double size = Math.min(canvas.getWidth(), canvas.getHeight()) - 10;
        double margin = size * 0.09;
        double cell = (size - 2 * margin) / 6.0;
        double ox = (canvas.getWidth()  - size) / 2.0 + margin;
        double oy = (canvas.getHeight() - size) / 2.0 + margin;

        Color labelColor = Color.web("#D4A960");
        double lsz = Math.max(12, cell * 0.22);

        gc.setFill(labelColor);
        gc.setFont(Font.font("Georgia", FontWeight.BOLD, lsz));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        String[] cols = {"1","2","3","4","5","6","7"};
        String[] rows = {"A","B","C","D","E","F","G"};
        for (int c = 0; c < 7; c++) gc.fillText(cols[c], ox + c * cell, oy - cell * 0.55);
        for (int r = 0; r < 7; r++) gc.fillText(rows[r], ox - cell * 0.55, oy + r * cell);
    }

    private void drawNodes(GraphicsContext gc) {
        Color emptyFill  = Color.web("#3A2008");
        Color nodeStroke = Color.web("#A07828");

        for (int pos = 0; pos < 24; pos++) {
            double[] p = nodePos.get(pos);
            double x = p[0], y = p[1];

            MerellePawn pawn = (currentBoard != null) ? currentBoard.getPawnAt(pos) : null;
            boolean isSel = (selectedPos != null && selectedPos == pos);
            boolean isMill = millPositions.contains(pos);
            boolean isCapturable = capturablePositions.contains(pos);
            boolean isPlayable = playablePositions.contains(pos);

            if (pawn == null) {
                // Case vide
                gc.setFill(emptyFill);
                gc.fillOval(x - nodeR, y - nodeR, nodeR * 2, nodeR * 2);

                if (isPlayable) {
                    // Possible move: thick green outline only
                    gc.setStroke(Color.web("#00ff88"));
                    gc.setLineWidth(3.5);
                    gc.strokeOval(x - nodeR, y - nodeR, nodeR * 2, nodeR * 2);
                } else {
                    gc.setStroke(nodeStroke);
                    gc.setLineWidth(1.5);
                    gc.strokeOval(x - nodeR, y - nodeR, nodeR * 2, nodeR * 2);
                }

            } else {
                // Pion : couleur unie
                Color base = fxColorOf(pawn.getColor());
                gc.setFill(base);
                gc.fillOval(x - nodeR, y - nodeR, nodeR * 2, nodeR * 2);

                // Choose the outline color based on the state AND the pawn color
                // to guarantee sufficient contrast in all cases
                if (isMill) {
                    Color outline = millOutlineFor(base);
                    gc.setStroke(outline);
                    gc.setLineWidth(4.5);
                    gc.strokeOval(x - nodeR, y - nodeR, nodeR * 2, nodeR * 2);
                } else if (isSel) {
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(4);
                    gc.strokeOval(x - nodeR, y - nodeR, nodeR * 2, nodeR * 2);
                } else if (isCapturable) {
                    Color outline = capturableOutlineFor(base);
                    gc.setStroke(outline);
                    gc.setLineWidth(4.5);
                    gc.strokeOval(x - nodeR, y - nodeR, nodeR * 2, nodeR * 2);
                } else {
                    gc.setStroke(base.brighter());
                    gc.setLineWidth(1.5);
                    gc.strokeOval(x - nodeR, y - nodeR, nodeR * 2, nodeR * 2);
                }
            }
        }
    }

    /**
     * Outline color for a mill, chosen to contrast with the pawn color.
     * Yellow/gold -> bright cyan. White/gray -> orange. Otherwise -> white.
     */
    /**
     * Outline color for a mill highlight.
     * Default: yellow. Exception: yellow pawn -> cyan.
     */
    private Color millOutlineFor(Color base) {
        double r = base.getRed(), g = base.getGreen(), b = base.getBlue();
        // Yellow pawn: use cyan to contrast with the pawn itself
        if (r > 0.6 && g > 0.6 && b < 0.35) return Color.web("#00ffff");
        // Default -> yellow
        return Color.web("#ffdd00");
    }

    /**
     * Outline color for a capturable pawn, chosen to contrast.
     * Yellow/gold -> magenta. Red -> white. Otherwise -> bright orange-red.
     */
    private Color capturableOutlineFor(Color base) {
        double r = base.getRed(), g = base.getGreen(), b = base.getBlue();
        // Yellow/gold pawn -> magenta
        if (r > 0.6 && g > 0.6 && b < 0.35) return Color.web("#ff00dd");
        // Red pawn (risk of confusing red/red) -> white
        if (r > 0.6 && g < 0.4 && b < 0.4) return Color.web("#ffffff");
        // Bright orange pawn -> white
        if (r > 0.7 && g > 0.35 && g < 0.6 && b < 0.3) return Color.web("#ffffff");
        // Default -> bright orange-red
        return Color.web("#ff4400");
    }

    private String pawnLabel(int color) {
        switch (color) {
            case MerellePawn.PAWN_BLACK: return "A";
            case MerellePawn.PAWN_RED: return "R";
            case MerellePawn.PAWN_BLUE: return "B";
            case MerellePawn.PAWN_GREEN: return "V";
            case MerellePawn.PAWN_YELLOW: return "J";
            case MerellePawn.PAWN_PURPLE: return "P";
            case MerellePawn.PAWN_CYAN: return "C";
            default: return "?";
        }
    }

    private void handleClick(double mx, double my) {
        for (int pos = 0; pos < 24; pos++) {
            double[] p = nodePos.get(pos);
            if (p == null) continue;
            double dx = mx - p[0], dy = my - p[1];
            if (dx * dx + dy * dy <= (nodeR * 1.6) * (nodeR * 1.6)) {
                clickHandler.accept(pos);
                return;
            }
        }
    }

    public void refresh(MerelleBoard board) {
        this.currentBoard = board;
        redraw();
    }

    public void highlightSelected(int pos) {
        selectedPos = pos;
        redraw();
    }

    public void clearHighlight() {
        selectedPos = null;
        millPositions.clear();
        capturablePositions.clear();
        playablePositions.clear();
        redraw();
    }

    public void highlightPlayable(java.util.Collection<Integer> positions) {
        playablePositions.clear();
        playablePositions.addAll(positions);
        redraw();
    }

    public void highlightMill(int[] positions) {
        millPositions.clear();
        for (int p : positions) millPositions.add(p);
        redraw();
    }

    public void highlightCapturable(MerelleBoard board, int oppColor) {
        capturablePositions.clear();
        boolean allInMill = board.allPawnsInMills(oppColor);
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pawn = board.getPawnAt(pos);
            if (pawn != null && pawn.getColor() == oppColor) {
                if (allInMill || !board.isInMill(pos, oppColor)) {
                    capturablePositions.add(pos);
                }
            }
        }
        redraw();
    }

    public static Color fxColorOf(int color) {
        switch (color) {
            case MerellePawn.PAWN_BLACK:  return Color.web("#b0b0b0");
            case MerellePawn.PAWN_RED: return Color.web("#e03030");
            case MerellePawn.PAWN_BLUE: return Color.web("#3070e0");
            case MerellePawn.PAWN_GREEN: return Color.web("#28a840");
            case MerellePawn.PAWN_YELLOW: return Color.web("#e0c020");
            case MerellePawn.PAWN_PURPLE: return Color.web("#9040c0");
            case MerellePawn.PAWN_CYAN: return Color.web("#20b8c8");
            default: return Color.GRAY;
        }
    }
}
