package com.school.app.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class LogEncryptor {

    private static final String ALGORITHM = "AES";
    private final SecretKeySpec key;

    public LogEncryptor(@Value("${audit.logs.secret}") String secret) {
        // El secret debe tener 16 caracteres para AES-128
        this.key = new SecretKeySpec(secret.getBytes(), ALGORITHM);
    }

    public String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return "ERROR_ENC";
        }
    }

    public String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted);
        } catch (Exception e) {
            return "ERROR_DECRYPTING: Verifique la llave o el formato.";
        }
    }

}
