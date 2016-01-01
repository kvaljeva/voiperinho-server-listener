import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.google.gson.Gson;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public final class WebConnector {

    private static String baseLoginUrl = "http://thedevspot.xyz:90/";
    private static HttpClient httpClient;

    private WebConnector() { }

    private static String processRequest(Object request) {
        HttpGet getRequest = null;
        HttpPost postRequest = null;

        if (request instanceof HttpGet)
            getRequest = HttpGet.class.cast(request);
        else
            postRequest = HttpPost.class.cast(request);

        try {
            httpClient = new DefaultHttpClient();
            HttpResponse response;

            if (getRequest != null)
                response = httpClient.execute(getRequest);
            else
                response = httpClient.execute(postRequest);

            if (response != null) {
                HttpEntity entity = response.getEntity();

                if (entity != null) return EntityUtils.toString(entity, "UTF-8");;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        return "";
    }

    private static String sendPostRequest(String jsonData, String urlEndPoint)    {
        HttpPost postRequest = new HttpPost(baseLoginUrl + urlEndPoint);
        StringEntity postString;

        try {
            postString = new StringEntity(jsonData);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        if (postString != null) {
            postRequest.setEntity(postString);
            postRequest.setHeader("Content-type", "application/json");

            return processRequest(postRequest);
        }

        return "";
    }

    private static String sendGetRequest(String urlEndPoint) {
        HttpGet getRequest = new HttpGet(baseLoginUrl + urlEndPoint);
        getRequest.setHeader("Content-Type", "application/json");

        return processRequest(getRequest);
    }

    public static UserInformation TryAuthorizeUser(String userJsonData) {
        Gson gson = new Gson();
        String jsonString;
        BaseResponse responseModel = null;

        jsonString = sendPostRequest(userJsonData, "user");
        if (jsonString != null && !jsonString.equals("")) {
            responseModel = gson.fromJson(jsonString, BaseResponse.class);
        }

        if (responseModel != null && responseModel.getStatus() == 200) return responseModel.getMessage();

        return null;
    }

    public static ArrayList<UserInformation> GetClientContacts(int clientId) {
        Gson gson = new Gson();
        String jsonString;
        ContactsInformation responseModel = null;

        jsonString = sendGetRequest("user/" + clientId + "/contacts");
        if (jsonString != null && !jsonString.equals("")) {
            responseModel = gson.fromJson(jsonString, ContactsInformation.class);
        }

        if (responseModel != null && responseModel.getStatus() == 200) return responseModel.getMessage();

        return null;
    }
}
