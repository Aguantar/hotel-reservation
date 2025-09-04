package com.example.hotelres.security;

import com.example.hotelres.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwt;
    private final UserRepository users;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
            throws IOException {

        OAuth2User principal = (OAuth2User) auth.getPrincipal();

        // 현재 요청 URI: /login/oauth2/code/{provider}
        String uri = req.getRequestURI();
        String provider = uri.substring(uri.lastIndexOf('/') + 1); // google/kakao/naver

        // provider별 externalId 추출
        String externalId;
        if ("naver".equals(provider)) {
            Map<String,Object> resp = (Map<String,Object>) principal.getAttributes().get("response");
            externalId = resp.get("id").toString();
        } else if ("kakao".equals(provider)) {
            externalId = String.valueOf(principal.getAttributes().get("id"));
        } else { // google
            externalId = String.valueOf(principal.getAttributes().get("sub"));
        }

        String loginId = provider + "_" + externalId;

        var u = users.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginId));

        String access  = jwt.generateAccess(u.getLoginId(), u.getRole().name());
        String refresh = jwt.generateRefresh(u.getLoginId());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(false)               // 배포 시 true + HTTPS
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(jwt.getRefreshExpMs() / 1000)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String redirect = "http://localhost:5173/main#token=" +
                URLEncoder.encode(access, StandardCharsets.UTF_8);

        res.setStatus(HttpServletResponse.SC_FOUND);
        res.setHeader("Location", redirect);
    }
}
