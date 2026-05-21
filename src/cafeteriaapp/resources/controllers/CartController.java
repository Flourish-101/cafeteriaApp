package cafeteriaapp.resources.controllers;

import cafeteriaapp.*;
import java.io.IOException;
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

public class CartController implements Initializable {

    @FXML private VBox cartItemsContainer;
    @FXML private Label totalLabel;
    @FXML private Label emptyLabel;
    @FXML private Button placeOrderBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        renderCart();
    }

    private void renderCart() {
        cartItemsContainer.getChildren().clear();
        var items = CartManager.getInstance().getItems();

        if (items.isEmpty()) {
            emptyLabel.setVisible(true);
            placeOrderBtn.setDisable(true);
            totalLabel.setText("Total: ₦0.00");
            return;
        }

        emptyLabel.setVisible(false);
        placeOrderBtn.setDisable(false);

        for (CartItem item : items) {
            cartItemsContainer.getChildren().add(buildRow(item));
        }

        updateTotal();
    }

    private HBox buildRow(CartItem item) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");
        VBox.setMargin(row, new javafx.geometry.Insets(0, 0, 10, 0));

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLbl = new Label(item.getName());
        nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label unitLbl = new Label("₦" + String.format("%,.2f", item.getPrice()) + " each");
        unitLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        info.getChildren().addAll(nameLbl, unitLbl);

        // Qty stepper
        HBox stepper = new HBox(8);
        stepper.setAlignment(Pos.CENTER);
        Button minus = new Button("−");
        minus.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 20; "
                + "-fx-border-radius: 20; -fx-min-width: 30; -fx-min-height: 30; "
                + "-fx-font-size: 16px; -fx-cursor: hand;");
        Label qtyLbl = new Label(String.valueOf(item.getQuantity()));
        qtyLbl.setMinWidth(25);
        qtyLbl.setAlignment(Pos.CENTER);
        qtyLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Button plus = new Button("+");
        plus.setStyle(minus.getStyle());

        minus.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                qtyLbl.setText(String.valueOf(item.getQuantity()));
                updateTotal();
            } else {
                CartManager.getInstance().removeItem(item);
                renderCart();
            }
        });

        plus.setOnAction(e -> {
            item.setQuantity(item.getQuantity() + 1);
            qtyLbl.setText(String.valueOf(item.getQuantity()));
            updateTotal();
        });

        stepper.getChildren().addAll(minus, qtyLbl, plus);

        Label subtotal = new Label("₦" + String.format("%,.2f", item.getSubtotal()));
        subtotal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-min-width: 90; -fx-alignment: CENTER_RIGHT;");

        // Update subtotal label reactively
        minus.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
            } else {
                CartManager.getInstance().removeItem(item);
                renderCart();
                return;
            }
            qtyLbl.setText(String.valueOf(item.getQuantity()));
            subtotal.setText("₦" + String.format("%,.2f", item.getSubtotal()));
            updateTotal();
        });

        plus.setOnAction(e -> {
            item.setQuantity(item.getQuantity() + 1);
            qtyLbl.setText(String.valueOf(item.getQuantity()));
            subtotal.setText("₦" + String.format("%,.2f", item.getSubtotal()));
            updateTotal();
        });

        Button removeBtn = new Button("✕");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; "
                + "-fx-font-size: 14px; -fx-cursor: hand; -fx-border-color: transparent;");
        removeBtn.setOnAction(e -> {
            CartManager.getInstance().removeItem(item);
            renderCart();
        });

        row.getChildren().addAll(info, stepper, subtotal, removeBtn);
        return row;
    }

    private void updateTotal() {
        totalLabel.setText("Total: ₦" + String.format("%,.2f", CartManager.getInstance().getTotal()));
    }

    @FXML
    private void handlePlaceOrder() {
        User user = Session.getInstance().getCurrentUser();
        if (user == null) return;

        var items = CartManager.getInstance().getItems();
        if (items.isEmpty()) return;

        double total = CartManager.getInstance().getTotal();

        // Check sufficient wallet balance before proceeding
        try (Connection balConn = DatabaseConnection.getConnect()) {
            PreparedStatement balStmt = balConn.prepareStatement(
                "SELECT wallet FROM users WHERE email = ?"
            );
            balStmt.setString(1, user.getEmail());
            ResultSet rs = balStmt.executeQuery();
            if (rs.next() && rs.getDouble("wallet") < total) {
                showError("Insufficient wallet balance. Please top up.");
                return;
            }
        } catch (SQLException ex) {
            showError("Could not verify wallet balance: " + ex.getMessage());
            return;
        }

        try (Connection conn = DatabaseConnection.getConnect()) {
            conn.setAutoCommit(false);

            // Insert order
            PreparedStatement orderStmt = conn.prepareStatement(
                    "INSERT INTO orders (user_email, total_amount, status, created_at) VALUES (?, ?, 'Completed', NOW())",
                    Statement.RETURN_GENERATED_KEYS
            );
            orderStmt.setString(1, user.getEmail());
            orderStmt.setDouble(2, total);
            orderStmt.executeUpdate();

            ResultSet keys = orderStmt.getGeneratedKeys();
            if (!keys.next()) { conn.rollback(); return; }
            int orderId = keys.getInt(1);

            // Insert order items
            PreparedStatement itemStmt = conn.prepareStatement(
                    "INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price) VALUES (?, ?, ?, ?)"
            );
            for (CartItem ci : items) {
                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, ci.getMenuItemId());
                itemStmt.setInt(3, ci.getQuantity());
                itemStmt.setDouble(4, ci.getPrice());
                itemStmt.addBatch();
            }
            itemStmt.executeBatch();

            // Deduct wallet balance
            PreparedStatement walletStmt = conn.prepareStatement(
                "UPDATE users SET wallet = wallet - ? WHERE email = ?"
            );
            walletStmt.setDouble(1, total);
            walletStmt.setString(2, user.getEmail());
            walletStmt.executeUpdate();

            conn.commit();

            // Update the in-session balance so the UI reflects it immediately
            user.setAccountBalance((float)(user.getAccountBalance() - total));

            CartManager.getInstance().clear();
            showReceipt(orderId, total, new java.util.ArrayList<>(items));

        } catch (SQLException ex) {
            Logger.getLogger(CartController.class.getName()).log(Level.SEVERE, null, ex);
            showError("Order failed: " + ex.getMessage());
        }
    }

    private void showReceipt(int orderId, double total, java.util.List<CartItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════╗\n");
        sb.append("         PAU CAFETERIA\n");
        sb.append("──────────────────────────────\n");
        sb.append("Order #").append(orderId).append("\n\n");
        for (CartItem ci : items) {
            sb.append(String.format("%-20s x%d\n", ci.getName(), ci.getQuantity()));
            sb.append(String.format("  ₦%,.2f\n", ci.getSubtotal()));
        }
        sb.append("──────────────────────────────\n");
        sb.append(String.format("TOTAL:   ₦%,.2f\n", total));
        sb.append("╚══════════════════════════════╝\n");
        sb.append("\nThank you! Your order is confirmed.");

        Alert receipt = new Alert(Alert.AlertType.INFORMATION);
        receipt.setTitle("Order Receipt");
        receipt.setHeaderText("✅ Order Placed Successfully!");
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");
        receipt.getDialogPane().setContent(ta);
        receipt.showAndWait();
        renderCart(); // clears screen visually
        try {
            SceneManager.switchTo("resources/views/dashboard.fxml", true);
        } catch (IOException ex) {
            Logger.getLogger(CartController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }

    @FXML
    private void goBack() {
        try {
            SceneManager.switchTo("resources/views/menu.fxml", false);
        } catch (IOException ex) {
            Logger.getLogger(CartController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
