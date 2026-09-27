package net.daoke.openrestyplus.httpconfig;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Built-in editable HTTP default pages. */
public final class HttpDefaultPages {
    public static final Map<String, String> DEFAULTS = loadDefaults();
    private HttpDefaultPages() { }
    public static Map<String, String> merge(Map<String, String> configured) {
        var result = new LinkedHashMap<>(DEFAULTS);
        if (configured != null) configured.forEach((key, value) -> {
            if (DEFAULTS.containsKey(key) && value != null) result.put(key, value);
        });
        return Map.copyOf(result);
    }
    private static Map<String, String> loadDefaults() {
        var result = new LinkedHashMap<String, String>();
        for (var name : new String[]{"404.html", "site-not-found.html", "index.html", "index.php", "site-suspended.html"}) {
            try (InputStream stream = HttpDefaultPages.class.getResourceAsStream("/default-pages/" + name)) {
                if (stream == null) throw new IllegalStateException("Missing built-in default page: " + name);
                result.put(name, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception exception) { throw new IllegalStateException("Cannot read built-in default page: " + name, exception); }
        }
        return Map.copyOf(result);
    }
}
