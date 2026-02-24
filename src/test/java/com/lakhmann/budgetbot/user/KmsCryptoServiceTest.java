package com.lakhmann.budgetbot.user;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class KmsCryptoServiceTest {

    @Test
    void encryptsAndDecryptsText() {
        KmsClient kmsClient = mock(KmsClient.class);

        when(kmsClient.encrypt(any(EncryptRequest.class))).thenReturn(EncryptResponse.builder()
                .ciphertextBlob(SdkBytes.fromUtf8String("cipher"))
                .build());

        when(kmsClient.decrypt(argThat((DecryptRequest req) ->
                req.ciphertextBlob() != null && req.ciphertextBlob().asByteArray().length > 0
        ))).thenReturn(DecryptResponse.builder()
                .plaintext(SdkBytes.fromUtf8String("plain"))
                .build());

        KmsCryptoService service = new KmsCryptoService(kmsClient, "kms-key");

        String encrypted = service.encrypt("plain");

        assertThat(encrypted).isNotBlank();
        assertThat(service.decrypt(encrypted)).isEqualTo("plain");
    }
}
