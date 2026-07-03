package me.izuki.ChunkTowns;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChunkTowns extends JavaPlugin {

    private static ChunkTowns instance;
    private LanguageManager languageManager;
    private static Economy econ = null;
    private boolean vaultDisponivel = false;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Configuração padrão
        saveDefaultConfig();

        // 2. Inicializa o gerenciador de idiomas dinâmico
        this.languageManager = new LanguageManager(this);

        // 3. Tenta vincular ao Vault apenas se a opção estiver ligada na config
        if (getConfig().getBoolean("economia.ativar-sistema-de-dinheiro", false)) {
            if (setupEconomy()) {
                this.vaultDisponivel = true;
                getLogger().info("[ChunkTowns] Integração opcional com o Vault ativa!");
            } else {
                getLogger().warning("[ChunkTowns] Sistema de dinheiro ativo na config, mas o Vault não foi achado!");
            }
        }

        // 4. Registra os comandos
        if (getCommand("cidade") != null) {
            getCommand("cidade").setExecutor(new CidadeCommand(this));
        }

        // 5. Registra os eventos
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        // 6. Carrega as cidades salvas do banco de dados em arquivo YAML
        CityManager.carregarCidades();

        getLogger().info("§a[ChunkTowns] Inicializado com sucesso!");
    }

    @Override
    public void onDisable() {
        // CORREÇÃO RELOAD: Remove as bordas falsas antes do desligamento do mundo, evitando salvar ouro/glowstone no mapa físico
        CityManager.desativarBordasParaSalvar();
        CityManager.salvarCidades();
        getLogger().info("§c[ChunkTowns] Desativado com sucesso!");
    }

    public void recarregarConfiguracoes() {
        // Remove blocos de borda temporários e salva os dados atuais
        CityManager.desativarBordasParaSalvar();
        CityManager.salvarCidades();

        reloadConfig();

        if (this.languageManager != null) {
            this.languageManager.recarregarIdioma();
        }

        if (getConfig().getBoolean("economia.ativar-sistema-de-dinheiro", false)) {
            if (setupEconomy()) {
                this.vaultDisponivel = true;
            } else {
                this.vaultDisponivel = false;
                econ = null;
            }
        } else {
            this.vaultDisponivel = false;
            econ = null;
        }

        // Recarrega do arquivo yaml recém salvo e coloca as bordas de volta nas novas posições limpas
        CityManager.carregarCidades();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        try {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            }
            econ = rsp.getProvider();
            return econ != null;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    public static ChunkTowns getInstance() {
        return instance;
    }

    public LanguageManager getLang() {
        return languageManager;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public boolean isVaultDisponivel() {
        return vaultDisponivel;
    }
}