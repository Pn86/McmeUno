package uno.mcme.pnmoneyshop.shop;

import java.math.BigDecimal;
import java.util.List;

public record ShopEntry(String id, BigDecimal price, Action action, List<String> commands) {

    public enum Action {
        DEDUCT,
        ADD;

        public static Action fromString(String raw) {
            if (raw == null) {
                return DEDUCT;
            }
            return "add".equalsIgnoreCase(raw) ? ADD : DEDUCT;
        }
    }
}
