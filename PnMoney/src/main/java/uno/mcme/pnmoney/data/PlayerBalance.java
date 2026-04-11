package uno.mcme.pnmoney.data;

import java.math.BigDecimal;

public class PlayerBalance {

    private final String playerName;
    private final BigDecimal balance;

    public PlayerBalance(String playerName, BigDecimal balance) {
        this.playerName = playerName;
        this.balance = balance;
    }

    public String getPlayerName() {
        return playerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
