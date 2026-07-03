package me.izuki.ChunkTowns;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class ProtectionListener implements Listener {

    private final ChunkTowns plugin;

    public ProtectionListener(ChunkTowns plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockDamageBorda(BlockDamageEvent event) {
        Block bloco = event.getBlock();
        if (CityManager.lidarComBlocoQuebrado(bloco, false)) {
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block bloco = event.getBlock();
        Player player = event.getPlayer();
        Chunk chunk = bloco.getChunk();

        if (CityManager.lidarComBlocoQuebrado(bloco, true)) {
            return;
        }

        if (!plugin.getConfig().getStringList("mundos-ativados").contains(chunk.getWorld().getName())) {
            return;
        }

        if (CityManager.isChunkClaimed(chunk)) {
            if (!CityManager.getChunkOwner(chunk).equals(player.getUniqueId())) {
                player.sendMessage(plugin.getLang().getMessage("erros.terreno-alheio"));
                event.setCancelled(true);
            }
        } else {
            player.sendMessage(plugin.getLang().getMessage("erros.mundo-bloqueado"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block blocoColocado = event.getBlockPlaced();
        Chunk chunk = blocoColocado.getChunk();

        if (!plugin.getConfig().getStringList("mundos-ativados").contains(chunk.getWorld().getName())) {
            return;
        }

        if (CityManager.isChunkClaimed(chunk)) {
            if (!CityManager.getChunkOwner(chunk).equals(player.getUniqueId())) {
                player.sendMessage(plugin.getLang().getMessage("erros.terreno-alheio"));
                event.setCancelled(true);
                return;
            }

            // SE O DONO DA CIDADE COLOCOU O BLOCO:
            // Processa se foi colocado logo acima de uma barreira existente para empurrá-la para cima!
            CityManager.processarBlocoColocadoNaBorda(blocoColocado);

        } else {
            player.sendMessage(plugin.getLang().getMessage("erros.mundo-bloqueado"));
            event.setCancelled(true);
        }
    }
}