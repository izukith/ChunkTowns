package me.izuki.ChunkTowns;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CidadeCommand implements CommandExecutor {

    private final ChunkTowns plugin;

    public CidadeCommand(ChunkTowns plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores in-game podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;

        // Subcomando de suporte para administradores recarregarem as configurações
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("chunktowns.admin")) {
                player.sendMessage("§cVocê não tem permissão para usar este comando.");
                return true;
            }
            plugin.recarregarConfiguracoes();
            player.sendMessage("§a[ChunkTowns] Configurações, banco de dados e traduções recarregados!");
            return true;
        }

        // Abre a GUI Principal estruturada com os 4 slots limitados
        MenuListener.abrirMenuPrincipal(player);
        return true;
    }
}