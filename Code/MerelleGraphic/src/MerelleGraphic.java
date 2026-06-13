import javafx.application.Application;
import javafx.stage.Stage;
import view.javafx.SceneManager;

public class MerelleGraphic extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Nine Men's Morris");
        primaryStage.setResizable(false);

        SceneManager manager = new SceneManager(primaryStage);
        manager.showMenu();

        primaryStage.show();
    }
}
