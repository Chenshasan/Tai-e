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
                "crypto-config:src/test/resources/pta/cryptomisuse/patternmatcher/crypto-config.yml");
    }

    @Test
    public void testPredictableSource() {
        Tests.testPTA(DIR+"/"+OTHER, "ImproperSocketManualHostBBCase1",
                "crypto-config:src/test/resources/pta/cryptomisuse/other/crypto-config.yml");
    }

    @Test
    public void testNumberSize() {
        Tests.testPTA(DIR+"/"+PREDICTABLE, "StaticSaltsBBCase1",
                "crypto-config:src/test/resources/pta/cryptomisuse/predictablesource/crypto-config.yml");
    }
    @Test
    public void testCompositeRule() {
        Tests.testPTA(DIR+"/"+ASSYM, "InsecureAsymmetricCipherBBCase1",
                "crypto-config:src/test/resources/pta/cryptomisuse/insecureassymcrypto/crypto-config.yml");
    }
}
