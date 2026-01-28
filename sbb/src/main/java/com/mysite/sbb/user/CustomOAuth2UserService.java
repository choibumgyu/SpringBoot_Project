package com.mysite.sbb.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // google
        String providerId = oauth2User.getAttribute("sub"); // ⭐ 구글 고유 ID
        String email = oauth2User.getAttribute("email");

        SiteUser siteUser = userRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    SiteUser newUser = new SiteUser();
                    newUser.setProvider(provider);
                    newUser.setProviderId(providerId);
                    newUser.setEmail(email);
                    newUser.setUsername(provider + "_" + providerId.substring(0, 10));
                    newUser.setPassword(null); // 소셜 로그인은 비밀번호 없음
                    return userRepository.save(newUser);
                });
        Map<String, Object> attrs = new HashMap<>(oauth2User.getAttributes());
        attrs.put("username", siteUser.getUsername());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attrs,
                "username" // ✅ 이제 #authentication.name == siteUser.username
         );
    }
}
