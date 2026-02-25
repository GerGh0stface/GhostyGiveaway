package ghostygiveaway.utils;

import ghostygiveaway.GhostyGiveaway;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LanguageManager {

    private final GhostyGiveaway plugin;
    private FileConfiguration langConfig;
    private FileConfiguration fallbackConfig;

    public LanguageManager(GhostyGiveaway plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String lang = plugin.getConfig().getString("language", "en");
        langConfig     = loadLang(lang);
        fallbackConfig = loadLang("en");
    }

    // ── Primary public API: varargs key-value pairs ───────────────────────────

    /** get("key")  – no placeholders */
    public String get(String key) {
        return resolve(key, new HashMap<>());
    }

    /** get("key", "placeholder1", "value1", "placeholder2", "value2", ...) */
    public String get(String key, String... pairs) {
        return resolve(key, toMap(pairs));
    }

    /** get("key", Map.of("placeholder", "value")) */
    public String get(String key, Map<String, String> placeholders) {
        return resolve(key, placeholders != null ? placeholders : new HashMap<>());
    }

    /** getList("key")  – no placeholders */
    public List<String> getList(String key) {
        return resolveList(key, new HashMap<>());
    }

    /** getList("key", "placeholder1", "value1", ...) */
    public List<String> getList(String key, String... pairs) {
        return resolveList(key, toMap(pairs));
    }

    /** getList("key", Map.of("placeholder", "value")) */
    public List<String> getList(String key, Map<String, String> placeholders) {
        return resolveList(key, placeholders != null ? placeholders : new HashMap<>());
    }

    // ── Internal implementation ───────────────────────────────────────────────

    private String resolve(String key, Map<String, String> ph) {
        String raw = getRaw(key);
        for (Map.Entry<String, String> e : ph.entrySet())
            raw = raw.replace("{" + e.getKey() + "}", e.getValue());
        raw = raw.replace("{prefix}", getRaw("prefix"));
        return ColorUtils.colorize(raw);
    }

    private List<String> resolveList(String key, Map<String, String> ph) {
        List<String> raw = langConfig.getStringList(key);
        if (raw.isEmpty()) raw = fallbackConfig.getStringList(key);
        String prefix = getRaw("prefix");
        return raw.stream().map(line -> {
            for (Map.Entry<String, String> e : ph.entrySet())
                line = line.replace("{" + e.getKey() + "}", e.getValue());
            line = line.replace("{prefix}", prefix);
            return ColorUtils.colorize(line);
        }).collect(Collectors.toList());
    }

    private String getRaw(String key) {
        String val = langConfig.getString(key);
        if (val == null || val.isEmpty()) val = fallbackConfig.getString(key);
        return val != null ? val : "&c[Missing: " + key + "]";
    }

    private static Map<String, String> toMap(String[] pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2)
            map.put(pairs[i], pairs[i + 1]);
        return map;
    }

    // ── Lang file loader ──────────────────────────────────────────────────────

    private FileConfiguration loadLang(String lang) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        File langFile = new File(langFolder, lang + ".yml");

        if (!langFile.exists()) {
            InputStream res = plugin.getResource("lang/" + lang + ".yml");
            if (res != null) {
                plugin.saveResource("lang/" + lang + ".yml", false);
            } else {
                res = plugin.getResource("lang/en.yml");
                if (res != null) {
                    plugin.saveResource("lang/en.yml", false);
                    langFile = new File(langFolder, "en.yml");
                } else {
                    return new YamlConfiguration();
                }
            }
        }

        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(langFile);
        InputStream defStream = plugin.getResource("lang/" + lang + ".yml");
        if (defStream == null) defStream = plugin.getResource("lang/en.yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            loaded.setDefaults(defaults);
        }
        return loaded;
    }
}
