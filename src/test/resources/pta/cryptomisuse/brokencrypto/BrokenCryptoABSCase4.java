
import javax.crypto.*;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class BrokenCryptoABSCase4 {
    Crypto5 crypto;
    public BrokenCryptoABSCase4() throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, UnsupportedEncodingException {
        crypto = new Crypto5("RC2");
        crypto.encrypt("abc","");
    }

    public static void main(String args[]) throws Exception{
        BrokenCryptoABSCase4 absCase1=new BrokenCryptoABSCase4();
    }
}


class Crypto5 {
    Cipher cipher;
    String defaultAlgo;
    public Crypto5(String defAlgo) throws NoSuchPaddingException, NoSuchAlgorithmException {
        defaultAlgo = defAlgo;
    }

    public byte[] encrypt(String txt, String passedAlgo) throws UnsupportedEncodingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException {
        if(passedAlgo.isEmpty()){
            passedAlgo = defaultAlgo;
        }

        KeyGenerator keyGen = KeyGenerator.getInstance(defaultAlgo);
        SecretKey key = keyGen.generateKey();
        Cipher cipher = Cipher.getInstance(defaultAlgo);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte [] txtBytes = txt.getBytes();
        return cipher.doFinal(txtBytes);
    }
}
