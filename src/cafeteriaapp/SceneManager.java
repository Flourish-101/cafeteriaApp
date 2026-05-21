package cafeteriaapp;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchTo(String fxmlPath, boolean resizable) throws IOException {
        // Always load fresh so back button and state always reflect current data
        Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlPath));
        primaryStage.setResizable(resizable);
        primaryStage.getScene().setRoot(root);
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();
    }
}
