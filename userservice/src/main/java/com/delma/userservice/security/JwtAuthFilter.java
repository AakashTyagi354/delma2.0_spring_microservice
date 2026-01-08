package com.delma.userservice.security;

import com.delma.userservice.entity.User;
import com.delma.userservice.reposistory.UserReposistory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserReposistory userReposistory;
    private final AuthUtil authUtil;
    private final HandlerExceptionResolver handlerExceptionResolver;


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            log.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());

            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);

            Claims claims = authUtil.extractAllClaims(token);

            String email = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                List<SimpleGrantedAuthority> authorities =
                        roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {

            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "TOKEN_EXPIRED", "JWT token has expired");

        } catch (JwtException | IllegalArgumentException e) {

            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN", "Invalid JWT token");

        } catch (Exception e) {

            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "AUTH_500", "Authentication error");
        }
    }
    private void sendErrorResponse(
            HttpServletResponse response,
            int status,
            String errorCode,
            String message
    ) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        String json = """
        {
          "success": false,
          "errorCode": "%s",
          "message": "%s"
        }
        """.formatted(errorCode, message);

        response.getWriter().write(json);
    }
}
