package xyz.thedevspot.models;
import com.google.gson.annotations.SerializedName;

public class BaseResponse {
    private int status;
    private UserInformation message;
    @SerializedName("error_message")
    private String errorMessage;

    public int getStatus() {
        return this.status;
    }
    public UserInformation getMessage() {
        return this.message;
    }
    public String GetErrorMessage() {
        return this.errorMessage;
    }

    public BaseResponse(int status, UserInformation msg, String errorMsg) {
        this.status = status;
        this.message = msg;
        this.errorMessage = errorMsg;
    }
}
