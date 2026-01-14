package com.delma.documentservice.security;


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

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthUtil jwtUtil;

    public List<String> getRoles(String token) {
        Claims claims = jwtUtil.extractClains(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj).stream()
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JWT Filter hit for URI: {}", request.getRequestURI());
        String headers = request.getHeader("Authorization");
        if (headers == null || !headers.startsWith("Bearer ")) {
            System.out.println("No JWT token found in request headers");
            filterChain.doFilter(request, response);
            return;
        }
        String token = headers.substring(7);
        String userId = jwtUtil.getUserId(token);
        List<String> roles = getRoles(token);

//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        System.out.println("Auth roles: " + auth.getAuthorities());
//
//        List<GrantedAuthority> authorities =
//                List.of(new SimpleGrantedAuthority("ROLE_" + role));
//        UsernamePasswordAuthenticationToken authenticationToken =
//                new UsernamePasswordAuthenticationToken(userId, null, authorities);
//        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        System.out.println("Roles from token: " + roles);
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        // ⚠️ Set authentication before continuing the filter chain
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

// Now continue the filter chain
        System.out.println("Authorities: " + SecurityContextHolder.getContext().getAuthentication());
        filterChain.doFilter(request, response);



    }
}
