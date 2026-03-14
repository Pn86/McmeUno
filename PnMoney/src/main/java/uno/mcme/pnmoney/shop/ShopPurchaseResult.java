package uno.mcme.pnmoney.shop;

import java.math.BigDecimal;

public record ShopPurchaseResult(ShopPurchaseStatus status, BigDecimal price) {

    public static ShopPurchaseResult of(ShopPurchaseStatus status) {
        return new ShopPurchaseResult(status, BigDecimal.ZERO);
    }

    public static ShopPurchaseResult success(BigDecimal price) {
        return new ShopPurchaseResult(ShopPurchaseStatus.SUCCESS, price);
    }
}
