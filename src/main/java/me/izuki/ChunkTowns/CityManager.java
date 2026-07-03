package me.izuki.ChunkTowns;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class CityManager {

    public static class Cidade {
        private final String nome;
        private final UUID dono;
        private final Set<String> chunksMapeadas = new HashSet<>();
        private boolean bordasAtivasPermanente = false;

        public Cidade(String nome, UUID dono) {
            this.nome = nome;
            this.dono = dono;
        }

        public String getNome() { return nome; }
        public UUID getDono() { return dono; }
        public Set<String> getChunksMapeadas() { return chunksMapeadas; }
        public boolean isBordasAtivasPermanente() { return bordasAtivasPermanente; }
        public void setBordasAtivasPermanente(boolean estado) { this.bordasAtivasPermanente = estado; }

        public void adicionarChunk(Chunk chunk) {
            chunksMapeadas.add(chunk.getX() + "," + chunk.getZ());
        }
    }

    private static final Map<String, Map<String, Cidade>> cidadesPorMundo = new HashMap<>();
    private static final Map<String, String> vinculoChunkCidade = new HashMap<>();
    private static final Map<UUID, Chunk> modoCriacao = new HashMap<>();

    private static final Map<Location, Material> blocosMarcadosGlobal = new HashMap<>();
    private static final Map<Location, Integer> temporizadoresBordas = new HashMap<>();
    // Controla o delay de colocar blocos para não subir instantaneamente
    private static final Map<Location, Integer> temporizadoresColocacao = new HashMap<>();

    public static void criarNovaCidade(String nome, UUID dono, Chunk chunkInicial) {
        String mundo = chunkInicial.getWorld().getName();
        cidadesPorMundo.putIfAbsent(mundo, new HashMap<>());

        Cidade novaCidade = new Cidade(nome, dono);
        novaCidade.adicionarChunk(chunkInicial);

        cidadesPorMundo.get(mundo).put(nome.toLowerCase(), novaCidade);

        String chaveChunk = mundo + "," + chunkInicial.getX() + "," + chunkInicial.getZ();
        vinculoChunkCidade.put(chaveChunk, nome.toLowerCase());

        salvarCidades();

        gerarBordasDaCidade(novaCidade, chunkInicial.getWorld());
        Bukkit.getScheduler().runTaskLater(ChunkTowns.getInstance(), () -> {
            if (!novaCidade.isBordasAtivasPermanente()) {
                removerBordasDaCidade(novaCidade, chunkInicial.getWorld());
            }
        }, 100L);
    }

    public static void expandirCidadeExistente(String nomeCidade, Chunk novaChunk) {
        String mundo = novaChunk.getWorld().getName();
        Map<String, Cidade> mapaMundo = cidadesPorMundo.get(mundo);
        if (mapaMundo == null) return;

        Cidade cidade = mapaMundo.get(nomeCidade.toLowerCase());
        if (cidade == null) return;

        removerBordasDaCidade(cidade, novaChunk.getWorld());
        cidade.adicionarChunk(novaChunk);

        String chaveChunk = mundo + "," + novaChunk.getX() + "," + novaChunk.getZ();
        vinculoChunkCidade.put(chaveChunk, nomeCidade.toLowerCase());

        salvarCidades();

        gerarBordasDaCidade(cidade, novaChunk.getWorld());
        Bukkit.getScheduler().runTaskLater(ChunkTowns.getInstance(), () -> {
            if (!cidade.isBordasAtivasPermanente()) {
                removerBordasDaCidade(cidade, novaChunk.getWorld());
            }
        }, 100L);
    }

    // CORREÇÃO: Restaura o terreno perfeitamente antes de apagar a cidade
    public static String deletarCidade(Chunk chunk) {
        String mundo = chunk.getWorld().getName();
        String chaveChunk = mundo + "," + chunk.getX() + "," + chunk.getZ();
        String nomeCidadeBaixo = vinculoChunkCidade.get(chaveChunk);

        if (nomeCidadeBaixo == null) return null;

        Map<String, Cidade> mapaMundo = cidadesPorMundo.get(mundo);
        if (mapaMundo == null) return null;

        Cidade cidade = mapaMundo.remove(nomeCidadeBaixo);
        if (cidade != null) {
            // Remove fisicamente as bordas e devolve os blocos originais para o mapa do Minecraft
            removerBordasDaCidade(cidade, chunk.getWorld());

            for (String cKey : cidade.getChunksMapeadas()) {
                vinculoChunkCidade.remove(mundo + "," + cKey);
            }
            salvarCidades();
            return cidade.getNome();
        }
        return null;
    }

    public static void alternarBordasPermanente(Cidade cidade, World world) {
        cidade.setBordasAtivasPermanente(!cidade.isBordasAtivasPermanente());
        if (cidade.isBordasAtivasPermanente()) {
            gerarBordasDaCidade(cidade, world);
        } else {
            removerBordasDaCidade(cidade, world);
        }
    }

    public static void gerarBordasDaCidade(Cidade cidade, World world) {
        Set<String> malha = cidade.getChunksMapeadas();

        for (String cKey : malha) {
            String[] partes = cKey.split(",");
            int cx = Integer.parseInt(partes[0]);
            int cz = Integer.parseInt(partes[1]);

            int minX = cx << 4;
            int minZ = cz << 4;
            int maxX = minX + 15;
            int maxZ = minZ + 15;

            boolean temNorte = malha.contains(cx + "," + (cz - 1));
            boolean temSul   = malha.contains(cx + "," + (cz + 1));
            boolean temOeste = malha.contains((cx - 1) + "," + cz);
            boolean temLeste = malha.contains((cx + 1) + "," + cz);

            if (!temNorte && !temOeste) {
                marcarBlocoBorda(world, minX, minZ, Material.GLOWSTONE);
                marcarBlocoBorda(world, minX + 1, minZ, Material.GOLD_BLOCK);
                marcarBlocoBorda(world, minX, minZ + 1, Material.GOLD_BLOCK);
            }
            if (!temNorte && !temLeste) {
                marcarBlocoBorda(world, maxX, minZ, Material.GLOWSTONE);
                marcarBlocoBorda(world, maxX - 1, minZ, Material.GOLD_BLOCK);
                marcarBlocoBorda(world, maxX, minZ + 1, Material.GOLD_BLOCK);
            }
            if (!temSul && !temOeste) {
                marcarBlocoBorda(world, minX, maxZ, Material.GLOWSTONE);
                marcarBlocoBorda(world, minX + 1, maxZ, Material.GOLD_BLOCK);
                marcarBlocoBorda(world, minX, maxZ - 1, Material.GOLD_BLOCK);
            }
            if (!temSul && !temLeste) {
                marcarBlocoBorda(world, maxX, maxZ, Material.GLOWSTONE);
                marcarBlocoBorda(world, maxX - 1, maxZ, Material.GOLD_BLOCK);
                marcarBlocoBorda(world, maxX, maxZ - 1, Material.GOLD_BLOCK);
            }
        }
    }

    private static void marcarBlocoBorda(World world, int x, int z, Material mat) {
        int y = world.getHighestBlockYAt(x, z);
        Block bloco = world.getBlockAt(x, y, z);
        Location loc = bloco.getLocation().clone();

        if (temporizadoresBordas.containsKey(loc) || temporizadoresColocacao.containsKey(loc)) return;

        blocosMarcadosGlobal.putIfAbsent(loc, bloco.getType());
        bloco.setType(mat);
    }

    public static void removerBordasDaCidade(Cidade cidade, World world) {
        for (String cKey : cidade.getChunksMapeadas()) {
            String[] partes = cKey.split(",");
            int cx = Integer.parseInt(partes[0]);
            int cz = Integer.parseInt(partes[1]);

            int minX = cx << 4;
            int minZ = cz << 4;
            int maxX = minX + 15;
            int maxZ = minZ + 15;

            int[] alcancesX = {minX, minX + 1, maxX, maxX - 1};
            int[] alcancesZ = {minZ, minZ + 1, maxZ, maxZ - 1};

            for (int x : alcancesX) {
                for (int z : alcancesZ) {
                    // Varre uma coluna vertical estendida para limpar as quinas completamente
                    for (int dy = 0; dy <= 256; dy++) {
                        Location loc = new Location(world, x, dy, z);
                        if (blocosMarcadosGlobal.containsKey(loc)) {
                            Material original = blocosMarcadosGlobal.remove(loc);
                            world.getBlockAt(loc).setType(original != null ? original : Material.AIR);
                        }
                        if (temporizadoresBordas.containsKey(loc)) {
                            Bukkit.getScheduler().cancelTask(temporizadoresBordas.remove(loc));
                        }
                        if (temporizadoresColocacao.containsKey(loc)) {
                            Bukkit.getScheduler().cancelTask(temporizadoresColocacao.remove(loc));
                        }
                    }
                }
            }
        }
    }

    public static boolean lidarComBlocoQuebrado(Block bloco, boolean foiMineradoAteOFinal) {
        Location loc = bloco.getLocation().clone();
        if (blocosMarcadosGlobal.containsKey(loc)) {
            Material original = blocosMarcadosGlobal.remove(loc);
            Material materialBordaQuebrada = bloco.getType();

            bloco.setType(original != Material.AIR ? original : Material.AIR);

            if (foiMineradoAteOFinal) {
                temporizadoresBordas.remove(loc);
                return true;
            }

            Chunk chunk = bloco.getChunk();
            String mundo = chunk.getWorld().getName();
            String chaveChunk = mundo + "," + chunk.getX() + "," + chunk.getZ();
            String nomeCidadeBaixo = vinculoChunkCidade.get(chaveChunk);

            if (nomeCidadeBaixo != null) {
                Map<String, Cidade> mapaMundo = cidadesPorMundo.get(mundo);
                if (mapaMundo != null) {
                    Cidade cidade = mapaMundo.get(nomeCidadeBaixo);
                    if (cidade != null && (cidade.isBordasAtivasPermanente() || modoCriacao.containsKey(cidade.getDono()))) {

                        if (temporizadoresBordas.containsKey(loc)) {
                            Bukkit.getScheduler().cancelTask(temporizadoresBordas.remove(loc));
                        }

                        int taskId = new BukkitRunnable() {
                            @Override
                            public void run() {
                                temporizadoresBordas.remove(loc);
                                if (vinculoChunkCidade.containsKey(chaveChunk)) {
                                    int novaAlturaY = chunk.getWorld().getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
                                    Block novoBlocoTopo = chunk.getWorld().getBlockAt(loc.getBlockX(), novaAlturaY, loc.getBlockZ());
                                    Location novaLoc = novoBlocoTopo.getLocation();

                                    if (!blocosMarcadosGlobal.containsKey(novaLoc)) {
                                        blocosMarcadosGlobal.put(novaLoc, novoBlocoTopo.getType());
                                        novoBlocoTopo.setType(materialBordaQuebrada);
                                    }
                                }
                            }
                        }.runTaskLater(ChunkTowns.getInstance(), 60L).getTaskId(); // 3 Segundos para reaparecer

                        temporizadoresBordas.put(loc, taskId);
                    }
                }
            }
            return true;
        }
        return false;
    }

    // CORREÇÃO: Com delay inteligente de 1.5s antes de subir a barreira
    public static void processarBlocoColocadoNaBorda(Block blocoColocado) {
        Location locDoBlocoNovo = blocoColocado.getLocation().clone();
        Location locAbaixo = locDoBlocoNovo.clone().add(0, -1, 0);

        if (blocosMarcadosGlobal.containsKey(locAbaixo)) {
            Material blocoOriginalEscondido = blocosMarcadosGlobal.remove(locAbaixo);
            Block blocoDeBaixo = locAbaixo.getBlock();

            blocoDeBaixo.setType(blocoOriginalEscondido != Material.AIR ? blocoOriginalEscondido : Material.AIR);

            if (temporizadoresBordas.containsKey(locAbaixo)) {
                Bukkit.getScheduler().cancelTask(temporizadoresBordas.remove(locAbaixo));
            }
            if (temporizadoresColocacao.containsKey(locAbaixo)) {
                Bukkit.getScheduler().cancelTask(temporizadoresColocacao.remove(locAbaixo));
            }

            Chunk chunk = blocoColocado.getChunk();
            String mundo = chunk.getWorld().getName();
            String chaveChunk = mundo + "," + chunk.getX() + "," + chunk.getZ();
            String nomeCidadeBaixo = vinculoChunkCidade.get(chaveChunk);

            if (nomeCidadeBaixo != null) {
                Map<String, Cidade> mapaMundo = cidadesPorMundo.get(mundo);
                if (mapaMundo != null) {
                    Cidade cidade = mapaMundo.get(nomeCidadeBaixo);
                    if (cidade != null) {
                        // Agenda para recalcular a altura e subir a barreira em 1.5 segundos (30 Ticks)
                        int taskId = new BukkitRunnable() {
                            @Override
                            public void run() {
                                temporizadoresColocacao.remove(locAbaixo);
                                if (vinculoChunkCidade.containsKey(chaveChunk)) {
                                    gerarBordasDaCidade(cidade, chunk.getWorld());
                                }
                            }
                        }.runTaskLater(ChunkTowns.getInstance(), 30L).getTaskId();

                        temporizadoresColocacao.put(locAbaixo, taskId);
                    }
                }
            }
        }
    }

    // CORREÇÃO RELOAD: Limpa as bordas visuais temporárias antes de salvar o arquivo físico
    public static void desativarBordasParaSalvar() {
        for (String mundoNome : cidadesPorMundo.keySet()) {
            World world = Bukkit.getWorld(mundoNome);
            if (world == null) continue;
            for (Cidade cidade : cidadesPorMundo.get(mundoNome).values()) {
                removerBordasDaCidade(cidade, world);
            }
        }
    }

    public static void salvarCidades() {
        File arquivo = new File(ChunkTowns.getInstance().getDataFolder(), "cidades.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(arquivo);
        config.set("cidades", null);

        for (String mundo : cidadesPorMundo.keySet()) {
            for (Cidade cidade : cidadesPorMundo.get(mundo).values()) {
                String caminhoBase = "cidades." + mundo + "." + cidade.getNome().toLowerCase();
                config.set(caminhoBase + ".nomeReal", cidade.getNome());
                config.set(caminhoBase + ".dono", cidade.getDono().toString());
                config.set(caminhoBase + ".bordasPermanentes", cidade.isBordasAtivasPermanente());
                config.set(caminhoBase + ".chunks", new ArrayList<>(cidade.getChunksMapeadas()));
            }
        }
        try {
            config.save(arquivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void carregarCidades() {
        File arquivo = new File(ChunkTowns.getInstance().getDataFolder(), "cidades.yml");
        if (!arquivo.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(arquivo);
        if (!config.contains("cidades")) return;

        cidadesPorMundo.clear();
        vinculoChunkCidade.clear();

        for (String mundo : config.getConfigurationSection("cidades").getKeys(false)) {
            cidadesPorMundo.put(mundo, new HashMap<>());
            for (String nomeBaixo : config.getConfigurationSection("cidades." + mundo).getKeys(false)) {
                String caminhoBase = "cidades." + mundo + "." + nomeBaixo;

                String nomeReal = config.getString(caminhoBase + ".nomeReal");
                UUID dono = UUID.fromString(config.getString(caminhoBase + ".dono"));
                boolean bordasPerm = config.getBoolean(caminhoBase + ".bordasPermanentes");
                List<String> chunksCarregadas = config.getStringList(caminhoBase + ".chunks");

                Cidade cidade = new Cidade(nomeReal, dono);
                cidade.setBordasAtivasPermanente(bordasPerm);

                for (String cKey : chunksCarregadas) {
                    cidade.getChunksMapeadas().add(cKey);
                    vinculoChunkCidade.put(mundo + "," + cKey, nomeBaixo);
                }
                cidadesPorMundo.get(mundo).put(nomeBaixo, cidade != null ? cidade : cidade);
            }
        }

        // Redesenha as bordas das cidades após o carregamento limpo da configuração
        for (String mundoNome : cidadesPorMundo.keySet()) {
            World world = Bukkit.getWorld(mundoNome);
            if (world == null) continue;
            for (Cidade cidade : cidadesPorMundo.get(mundoNome).values()) {
                if (cidade.isBordasAtivasPermanente()) {
                    gerarBordasDaCidade(cidade, world);
                }
            }
        }
    }

    public static boolean isChunkClaimed(Chunk chunk) {
        return vinculoChunkCidade.containsKey(chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ());
    }

    public static UUID getChunkOwner(Chunk chunk) {
        String mundo = chunk.getWorld().getName();
        String nomeCidadeBaixo = vinculoChunkCidade.get(mundo + "," + chunk.getX() + "," + chunk.getZ());
        if (nomeCidadeBaixo == null) return null;
        Map<String, Cidade> mapaMundo = cidadesPorMundo.get(mundo);
        if (mapaMundo == null) return null;
        Cidade c = mapaMundo.get(nomeCidadeBaixo);
        return c != null ? c.getDono() : null;
    }

    public static Cidade getCidadePeloNome(String mundo, String nome) {
        Map<String, Cidade> mapaMundo = cidadesPorMundo.get(mundo);
        return mapaMundo != null ? mapaMundo.get(nome.toLowerCase()) : null;
    }

    public static List<Cidade> getCidadesDoJogador(String mundo, UUID uuid) {
        List<Cidade> lista = new ArrayList<>();
        Map<String, Cidade> mapaMundo = cidadesPorMundo.get(mundo);
        if (mapaMundo != null) {
            for (Cidade c : mapaMundo.values()) {
                if (c.getDono().equals(uuid)) lista.add(c);
            }
        }
        return lista;
    }

    public static boolean estaMuitoPertoDeOutraCidade(Chunk chunk, int raio, UUID jogadorAExpansao) {
        String nomeMundo = chunk.getWorld().getName();
        for (int x = -raio; x <= raio; x++) {
            for (int z = -raio; z <= raio; z++) {
                String nomeCidadeBaixo = vinculoChunkCidade.get(nomeMundo + "," + (chunk.getX() + x) + "," + (chunk.getZ() + z));
                if (nomeCidadeBaixo != null) {
                    Map<String, Cidade> mapaMundo = cidadesPorMundo.get(nomeMundo);
                    if (mapaMundo != null) {
                        Cidade proxima = mapaMundo.get(nomeCidadeBaixo);
                        if (proxima != null && !proxima.getDono().equals(jogadorAExpansao)) return true;
                    }
                }
            }
        }
        return false;
    }

    public static void entrarEmModoCriacao(UUID uuid, Chunk chunk) { modoCriacao.put(uuid, chunk); }
    public static boolean estaAguardandoNome(UUID uuid) { return modoCriacao.containsKey(uuid); }
    public static Chunk getChunkPendente(UUID uuid) { return modoCriacao.get(uuid); }
    public static void removerDeModoCriacao(UUID uuid) { modoCriacao.remove(uuid); }
}