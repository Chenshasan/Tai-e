package pascal.taie.util;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class YamlConfiguration extends AbstractFileBasedConfiguration {

    private static final Logger logger = LogManager.getLogger(YamlConfiguration.class);

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final TwoKeyMap<String, String, JsonNode> key2Filename2Node = Maps.newTwoKeyMap();

    public YamlConfiguration(String classPath) {
        super(classPath);
    }

    @Override
    protected String[] getAcceptableExtensions() {
        return new String[] {
            ".yaml", "yml"
        };
    }

    @Override
    protected void read(String filename, InputStream inputStream) {
        try {
            JsonNode rootNode = mapper.readTree(inputStream);
            dfsNode(new Stack<>(), filename, rootNode);
        } catch (IOException e) {
            logger.error("Failed to read yaml configuration file", e);
        }
    }

    private void dfsNode(Stack<String> prefix,
                         String filename,
                         JsonNode jsonNode) {
        if (!prefix.isEmpty()) {
            key2Filename2Node.put(String.join(".", prefix), filename, jsonNode);
        }
        for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String key = entry.getKey();
            JsonNode child = entry.getValue();
            prefix.push(key);
            dfsNode(prefix, filename, child);
            prefix.pop();
        }
    }

    @Nullable
    public String getString(String key) {
        return Optional.ofNullable(getOneNode(key))
                       .map(JsonNode::textValue)
                       .orElse(null);
    }

    @Nullable
    public Integer getInt(String key) {
        return Optional.ofNullable(getOneNode(key))
                       .map(JsonNode::intValue)
                       .orElse(null);
    }

    @Nullable
    private JsonNode getOneNode(String key) {
        Map<String, JsonNode> filename2Node = key2Filename2Node.get(key);
        if (filename2Node == null || filename2Node.size() != 1) {
            logger.warn("'{}' property not found", key);
        }
        return Optional.ofNullable(filename2Node)
                       .map(map -> map.values().iterator().next())
                       .map(this::resolvePlaceholders)
                       .orElse(null);
    }

    @Nullable
    private JsonNode resolvePlaceholders(@Nonnull JsonNode jsonNode) {
        try {
            String value = jsonNode.asText();
            // TODO: @see org.springframework.core.env.AbstractPropertyResolver#resolveNestedPlaceholders
            if (value.matches("^\\$\\{.*\\}$")) {
                return getOneNode(value.substring(2, value.length() - 1));
            }
            return jsonNode;
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "YamlConfiguration{" +
            "classPath='" + classPath + '\'' +
            ", key2Filename2Node=" + key2Filename2Node +
            '}';
    }
}
