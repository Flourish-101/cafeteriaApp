package cafeteriaapp.resources.controllers;

import cafeteriaapp.SceneManager;
import cafeteriaapp.Session;
import cafeteriaapp.User;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HomeTabController implements Initializable {

    @FXML private Label greetingsLabel;
    @FXML private Label accountBalanceLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = Session.getInstance().getCurrentUser();
        if (user != null) {
            greetingsLabel.setText("Welcome, " + user.getFirstName() + "!");
            accountBalanceLabel.setText("₦" + String.format("%,.2f", user.getAccountBalance()));
        }
    }

    @FXML private void goToAllMenu()       { openMenu("All"); }
    @FXML private void goToMainMeals()     { openMenu("Main Meal"); }
    @FXML private void goToDrinks()        { openMenu("Drinks"); }
    @FXML private void goToSnacks()        { openMenu("Snacks"); }
    @FXML private void goToProtein()       { openMenu("Protein"); }
    @FXML private void goToSwallow()       { openMenu("Swallow"); }

    private void openMenu(String category) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getClassLoader().getResource("cafeteriaapp/resources/views/menu.fxml")
            );
            Parent root = loader.load();
            MenuController controller = loader.getController();
            controller.setCategory(category);

            Stage stage = (Stage) greetingsLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            Logger.getLogger(HomeTabController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
