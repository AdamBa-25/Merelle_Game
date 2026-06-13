package view.javafx.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import model.GameConfig;
import view.javafx.SceneManager;
import view.javafx.board.BoardTheme;

/**
 * Screen to choose a board visual theme before starting the game.
 * Renders a mini-preview of each theme (3x3 sub-board).
 */
public class ThemeSelectionScreen extends VBox {

    private int selectedTheme = -1;
    private Button confirmBtn;
    private final Rectangle[] cards;

    public ThemeSelectionScreen(SceneManager manager, GameConfig cfg) {
        setAlignment(Pos.CENTER);
        setSpacing(30);
        getStyleClass().add("menu-root");
        setPadding(new Insets(40));

        Text title = new Text("Choose the board theme");
        title.getStyleClass().add("screen-title");

        // 4 theme cards in a 2x2 grid
        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(24);
        grid.setAlignment(Pos.CENTER);

        cards = new Rectangle[BoardTheme.ALL.length];

        for (int i = 0; i < BoardTheme.ALL.length; i++) {
            BoardTheme t = BoardTheme.ALL[i];
            StackPane card = buildCard(t, i);
            grid.add(card, i % 2, i / 2);
        }

        confirmBtn = new Button("Play");
        confirmBtn.getStyleClass().addAll("btn", "btn-primary");
        confirmBtn.setPrefWidth(200);
        confirmBtn.setDisable(true);
        confirmBtn.setOnAction(e -> {
            cfg.boardTheme = selectedTheme;
            manager.showGame();
        });

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().addAll("btn", "btn-back");
        backBtn.setOnAction(e -> {
            if (cfg.mode == 0) manager.showColorSelection();
            else manager.showAISelection();
        });

        getChildren().addAll(title, grid, confirmBtn, backBtn);
    }

    private StackPane buildCard(BoardTheme theme, int index) {
        Rectangle bg = new Rectangle(180, 160);
        bg.setArcWidth(14);
        bg.setArcHeight(14);
        bg.setFill(theme.boardFill);
        bg.setStroke(theme.emptyNodeStroke);
        bg.setStrokeWidth(2);
        cards[index] = bg;

        DropShadow ds = new DropShadow();
        ds.setColor(Color.BLACK.deriveColor(0,1,1,0.6));
        ds.setRadius(10);
        bg.setEffect(ds);

        Pane preview = buildMiniPreview(theme);
        preview.setMouseTransparent(true);

        Text label = new Text(theme.displayName);
        label.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        label.setFill(theme.lineColor.deriveColor(0, 1, 1.5, 1.0));

        VBox content = new VBox(8, preview, label);
        content.setAlignment(Pos.CENTER);
        content.setMouseTransparent(true);

        StackPane card = new StackPane(bg, content);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> selectTheme(index));
        return card;
    }

    /** Builds a tiny 2-square version of the board for the preview. */
    private Pane buildMiniPreview(BoardTheme theme) {
        Pane p = new Pane();
        p.setPrefSize(130, 100);

        double cell = 20.0;
        double margin = 10.0;

        for (int sq = 0; sq < 2; sq++) {
            double off = sq * cell;
            double x = margin + off;
            double y = 10 + off;
            double w = 4 * cell - 2 * off;
            double h = 4 * cell - 2 * off;
            Rectangle r = new Rectangle(x + 15, y, w, h);
            r.setFill(Color.TRANSPARENT);
            r.setStroke(theme.lineColor);
            r.setStrokeWidth(1.5);
            p.getChildren().add(r);
        }

        double cx = margin + 2 * cell + 15;
        double cy = 10 + 2 * cell;
        double top = 10.0, bot = 10 + 4 * cell, left = margin + 15, right = margin + 4 * cell + 15;
        addMiniLine(p, cx, top, cx, top + cell, theme);
        addMiniLine(p, cx, bot - cell, cx, bot, theme);
        addMiniLine(p, left, cy, left + cell, cy, theme);
        addMiniLine(p, right - cell, cy, right, cy, theme);

        int[][] pts = {{0,0},{0,2},{0,4},{2,0},{2,4},{4,0},{4,2},{4,4},{1,1},{1,3},{3,1},{3,3}};
        for (int[] pt : pts) {
            double nx = margin + 15 + pt[1] * cell;
            double ny = 10 + pt[0] * cell;
            Circle c = new Circle(nx, ny, 5);
            c.setFill(theme.emptyNodeFill);
            c.setStroke(theme.emptyNodeStroke);
            c.setStrokeWidth(1);
            p.getChildren().add(c);
        }
        return p;
    }

    private void addMiniLine(Pane p, double x1, double y1, double x2, double y2, BoardTheme t) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(t.lineColor);
        l.setStrokeWidth(1.5);
        p.getChildren().add(l);
    }

    private void selectTheme(int index) {
        if (selectedTheme >= 0) {
            BoardTheme prev = BoardTheme.ALL[selectedTheme];
            cards[selectedTheme].setStroke(prev.emptyNodeStroke);
            cards[selectedTheme].setStrokeWidth(2);
            cards[selectedTheme].setEffect(new DropShadow(10, Color.BLACK.deriveColor(0,1,1,0.6)));
        }
        selectedTheme = index;
        BoardTheme t = BoardTheme.ALL[index];
        cards[index].setStroke(Color.WHITE);
        cards[index].setStrokeWidth(4);
        DropShadow glow = new DropShadow();
        glow.setColor(Color.WHITE.deriveColor(0,1,1,0.8));
        glow.setRadius(20);
        glow.setSpread(0.3);
        cards[index].setEffect(glow);
        confirmBtn.setDisable(false);
    }
}
