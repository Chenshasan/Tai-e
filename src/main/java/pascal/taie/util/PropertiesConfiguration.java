package pascal.taie.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Properties;

public class PropertiesConfiguration extends AbstractFileBasedConfiguration {

    private static final Logger logger = LogManager.getLogger(PropertiesConfiguration.class);

    private final MultiMap<Object, Object> key2Properties = Maps.newMultiMap();

    public PropertiesConfiguration(String classPath) {
        super(classPath);
    }

    @Override
    protected String[] getAcceptableExtensions() {
        return new String[] {
            ".properties",
        };
    }

    @Override
    protected void read(String filename, InputStream inputStream) {
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error("read properties error", e);
        }
        // TODO: handle the concerned keys
        properties.forEach(key2Properties::put);
    }

    /**
     * TODO: handle multi-value properties error
     */
    @Nullable
    public String getProperty(String key) {
        try {
            return (String) key2Properties.get(key).iterator().next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
