package util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import repository.TokenDao;

import java.security.Key;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET = "bXktdXJsLXNlY3JldC1rZXktdGhhdC1pcy1iYXNlNjQtZW5jb2RlZA==";
    private static final Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    private static final long EXPIRATION_MS = 3 * 60 * 60 * 1000 * 10; // 3 hours
    private final TokenDao tokenDao;

    public JwtUtil() {
        this.tokenDao = new TokenDao();
    }

    public String generateToken(String phone, String role) {
        return Jwts.builder()
                .setSubject(phone) // Set phone as subject
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String validateToken(String token) {
        try {
            // Check if token is invalidated
            if (tokenDao.isTokenInvalidated(token)) {
                System.out.println("Token is invalidated");
                return null;
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject(); // Returns phone
        } catch (ExpiredJwtException e) {
            System.out.println("Token expired: " + e.getMessage());
            return null;
        } catch (JwtException e) {
            System.out.println("Invalid token: " + e.getMessage());
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            // Check if token is invalidated
            if (tokenDao.isTokenInvalidated(token)) {
                System.out.println("Token is invalidated");
                return null;
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (JwtException e) {
            System.out.println("Invalid token for role extraction: " + e.getMessage());
            return null;
        }
    }

}