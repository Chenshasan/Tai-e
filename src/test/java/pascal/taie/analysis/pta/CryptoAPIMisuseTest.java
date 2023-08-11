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

    static final String IV = "staticinitializationvector";
    static final String SEED = "predictableseeds";

    static final String KEYSTORE = "predictablekeystorepassword";

    static final String ITERATION = "pbeiteration";

    static final String RANDOM = "untrustedprng";

    static final String SSLFACTORY = "impropersslsocketfactory";

    static final String CREDENTIAL = "credentialinstring";

    @Test
    public void testPatternMatcher() {
        Tests.testPTA(DIR + "/" + CRYPTO,
                "BrokenCryptoABICase2",
                "crypto-config:src/test/resources/pta/cryptomisuse/brokencrypto/crypto-config.yml");
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
    public void testInSecureAssym() {
        testDirectory(ASSYM, true);
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

    @Test
    public void testIV() {
        testDirectory(IV, true);
    }

    @Test
    public void testSEED() {
        testDirectory(SEED, true);
    }

    @Test
    public void testKeyStore() {
        testDirectory(KEYSTORE, true);
    }

    @Test
    public void testIterationCount() {
        testDirectory(ITERATION, true);
    }

    @Test
    public void testRandom() {
        testDirectory(RANDOM, true);
    }

    @Test
    public void testSSLFactory() {
        testDirectory(SSLFACTORY, false);
    }

    private void testDirectory(String dirName, boolean withConst) {
        File file = new File("src/test/resources/pta/cryptomisuse/" + dirName);
        if (file.isDirectory()) {
            Arrays.stream(Objects.requireNonNull(file.listFiles())).forEach(file1 -> {
                if (file1.getName().contains(".java") || file1.getName().contains(".class")) {
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
        Tests.testPTA(DIR + "/" + SALT, "StaticSaltsABHCase1",
                "propagate-types:[reference,int,byte,char];"
                        + "crypto-config:src/test/resources/pta/cryptomisuse/"
                        + SALT + "/crypto-config.yml");
    }

    @Test
    public void testPredictableCryptographicKey() {
        Tests.testPTA(DIR + "/" + GRAPHIC, "PredictableCryptographicKeyABSCase1",
                "crypto-config:src/test/resources/pta/cryptomisuse/"
                        + GRAPHIC + "/crypto-config.yml");
    }

    @Test
    public void testIVCase() {
        Tests.testPTA(DIR + "/" + IV, "StaticInitializationVectorABHCase2",
                "propagate-types:[reference,int,byte,char];"
                        + "crypto-config:src/test/resources/pta/cryptomisuse/"
                        + IV + "/crypto-config.yml");
    }

    @Test
    public void testSeedCase() {
        Tests.testPTA(DIR + "/" + SEED, "PredictableSeedsABHCase4",
                "propagate-types:[reference,int,byte,char];"
                        + "crypto-config:src/test/resources/pta/cryptomisuse/"
                        + SEED + "/crypto-config.yml");
    }

    @Test
    public void testKeyStoreCase() {
        Tests.testPTA(DIR + "/" + KEYSTORE, "PredictableKeyStorePasswordABHCase2",
                "propagate-types:[reference,int,byte,char];"
                        + "crypto-config:src/test/resources/pta/cryptomisuse/"
                        + KEYSTORE + "/crypto-config.yml");
    }

    @Test
    public void testIteration() {
        Tests.testPTA(DIR + "/" + ITERATION, "LessThan1000IterationPBEABICase2",
                "propagate-types:[reference,int,byte,char];"
                        + "crypto-config:src/test/resources/pta/cryptomisuse/"
                        + ITERATION + "/crypto-config.yml");
    }

    @Test
    public void testCredential() {
        File file = new File("src/test/resources/pta/cryptomisuse/" + CREDENTIAL);
        if (file.isDirectory()) {
            Arrays.stream(Objects.requireNonNull(file.listFiles())).forEach(file1 -> {
                if (file1.getName().contains(".class")) {
                    String name = file1.getName().substring(
                            file1.getName().lastIndexOf("/") + 1);
                    name = name.substring(0, name.lastIndexOf("."));
                    System.out.println(name);
                    if (!name.contains("ABMC") || name.contains("ABMCCase")) {
                        if (!name.contains("Crypto")) {
                            Tests.testPTA(DIR + "/" + CREDENTIAL, name,
                                    "propagate-types:[reference,int,byte,char];"
                                            + "crypto-config:src/test/resources/pta/cryptomisuse/"
                                            + CREDENTIAL + "/crypto-config.yml");
                        }
                    }
                }
            });
        }
    }

}
