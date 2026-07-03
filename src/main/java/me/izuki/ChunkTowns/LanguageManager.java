package me.izuki.ChunkTowns;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LanguageManager {

    private final ChunkTowns plugin;
    private FileConfiguration langConfig;
    private String prefixo;

    public LanguageManager(ChunkTowns plugin) {
        this.plugin = plugin;
        recarregarIdioma();
    }

    public void recarregarIdioma() {
        // Pega o idioma definido na config.yml (Ex: "pt")
        String códigoIdioma = plugin.getConfig().getString("idioma", "pt");
        String nomeArquivo = "messages_" + códigoIdioma + ".yml";

        File arquivoIdioma = new File(plugin.getDataFolder(), nomeArquivo);

        // Se o arquivo de tradução não existir na pasta, tenta salvar o padrão do jar
        if (!arquivoIdioma.exists()) {
            try {
                plugin.saveResource(nomeArquivo, false);
            } catch (Exception e) {
                // Se não houver esse arquivo dentro do jar, cria um arquivo vazio para o admin traduzir
                try {
                    arquivoIdioma.createNewFile();
                } catch (Exception ignored) {}
            }
        }

        this.langConfig = YamlConfiguration.loadConfiguration(arquivoIdioma);

        // Carrega um fallback interno caso faltem chaves no arquivo externo
        InputStream streamInterna = plugin.getResource(nomeArquivo);
        if (streamInterna != null) {
            YamlConfiguration configInterna = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(streamInterna, StandardCharsets.UTF_8)
            );
            this.langConfig.setDefaults(configInterna);
        }

        // Define o prefixo global do plugin
        this.prefixo = langConfig.getString("config.prefix", "§b[ChunkTowns] ");
    }

    public String getMessage(String path) {
        if (langConfig == null || !langConfig.contains(path)) {
            return "§c[Chave '" + path + "' não encontrada]";
        }
        String mensagem = langConfig.getString(path);
        if (mensagem == null) return "";

        // Se for uma mensagem de erro ou sucesso comum, adiciona o prefixo automático
        if (path.startsWith("erros.") || path.startsWith("cidade.")) {
            return prefixo + mensagem;
        }
        return mensagem;
    }

    public List<String> getMessageList(String path) {
        if (langConfig == null || !langConfig.contains(path)) {
            List<String> erro = new ArrayList<>();
            erro.add("§c[Lista '" + path + "' não encontrada]");
            return erro;
        }
        return langConfig.getStringList(path);
    }
}