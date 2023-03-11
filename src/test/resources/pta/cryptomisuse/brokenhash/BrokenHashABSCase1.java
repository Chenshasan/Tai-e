
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BrokenHashABSCase1 {
    CryptoHash1 crypto;
    public BrokenHashABSCase1() throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, UnsupportedEncodingException {
        crypto = new CryptoHash1("SHA1");
        crypto.encrypt("abc","");
    }


    public static void main(String args[]) throws Exception{
        BrokenHashABSCase1 absCase1=new BrokenHashABSCase1();
    }
}

class CryptoHash1 {
    String crypto;

    public CryptoHash1(String defCrypto) throws NoSuchPaddingException, NoSuchAlgorithmException {
        crypto = defCrypto;
    }

    public void encrypt(String str, String passedAlgo) throws UnsupportedEncodingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException {
        if(passedAlgo.isEmpty()){
            passedAlgo = crypto;
        }

        MessageDigest md = MessageDigest.getInstance(crypto);
        md.update(str.getBytes());
        System.out.println(md.digest());

    }
}
