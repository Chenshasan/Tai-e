package pascal.taie.analysis.pta;

import org.junit.Test;
import pascal.taie.analysis.Tests;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public class CryptoAPIMisuseTest {

    static final String DIR = "cryptomisuse";

    static final String CRYPTO = "brokencrypto";

    static final String HASH = "brokenhash";

    static final String MAC = "brokenmac";

    static final String ECBCRYPTO = "ecbcrypto";

    static final String HTTP = "http";

    static final String PATTERN = "patternmatcher";

    static final String SALT = "staticsalts";

    static final String GRAPHIC = "predictablecryptographickey";


    static final String OTHER = "other";

    static final String ASSYM = "insecureassymcrypto";

    static final String PREDICTABLE = "predictablesource";

    @Test
    public void testPatternMatcher() {
        Tests.testPTA(DIR + "/" + PATTERN,
                "BrokenCryptoBBCase1",
                "crypto-config:src/test/resources/pta/cryptomisuse/patternmatcher/crypto-config.yml");
    }

    @Test
    public void testPredictableSource() {
        Tests.testPTA(DIR + "/" + OTHER,
                "ImproperSocketManualHostBBCase1",
                "crypto-config:src/test/resources/pta/cryptomisuse/other/crypto-config.yml");
    }

    @Test
    public void testNumberSize() {
        Tests.testPTA(DIR + "/" + PREDICTABLE,
                "StaticSaltsBBCase1",
                "crypto-config:src/test/resources/pta/cryptomisuse/predictablesource/crypto-config.yml");
    }

    @Test
    public void testCompositeRule() {
        Tests.testPTA(DIR + "/" + ASSYM,
                "InsecureAsymmetricCipherBBCase1",
                "propagate-types:[reference,int];"
                        + "crypto-config:src/test/resources/pta/cryptomisuse/insecureassymcrypto/crypto-config.yml");
    }

    @Test
    public void testBrokenCrypto() {
        testDirectory(CRYPTO, false);
    }

    @Test
    public void testBrokenHash() {
        testDirectory(HASH, false);
    }

    @Test
    public void testBrokenMac() {
        testDirectory(MAC, false);
    }

    @Test
    public void testBrokenHttp() {
        testDirectory(HTTP, false);
    }

    @Test
    public void testSalt() {
        testDirectory(SALT, true);
    }

    @Test
    public void testEcbCrypto() {
        testDirectory(ECBCRYPTO, false);
    }

    @Test
    public void testGraphicKey() {
        testDirectory(GRAPHIC, true);
    }

    private void testDirectory(String dirName, boolean withConst) {
        File file = new File("src/test/resources/pta/cryptomisuse/" + dirName);
        if (file.isDirectory()) {
            Arrays.stream(Objects.requireNonNull(file.listFiles())).forEach(file1 -> {
                if (file1.getName().contains(".java")) {
                    String name = file1.getName().substring(
                            file1.getName().lastIndexOf("/") + 1);
                    name = name.substring(0, name.lastIndexOf("."));
                    System.out.println(name);
                    if (!name.contains("ABMC") || name.contains("ABMCCase")) {
                        if (withConst) {
                            Tests.testPTA(DIR + "/" + dirName, name,
                                    "propagate-types:[reference,int,byte,char];"
                                            + "crypto-config:src/test/resources/pta/cryptomisuse/"
                                            + dirName + "/crypto-config.yml");
                        } else {
                            Tests.testPTA(DIR + "/" + dirName, name,
                                    "crypto-config:src/test/resources/pta/cryptomisuse/"
                                            + dirName + "/crypto-config.yml");
                        }
                    }
                }
            });
        }
    }

    @Test
    public void testPredictableCharArray() {
        Tests.testPTA(DIR + "/" + SALT, "StaticSaltsABICase2",
                "propagate-types:[reference,int,byte,char];"
                        + "crypto-config:src/test/resources/pta/cryptomisuse/"
                        + SALT + "/crypto-config.yml");
    }

    @Test
    public void testPredictableCryptographicKey() {
        Tests.testPTA(DIR + "/" + GRAPHIC, "PredictableCryptographicKeyBBCase1",
                "crypto-config:src/test/resources/pta/cryptomisuse/"
                        + GRAPHIC + "/crypto-config.yml");
    }
}
