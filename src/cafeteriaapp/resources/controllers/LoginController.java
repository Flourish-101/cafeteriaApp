package cafeteriaapp.resources.controllers;

import cafeteriaapp.DatabaseConnection;
import cafeteriaapp.SceneManager;
import cafeteriaapp.Session;
import cafeteriaapp.User;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.sql.*;
import java.util.logging.*;
import javafx.concurrent.Task;
import javafx.scene.layout.StackPane;
import org.mindrot.jbcrypt.BCrypt;

public class LoginController implements Initializable {

    @FXML private TextField emailTextField;
    @FXML private PasswordField passwordField;
    @FXML private Button continueBtn;
    @FXML private FontAwesomeIconView eyeIcon;
    @FXML private TextField visiblePasswordField;
    @FXML private StackPane overlayPane;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField signUpEmailField;
    @FXML private PasswordField signUpPasswordField;
    @FXML private TextField signUpVisiblePasswordField;
    @FXML private PasswordField signUpConfirmPasswordField;
    @FXML private TextField signUpVisibleConfirmPasswordField;
    @FXML private FontAwesomeIconView signUpEyeIcon;
    @FXML private FontAwesomeIconView signUpConfirmEyeIcon;
    @FXML private Tab signUpTab;
    @FXML private Tab loginTab;
    @FXML private TabPane tabPane;
    @FXML private Label signUpLabel;
    @FXML private Label passwordErrorLabel;
    @FXML Button submitBtn;

    private boolean showPassword;
    private boolean showSignUpPassword;
    private boolean showSignUpConfirmPassword;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        overlayPane.setVisible(false);
        passwordErrorLabel.setVisible(false);

        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        signUpPasswordField.textProperty().bindBidirectional(signUpVisiblePasswordField.textProperty());
        signUpConfirmPasswordField.textProperty().bindBidirectional(signUpVisibleConfirmPasswordField.textProperty());

        continueBtn.disableProperty().bind(
                emailTextField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );

        submitBtn.disableProperty().bind(
                signUpEmailField.textProperty().isEmpty()
                        .or(signUpPasswordField.textProperty().isEmpty())
                        .or(signUpConfirmPasswordField.textProperty().isEmpty())
        );

        signUpConfirmPasswordField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!signUpPasswordField.getText().equals(newValue)) {
                passwordErrorLabel.setText("Passwords do not match");
                passwordErrorLabel.setVisible(true);
            } else {
                passwordErrorLabel.setText("");
                passwordErrorLabel.setVisible(false);
            }
        });
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailTextField.getText().trim();
        String password = passwordField.getText();

        if (!isValidEmail(email)) {
            showFieldError(emailTextField, "Please enter a valid email");
            return;
        }

        overlayPane.setVisible(true);

        Task<String[]> loginTask = new Task<>() {
            @Override
            protected String[] call() throws Exception {
                // Manager hardcoded check (can also be DB-based)
                try (Connection connection = DatabaseConnection.getConnect()) {
                    PreparedStatement stmt = connection.prepareStatement(
                            "SELECT * FROM users WHERE email = ?");
                    stmt.setString(1, email);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        String stored = rs.getString("password");
                        if (comparePassword(password, stored)) {
                            String role = "customer";
                            try { role = rs.getString("role"); } catch (Exception ignored) {}
                            if (role == null) role = "customer";
                            double wallet = 0;
                            try { wallet = rs.getDouble("wallet"); } catch (Exception ignored) {}
                            return new String[]{role,
                                rs.getString("first_name"), rs.getString("last_name"),
                                String.valueOf(wallet)};
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        };

        loginTask.setOnSucceeded(e -> {
            overlayPane.setVisible(false);
            String[] result = loginTask.getValue();
            if (result != null) {
                try {
                    User user = new User(result[1], result[2], email, result.length > 3 ? (float)Double.parseDouble(result[3]) : 0f);
                    Session.getInstance().setCurrentUser(user);

                    if ("manager".equals(result[0])) {
                        SceneManager.switchTo("resources/views/manager.fxml", true);
                    } else {
                        SceneManager.switchTo("resources/views/dashboard.fxml", true);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                showError("Invalid email or password.");
            }
        });

        loginTask.setOnFailed(e -> {
            overlayPane.setVisible(false);
            showError("Login failed: " + loginTask.getException().getMessage());
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void handleSignUp() {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = signUpEmailField.getText();
        String password = signUpPasswordField.getText();

        if (!isValidEmail(email)) {
            showFieldError(signUpEmailField, "Please enter a valid email");
            return;
        }

        String hashedPassword = hash(password);
        overlayPane.setVisible(true);

        Task<Boolean> signUpTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try (Connection connection = DatabaseConnection.getConnect()) {
                    PreparedStatement check = connection.prepareStatement(
                            "SELECT * FROM users WHERE email = ?");
                    check.setString(1, email);
                    if (check.executeQuery().next()) return false;

                    PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO users(first_name, last_name, email, password) VALUES (?,?,?,?)");
                    stmt.setString(1, firstName);
                    stmt.setString(2, lastName);
                    stmt.setString(3, email);
                    stmt.setString(4, hashedPassword);
                    return stmt.executeUpdate() > 0;
               } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
               
            }
        };

        signUpTask.setOnSucceeded(e -> {
            overlayPane.setVisible(false);
            if (signUpTask.getValue()) {
                showSuccess("Signup successful! Please log in.");
                tabPane.getSelectionModel().select(loginTab);
            } else {
                showError("User already exists with that email.");
            }
        });

        signUpTask.setOnFailed(e -> {
            overlayPane.setVisible(false);
            showError("Signup failed: " + signUpTask.getException().getMessage());
        });

        new Thread(signUpTask).start();
    }

    @FXML private void handleShowPassword() {
        showPassword = !showPassword;
        passwordField.setVisible(!showPassword); passwordField.setManaged(!showPassword);
        visiblePasswordField.setVisible(showPassword); visiblePasswordField.setManaged(showPassword);
        eyeIcon.setGlyphName(showPassword ? FontAwesomeIcon.EYE_SLASH.name() : FontAwesomeIcon.EYE.name());
    }

    @FXML private void handleSignUpShowPassword() {
        showSignUpPassword = !showSignUpPassword;
        signUpPasswordField.setVisible(!showSignUpPassword); signUpPasswordField.setManaged(!showSignUpPassword);
        signUpVisiblePasswordField.setVisible(showSignUpPassword); signUpVisiblePasswordField.setManaged(showSignUpPassword);
        signUpEyeIcon.setGlyphName(showSignUpPassword ? FontAwesomeIcon.EYE_SLASH.name() : FontAwesomeIcon.EYE.name());
    }

    @FXML private void handleSignUpShowConfirmPassword() {
        showSignUpConfirmPassword = !showSignUpConfirmPassword;
        signUpConfirmPasswordField.setVisible(!showSignUpConfirmPassword); signUpConfirmPasswordField.setManaged(!showSignUpConfirmPassword);
        signUpVisibleConfirmPasswordField.setVisible(showSignUpConfirmPassword); signUpVisibleConfirmPasswordField.setManaged(showSignUpConfirmPassword);
        signUpConfirmEyeIcon.setGlyphName(showSignUpConfirmPassword ? FontAwesomeIcon.EYE_SLASH.name() : FontAwesomeIcon.EYE.name());
    }

    @FXML private void switchToSignUpTab() { tabPane.getSelectionModel().select(signUpTab); }

    private void showFieldError(TextField field, String message) {
        field.getStyleClass().add("field-error");
        field.setTooltip(new Tooltip(message));
        field.textProperty().addListener((obs, o, n) -> {
            field.getStyleClass().remove("field-error");
            field.setTooltip(null);
        });
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    private void showSuccess(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    private String hash(String password) { return BCrypt.hashpw(password, BCrypt.gensalt()); }
    private boolean comparePassword(String plain, String hashed) { return BCrypt.checkpw(plain, hashed); }
}
