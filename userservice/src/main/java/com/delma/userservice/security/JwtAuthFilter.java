package com.delma.userservice.security;

import com.delma.userservice.entity.User;
import com.delma.userservice.reposistory.UserReposistory;
import io.jsonwebtoken.Claims;
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//
//        “How does Spring Security handle roles from JWT?”
//
//        You answer:
//
//“JWT roles are converted into GrantedAuthority objects and stored in SecurityContext. Method-level security uses these authorities for access control.”
        try {


            log.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());

            final String requestTokenHeader = request.getHeader("Authorization");
            if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            String jwtToken = requestTokenHeader.substring(7);
            Claims claims = authUtil.extractAllClaims(jwtToken);
//            String username = authUtil.getUsernameFromToken(jwtToken);

            String email = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                User user = userReposistory.findByName(username).orElseThrow();
//                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
//                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                List<SimpleGrantedAuthority> authorities =
                        roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList();
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Error in JwtAuthFilter: {}", e.getMessage());
            handlerExceptionResolver.resolveException(request, response, null, e);
            return;


        }
    }
}
