
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BrokenHashABMC3 {
    public void go(String str, String crypto) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(crypto);
        md.update(str.getBytes());
        System.out.println(md.digest());
    }
}
