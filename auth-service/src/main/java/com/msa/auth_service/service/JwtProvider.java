package com.msa.auth_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

@Component
public class JwtProvider {

    private final long validityInMilliseconds = 3600000; // 1h
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private String keyId;

    @org.springframework.beans.factory.annotation.Value("${JWT_PRIVATE_KEY}")
    private String privateKeyPem;

    @org.springframework.beans.factory.annotation.Value("${JWT_PUBLIC_KEY}")
    private String publicKeyPem;

    @PostConstruct
    protected void init() throws Exception {
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");

        // Parse Private Key
        String privateKeyContent = privateKeyPem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        java.security.spec.PKCS8EncodedKeySpec keySpecPKCS8 = new java.security.spec.PKCS8EncodedKeySpec(
                java.util.Base64.getDecoder().decode(privateKeyContent));
        this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpecPKCS8);

        // Parse Public Key
        String publicKeyContent = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        java.security.spec.X509EncodedKeySpec keySpecX509 = new java.security.spec.X509EncodedKeySpec(
                java.util.Base64.getDecoder().decode(publicKeyContent));
        this.publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpecX509);

        this.keyId = "msa-prod-key-1";
    }

    public String createToken(String userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuer("auth-service")
                .setIssuedAt(now)
                .setExpiration(validity)
                .setHeaderParam("kid", keyId) // Crucial for JWKS matching
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpiration(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        long now = new Date().getTime();
        return (expiration.getTime() - now);
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return keyId;
    }
}