package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {

    @Value("${encryption.key}")
    public String encryptionKey;

    @Value("${encryption.iv}")
    public String encryptionIv;

    public EncryptionService() {
    }

    public SecretKey getKeyFromKeyString() {
        if (encryptionKey == null) {
            return new SecretKeySpec("DEC3B7C29464FF50832B16B50313A21F".getBytes(), "AES");
        }
        return new SecretKeySpec(encryptionKey.getBytes(), "AES");
    }

    public IvParameterSpec generateIv() {
        if (encryptionIv == null) {
            return new IvParameterSpec("5422A4DDFA4F399B".getBytes());
        }
        return new IvParameterSpec(encryptionIv.getBytes());
    }

    public String encrypt(String input) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getKeyFromKeyString(), generateIv());
            byte[] cipherText = cipher.doFinal(input.getBytes());
            return Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] addPKCS5Padding(byte[] input, int base) {
        int paddingLength = base - (input.length % base);
        byte[] paddedData = Arrays.copyOf(input, input.length + paddingLength);
        Arrays.fill(paddedData, input.length, paddedData.length, (byte) paddingLength);
        return paddedData;
    }

    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, getKeyFromKeyString(), generateIv());
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(plainText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
