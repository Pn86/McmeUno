package cn.pn86.pnlevel.exp;

import java.util.ArrayList;
import java.util.List;

public class ExpRule {
    public enum Type {TIME, KILL, DESTROY, PLACE}

    private final String id;
    private final Type type;
    private final List<String> values;
    private final int upExp;
    private final String message;
    private final String title;
    private final String subtitle;

    public ExpRule(String id, Type type, List<String> values, int upExp, String message, String title, String subtitle) {
        this.id = id;
        this.type = type;
        this.values = new ArrayList<>(values);
        this.upExp = upExp;
        this.message = message;
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getId() { return id; }

    public Type getType() { return type; }

    public List<String> getValues() { return values; }

    public int getUpExp() { return upExp; }

    public String getMessage() { return message; }

    public String getTitle() { return title; }

    public String getSubtitle() { return subtitle; }
}
