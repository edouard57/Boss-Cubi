package fr.moteldesope.mainpackage;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class removeBoss implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && cmd.getName().equalsIgnoreCase("removeBoss")) {
			Player p = (Player)sender;
			p.sendMessage("§6[§eLifesteal SMP Metz§6] §2Le boss §2a été supprimé §a!");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "minecraft:kill @e[type=minecraft:giant]");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "minecraft:kill @e[type=minecraft:iron_golem]");
			return true;
		}
		return false;
	}

}
