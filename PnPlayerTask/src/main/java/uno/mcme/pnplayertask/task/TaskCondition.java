package uno.mcme.pnplayertask.task;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import uno.mcme.pnplayertask.util.Text;

public record TaskCondition(ConditionType type, String left, String operator, String right) {
    public boolean matches(Player player, PlayerTaskManager manager) {
        return switch (type) {
            case HAVE_ITEM -> compare(countItem(player, left), operator, parseDouble(right));
            case USE_ITEM -> compare(manager.getProgress(player.getUniqueId(), left, "use_item"), operator, parseDouble(right));
            case GET_ITEM -> compare(manager.getProgress(player.getUniqueId(), left, "get_item"), operator, parseDouble(right));
            case DROP_ITEM -> compare(manager.getProgress(player.getUniqueId(), left, "drop_item"), operator, parseDouble(right));
            case INT -> compare(parseDouble(Text.papi(player, left)), operator, parseDouble(Text.papi(player, right)));
        };
    }
    private static int countItem(Player player, String matName) {
        Material mat = Material.matchMaterial(matName);
        if (mat == null) return 0;
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) if (item != null && item.getType() == mat) count += item.getAmount();
        return count;
    }
    static double parseDouble(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0D; } }
    static boolean compare(double a, String op, double b) { return switch (op) { case ">" -> a > b; case ">=" -> a >= b; case "<" -> a < b; case "<=" -> a <= b; case "!=" -> a != b; case "=", "==" -> a == b; default -> false; }; }
}
