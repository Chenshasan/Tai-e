import javax.crypto.spec.PBEParameterSpec;
import java.util.Random;

public class UntrustedPRNGCase1 {
    public static void main(String [] args)
    {
        Random random = new Random();
        PBEParameterSpec pbeParamSpec = null;
        byte[] salt = new byte[32];
        random.nextBytes(salt);
        int count = 10020;
        pbeParamSpec = new PBEParameterSpec(salt, count);
    }



}
