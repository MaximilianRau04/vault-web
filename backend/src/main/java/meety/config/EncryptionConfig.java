package meety.config;

import meety.security.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Value("${encryption.master-key}")
    private String masterKey;

    @Bean
    public EncryptionUtil encryptionUtil() {
        return new EncryptionUtil(masterKey);
    }
}