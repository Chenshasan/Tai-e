package pascal.taie.analysis.pta.plugin.spring;

import pascal.taie.analysis.pta.plugin.spring.enums.SpringAnnotation;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * an instance of this class represents a request mapping in Spring MVC
 *
 */
public class RequestMappingEntry {

    private final Annotation sourceAnno;

    private final String url;

    /**
     * one of the following:
     * <ul>
     *     <li>{@link SpringAnnotation#REQUEST_MAPPING}</li>
     *     <li>{@link SpringAnnotation#GET_MAPPING}</li>
     *     <li>{@link SpringAnnotation#POST_MAPPING}</li>
     *     <li>{@link SpringAnnotation#PATCH_MAPPING}</li>
     *     <li>{@link SpringAnnotation#DELETE_MAPPING}</li>
     *     <li>{@link SpringAnnotation#PUT_MAPPING}</li>
     * </ul>
     */
    private final SpringAnnotation mappingType;

    private final JMethod method;

    public RequestMappingEntry(Annotation sourceAnno,
                               SpringAnnotation mappingType,
                               String url,
                               JMethod entryMethod) {
        this.sourceAnno = sourceAnno;
        this.mappingType = mappingType;
        this.url = url;
        this.method = entryMethod;
    }

    public Annotation getSourceAnno() {
        return sourceAnno;
    }

    public String getUrl() {
        return url;
    }

    public SpringAnnotation getMappingType() {
        return mappingType;
    }

    public JMethod getMethod() {
        return method;
    }

    /**
     * return true if client can call server.
     */
    public static Collection<RequestMappingEntry> matches(RequestMappingEntry client,
                                                          String apiUrlPrefix,
                                                          Collection<RequestMappingEntry> endpoints) {
        String url = mergeUrl(apiUrlPrefix, client.getUrl());

        Collection<RequestMappingEntry> results = Sets.newHybridOrderedSet();
        // the tightest match
        for (RequestMappingEntry endpoint : endpoints) {
            if (url.equals(endpoint.getUrl())
                    && (client.getMappingType() == endpoint.getMappingType()
                    || SpringAnnotation.REQUEST_MAPPING == endpoint.getMappingType())
                    && client.getMethod().getSubsignature().equals(endpoint.getMethod().getSubsignature())) {
                results.add(endpoint);
            }
        }
        // other matches
        // the following is a patch for the case: redirected by the gateway will strip the prefix
        for (RequestMappingEntry endpoint : endpoints) {
            if (url.endsWith(endpoint.getUrl())
                    && (client.getMappingType() == endpoint.getMappingType()
                    || SpringAnnotation.REQUEST_MAPPING == endpoint.getMappingType())
                    && client.getMethod().getParamTypes().equals(endpoint.getMethod().getParamTypes())
                    && client.getMethod().getReturnType().equals(endpoint.getMethod().getReturnType())) {
                results.add(endpoint);
            }
        }
        return results;
    }

    public static String mergeUrl(String url1, String url2) {
        return Arrays.stream((url1 + "/" + url2).split("/"))
                     .filter(Predicate.not(String::isEmpty))
                     .collect(Collectors.joining("/", "/", ""));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (SpringAnnotation.REQUEST_MAPPING != mappingType) {
            switch (mappingType) {
                case GET_MAPPING -> sb.append("GET ");
                case POST_MAPPING -> sb.append("POST ");
                case PATCH_MAPPING -> sb.append("PATCH ");
                case DELETE_MAPPING -> sb.append("DELETE ");
                case PUT_MAPPING -> sb.append("PUT ");
            }
        } else {
            sb.append("HTTP ");
        }
        sb.append(url).append(" -> ").append(method.toString());
        return sb.toString();
    }
}
