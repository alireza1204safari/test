package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.AuthService;
import service.ProfileService;

import java.io.IOException;

public class AuthHandler implements HttpHandler {

    private AuthService authService;
    private ProfileService profileService;

    public AuthHandler() {
        this.authService = new AuthService();
        this.profileService = new ProfileService();
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/auth/register")) {
                authService.handleSignup(exchange);
            } else if (path.equals("/auth/login")) {
                authService.handleLogin(exchange);
            } else if (path.equals("/auth/logout")) {
                authService.handleLogout(exchange);
            } else {
                exchange.sendResponseHeaders(404, -1); // Not Found
            }
        }else if ("GET".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/auth/profile")) {
                profileService.handleGetProfile(exchange);
            } else {
                exchange.sendResponseHeaders(404, -1); // Not Found
            }
        }else if ("PUT".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/auth/profile")) {
                profileService.handleUpdateProfile(exchange);
            } else {
                exchange.sendResponseHeaders(404, -1); // Not Found
            }
        }
        else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

}
