package cafeteriaapp.resources.controllers;

import cafeteriaapp.DatabaseConnection;
import cafeteriaapp.Session;
import cafeteriaapp.User;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.util.logging.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class OrderHistoryController implements Initializable {

    @FXML private VBox ordersContainer;
    @FXML private Label emptyLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadOrders();
    }

    private void loadOrders() {
        User user = Session.getInstance().getCurrentUser();
        if (user == null) return;

        ordersContainer.getChildren().clear();

        try (Connection conn = DatabaseConnection.getConnect()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT o.id, o.total_amount, o.status, o.created_at, " +
                    "       mi.name AS item_name, oi.quantity, oi.unit_price " +
                    "FROM orders o " +
                    "JOIN order_items oi ON oi.order_id = o.id " +
                    "JOIN menu_items mi ON mi.id = oi.menu_item_id " +
                    "WHERE o.user_email = ? " +
                    "ORDER BY o.created_at DESC, o.id DESC"
            );
            stmt.setString(1, user.getEmail());
            ResultSet rs = stmt.executeQuery();

            int lastOrderId = -1;
            VBox currentOrderBox = null;

            while (rs.next()) {
                int orderId = rs.getInt("id");

                if (orderId != lastOrderId) {
                    // New order card
                    currentOrderBox = new VBox(6);
                    currentOrderBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                            + "-fx-padding: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2);");
                    VBox.setMargin(currentOrderBox, new javafx.geometry.Insets(0, 0, 12, 0));

                    // Header row
                    HBox header = new HBox();
                    header.setAlignment(Pos.CENTER_LEFT);
                    Label orderIdLbl = new Label("Order #" + orderId);
                    orderIdLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                    orderIdLbl.setTextFill(Color.web("#1a1a2e"));
                    HBox.setHgrow(orderIdLbl, Priority.ALWAYS);

                    String status = rs.getString("status");
                    Label statusLbl = new Label(status);
                    String statusColor = status.equals("Completed") ? "#22c55e" : "#f59e0b";
                    statusLbl.setStyle("-fx-background-color: " + statusColor + "22; "
                            + "-fx-text-fill: " + statusColor + "; -fx-padding: 3 10 3 10; "
                            + "-fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold;");

                    header.getChildren().addAll(orderIdLbl, statusLbl);

                    Timestamp ts = rs.getTimestamp("created_at");
                    Label dateLbl = new Label(ts != null ? ts.toLocalDateTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "");
                    dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

                    Separator sep = new Separator();
                    sep.setStyle("-fx-background-color: #f0f0f0;");

                    currentOrderBox.getChildren().addAll(header, dateLbl, sep);
                    ordersContainer.getChildren().add(currentOrderBox);
                    lastOrderId = orderId;
                }

                // Item row
                HBox itemRow = new HBox(8);
                itemRow.setAlignment(Pos.CENTER_LEFT);
                Label itemName = new Label(rs.getString("item_name"));
                itemName.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
                HBox.setHgrow(itemName, Priority.ALWAYS);
                Label itemQty = new Label("x" + rs.getInt("quantity"));
                itemQty.setStyle("-fx-font-size: 13px; -fx-text-fill: #888;");
                Label itemPrice = new Label("₦" + String.format("%,.2f", rs.getDouble("unit_price") * rs.getInt("quantity")));
                itemPrice.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
                itemRow.getChildren().addAll(itemName, itemQty, itemPrice);
                if (currentOrderBox != null) currentOrderBox.getChildren().add(itemRow);
            }

            if (ordersContainer.getChildren().isEmpty()) {
                emptyLabel.setVisible(true);
            } else {
                // Add total at bottom of each card
                // (totals are already stored in orders table, cards look clean)
                if (emptyLabel != null) emptyLabel.setVisible(false);
            }

        } catch (SQLException e) {
            Logger.getLogger(OrderHistoryController.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
