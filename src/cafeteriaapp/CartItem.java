package cafeteriaapp;

import javafx.beans.property.*;

public class CartItem {

    private final int menuItemId;
    private final StringProperty name = new SimpleStringProperty();
    private final DoubleProperty price = new SimpleDoubleProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();

    public CartItem(int menuItemId, String name, double price, int quantity) {
        this.menuItemId = menuItemId;
        this.name.set(name);
        this.price.set(price);
        this.quantity.set(quantity);
    }

    public int getMenuItemId() { return menuItemId; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public double getPrice() { return price.get(); }
    public DoubleProperty priceProperty() { return price; }

    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int qty) { this.quantity.set(qty); }
    public IntegerProperty quantityProperty() { return quantity; }

    public double getSubtotal() { return price.get() * quantity.get(); }
}
