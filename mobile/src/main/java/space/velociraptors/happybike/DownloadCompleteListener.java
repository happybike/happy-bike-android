package space.velociraptors.happybike;


import org.json.JSONArray;
import org.json.JSONObject;

public interface DownloadCompleteListener {
    void downloadComplete(JSONArray stations);
}
