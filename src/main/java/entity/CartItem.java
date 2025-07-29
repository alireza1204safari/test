package entity;

public class CartItem {
    public int foodId;
    public int quantity;

    public CartItem(int foodId, int quantity) {
        this.foodId = foodId;
        this.quantity = quantity;
    }
}
