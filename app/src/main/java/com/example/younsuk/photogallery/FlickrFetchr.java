package com.example.younsuk.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Younsuk on 10/3/2015.
 */
public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "c0952ccb8ecec523e07f877b3a4a2949";
    private static final String FLICKR_GET_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String FLICKR_SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest").buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();
    //----------------------------------------------------------------------------------------------
    public List<GalleryItem> fetchRecentPhotos(Integer page){
        String url = buildUrl(FLICKR_GET_RECENT_METHOD, null, page);
        return downloadGalleryItems(url);
    }
    //----------------------------------------------------------------------------------------------
    public List<GalleryItem> searchPhotos(String query, Integer page){
        String url = buildUrl(FLICKR_SEARCH_METHOD, query, page);
        return downloadGalleryItems(url);
    }
    //----------------------------------------------------------------------------------------------
    private String buildUrl(String method, String query, Integer page){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter("method", method);

        if (method.equals(FLICKR_GET_RECENT_METHOD))
            uriBuilder.appendQueryParameter("page", page.toString());

        else if(method.equals(FLICKR_SEARCH_METHOD)){
            uriBuilder
                    .appendQueryParameter("text", query)
                    .appendQueryParameter("page", page.toString());
        }

        return uriBuilder.build().toString();
    }
    //----------------------------------------------------------------------------------------------
    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();

        try {
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse JSON", je);
        }

        return items;
    }
    //----------------------------------------------------------------------------------------------
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0)
                out.write(buffer, 0, bytesRead);
            out.close();
            return out.toByteArray();
        }
        finally {
            connection.disconnect();
        }
    }
    //----------------------------------------------------------------------------------------------
    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }
    //----------------------------------------------------------------------------------------------
    private void parseItems(List<GalleryItem> items, JSONObject jsonObject) throws IOException, JSONException {
        JSONObject photosJsonObject = jsonObject.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photoJsonArray.length(); i ++){
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));

            if(!photoJsonObject.has("url_s"))
                continue;

            item.setUrl(photoJsonObject.getString("url_s"));
            item.setOwner(photoJsonObject.getString("owner"));
            items.add(item);
        }
    }
    //----------------------------------------------------------------------------------------------
}
