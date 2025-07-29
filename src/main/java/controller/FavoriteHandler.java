package controller;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.ErrorDto;
import service.FavoriteService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class FavoriteHandler implements HttpHandler {
    private final FavoriteService favoriteService;
    private final Gson gson;

    public FavoriteHandler() {
        this.favoriteService = new FavoriteService();
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/favorites") && method.equals("GET")) {
                favoriteService.getFavorites(exchange);
            } else if (path.equals("/favorites") && method.equals("PUT")) {
                Map<String, Object> requestBody = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        Map.class
                );
                favoriteService.addFavorite(exchange, requestBody);
            } else if (path.matches("/favorites/\\d+") && method.equals("DELETE")) {
                Long itemId = Long.parseLong(path.split("/")[2]);
                favoriteService.removeFavorite(exchange, itemId);
            } else {
                favoriteService.sendResponse(exchange, new ErrorDto("Not found"), 404);
            }
        } catch (Exception e) {
            favoriteService.sendResponse(exchange, new ErrorDto("Internal server error: " + e.getMessage()), 500);
        }
    }
}