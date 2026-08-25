package com.kds.backend.identity.application;

import com.kds.backend.identity.domain.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class JwtTokenService {
    public static final String ISSUER = "https://kds-club-manager";
    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration accessTokenTtl;

    public JwtTokenService(JwtEncoder encoder, Clock clock,
                           @Value("${app.auth.access-token-ttl}") Duration accessTokenTtl) {
        this.encoder = encoder;
        this.clock = clock;
        this.accessTokenTtl = accessTokenTtl;
    }

    public String issue(UserEntity user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .subject(user.getId().toString())
                .claim("userId", user.getId().toString())
                .claim("permissions", List.of())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds() { return accessTokenTtl.toSeconds(); }
}
