package com.cart.config.jwt;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);
            List<String> rawRoles = jwtUtil.extractRoles(token);
            Integer user_id = jwtUtil.extractUserId(token);
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<SimpleGrantedAuthority> authorities = rawRoles.stream()
                        .map(role -> {
                            String r = role.toUpperCase();
                            if ("ADMINISTRATOR".equals(r)) {
                                return new SimpleGrantedAuthority("ADMIN");
                            } else if ("USER".equals(r)) {
                                return new SimpleGrantedAuthority("CUSTOMER");
                            }
                            return new SimpleGrantedAuthority(r);
                        })
                        .collect(Collectors.toList());

                UserDetails userDetails = User.withUsername(username)
                        .password("")
                        .authorities(authorities)
                        .build();

                Map<String, Object> payload = new HashMap<>();
                payload.put("id", user_id);

                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(userDetails, payload, userDetails.getAuthorities());
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
