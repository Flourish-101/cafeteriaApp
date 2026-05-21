package cafeteriaapp.resources.controllers;

import cafeteriaapp.SceneManager;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

public class DashboardController implements Initializable {

    @FXML private Pane centerPane;
    @FXML private FontAwesomeIconView homeBtn;
    @FXML private FontAwesomeIconView walletBtn;
    @FXML private FontAwesomeIconView ordersBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadView("resources/views/home-view.fxml", homeBtn);
    }

    @FXML
    private void handleHomeBtn() {
        loadView("resources/views/home-view.fxml", homeBtn);
    }
//Handles cart not wallet button
    @FXML
    private void handleWalletBtn() {
        loadView("resources/views/cart.fxml", walletBtn);
    }

    @FXML
    private void handleOrdersBtn() {
        loadView("resources/views/ReportsView.fxml", ordersBtn);
    }

    @FXML
    private void handleProfileBtn() {
        loadView("resources/views/ProfileView.fxml", null);
    }

    private void loadView(String fxmlPath, FontAwesomeIconView activeBtn) {
        // Reset all icon colours
        if (homeBtn != null) homeBtn.setFill(javafx.scene.paint.Color.web("#888888"));
        if (walletBtn != null) walletBtn.setFill(javafx.scene.paint.Color.web("#888888"));
        if (ordersBtn != null) ordersBtn.setFill(javafx.scene.paint.Color.web("#888888"));

        // Highlight active
        if (activeBtn != null) activeBtn.setFill(javafx.scene.paint.Color.WHITE);

        try {
            Node view = FXMLLoader.load(
                getClass().getClassLoader().getResource("cafeteriaapp/" + fxmlPath)
            );
            centerPane.getChildren().setAll(view);
            // Make it fill the pane
            if (view instanceof javafx.scene.layout.Region r) {
                r.prefWidthProperty().bind(centerPane.widthProperty());
                r.prefHeightProperty().bind(centerPane.heightProperty());
            }
        } catch (IOException ex) {
            Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
