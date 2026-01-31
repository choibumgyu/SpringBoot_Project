package com.mysite.sbb;

import com.mysite.sbb.user.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    private void common(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers(
                        new AntPathRequestMatcher("/**/*.css"),
                        new AntPathRequestMatcher("/**/*.js"),
                        new AntPathRequestMatcher("/**/*.map"),
                        new AntPathRequestMatcher("/**/*.png"),
                        new AntPathRequestMatcher("/**/*.jpg"),
                        new AntPathRequestMatcher("/**/*.jpeg"),
                        new AntPathRequestMatcher("/**/*.gif"),
                        new AntPathRequestMatcher("/**/*.svg"),
                        new AntPathRequestMatcher("/**/*.woff"),
                        new AntPathRequestMatcher("/**/*.woff2"),
                        new AntPathRequestMatcher("/**/*.ttf"),
                        new AntPathRequestMatcher("/webjars/**")
                ).permitAll()
                .requestMatchers(
                        new AntPathRequestMatcher("/"),
                        new AntPathRequestMatcher("/user/**"),
                        new AntPathRequestMatcher("/h2-console/**")
                ).permitAll()
                .anyRequest().authenticated()
        );

        http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")));
        http.headers(headers -> headers.addHeaderWriter(
                new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)
        ));

        http.formLogin(formLogin -> formLogin
                .loginPage("/user/login")
                .defaultSuccessUrl("/", true)
        );

        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/user/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
        );
    }

    @Bean
    @Profile("local")
    SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        common(http);
        // ✅ local에서는 OAuth2 관련 설정을 “아예 호출하지 않는다”
        return http.build();
    }

    @Bean
    @Profile("prod")
    SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
        common(http);

        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/user/login")
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .defaultSuccessUrl("/", true)
        );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
