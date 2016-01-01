import com.google.gson.annotations.SerializedName;

public class RequestInformation {
    private int id;
    @SerializedName("user_id")
    private int userId;
    @SerializedName("requester_id")
    private int requesterId;
    private int state;
    @SerializedName("request_text")
    private String requestText;
    private UserInformation requester;

    public UserInformation getRequester()
    {
        return this.requester;
    }
}
