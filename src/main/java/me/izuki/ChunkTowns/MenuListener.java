package me.izuki.ChunkTowns;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MenuListener implements Listener {

    private final ChunkTowns plugin;
    private static final String TITULO_PRINCIPAL = "§8Sua Cidades (Max 4)";
    private static final String TITULO_CONFIRMAR = "§cDeletar: ";
    private static final String TITULO_CONFIG = "§8Configurações: ";

    // Slots das 4 cidades na linha do meio
    private static final int[] SLOTS_CIDADES = {10, 12, 14, 16};

    public MenuListener(ChunkTowns plugin) {
        this.plugin = plugin;
    }

    // --- 1. MENU PRINCIPAL (27 SLOTS) ---
    public static void abrirMenuPrincipal(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITULO_PRINCIPAL);
        UUID uuid = player.getUniqueId();
        String mundo = player.getWorld().getName();
        List<CityManager.Cidade> cidades = CityManager.getCidadesDoJogador(mundo, uuid);

        // Fundo padrão cinza
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, criarItem(Material.GRAY_STAINED_GLASS_PANE, "§7 ", null));
        }

        // Renderiza as cidades ou os slots vazios (Preto)
        for (int i = 0; i < 4; i++) {
            int slotAlvo = SLOTS_CIDADES[i];
            if (i < cidades.size()) {
                CityManager.Cidade cidade = cidades.get(i);
                int totalChunks = (cidade.getChunksMapeadas() != null) ? cidade.getChunksMapeadas().size() : 0;

                List<String> lore = new ArrayList<>();
                lore.add("§7-----------------------");
                lore.add("§7» Chunks dominadas: §a" + totalChunks);
                lore.add("§7-----------------------");
                lore.add("§eClique para abrir configurações.");

                gui.setItem(slotAlvo, criarItemComLoreList(Material.PAPER, "§aCidade: §f" + cidade.getNome(), lore));
            } else {
                gui.setItem(slotAlvo, criarItem(Material.BLACK_STAINED_GLASS_PANE, "§8Slot Vazio", "§7Você pode criar uma cidade aqui."));
            }
        }

        // Botão de Criar Cidade alterado para Bloco de Esmeralda no slot central inferior (22)
        gui.setItem(22, criarItem(Material.EMERALD_BLOCK, "§a§lCRIAR CIDADE", "§7Clique para iniciar a criação."));

        player.openInventory(gui);
    }

    // --- 2. MENU DE CONFIGURAÇÕES DA CIDADE (27 SLOTS) ---
    private static void abrirMenuConfiguracoesCidade(Player player, String nomeCidade) {
        Inventory gui = Bukkit.createInventory(null, 27, TITULO_CONFIG + nomeCidade);
        String mundo = player.getWorld().getName();
        CityManager.Cidade cidade = CityManager.getCidadePeloNome(mundo, nomeCidade);

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, criarItem(Material.GRAY_STAINED_GLASS_PANE, "§7 ", null));
        }

        // Papel de Informações movido para o topo central (Slot 4)
        List<String> loreInfo = new ArrayList<>();
        if (cidade != null) {
            int totalChunks = (cidade.getChunksMapeadas() != null) ? cidade.getChunksMapeadas().size() : 0;
            loreInfo.add("§7-----------------------");
            loreInfo.add("§7» Território: §a" + totalChunks + " Chunks");
            loreInfo.add("§7-----------------------");
        }
        gui.setItem(4, criarItemComLoreList(Material.PAPER, "§aInformações: §f" + nomeCidade, loreInfo));

        // OPÇÃO 1: Botão Liga/Desliga Borda (Slot 11)
        // Aqui assumimos um comportamento visual dinâmico.
        // Se o seu plugin tiver uma forma de checar se a borda está ativa, substitua o 'true' abaixo.
        boolean bordaAtiva = true;
        if (bordaAtiva) {
            gui.setItem(11, criarItem(Material.GREEN_STAINED_GLASS_PANE, "§a§lBorda: LIGADA", "§7Clique para desligar a visualização física."));
        } else {
            gui.setItem(11, criarItem(Material.GRAY_STAINED_GLASS_PANE, "§7§lBorda: DESLIGADA", "§7Clique para ligar a visualização física."));
        }

        // OPÇÃO 2: Botão Deletar Cidade movido para cá como uma TNT (Slot 15)
        gui.setItem(15, criarItem(Material.TNT, "§c§lDELETAR CIDADE", "§7Clique para apagar permanentemente esta cidade."));

        // Botão Voltar centralizado na última linha (Slot 22)
        gui.setItem(22, criarItem(Material.BARRIER, "§c§lVOLTAR", "§7Retornar ao menu principal."));

        player.openInventory(gui);
    }

    // --- 3. MENU DE CONFIRMAÇÃO DIRETO (27 SLOTS) ---
    private static void abrirMenuConfirmacao(Player player, String nomeCidade) {
        Inventory gui = Bukkit.createInventory(null, 27, TITULO_CONFIRMAR + nomeCidade);

        for (int linha = 0; linha < 3; linha++) {
            int base = linha * 9;

            // Confirmar (Esquerda)
            gui.setItem(base, criarItem(Material.LIME_STAINED_GLASS_PANE, "§a§lCONFIRMAR", "§7Apagar permanentemente."));
            gui.setItem(base + 1, criarItem(Material.LIME_STAINED_GLASS_PANE, "§a§lCONFIRMAR", "§7Apagar permanentemente."));
            gui.setItem(base + 2, criarItem(Material.LIME_STAINED_GLASS_PANE, "§a§lCONFIRMAR", "§7Apagar permanentemente."));

            gui.setItem(base + 3, criarItem(Material.GRAY_STAINED_GLASS_PANE, "§7 ", null));
            gui.setItem(base + 5, criarItem(Material.GRAY_STAINED_GLASS_PANE, "§7 ", null));

            // Cancelar (Direita)
            gui.setItem(base + 6, criarItem(Material.RED_STAINED_GLASS_PANE, "§c§lCANCELAR", "§7Voltar ao menu seguro."));
            gui.setItem(base + 7, criarItem(Material.RED_STAINED_GLASS_PANE, "§c§lCANCELAR", "§7Voltar ao menu seguro."));
            gui.setItem(base + 8, criarItem(Material.RED_STAINED_GLASS_PANE, "§c§lCANCELAR", "§7Voltar ao menu seguro."));
        }

        gui.setItem(13, criarItem(Material.PAPER, "§eCidade: §f" + nomeCidade, "§cEsta ação não tem volta!"));

        player.openInventory(gui);
    }

    // --- EVENTOS DE CLIQUE COORDENADOS ---
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        ItemStack clicado = event.getCurrentItem();

        if (clicado == null || clicado.getType() == Material.AIR) return;

        // 1. Cliques no Menu Principal
        if (titulo.equals(TITULO_PRINCIPAL)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            // Abre configurações ao clicar no papel da cidade correspondente
            if (clicado.getType() == Material.PAPER && clicado.hasItemMeta()) {
                boolean slotCidadeValido = false;
                for (int s : SLOTS_CIDADES) {
                    if (s == slot) { slotCidadeValido = true; break; }
                }
                if (slotCidadeValido) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                    String nomeCidade = clicado.getItemMeta().getDisplayName().replace("§aCidade: §f", "");
                    abrirMenuConfiguracoesCidade(player, nomeCidade);
                    return;
                }
            }

            if (slot == 22) { // Clicou no Bloco de Esmeralda (Criar)
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                String mundo = player.getWorld().getName();
                List<CityManager.Cidade> cidades = CityManager.getCidadesDoJogador(mundo, player.getUniqueId());

                if (cidades.size() >= 4) {
                    player.sendMessage("§cLimite atingido! Você só pode ter no máximo 4 cidades.");
                    player.closeInventory();
                    return;
                }

                int raio = plugin.getConfig().getInt("distancia-entre-cidades", 3);
                Chunk chunkAtual = player.getLocation().getChunk();

                if (CityManager.estaMuitoPertoDeOutraCidade(chunkAtual, raio, player.getUniqueId())) {
                    player.sendMessage("§cVocê está muito perto de outra cidade! Respeite a distância limite.");
                    player.closeInventory();
                    return;
                }

                CityManager.entrarEmModoCriacao(player.getUniqueId(), chunkAtual);
                player.sendMessage("§a[Cidade] Digite o nome da nova cidade no chat:");
                player.closeInventory();
            }
        }

        // 2. Cliques no Menu de Configurações da Cidade
        else if (titulo.startsWith(TITULO_CONFIG)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            String nomeCidade = titulo.replace(TITULO_CONFIG, "").trim();

            if (slot == 11) { // Clicou para alternar a Borda
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.1f);
                // Executa o comando de borda do plugin e atualiza o menu
                player.performCommand("cidade borda " + nomeCidade);
                abrirMenuConfiguracoesCidade(player, nomeCidade);
            }
            else if (slot == 15) { // Clicou na TNT (Vai direto confirmar deleção)
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                abrirMenuConfirmacao(player, nomeCidade);
            }
            else if (slot == 22) { // Clicou em Voltar
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                abrirMenuPrincipal(player);
            }
        }

        // 3. Cliques no Menu de Confirmação Final
        else if (titulo.startsWith(TITULO_CONFIRMAR)) {
            event.setCancelled(true);
            String nomeCidade = titulo.replace(TITULO_CONFIRMAR, "").trim();
            int slot = event.getRawSlot();
            int coluna = slot % 9;

            if (coluna >= 0 && coluna <= 2) { // Sim, deletar
                World mundo = player.getWorld();
                CityManager.Cidade cidadeAlvo = CityManager.getCidadePeloNome(mundo.getName(), nomeCidade);

                if (cidadeAlvo != null && !cidadeAlvo.getChunksMapeadas().isEmpty()) {
                    String deChunKey = cidadeAlvo.getChunksMapeadas().iterator().next();
                    String[] partes = deChunKey.split(",");
                    Chunk chunkDaCidade = mundo.getChunkAt(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]));

                    CityManager.deletarCidade(chunkDaCidade);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
                    player.sendMessage("§aCidade " + nomeCidade + " deletada e terreno restaurado!");
                } else {
                    player.sendMessage("§cCidade não encontrada ou já deletada.");
                }
                player.closeInventory();
            }
            else if (coluna >= 6 && coluna <= 8) { // Não, cancelar (volta pras configs da cidade)
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                player.sendMessage("§cDeleção cancelada.");
                abrirMenuConfiguracoesCidade(player, nomeCidade);
            }
        }
    }

    private static ItemStack criarItem(Material material, String nome, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nome);
            if (lore != null) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack criarItemComLoreList(Material material, String nome, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nome);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}