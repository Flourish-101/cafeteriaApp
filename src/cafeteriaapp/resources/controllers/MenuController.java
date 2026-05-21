package cafeteriaapp.resources.controllers;

import cafeteriaapp.CartManager;
import cafeteriaapp.DatabaseConnection;
import cafeteriaapp.SceneManager;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MenuController implements Initializable {

    @FXML private VBox menuListContainer;
    @FXML private TextField searchField;
    @FXML private Label cartCountLabel;
    @FXML private HBox categoryTabsBox;

    private final List<MenuItem> allItems = new ArrayList<>();
    private String activeCategory = "All";

    private static final String[] CATEGORIES = {"All", "Main Meal", "Drinks", "Snacks", "Protein", "Swallow"};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadFromDatabase();
        buildCategoryTabs();
        renderFiltered();
        updateCartBadge();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> renderFiltered());
        }
    }

    // ── Called from HomeTabController passing a category ──────────────
    public void setCategory(String category) {
        this.activeCategory = category;
        buildCategoryTabs();
        renderFiltered();
    }

    private void buildCategoryTabs() {
        if (categoryTabsBox == null) return;
        categoryTabsBox.getChildren().clear();
        for (String cat : CATEGORIES) {
            Button tab = new Button(cat);
            boolean active = cat.equals(activeCategory);
            tab.setStyle(active
                ? "-fx-background-color: #0d7377; -fx-text-fill: white; -fx-background-radius: 20; "
                + "-fx-border-radius: 20; -fx-padding: 7 18 7 18; -fx-font-weight: bold; "
                + "-fx-font-size: 13px; -fx-cursor: hand;"
                : "-fx-background-color: white; -fx-text-fill: #0d7377; -fx-background-radius: 20; "
                + "-fx-border-radius: 20; -fx-border-color: #0d7377; -fx-border-width: 1.5; "
                + "-fx-padding: 7 18 7 18; -fx-font-size: 13px; -fx-cursor: hand;");
            tab.setOnAction(e -> {
                activeCategory = cat;
                buildCategoryTabs();
                renderFiltered();
            });
            categoryTabsBox.getChildren().add(tab);
        }
    }

    private void renderFiltered() {
        String search = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        List<MenuItem> filtered = new ArrayList<>();
        for (MenuItem m : allItems) {
            boolean matchCat = activeCategory.equals("All") || m.getCategory().equalsIgnoreCase(activeCategory);
            boolean matchSearch = search.isEmpty()
                    || m.getName().toLowerCase().contains(search)
                    || m.getCategory().toLowerCase().contains(search);
            if (matchCat && matchSearch) filtered.add(m);
        }
        renderGrouped(filtered);
    }

    private void updateCartBadge() {
        if (cartCountLabel != null) {
            int count = CartManager.getInstance().getTotalCount();
            cartCountLabel.setText(count > 0 ? String.valueOf(count) : "");
            cartCountLabel.setVisible(count > 0);
        }
    }

    private void loadFromDatabase() {
        try (Connection conn = DatabaseConnection.getConnect()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, name, category, stock_quantity, price, description FROM menu_items ORDER BY category, name");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                allItems.add(new MenuItem(
                        rs.getInt("id"), rs.getString("name"), rs.getString("category"),
                        rs.getInt("stock_quantity"), rs.getDouble("price"), rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            Logger.getLogger(MenuController.class.getName()).log(Level.SEVERE, null, e);
            loadFromCSV();
        }
    }

    private void loadFromCSV() {
        try (var is = getClass().getResourceAsStream("../data/menu_items.csv");
             var br = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
            String line; boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] p = line.split(",");
                allItems.add(new MenuItem(Integer.parseInt(p[0].trim()), p[1].trim(), p[2].trim(),
                        Integer.parseInt(p[3].trim()), Double.parseDouble(p[4].trim()), p[5].trim()));
            }
        } catch (Exception ex) {
            Logger.getLogger(MenuController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void renderGrouped(List<MenuItem> items) {
        menuListContainer.getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label("No items found.");
            empty.getStyleClass().add("empty-label");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            menuListContainer.getChildren().add(empty);
            return;
        }

        LinkedHashMap<String, List<MenuItem>> grouped = new LinkedHashMap<>();
        for (MenuItem item : items) {
            grouped.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<MenuItem>> entry : grouped.entrySet()) {
            Label header = new Label(entry.getKey());
            header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            header.setTextFill(Color.web("#0d7377"));
            header.setPadding(new javafx.geometry.Insets(16, 0, 8, 0));
            menuListContainer.getChildren().add(header);
            for (MenuItem item : entry.getValue()) {
                menuListContainer.getChildren().add(buildCard(item));
            }
        }
    }

    private VBox buildCard(MenuItem item) {
        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("menu-item-wrapper");

        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2);");

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("item-image-container");
        imageBox.setMinSize(72, 72); imageBox.setMaxSize(72, 72);
        ImageView imageView = new ImageView();
        imageView.setFitWidth(72); imageView.setFitHeight(72); imageView.setPreserveRatio(true);
        try {
            URL imgUrl = getClass().getResource("../images/" + item.getName().toLowerCase().replace(" ", "_") + ".png");
            if (imgUrl != null) imageView.setImage(new Image(imgUrl.toExternalForm(), true));
        } catch (Exception ignored) {}
        imageBox.getChildren().add(imageView);

        VBox info = new VBox(4);
        info.getStyleClass().add("item-info");
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLbl = new Label(item.getName());
        nameLbl.getStyleClass().add("item-name");
        nameLbl.setWrapText(true); nameLbl.setMaxWidth(160);

        Label qtyLbl = new Label(item.getQuantity() > 0 ? "In stock: " + item.getQuantity() : "Out of stock");
        qtyLbl.getStyleClass().add("item-quantity");
        if (item.getQuantity() == 0) qtyLbl.setTextFill(Color.RED);
        info.getChildren().addAll(nameLbl, qtyLbl);

        VBox rightCol = new VBox(6);
        rightCol.setAlignment(Pos.CENTER_RIGHT);
        Label priceLbl = new Label("₦" + String.format("%,.2f", item.getPrice()));
        priceLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0d7377;");

        Button addBtn = new Button("Add  +");
        addBtn.getStyleClass().add("add-btn");
        addBtn.setDisable(item.getQuantity() == 0);
        addBtn.setOnAction(e -> {
            CartManager.getInstance().addItem(item.getId(), item.getName(), item.getPrice());
            updateCartBadge();
            addBtn.setText("Added ✓");
            addBtn.setStyle("-fx-background-color: #0d7377; -fx-border-color: #0d7377; "
                    + "-fx-text-fill: white; -fx-border-radius: 20; -fx-background-radius: 20; "
                    + "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 7 18 7 18; -fx-min-width: 90;");
            new Thread(() -> {
                try { Thread.sleep(900); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> { addBtn.setText("Add  +"); addBtn.setStyle(""); });
            }).start();
        });

        rightCol.getChildren().addAll(priceLbl, addBtn);
        card.getChildren().addAll(imageBox, info, rightCol);
        wrapper.getChildren().add(card);
        return wrapper;
    }

    @FXML private void goToCart() {
        try { SceneManager.switchTo("resources/views/cart.fxml", true); }
        catch (IOException ex) { Logger.getLogger(MenuController.class.getName()).log(Level.SEVERE, null, ex); }
    }

    @FXML private void goBack() {
        try { SceneManager.switchTo("resources/views/dashboard.fxml", true); }
        catch (IOException ex) { Logger.getLogger(MenuController.class.getName()).log(Level.SEVERE, null, ex); }
    }

    public static class MenuItem {
        private final int id;
        private final String name, category, description;
        private final int quantity;
        private final double price;
        public MenuItem(int id, String name, String category, int quantity, double price, String description) {
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
