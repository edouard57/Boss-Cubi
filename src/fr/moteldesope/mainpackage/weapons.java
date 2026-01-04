package fr.moteldesope.mainpackage;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class weapons implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && cmd.getName().equalsIgnoreCase("getUltimateSword")) {
			Player p = (Player)sender;
			p.sendMessage("§6[§eLifesteal SMP Metz§6] §cUne §dUltimateSword§c vous a été donné §4!");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "minecraft:give @p minecraft:netherite_sword[custom_name=[{\"text\":\"UltimateSword\"}],enchantments={bane_of_arthropods:255,sharpness:255,smite:255,fortune:255,fire_aspect:255,looting:255,sweeping_edge:255},rarity=epic] 1");
			return true;
		}
		return false;
	}

}
