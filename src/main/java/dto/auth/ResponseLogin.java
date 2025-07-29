package dto.auth;

import dto.ResponseUser;

public class ResponseLogin {
    private String message;

    public ResponseLogin(String message, ResponseUser user, String token) {
        this.message = message;
        this.user = user;
        this.token = token;
    }

    private ResponseUser user;
    private String token;
}
