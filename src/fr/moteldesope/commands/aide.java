package fr.moteldesope.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class aide implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && cmd.getName().equalsIgnoreCase("boss")) {
			Player p = (Player)sender;
			p.sendMessage("§8+§7------------------------------------------§8+");
			p.sendMessage("  §eGuide d'utilisation des commandes du plugin");
			p.sendMessage("");
			p.sendMessage("  §c/summonBoss§7: §fPour accéder au menu pour faire");
			p.sendMessage("  apparaitre un boss.");
			p.sendMessage("  §c/removeBoss§7: §fPour supprimer le boss choisi.");
			p.sendMessage("  §c/getUltimateKit§7: §fPour les phases de combat de test");
			p.sendMessage("  §c/boss§7: §fPour connaitre les commandes.");
			p.sendMessage("");
			p.sendMessage("  §6Créateur du plugin §e: §fMoteldeSope");
			p.sendMessage("§8+§7------------------------------------------§8+");
			return true;
		}
		return false;
	}
}
