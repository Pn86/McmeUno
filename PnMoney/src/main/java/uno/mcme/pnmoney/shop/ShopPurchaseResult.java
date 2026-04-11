package uno.mcme.pnmoney.shop;

import java.math.BigDecimal;

public class ShopPurchaseResult {

    private final ShopPurchaseStatus status;
    private final BigDecimal price;

    public ShopPurchaseResult(ShopPurchaseStatus status, BigDecimal price) {
        this.status = status;
        this.price = price;
    }

    public static ShopPurchaseResult of(ShopPurchaseStatus status) {
        return new ShopPurchaseResult(status, BigDecimal.ZERO);
    }

    public static ShopPurchaseResult success(BigDecimal price) {
        return new ShopPurchaseResult(ShopPurchaseStatus.SUCCESS, price);
    }

    public ShopPurchaseStatus getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
