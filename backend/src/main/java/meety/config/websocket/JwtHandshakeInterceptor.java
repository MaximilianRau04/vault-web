package meety.config.websocket;

import jakarta.servlet.http.HttpServletRequest;
import meety.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Intercepts WebSocket handshake requests to perform JWT-based authentication.
 * <p>
 * Extracts the JWT token from the query parameter "token" or "Authorization" header,
 * validates it, and stores the corresponding Authentication object in attributes.
 * If the token is invalid or missing, the handshake is rejected with HTTP 401.
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Intercepts the handshake request before it is processed.
     *
     * @param request    the current HTTP request
     * @param response   the current HTTP response
     * @param wsHandler  the target WebSocket handler
     * @param attributes attributes shared across the handshake and WebSocket session
     * @return true to proceed with the handshake, false to reject it
     * @throws Exception in case of errors
     */
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        String token = null;

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

            token = httpServletRequest.getParameter("token");
            if (token == null) {
                String authHeader = httpServletRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
        }

        if (token != null && jwtUtil.validateToken(token)) {
            Authentication auth = jwtUtil.getAuthentication(token);
            attributes.put("auth", auth);
            return true;
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    /**
     * Invoked after the handshake is done. No actions needed here.
     *
     * @param request   the current HTTP request
     * @param response  the current HTTP response
     * @param wsHandler the target WebSocket handler
     * @param exception an exception raised during handshake, or null if none
     */
    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // no implementation needed
    }
}
