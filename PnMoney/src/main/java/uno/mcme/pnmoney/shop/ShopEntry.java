package uno.mcme.pnmoney.shop;

import java.math.BigDecimal;
import java.util.List;

public record ShopEntry(String id, BigDecimal price, List<String> commands) {
}
