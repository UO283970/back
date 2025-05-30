package tfg.books.back.model.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class PassEncryption {

    @Value("${app.decryption-vector}")
    private String init_vector;

    @Value("${app.decryption-key}")
    private String secretKey;

    public String encrypt(String value) {
        IvParameterSpec iv = new IvParameterSpec(init_vector.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec secretkeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

        Cipher cipher = null;
        byte[] encrypted;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretkeySpec, iv);
            encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException |
                 InvalidAlgorithmParameterException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }


        return Base64.getEncoder().encodeToString(encrypted);
    }
}
