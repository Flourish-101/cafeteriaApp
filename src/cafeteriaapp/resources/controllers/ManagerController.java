package cafeteriaapp.resources.controllers;

import cafeteriaapp.DatabaseConnection;
import cafeteriaapp.SceneManager;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class ManagerController implements Initializable {

    @FXML private TableView<MenuItemRow> menuTable;
    @FXML private TableColumn<MenuItemRow, Integer> colId;
    @FXML private TableColumn<MenuItemRow, String> colName;
    @FXML private TableColumn<MenuItemRow, String> colCategory;
    @FXML private TableColumn<MenuItemRow, Integer> colQty;
    @FXML private TableColumn<MenuItemRow, Double> colPrice;
    @FXML private TableColumn<MenuItemRow, String> colDesc;

    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField priceField;
    @FXML private TextField qtyField;
    @FXML private TextArea descField;
    @FXML private Button saveBtn;
    @FXML private Button clearBtn;

    private final ObservableList<MenuItemRow> items = FXCollections.observableArrayList();
    private MenuItemRow editingItem = null;

    private static final String[] CATEGORIES = {"Drinks", "Main Meal", "Swallow", "Protein", "Snacks"};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        categoryCombo.getItems().addAll(CATEGORIES);
        categoryCombo.setValue("Drinks");

        menuTable.setItems(items);
        loadItems();

        // Click row to populate form for editing
        menuTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateForm(newVal);
        });
    }

    private void loadItems() {
        items.clear();
        try (Connection conn = DatabaseConnection.getConnect()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT id, name, category, stock_quantity, price, description FROM menu_items ORDER BY category, name");
            while (rs.next()) {
                items.add(new MenuItemRow(
                        rs.getInt("id"), rs.getString("name"), rs.getString("category"),
                        rs.getInt("stock_quantity"), rs.getDouble("price"), rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            Logger.getLogger(ManagerController.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void populateForm(MenuItemRow row) {
        editingItem = row;
        nameField.setText(row.getName());
        categoryCombo.setValue(row.getCategory());
        priceField.setText(String.valueOf(row.getPrice()));
        qtyField.setText(String.valueOf(row.getQuantity()));
        descField.setText(row.getDescription());
        saveBtn.setText("Update Item");
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        String category = categoryCombo.getValue();
        String priceText = priceField.getText().trim();
        String qtyText = qtyField.getText().trim();
        String desc = descField.getText().trim();

        if (name.isEmpty() || priceText.isEmpty() || qtyText.isEmpty()) {
            showError("Name, price, and quantity are required.");
            return;
        }

        double price;
        int qty;
        try {
            price = Double.parseDouble(priceText);
            qty = Integer.parseInt(qtyText);
        } catch (NumberFormatException e) {
            showError("Price must be a number and quantity must be a whole number.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnect()) {
            if (editingItem == null) {
                // INSERT
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO menu_items (name, category, stock_quantity, price, description) VALUES (?, ?, ?, ?, ?)");
                stmt.setString(1, name);
                stmt.setString(2, category);
                stmt.setInt(3, qty);
                stmt.setDouble(4, price);
                stmt.setString(5, desc);
                stmt.executeUpdate();
                showInfo("Menu item added successfully!");
            } else {
                // UPDATE
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE menu_items SET name=?, category=?, stock_quantity=?, price=?, description=? WHERE id=?");
                stmt.setString(1, name);
                stmt.setString(2, category);
                stmt.setInt(3, qty);
                stmt.setDouble(4, price);
                stmt.setString(5, desc);
                stmt.setInt(6, editingItem.getId());
                stmt.executeUpdate();
                showInfo("Menu item updated successfully!");
            }
            clearForm();
            loadItems();
        } catch (SQLException e) {
            Logger.getLogger(ManagerController.class.getName()).log(Level.SEVERE, null, e);
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        MenuItemRow selected = menuTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an item to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selected.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        try (Connection conn = DatabaseConnection.getConnect()) {
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM menu_items WHERE id = ?");
            stmt.setInt(1, selected.getId());
            stmt.executeUpdate();
            clearForm();
            loadItems();
        } catch (SQLException e) {
            Logger.getLogger(ManagerController.class.getName()).log(Level.SEVERE, null, e);
            showError("Could not delete item: " + e.getMessage());
        }
    }

    @FXML
    private void clearForm() {
        editingItem = null;
        nameField.clear();
        categoryCombo.setValue("Drinks");
        priceField.clear();
        qtyField.clear();
        descField.clear();
        saveBtn.setText("Add Item");
        menuTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleLogout() {
        try {
            cafeteriaapp.Session.getInstance().logout();
            SceneManager.switchTo("resources/views/login.fxml", false);
        } catch (IOException ex) {
            Logger.getLogger(ManagerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void showInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    // ── Inner model class for TableView ──────────────────────────────
    public static class MenuItemRow {
        private final int id;
        private String name, category, description;
        private int quantity;
        private double price;

        public MenuItemRow(int id, String name, String category, int quantity, double price, String description) {
            this.id = id; this.name = name; this.category = category;
            this.quantity = quantity; this.price = price; this.description = description;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public String getDescription() { return description; }
    }
}
