package cafeteriaapp;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CartManager {

    private static final CartManager instance = new CartManager();
    private final ObservableList<CartItem> items = FXCollections.observableArrayList();

    private CartManager() {}

    public static CartManager getInstance() { return instance; }

    public ObservableList<CartItem> getItems() { return items; }

    public void addItem(int menuItemId, String name, double price) {
        for (CartItem item : items) {
            if (item.getMenuItemId() == menuItemId) {
                item.setQuantity(item.getQuantity() + 1);
                return;
            }
        }
        items.add(new CartItem(menuItemId, name, price, 1));
    }

    public void removeItem(CartItem item) {
        items.remove(item);
    }

    public double getTotal() {
        return items.stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public int getTotalCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public void clear() {
        items.clear();
    }
}
