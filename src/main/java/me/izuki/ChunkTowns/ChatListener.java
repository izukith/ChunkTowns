package me.izuki.ChunkTowns;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final ChunkTowns plugin;

    public ChatListener(ChunkTowns plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!CityManager.estaAguardandoNome(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String mensagem = event.getMessage().trim();

        if (mensagem.equalsIgnoreCase("cancelar") || mensagem.equalsIgnoreCase("cancel")) {
            CityManager.removerDeModoCriacao(player.getUniqueId());
            player.sendMessage(plugin.getLang().getMessage("erros.processo-cancelado"));
            return;
        }

        if (mensagem.length() < 3 || mensagem.length() > 16) {
            player.sendMessage(plugin.getLang().getMessage("erros.nome-invalido"));
            return;
        }

        Chunk chunkAlvo = CityManager.getChunkPendente(player.getUniqueId());
        CityManager.removerDeModoCriacao(player.getUniqueId());

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            int raioDistancia = plugin.getConfig().getInt("configurações.raio-distancia-chunks", 3);
            if (CityManager.estaMuitoPertoDeOutraCidade(chunkAlvo, raioDistancia, player.getUniqueId())) {
                player.sendMessage(plugin.getLang().getMessage("erros.muito-perto"));
                return;
            }

            boolean usarDinheiro = plugin.getConfig().getBoolean("economia.ativar-sistema-de-dinheiro", false);
            if (usarDinheiro) {
                if (!plugin.isVaultDisponivel()) {
                    player.sendMessage(plugin.getLang().getMessage("erros.vault-nao-encontrado"));
                    return;
                }
                double custo = plugin.getConfig().getDouble("economia.preco-criar-cidade", 0.0);
                Economy econ = ChunkTowns.getEconomy();
                if (econ != null && custo > 0.0 && !econ.has(player, custo)) {
                    player.sendMessage(plugin.getLang().getMessage("erros.sem-dinheiro").replace("{custo}", String.valueOf(custo)));
                    return;
                }
                if (econ != null && custo > 0.0) {
                    econ.withdrawPlayer(player, custo);
                }
            }

            CityManager.criarNovaCidade(mensagem, player.getUniqueId(), chunkAlvo);
            String sucessoMsg = plugin.getLang().getMessage("cidade.criada-sucesso").replace("{nome}", mensagem);
            player.sendMessage(sucessoMsg);
        });
    }
}