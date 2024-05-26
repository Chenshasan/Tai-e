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
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule.CompositeRule;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule.FromSource;
import pascal.taie.analysis.pta.plugin.cryptomisuse.compositerule.ToSource;
import pascal.taie.analysis.pta.plugin.cryptomisuse.rule.*;
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

public record CryptoAPIMisuseConfig(Set<CryptoSource> sources,
                                    Set<CryptoObjPropagate> propagates,
                                    Set<PatternMatchRule> patternMatchRules,
                                    Set<PredictableSourceRule> predictableSourceRules,
                                    Set<NumberSizeRule> numberSizeRules,
                                    Set<ForbiddenMethodRule> forbiddenMethodRules,
                                    Set<InfluencingFactorRule> influencingFactorRules,
                                    Set<CompositeRule> compositeRules) {
    private static final Logger logger = LogManager.getLogger(CryptoAPIMisuseConfig.class);

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
            Set<CryptoSource> sources =
                    deserializeSources(node.get("cryptoSources"));
            Set<CryptoObjPropagate> propagates =
                    deserializePropagates(node.get("cryptoObjPropagate"));
            Set<PatternMatchRule> patternMatchRules =
                    deserializePatternMatchRules(node.get("patternMatchRules"));
            Set<PredictableSourceRule> predictableSourceRules =
                    deserializePredictableSourceRules(node.get("predictableSourceRules"));
            Set<NumberSizeRule> numberSizeRules =
                    deserializeNumberSizeRules(node.get("numberSizeRules"));
            Set<ForbiddenMethodRule> forbiddenMethodRules =
                    deserializeForbiddenMethodRules(node.get("forbiddenMethodRules"));
            Set<InfluencingFactorRule> influencingFactorRules =
                    deserializeInfluencingFactorRules(node.get("influencingFactorRules"));
            Set<CompositeRule> compositeRules =
                    deserializeCompositeRules(node.get("compositeRules"));
            return new CryptoAPIMisuseConfig(
                    sources,
                    propagates,
                    patternMatchRules,
                    predictableSourceRules,
                    numberSizeRules,
                    forbiddenMethodRules,
                    influencingFactorRules,
                    compositeRules);
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
                        int index = IndexUtils.toInt(
                                elem.get("index").asText());
                        sources.add(new CryptoSource(method, type, index));
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
         * to a set of {@link PatternMatchRule}.
         *
         * @param node the node to be deserialized
         * @return set of deserialized {@link PatternMatchRule}
         */
        private Set<PatternMatchRule> deserializePatternMatchRules(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                Set<PatternMatchRule> patternMatchRules = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        int index = elem.get("index").asInt();
                        String pattern = elem.get("pattern").asText();
                        patternMatchRules.add(new PatternMatchRule(method, index, pattern));
                    } else {
                        logger.warn("Cannot find cryptoAPI method '{}'", methodSig);
                    }
                }
                return Collections.unmodifiableSet(patternMatchRules);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }

        private Set<PredictableSourceRule> deserializePredictableSourceRules(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                Set<PredictableSourceRule> predictableSourceRules = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        int index = IndexUtils.toInt(elem.get("index").asText());
                        predictableSourceRules.add(new PredictableSourceRule(method, index));
                    } else {
                        logger.warn("Cannot find cryptoAPI method '{}'", methodSig);
                    }
                }
                return Collections.unmodifiableSet(predictableSourceRules);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }

        private Set<NumberSizeRule> deserializeNumberSizeRules(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                Set<NumberSizeRule> numberSizeRules = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        // if the method (given in config file) is absent in
                        // the class hierarchy, just ignore it.
                        int min = elem.get("min").asInt();
                        int max = elem.get("max").asInt();
                        int index = IndexUtils.toInt(elem.get("index").asText());
                        numberSizeRules.add(new NumberSizeRule(method, index, min, max));
                    } else {
                        logger.warn("Cannot find cryptoAPI method '{}'", methodSig);
                    }
                }
                return Collections.unmodifiableSet(numberSizeRules);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }

        private Set<ForbiddenMethodRule> deserializeForbiddenMethodRules(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                Set<ForbiddenMethodRule> forbiddenMethodRules = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    if (method != null) {
                        forbiddenMethodRules.add(new ForbiddenMethodRule(method));
                    } else {
                        logger.warn("Cannot find cryptoAPI method '{}'", methodSig);
                    }
                }
                return Collections.unmodifiableSet(forbiddenMethodRules);
            } else {
                // if node is not an instance of ArrayNode, just return an empty set.
                return Set.of();
            }
        }

        private Set<InfluencingFactorRule> deserializeInfluencingFactorRules(JsonNode node) {
            if (node instanceof ArrayNode arrayNode) {
                Set<InfluencingFactorRule> influencingFactorRules = Sets.newSet();
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    int index = IndexUtils.toInt(elem.get("index").asText());
                    hierarchy.allClasses().forEach(jClass -> {
                        if (jClass.isApplication()) {
                            jClass.getDeclaredMethods().forEach(jMethod -> {
                                if (jMethod.getSignature().contains(methodSig)) {
                                    influencingFactorRules.add(new InfluencingFactorRule(jMethod, index));
                                }
                            });
                        }
                    });
                }
                return Collections.unmodifiableSet(influencingFactorRules);
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

        private Set<CompositeRule> deserializeCompositeRules(JsonNode node) {

            if (node instanceof ArrayNode arrayNode) {
                Set<CompositeRule> compositeRules = Sets.newSet(arrayNode.size());
                for (JsonNode compositeNode : node) {
                    compositeNode = compositeNode.get("compositeRule");
                    FromSource fromSource =
                            deserializeFromSource(compositeNode.get("fromSource"));
                    Set<ToSource> toSources =
                            deserializeToSources(compositeNode.get("toSources"));
                    Set<CryptoObjPropagate> propagates =
                            deserializePropagates(compositeNode.get("propagates"));
                    if (fromSource != null) {
                        compositeRules.add(new CompositeRule(fromSource, toSources, propagates));
                    }
                }
                return compositeRules;
            }
            return Set.of();
        }

        private FromSource deserializeFromSource(JsonNode node) {
            String fromMethodSig = node.get("method").asText();
            JMethod fromMethod = hierarchy.getMethod(fromMethodSig);
            if (fromMethod != null) {
                int fromIndex = IndexUtils.toInt(node.get("index").asText());
                Type type = typeSystem.getType(
                        node.get("type").asText());
                System.out.println("add from source of method: " + fromMethodSig + "with source obj type of" + type);
                return new FromSource(fromMethod, fromIndex, type);
            } else {
                logger.warn("Cannot find from-source method '{}'", fromMethodSig);
                return null;
            }
        }

        private Set<ToSource> deserializeToSources(JsonNode node) {
            Set<ToSource> toSources = Set.of();
            if (node instanceof ArrayNode arrayNode) {
                toSources = Sets.newSet(arrayNode.size());
                for (JsonNode elem : arrayNode) {
                    String methodSig = elem.get("method").asText();
                    JMethod method = hierarchy.getMethod(methodSig);
                    int toIndex = IndexUtils.toInt(elem.get("index").asText());
                    if (method == null) {
                        logger.warn("Cannot find to source method '{}'", methodSig);
                    }
                    String ruleType = elem.get("ruleType").asText();
                    int index = elem.get("ruleIndex").asInt();
                    Rule rule = null;
                    switch (ruleType) {
                        case "PatternMatch":
                            String pattern = elem.get("pattern").asText();
                            rule = new PatternMatchRule(method, index, pattern);
                            break;
                        case "NumberSize":
                            int min = elem.get("min").asInt();
                            int max = elem.get("max").asInt();
                            rule = new NumberSizeRule(method, index, min, max);
                            break;
                        case "PredictableSource":
                            rule = new PredictableSourceRule(method, index);
                            break;
                        default:
                            logger.warn("Cannot find the legal rule type");
                    }
                    if (method != null && rule != null) {
                        toSources.add(new ToSource(method, toIndex, rule));
                        System.out.println("add to source of method: " + methodSig + "with rule type of " + ruleType);
                    }
                }
            }
            return toSources;
        }
    }
}
