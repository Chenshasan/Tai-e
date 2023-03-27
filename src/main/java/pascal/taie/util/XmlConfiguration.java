package pascal.taie.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import pascal.taie.config.ConfigException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class XmlConfiguration extends AbstractFileBasedConfiguration {

    private static final Logger logger = LogManager.getLogger(XmlConfiguration.class);

    private static final DocumentBuilder builder;

    static {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newDefaultInstance();
            builderFactory.setNamespaceAware(false);
            builderFactory.setValidating(false);
            // does not load external DTDs from the Internet
            builderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private final MultiMap<String, Pair<String, Node>> tag2FilenameNodes
            = Maps.newMultiMap();

    public XmlConfiguration(String classPath) {
        super(classPath);
    }

    @Override
    protected String[] getAcceptableExtensions() {
        return new String[]{
                "xml"
        };
    }

    @Override
    protected void read(String filename,
                        InputStream inputStream) {
        try {
            Document doc = builder.parse(inputStream);
            for (String concernedKey : this.concernedKeys) {
                NodeList nodes = doc.getElementsByTagName(concernedKey);
                for (int i = 0, n = nodes.getLength(); i < n; i++) {
                    Node node = nodes.item(i);
                    tag2FilenameNodes.put(concernedKey, new Pair<>(filename, node));
                }
            }
        } catch (IOException | SAXException e) {
            logger.error("", e);
            throw new ConfigException("Failed to read xml configuration file", e);
        }
    }

    @Nonnull
    public Collection<Node> getNodesByTag(String tagName) {
        return tag2FilenameNodes.get(tagName)
                                .stream()
                                .map(Pair::second)
                                .toList();
    }

}
