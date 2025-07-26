package com.textbasedgame.auth;

import com.textbasedgame.users.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class JwtUtil {
    Dotenv dotenv = Dotenv.load();

    //TODO: move to env
    private final String secretKey = dotenv.get("JWT_SECRET_KEY");
    private final long accessTokenValidity = Long.parseLong(Objects.requireNonNull(dotenv.get("JWT_TOKEN_VALIDITY_MS")));
    private final String TOKEN_HEADER = "Authorization";
    private final String TOKEN_PREFIX = "Bearer ";

    public record CreateTokenReturn(String token, Date expirationTime){}


    public CreateTokenReturn createToken(User user) throws JsonProcessingException {
        assert secretKey != null;
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Date expirationTime = new Date(System.currentTimeMillis() + accessTokenValidity);

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String userDataAsString = ow.writeValueAsString(user.getDetailsForToken(expirationTime));
        String token = Jwts.builder().subject(userDataAsString)
                .claim("roles", user.getRolesForToken())
                .expiration(expirationTime)
                .signWith(key).compact();

        return new CreateTokenReturn(token, expirationTime);
    }


    private Claims parseJwtClaims(String token) {
        assert secretKey != null;
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

    }

    public Claims resolveClaims(HttpServletRequest req) {
        try {
            String token = resolveToken(req);
            if (token != null) {
                return parseJwtClaims(token);
            }
            return null;
        } catch (ExpiredJwtException ex) {
            req.setAttribute("expired", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            req.setAttribute("invalid", ex.getMessage());
            throw ex;
        }
    }

    public String resolveToken(HttpServletRequest request) {

        String bearerToken = request
                .getHeader(TOKEN_HEADER);
        if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    public boolean validateClaims(Claims claims) throws AuthenticationException {
        try {
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            throw e;
        }
    }

    public List<String> getRoles(Claims claims) {
        return (List<String>) claims.get("roles");
    }


}