package com.typeahead.cache;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.zip.CRC32;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConsistentHashRing {

    private final List<CacheNode> logicalNodes;
    private final NavigableMap<Long, CacheNode> ring;

    @Autowired
    public ConsistentHashRing(CacheProperties cacheProperties) {
        this(cacheProperties.logicalNodes(), cacheProperties.virtualNodes());
    }

    ConsistentHashRing(List<String> logicalNodeNames, int virtualNodes) {
        List<String> safeNodeNames = logicalNodeNames == null ? List.of() : logicalNodeNames;
        this.logicalNodes = safeNodeNames.stream()
            .map(CacheNode::new)
            .toList();
        this.ring = new TreeMap<>();

        int safeVirtualNodes = Math.max(1, virtualNodes);
        for (CacheNode node : logicalNodes) {
            for (int index = 0; index < safeVirtualNodes; index++) {
                ring.put(hash(node.name() + "#" + index), node);
            }
        }
    }

    public CacheNode resolveOwner(String cacheKey) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("No logical cache nodes configured");
        }

        long hash = hash(cacheKey);
        var entry = ring.ceilingEntry(hash);
        if (entry != null) {
            return entry.getValue();
        }

        return ring.firstEntry().getValue();
    }

    public List<String> logicalNodeNames() {
        List<String> nodeNames = new ArrayList<>(logicalNodes.size());
        for (CacheNode logicalNode : logicalNodes) {
            nodeNames.add(logicalNode.name());
        }
        return Collections.unmodifiableList(nodeNames);
    }

    long hash(String value) {
        CRC32 crc32 = new CRC32();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }
}
