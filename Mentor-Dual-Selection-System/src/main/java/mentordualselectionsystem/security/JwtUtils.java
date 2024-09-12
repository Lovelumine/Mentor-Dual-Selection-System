package mentordualselectionsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    private static final long EXPIRATION_TIME = 864_000_000; // 10 天
    private final Key secretKey;  // 保持一个固定的密钥

    // 在构造函数中生成密钥，确保每次签名和验证使用相同的密钥
    public JwtUtils() {
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    // 验证并解析 JWT token，返回 uid，如果签名无效或过期则抛出 SignatureException
    public Long validateTokenAndGetUid(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.parseLong(claims.getSubject());  // 解析并返回 uid
        } catch (SignatureException e) {
            // 捕获签名不匹配的异常并重新抛出以便过滤器或控制器捕获
            throw new SignatureException("JWT签名不匹配或无效", e);
        } catch (Exception e) {
            // 捕获其他异常
            throw new RuntimeException("JWT解析失败: " + e.getMessage(), e);
        }
    }

    // 生成 JWT token，传入用户的 uid
    public String generateToken(Long uid) {
        return Jwts.builder()
                .setSubject(String.valueOf(uid))  // 将 uid 作为 subject 存储
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey, SignatureAlgorithm.HS512)  // 使用固定密钥签名
                .compact();
    }

    // 验证并解析 JWT token，返回 uid
    public String validateToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();  // 返回 uid（从 subject 中提取）
    }

    // 检查 token 是否有效
    public boolean isTokenValid(String token, Long uid) {
        final Long tokenUid = Long.valueOf(validateToken(token));
        return (uid.equals(tokenUid) && !isTokenExpired(token));
    }

    // 检查 token 是否过期
    private boolean isTokenExpired(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration().before(new Date());
    }
}
