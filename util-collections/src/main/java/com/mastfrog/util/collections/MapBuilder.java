package com.mastfrog.util.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A map builder with a fluent API, ability to convert to ordered Properties and ability
 * to compute a hash using a message digest.
 *
 * @author Tim Boudreau
 * @deprecated Use CollectionUtils.map()
 */
@Deprecated
public final class MapBuilder {

    final Map<String, Object> data = new LinkedHashMap<>();
    private final MessageDigest digest;

    public MapBuilder(MessageDigest digest) {
        this.digest = digest;
    }

    public MapBuilder() {
        this(null);
    }

    public String hash() {
        return digest == null ? null : 
                Base64.getEncoder().encodeToString(digest.digest());
    }

    public MapBuilder subMap(String name) {
        MapBuilder nue = new MapBuilder(digest);
        data.put(name, nue.data);
        if (digest != null) {
            digest.update(name.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '!');
        }
        return nue;
    }

    public MapBuilder put(String name, Object type) {
        data.put(name, type);
        if (digest != null) {
            digest.update(name.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '=');
            digest.update(("" + type).getBytes(StandardCharsets.UTF_8));
        }
        return this;
    }

    public Properties toProperties() {
        Properties props = new Properties();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (e.getValue() instanceof String) {
                props.setProperty(e.getKey(), e.getValue().toString());
            }
        }
        return props;
    }
    
    public Map<String,Object> build() {
        return new LinkedHashMap<>(data);
    }
}
