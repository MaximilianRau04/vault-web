package vaultWeb.security;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for AES-GCM encryption and decryption of strings.
 *
 * <p>Provides methods to encrypt plaintext into a Base64-encoded ciphertext along with a randomly
 * generated initialization vector (IV), and to decrypt it back to the original text. Uses AES
 * encryption with Galois/Counter Mode (GCM) for authenticated encryption.
 *
 * <p>The constructor accepts a Base64-encoded secret key used as the master key for encryption and
 * decryption.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EncryptionUtil util = new EncryptionUtil(base64Key);
 * EncryptionUtil.EncryptResult result = util.encrypt("Hello World");
 * String decrypted = util.decrypt(result.cipherTextBase64, result.ivBase64);
 * }</pre>
 */
public class EncryptionUtil {

  private static final String AES = "AES";
  private static final String AES_GCM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;
  private static final int IV_LENGTH_BYTES = 12;
  private static final SecureRandom secureRandom = new SecureRandom();

  private final SecretKey masterKey;

  /**
   * Creates an EncryptionUtil instance with a given Base64-encoded AES key.
   *
   * @param base64Key Base64-encoded secret key used for AES-GCM encryption/decryption
   */
  public EncryptionUtil(String base64Key) {
    byte[] decoded = Base64.getDecoder().decode(base64Key);
    this.masterKey = new SecretKeySpec(decoded, AES);
  }

  /**
   * Encrypts a plaintext string using AES-GCM with a randomly generated IV.
   *
   * @param plaintext the plaintext string to encrypt
   * @return an {@link EncryptResult} containing Base64-encoded ciphertext and IV
   * @throws Exception if encryption fails
   */
  public EncryptResult encrypt(String plaintext) throws Exception {
    byte[] iv = new byte[IV_LENGTH_BYTES];
    secureRandom.nextBytes(iv);

    Cipher cipher = Cipher.getInstance(AES_GCM);
    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
    cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

    byte[] cipherText = cipher.doFinal(plaintext.getBytes());
    return new EncryptResult(
        Base64.getEncoder().encodeToString(cipherText), Base64.getEncoder().encodeToString(iv));
  }

  /**
   * Decrypts a Base64-encoded ciphertext using the provided Base64-encoded IV.
   *
   * @param base64CipherText the Base64-encoded ciphertext
   * @param base64Iv the Base64-encoded initialization vector used during encryption
   * @return the original plaintext string
   * @throws Exception if decryption fails
   */
  public String decrypt(String base64CipherText, String base64Iv) throws Exception {
    byte[] cipherText = Base64.getDecoder().decode(base64CipherText);
    byte[] iv = Base64.getDecoder().decode(base64Iv);

    Cipher cipher = Cipher.getInstance(AES_GCM);
    cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

    return new String(cipher.doFinal(cipherText));
  }

  /**
   * Result object for the encryption operation.
   *
   * <p>Contains the Base64-encoded ciphertext and the Base64-encoded initialization vector (IV)
   * needed for decryption.
   */
  public static class EncryptResult {
    /** Base64-encoded ciphertext */
    public final String cipherTextBase64;

    /** Base64-encoded initialization vector (IV) */
    public final String ivBase64;

    /**
     * Constructs a new EncryptResult with the given ciphertext and IV.
     *
     * @param cipherTextBase64 Base64-encoded ciphertext
     * @param ivBase64 Base64-encoded IV
     */
    public EncryptResult(String cipherTextBase64, String ivBase64) {
      this.cipherTextBase64 = cipherTextBase64;
      this.ivBase64 = ivBase64;
    }
  }
}
