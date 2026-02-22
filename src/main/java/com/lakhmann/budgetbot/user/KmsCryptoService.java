package com.lakhmann.budgetbot.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class KmsCryptoService {

    private final KmsClient kmsClient;
    private final String keyId;

    public KmsCryptoService(KmsClient kmsClient, @Value("${KMS_KEY_ID:}") String keyId) {
        this.kmsClient = kmsClient;
        this.keyId = keyId;
    }

    public String encrypt(String plainText) {
        var response = kmsClient.encrypt(EncryptRequest.builder()
                .keyId(keyId)
                .plaintext(SdkBytes.fromUtf8String(plainText))
                .build());
        return Base64.getEncoder().encodeToString(response.ciphertextBlob().asByteArray());
    }

    public String decrypt(String encryptedText) {
        byte[] bytes = Base64.getDecoder().decode(encryptedText.getBytes(StandardCharsets.UTF_8));
        var response = kmsClient.decrypt(DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(bytes))
                .build());
        return response.plaintext().asUtf8String();
    }
}
