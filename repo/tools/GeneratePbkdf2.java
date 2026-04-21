import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class GeneratePbkdf2 {
  private static final String PREFIX = "pbkdf2_sha256";
  private static final int ITERATIONS = 310_000;
  private static final int SALT_BYTES = 16;
  private static final int KEY_BITS = 256;

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: java GeneratePbkdf2 <password>");
      System.exit(2);
    }

    byte[] salt = new byte[SALT_BYTES];
    new SecureRandom().nextBytes(salt);

    byte[] hash = pbkdf2(args[0].toCharArray(), salt, ITERATIONS, KEY_BITS);

    System.out.println(PREFIX
        + "$" + ITERATIONS
        + "$" + Base64.getEncoder().encodeToString(salt)
        + "$" + Base64.getEncoder().encodeToString(hash));
  }

  private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) throws Exception {
    PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    return skf.generateSecret(spec).getEncoded();
  }
}

