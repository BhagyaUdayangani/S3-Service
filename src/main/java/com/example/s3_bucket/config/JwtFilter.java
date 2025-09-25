package com.example.s3_bucket.config;


import com.example.s3_bucket.entity.SpordUser;
import com.example.s3_bucket.enums.AuthProvider;
import com.example.s3_bucket.exceptions.OAuth2AuthenticationProcessingException;
import com.example.s3_bucket.tokenmapper.objects.Role;

import com.example.s3_bucket.util.AttributesCommon;
import io.jsonwebtoken.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.Key;
import java.util.*;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.tokenDecryptCode}")
    private String tokenDecryptCode;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = tokenDecryption(authHeader.substring(7));
            username = jwtUtil.extractUsername(token);
            log.info("LOG :: USER_NAME : {}",username);
            log.info("LOG :: TOKEN : {}",token);
        }
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (this.validateToken(token)) {
                Authentication authentication = getAuthentication(token);
                if(authentication != null){
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(authentication, null, authentication.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }}
        }
        filterChain.doFilter(request, response);
    }
    public boolean validateToken(String token) {
        try {
            Claims claims =Jwts.parserBuilder().setSigningKey(secretKey.getBytes()).build().parseClaimsJws(token).getBody();
            // parseClaimsJws will check the expiration date. No need to do here.
            log.info("expiration date: {}", claims.getExpiration());
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.info("Invalid JWT token: {}", e.getMessage());
            log.trace("Invalid JWT token trace.", e);
        }
        return false;
    }

    private static final String DEVICE_ID = "JWT_DEVICE_ID";
    private static final String DEVICE = "DEVICE_NAME";
    private static final String AUTH_PROVIDER = "AUTH_PROVIDER";
    private static final String AUTH_MOBILE = "JWT_MOBILE";
    private static final String AUTH_EMAIL = "JWT_EMAIL";
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(secretKey.getBytes()).build().parseClaimsJws(token).getBody();
        JwsHeader<?> header = Jwts.parserBuilder().setSigningKey(secretKey.getBytes()).build().parseClaimsJws(token).getHeader();

        Object authoritiesClaim = extractRolesFromToken(token);
        log.info("LOG  :: Extracted roles : {}", authoritiesClaim);
        String deviceId = (String) header.get(DEVICE_ID);
        String device = (String)header.get(DEVICE);
        String authProvider = (String) header.get(AUTH_PROVIDER);
        String phoneNumber = (String) header.get(AUTH_MOBILE);
        String email = (String) header.get(AUTH_EMAIL);


        Collection<? extends GrantedAuthority> authorities = authoritiesClaim == null
                ? AuthorityUtils.NO_AUTHORITIES
                : AuthorityUtils.commaSeparatedStringToAuthorityList(authoritiesClaim.toString());

        SpordUser principal = new SpordUser(claims.getSubject(), "", authorities,deviceId,device,AuthProvider.valueOf(authProvider),phoneNumber,email);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }
    public List<Role> extractRolesFromToken(String token) {
        Key signingKey = new SecretKeySpec(secretKey.getBytes(), SignatureAlgorithm.HS256.getJcaName());
        try {
            JwsHeader<?> header = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getHeader();

            if (header.containsKey(AttributesCommon.JWT_HEADER_AUTH_ROLES)) {
                List<Map<String, String>> roleMaps = (List<Map<String, String>>) header.get(AttributesCommon.JWT_HEADER_AUTH_ROLES);
                List<Role> roles = new ArrayList<>();
                for (Map<String, String> roleMap : roleMaps) {
                    for(String key: roleMap.keySet()) {
                        if(key.equals("roleName")){
                            roles.add(new Role(roleMap.get(key)));
                        }
                    }
                }
                return roles;
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            // Optionally, log the stack trace in debug mode only
            log.debug("Stack trace: ", e);
        }
        return Collections.emptyList(); // Return an empty list if roles cannot be extracted
    }
    public String tokenDecryption(String encryptedToken) {
        byte[] keyBytes = tokenDecryptCode.getBytes();
        byte[] validKeyBytes = new byte[32]; // AES-256 key length
        System.arraycopy(keyBytes, 0, validKeyBytes, 0, Math.min(keyBytes.length, validKeyBytes.length));
        Key key = new SecretKeySpec(validKeyBytes, "AES");
        Cipher cipher;
        try {
            byte[] ivAndEncryptedTokenBytes = Base64.getDecoder().decode(encryptedToken);
            byte[] ivBytes = Arrays.copyOfRange(ivAndEncryptedTokenBytes, 0, 16); // Extract the IV from the encrypted data
            byte[] encryptedTokenBytes = Arrays.copyOfRange(ivAndEncryptedTokenBytes, 16, ivAndEncryptedTokenBytes.length);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, ivBytes);
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            byte[] decryptedTokenBytes = cipher.doFinal(encryptedTokenBytes);
            return new String(decryptedTokenBytes);
        } catch (Exception e) {
            throw new OAuth2AuthenticationProcessingException(e.getMessage());
        }
    }
}