package ru.muwa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.muwa.service.JwtService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        // ЕСЛИ ТОКЕНА НЕТ — СРАЗУ ИДЕМ ДАЛЬШЕ.
        // Поскольку у тебя .permitAll(), запрос успешно дойдет до index.html
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtService.isTokenValid(token)) {
                UUID userId = jwtService.extractUserId(token);
                String role = jwtService.extractRole(token);
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                System.out.println("User authenticated: " + userId + " with role: " + role);
            } else {
                System.out.println("Token is invalid according to JwtService");
                // Если токен пришел, но он тухлый/битый — лучше сразу вернуть 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } catch (Exception e) {
            System.out.println("Authentication failed: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Выносим логику поиска токена в отдельный метод для читаемости
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Если нет в заголовке, смотрим в параметры (для SSE)
        return request.getParameter("token");
    }


    /*
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");
    String token = null;

    if(authHeader != null && authHeader.startsWith("Bearer "))
        token = authHeader.substring(7); // Получаем токен из заголовка

    else if(request.getParameter("token") != null) // Если его там нет
        token = request.getParameter("token"); // Получаем из параметра запроса (в случае sse)

    else if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        // Если токена нет - просто идем дальше. 
        // FilterSecurityInterceptor позже выкинет 403, так как запрос не аутентифицирован.
        filterChain.doFilter(request, response);
        return;
    }

    try {
        if (jwtService.isTokenValid(token)) {

            UUID userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                               userId,
                               null,
                               List.of(authority)
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("User authenticated: " + userId + " with role: " + role);

        } else System.out.println("Token is invalid according to JwtService");

    } catch (Exception e) {
        System.out.println("Authentication failed: " + e.getMessage());
        // сразу отправить 401 Unauthorized
        // response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        // return;
        // TODO: или кинуть исключение на глобальный обработчик
    }
        filterChain.doFilter(request, response);
    }

     */

}