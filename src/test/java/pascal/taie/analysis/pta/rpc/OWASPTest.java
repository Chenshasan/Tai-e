package pascal.taie.analysis.pta.rpc;

import org.junit.Test;
import pascal.taie.analysis.Tests;

public class OWASPTest {

    @Test
    public void OWASP() {
        Tests.testPTAInOWASPProgramOfCrypto(Benchmark.OWASP, true);
    }


}
