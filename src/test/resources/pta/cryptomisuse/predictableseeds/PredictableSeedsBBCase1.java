

import java.security.SecureRandom;

public class PredictableSeedsBBCase1 {

    public static void main (String [] args){
        SecureRandom sr = new SecureRandom();
        byte [] keyBytes = {(byte) 100, (byte) 200};
        sr.setSeed(keyBytes);
        //sr.setSeed(456789L); // Noncompliant
        int v = sr.nextInt();
        System.out.println(v);
    }
}
