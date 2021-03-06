package com.github.jmkgreen.morphia.mapping.cache;

import com.github.jmkgreen.morphia.Key;
import com.github.jmkgreen.morphia.logging.Logr;
import com.github.jmkgreen.morphia.logging.MorphiaLoggerFactory;
import com.github.jmkgreen.morphia.mapping.lazy.LazyFeatureDependencies;
import com.github.jmkgreen.morphia.mapping.lazy.proxy.ProxyHelper;
import java.util.HashMap;
import java.util.Map;
import relocated.morphia.org.apache.commons.collections.ReferenceMap;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultEntityCache implements EntityCache {

    private static final Logr log = MorphiaLoggerFactory.get(DefaultEntityCache.class);

    private final Map<Key, Object> entityMap = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
    private final Map<Key, Object> proxyMap = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);
    private final Map<Key, Boolean> existenceMap = new HashMap<Key, Boolean>();
    private final EntityCacheStatistics stats = new EntityCacheStatistics();

    public Boolean exists(Key<?> k) {
        if (entityMap.containsKey(k)) {
            stats.hits++;
            return true;
        }

        Boolean b = existenceMap.get(k);
        if (b == null) {
            stats.misses++;
        } else {
            stats.hits++;
        }
        return b;
    }

    public void notifyExists(Key<?> k, boolean exists) {
        if (existenceMap.put(k, exists) != null) {
            stats.entities++;
        }
    }

    public <T> T getEntity(Key<T> k) {
        Object o = entityMap.get(k);
        if (o == null) {
            if (LazyFeatureDependencies.testDependencyFullFilled()) {
                Object proxy = proxyMap.get(k);
                if (proxy != null) {
                    ProxyHelper.isFetched(proxy);
                    stats.hits++;
                    return (T) ProxyHelper.unwrap(proxy);
                }
            }
            // System.out.println("miss entity " + k + ":" + this);
            stats.misses++;
        } else {
            stats.hits++;
        }
        return (T) o;
    }

    public <T> T getProxy(Key<T> k) {
        Object o = proxyMap.get(k);
        if (o == null) {
            // System.out.println("miss proxy " + k);
            stats.misses++;
        } else {
            stats.hits++;
        }
        return (T) o;
    }

    public <T> void putProxy(Key<T> k, T t) {
        proxyMap.put(k, t);
        stats.entities++;

    }

    public <T> void putEntity(Key<T> k, T t) {
        notifyExists(k, true); // already registers a write
        entityMap.put(k, t);
    }

    public void flush() {
        entityMap.clear();
        existenceMap.clear();
        proxyMap.clear();
        stats.reset();
    }

    public EntityCacheStatistics stats() {
        return stats.copy();
    }

}
