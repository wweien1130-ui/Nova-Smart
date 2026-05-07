package com.liang.agent.config;

import com.hmall.common.config.JwtProperties;
import com.hmall.common.utils.JwtTool;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;

import java.security.KeyPair;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public KeyPair keyPair(JwtProperties jwtProperties) {
        KeyStoreKeyFactory keyFactory = new KeyStoreKeyFactory(
                jwtProperties.getLocation(),
                jwtProperties.getPassword().toCharArray()
        );
        return keyFactory.getKeyPair(
                jwtProperties.getAlias(),
                jwtProperties.getPassword().toCharArray()
        );
    }

    @Bean
    public JwtTool jwtTool(KeyPair keyPair) {
        return new JwtTool(keyPair);
    }
}