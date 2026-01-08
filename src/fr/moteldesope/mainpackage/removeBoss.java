package fr.moteldesope.mainpackage;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class removeBoss implements CommandExecutor {

    private final JavaPlugin plugin;

    public removeBoss(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        Player p = (Player) sender;

        if (!cmd.getName().equalsIgnoreCase("removeboss")) return false;

        // Suppression des entités boss
        for (Entity entity : p.getWorld().getEntities()) {
            if (entity.getScoreboardTags().contains("boss")) {
                entity.remove();
            }
        }

        // Suppression du BossBar
        NamespacedKey key = new NamespacedKey(plugin, "bossbar");
        BossBar bar = Bukkit.getBossBar(key);
        if (bar != null) {
            bar.removeAll();
            Bukkit.removeBossBar(key);
        }

        p.sendMessage("§6[§eLifesteal SMP Metz§6] §aLe boss a été supprimé !");
        return true;
    }
}


