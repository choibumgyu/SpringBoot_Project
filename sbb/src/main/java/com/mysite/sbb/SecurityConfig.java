package com.mysite.sbb;

import org.springframework.context.annotation.Bean;
import com.mysite.sbb.user.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
	private final CustomOAuth2UserService customOAuth2UserService;

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService) throws Exception {
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
			        new AntPathRequestMatcher("/h2-console/**"),
			        new AntPathRequestMatcher("/oauth2/**"),
			        new AntPathRequestMatcher("/login/oauth2/**")
			    ).permitAll()
			    .anyRequest().authenticated()
			)
	    .csrf((csrf) -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")))
	    .headers((headers) -> headers.addHeaderWriter(
	            new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)))
	    .formLogin((formLogin) -> formLogin
	            .loginPage("/user/login")
	            .defaultSuccessUrl("/",true)
	    )
	    .oauth2Login(oauth2 -> oauth2
	    	    .loginPage("/user/login")
	    	    .userInfoEndpoint(userInfo ->
	    	        userInfo.userService(customOAuth2UserService)
	    	    )
	    	    .defaultSuccessUrl("/", true)
	    	)
	    .logout((logout) -> logout
	            .logoutRequestMatcher(new AntPathRequestMatcher("/user/logout"))
	            .logoutSuccessUrl("/")
	            .invalidateHttpSession(true)
	    );

	    return http.build();
	}


	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
}