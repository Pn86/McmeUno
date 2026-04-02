package uno.mcme.pnoremine.mine;

public enum DropMode {
    VALUE,
    ITEM;

    public static DropMode fromConfig(String value) {
        if (value == null) {
            return VALUE;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.equals("item")) {
            return ITEM;
        }
        return VALUE;
    }
}
