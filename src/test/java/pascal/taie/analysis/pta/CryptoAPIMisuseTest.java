package pascal.taie.analysis.pta;

import org.junit.Test;
import pascal.taie.analysis.Tests;
public class CryptoAPIMisuseTest {

    static final String DIR = "cryptomisuse";

    static final String PATTERN = "patternmatcher";

    static final String OTHER = "other";

    static final String ASSYM = "insecureassymcrypto";

    static final String PREDICTABLE = "predictablesource";

    @Test
    public void testPatternMatcher() {
        Tests.testPTA(DIR+"/"+PATTERN, "BrokenCryptoBBCase1",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }
}
