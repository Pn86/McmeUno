package cn.pn86.pnskill;

import cn.pn86.pnskill.command.PnSkillCommand;
import cn.pn86.pnskill.config.MessageService;
import cn.pn86.pnskill.config.SkillConfigLoader;
import cn.pn86.pnskill.listener.SkillTriggerListener;
import cn.pn86.pnskill.service.SkillCastService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PnSkillPlugin extends JavaPlugin {
    private MessageService messageService;
    private SkillConfigLoader skillConfigLoader;
    private SkillCastService skillCastService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("skill.yml", false);

        messageService = new MessageService(this);
        skillConfigLoader = new SkillConfigLoader(this);
        skillCastService = new SkillCastService(this, messageService, skillConfigLoader);

        reloadEverything();

        PnSkillCommand command = new PnSkillCommand(this, messageService, skillConfigLoader, skillCastService);
        PluginCommand pnsk = getCommand("pnsk");
        if (pnsk == null) {
            getLogger().severe("Missing command in plugin.yml: pnsk");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pnsk.setExecutor(command);
        pnsk.setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new SkillTriggerListener(skillCastService), this);
    }

    public void reloadEverything() {
        reloadConfig();
        messageService.reload();
        skillConfigLoader.reload();
        skillCastService.clearCooldownCache();
    }
}
