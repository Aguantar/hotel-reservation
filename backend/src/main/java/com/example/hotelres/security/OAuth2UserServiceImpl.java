package com.example.hotelres.security;

import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository users;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        var delegate = new DefaultOAuth2UserService();
        var user = delegate.loadUser(req);

        String provider = req.getClientRegistration().getRegistrationId(); // google/kakao/naver
        Map<String, Object> attrs = user.getAttributes();

        String externalId;
        String email = null;
        String name  = null;

        switch (provider) {
            case "google" -> {
                externalId = (String) attrs.get("sub");
                email = (String) attrs.get("email");
                name  = (String) attrs.get("name");
            }
            case "kakao" -> {
                externalId = String.valueOf(attrs.get("id"));
                Map<String,Object> account = (Map<String,Object>) attrs.get("kakao_account");
                if (account != null) {
                    email = (String) account.get("email"); // 검수 전이면 null 가능
                    Map<String,Object> profile = (Map<String,Object>) account.get("profile");
                    if (profile != null) name = (String) profile.get("nickname");
                }
            }
            case "naver" -> {
                Map<String,Object> resp = (Map<String,Object>) attrs.get("response");
                externalId = (String) resp.get("id");
                email = (String) resp.get("email");
                name  = (String) resp.get("name");
            }
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        }

        String loginId = provider + "_" + externalId;

        // ✅ 람다에서 사용할 확정값(final)로 고정
        final String resolvedName  = (name  != null && !name.isBlank())
                ? name  : provider.toUpperCase() + " 사용자";
        final String resolvedEmail = (email != null && !email.isBlank())
                ? email : loginId + "@example.local";

        User u = users.findByLoginId(loginId).orElseGet(() -> {
            User nu = new User();
            nu.setLoginId(loginId);
            nu.setPasswordHash("{noop}SOCIAL"); // 소셜계정은 비밀번호 사용 안 함
            nu.setName(resolvedName);
            nu.setEmail(resolvedEmail);
            nu.setStatus(User.Status.ACTIVE);
            nu.setRole(User.Role.ROLE_USER);
            return users.save(nu);
        });

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(u.getRole().name())),
                attrs,
                req.getClientRegistration().getProviderDetails()
                        .getUserInfoEndpoint().getUserNameAttributeName()
        );
    }
}
