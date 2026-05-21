package cafeteriaapp.resources.controllers;

import cafeteriaapp.SceneManager;
import cafeteriaapp.Session;
import cafeteriaapp.User;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

public class ProfileController implements Initializable {

    @FXML private Label fullNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label balanceLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = Session.getInstance().getCurrentUser();
        if (user != null) {
            fullNameLabel.setText(user.getFirstName() + " " + user.getLastName());
            emailLabel.setText(user.getEmail());
            balanceLabel.setText("₦" + String.format("%,.2f", user.getAccountBalance()));
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Session.getInstance().logout();
            SceneManager.switchTo("resources/views/login.fxml", false);
        } catch (IOException ex) {
            Logger.getLogger(ProfileController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
