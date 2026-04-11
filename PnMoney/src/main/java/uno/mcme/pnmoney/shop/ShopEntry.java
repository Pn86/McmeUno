package uno.mcme.pnmoney.shop;

import java.math.BigDecimal;
import java.util.List;

public class ShopEntry {

    private final String id;
    private final BigDecimal price;
    private final List<String> commands;

    public ShopEntry(String id, BigDecimal price, List<String> commands) {
        this.id = id;
        this.price = price;
        this.commands = commands;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public List<String> getCommands() {
        return commands;
    }
}
