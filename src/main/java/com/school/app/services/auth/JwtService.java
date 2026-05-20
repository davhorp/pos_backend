package com.school.app.services.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    // Expiración: 12 horas (útil para un turno de caja completo)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 12;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    // --- CÓDIGO ACTUALIZADO PARA JJWT 0.12.x ---
    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return Jwts.builder()
                .claims(extraClaims) // Antes era setClaims()
                .subject(userDetails.getUsername()) // Antes era setSubject()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                // Ya no es necesario especificar SignatureAlgorithm.HS256, lo infiere de la llave
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Desencripta el token y extrae el cuerpo (Payload) completo.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser() // Cambió de parserBuilder() a parser()
                .verifyWith(getSignInKey()) // Cambió de setSigningKey() a verifyWith()
                .build()
                .parseSignedClaims(token) // Cambió de parseClaimsJws() a parseSignedClaims()
                .getPayload(); // Cambió de getBody() a getPayload()
    }

    /**
     * Genera el objeto Key seguro requerido por JJWT a partir de nuestro String en Base64.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
