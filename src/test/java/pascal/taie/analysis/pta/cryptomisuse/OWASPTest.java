
package pascal.taie.analysis.pta.cryptomisuse;

import org.junit.jupiter.api.Test;
import pascal.taie.analysis.Tests;

public class OWASPTest {

    @Test
    public void OWASP() {
        Tests.testPTAInOWASPProgramOfCrypto(Benchmark.OWASP, true);
    }

}
