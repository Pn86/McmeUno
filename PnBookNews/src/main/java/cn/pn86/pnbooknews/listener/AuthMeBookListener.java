package cn.pn86.pnbooknews.listener;

import cn.pn86.pnbooknews.PnBookNewsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AuthMeBookListener implements Listener {

    private static final String AUTHME_LOGIN_EVENT_CLASS = "fr.xephi.authme.events.LoginEvent";

    private final PnBookNewsPlugin plugin;
    private final Method getPlayerMethod;

    private AuthMeBookListener(PnBookNewsPlugin plugin, Method getPlayerMethod) {
        this.plugin = plugin;
        this.getPlayerMethod = getPlayerMethod;
    }

    public static AuthMeBookListener register(PnBookNewsPlugin plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("AuthMe")) {
            return null;
        }

        try {
            Class<?> eventClass = Class.forName(AUTHME_LOGIN_EVENT_CLASS);
            if (!Event.class.isAssignableFrom(eventClass)) {
                plugin.getLogger().warning("AuthMe 登录事件不是 Bukkit Event，已跳过兼容挂钩。");
                return null;
            }

            Method getPlayerMethod = eventClass.getMethod("getPlayer");
            if (!Player.class.isAssignableFrom(getPlayerMethod.getReturnType())) {
                plugin.getLogger().warning("AuthMe LoginEvent#getPlayer 返回值不是 Player，已跳过兼容挂钩。");
                return null;
            }

            AuthMeBookListener listener = new AuthMeBookListener(plugin, getPlayerMethod);
            EventExecutor executor = listener::execute;
            plugin.getServer().getPluginManager().registerEvent(
                eventClass.asSubclass(Event.class),
                listener,
                EventPriority.MONITOR,
                executor,
                plugin,
                true
            );
            return listener;
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().warning("未找到 AuthMe LoginEvent，已跳过兼容挂钩。");
        } catch (NoSuchMethodException ex) {
            plugin.getLogger().warning("未找到 AuthMe LoginEvent#getPlayer，已跳过兼容挂钩。");
        }
        return null;
    }

    private void execute(Listener listener, Event event) throws EventException {
        try {
            Player player = (Player) getPlayerMethod.invoke(event);
            if (plugin.shouldShowOnJoin()) {
                plugin.openNewsBookLater(player);
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new EventException(ex);
        }
    }
}
