package com.medac.trello.api.config;

import com.medac.trello.api.model.User;
import com.medac.trello.api.service.UserService;
import com.medac.trello.api.service.JwtManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtManager jwtService;

    @Value("${app.domain.oauth-success-url:http://localhost:3000/oauth-callback}")
    private String frontendOauthCallbackUrl;


    public OAuth2SuccessHandler(UserService userService, JwtManager jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        // Extraer atributos del usuario de Google
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        // 1. Integración de usuario: Buscar o crear el usuario en tu base de datos
        User user = userService.findOrCreateOAuthUser(email, name);

        // 2. Generación del JWT propio
        String jwtToken = jwtService.generateToken(user);

        // 3. Redirección al frontend con el token en la URL (Query Parameter)
        String redirectUrl = frontendOauthCallbackUrl + "?token=" + jwtToken;

        response.sendRedirect(redirectUrl);
    }
}
