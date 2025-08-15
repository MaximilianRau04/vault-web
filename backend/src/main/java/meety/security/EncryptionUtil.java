package meety.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionUtil {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final SecureRandom secureRandom = new SecureRandom();

    private final SecretKey masterKey;

    public EncryptionUtil(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        this.masterKey = new SecretKeySpec(decoded, AES);
    }

    public EncryptResult encrypt(String plaintext) throws Exception {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

        byte[] cipherText = cipher.doFinal(plaintext.getBytes());
        return new EncryptResult(
                Base64.getEncoder().encodeToString(cipherText),
                Base64.getEncoder().encodeToString(iv)
        );
    }

    public String decrypt(String base64CipherText, String base64Iv) throws Exception {
        byte[] cipherText = Base64.getDecoder().decode(base64CipherText);
        byte[] iv = Base64.getDecoder().decode(base64Iv);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        return new String(cipher.doFinal(cipherText));
    }

    public static class EncryptResult {
        public final String cipherTextBase64;
        public final String ivBase64;

        public EncryptResult(String cipherTextBase64, String ivBase64) {
            this.cipherTextBase64 = cipherTextBase64;
            this.ivBase64 = ivBase64;
        }
    }
}
