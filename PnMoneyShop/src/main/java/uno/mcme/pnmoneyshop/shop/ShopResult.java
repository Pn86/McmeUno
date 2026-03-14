package uno.mcme.pnmoneyshop.shop;

import java.math.BigDecimal;

public record ShopResult(Status status, BigDecimal price, BigDecimal balance, String reason) {

    public static ShopResult of(Status status, String reason) {
        return new ShopResult(status, null, null, reason);
    }

    public static ShopResult of(Status status, BigDecimal price, BigDecimal balance, String reason) {
        return new ShopResult(status, price, balance, reason);
    }

    public enum Status {
        SUCCESS,
        DISABLED,
        ITEM_NOT_FOUND,
        INVALID_PRICE,
        PLACEHOLDER_MISSING,
        BALANCE_PARSE_FAILED,
        NOT_ENOUGH,
        MONEY_COMMAND_FAILED
    }
}
