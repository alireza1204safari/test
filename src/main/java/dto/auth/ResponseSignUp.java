package dto.auth;

public class ResponseSignUp {
    private String message;
    private String user_id;
    private String token;

    public ResponseSignUp() {}

    public ResponseSignUp(String message, String user_id, String token) {
        this.message = message;
        this.user_id = user_id;
        this.token = token;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
