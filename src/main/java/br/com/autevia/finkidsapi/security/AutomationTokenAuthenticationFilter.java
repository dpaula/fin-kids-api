package br.com.autevia.finkidsapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AutomationTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTOMATION_PATH_PREFIX = "/api/v1/automation/";

    private final String automationToken;

    public AutomationTokenAuthenticationFilter(@Value("${app.security.automation.token}") String automationToken) {
        this.automationToken = automationToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isAutomationPath(request) && isValidBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "automation-client",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_AUTOMATION_CLIENT"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isAutomationPath(request);
    }

    private boolean isAutomationPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith(AUTOMATION_PATH_PREFIX);
    }

    private boolean isValidBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return false;
        }

        String providedToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(providedToken) && providedToken.equals(automationToken);
    }
}
