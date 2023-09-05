package clientresponse;

public class GameoverResponse implements Response {
    protected final String command = "GAMEOVER";
    public String status;
    public String message;

    public GameoverResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {

        return status;
    }
}
