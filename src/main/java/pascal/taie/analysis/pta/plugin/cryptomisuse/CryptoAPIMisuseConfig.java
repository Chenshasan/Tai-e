package pascal.taie.analysis.pta.plugin.cryptomisuse;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.config.ConfigException;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Sets;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class CryptoAPIMisuseConfig {
    private static final Logger logger = LogManager.getLogger(CryptoAPIMisuseConfig.class);

    /**
     * Set of sources.
     */
    private final Set<CryptoSource> sources;

    /**
     * Set of CryptoAPIs.
     */
    private final Set<CryptoAPI> cryptoAPIS;

    /**
     * Set of taint propagates;
     */
    private final Set<CryptoObjPropagate> propagates;

    private CryptoAPIMisuseConfig(Set<CryptoSource> sources, Set<CryptoAPI> cryptoAPIS,
                        Set<CryptoObjPropagate> propagates) {
        this.sources = sources;
        this.cryptoAPIS = cryptoAPIS;
        this.propagates = propagates;
    }

    /**
     * Reads a taint analysis configuration from file
     *
     * @param path       the path to the config file
     * @param hierarchy  the class hierarchy
     * @param typeSystem the type manager
     * @return the CryptoAPIMisuseConfig object
     * @throws ConfigException if failed to load the config file
     */
    static CryptoAPIMisuseConfig readConfig(
            String path, ClassHierarchy hierarchy, TypeSystem typeSystem) {
        File file = new File(path);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(CryptoAPIMisuseConfig.class,
                new CryptoAPIMisuseConfig.Deserializer(hierarchy, typeSystem));
        mapper.registerModule(module);
        try {
            return mapper.readValue(file, CryptoAPIMisuseConfig.class);
        } catch (IOException e) {
            throw new ConfigException("Failed to read taint analysis config file " + file, e);
        }
    }

    /**
     * @return sources in the configuration.
     */
    Set<CryptoSource> getSources() {
        return sources;
    }

    /**
     * @return CryptoAPIs in the configuration.
     */
    Set<CryptoAPI> getCryptoAPIs() {
        return cryptoAPIS;
    }

    /**
     * @return taint propagates in the configuration.
     */
    Set<CryptoObjPropagate> getPropagates() {
        return propagates;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CryptoAPIMisuseConfig:");
        if (!sources.isEmpty()) {
            sb.append("\nsources:\n");
            sources.forEach(source ->
                    sb.append("  ").append(source).append("\n"));
        }
        if (!cryptoAPIS.isEmpty()) {
            sb.append("\nCryptoAPIs:\n");
            cryptoAPIS.forEach(cryptoAPI ->
                    sb.append("  ").append(cryptoAPI).append("\n"));
        }
        if (!propagates.isEmpty()) {
            sb.append("\npropagates:\n");
            propagates.forEach(propagate ->
                    sb.append("  ").append(propagate).append("\n"));
        }
        return sb.toString();
    }

    /**
     * Deserializer for {@link CryptoAPIMisuseConfig}.
     */
    private static class Deserializer extends JsonDeserializer<CryptoAPIMisuseConfig> {

        private final ClassHierarchy hierarchy;

        private final TypeSystem typeSystem;

        private Deserializer(ClassHierarchy hierarchy, TypeSystem typeSystem) {
            this.hierarchy = hierarchy;
            this.typeSystem = typeSystem;
        }

        @Override
        public CryptoAPIMisuseConfig deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);
            Set<CryptoSource> sources = deserializeSources(node.get("cryptoSources"));
            Set<CryptoAPI> CryptoAPIs = deserializeCryptoAPIs(node.get("cryptoAPIs"));
            Set<CryptoObjPropagate> propagates = deserializePropagates(node.get("cryptoObjPropagate"));
            return new CryptoAPIMisuseConfig(sources, CryptoAPIs, propagates);
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a set of {@link CryptoSource}.
         *
         * @param node the node to be deserialized
         * @return set of deserialized {@link CryptoSource}
         */
        private Set<CryptoSource> deserializeSources(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                Set<CryptoSource> sources = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        Type type = typeSystem.getType(
                                elem.get("type").asText());
                        sources.add(new CryptoSource(method, type));
                    } else {
                        logger.warn("Cannot find source method '{}'", methodSig);
                    }
                }
                return Collections.unmodifiableSet(sources);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a set of {@link CryptoAPI}.
         *
         * @param node the node to be deserialized
         * @return set of deserialized {@link CryptoAPI}
         */
        private Set<CryptoAPI> deserializeCryptoAPIs(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                Set<CryptoAPI> CryptoAPIs = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        int index = elem.get("index").asInt();
                        CryptoAPIs.add(new CryptoAPI(method, index));
                    } else {
                        logger.warn("Cannot find cryptoAPI method '{}'", methodSig);
                    }
                }
                return Collections.unmodifiableSet(CryptoAPIs);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }

        /**
         * Deserializes a {@link JsonNode} (assume it is an {@link ArrayNode})
         * to a set of {@link CryptoObjPropagate}.
         *
         * @param node the node to be deserialized
         * @return set of deserialized {@link CryptoObjPropagate}
         */
        private Set<CryptoObjPropagate> deserializePropagates(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                Set<CryptoObjPropagate> propagates = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        int from = CryptoObjPropagate.toInt(elem.get("from").asText());
                        int to = CryptoObjPropagate.toInt(elem.get("to").asText());
                        Type type = typeSystem.getType(
                                elem.get("type").asText());
                        propagates.add(new CryptoObjPropagate(method, from, to, type));
                    } else {
                        logger.warn("Cannot find crypto-propagate method '{}'", methodSig);
                    }
                }
                return Collections.unmodifiableSet(propagates);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }
    }
}
