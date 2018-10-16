package edu.skku.sparkdec.sparkdec;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class GoogleDirection extends AsyncTask<String, Void, ArrayList<LatLng>> {
    public static final int TRANSIT_MODE_DRIVE = 0;
    public static final int TRANSIT_MODE_WALK = 1;
    public static final int TRANSIT_MODE_BICYCLE = 2;
    public static final int TRANSIT_MODE_TRANSIT = 3;
    public int duration = 0;
    public int distance = 0;

    private final String TRANSIT_PARSE[] = {"driving", "walking", "bicycling", "transit"};


    private StringBuilder urlBuilder;
    private StringBuilder returnBuilder;

    /**
     * @param sx   출발지의 위도
     * @param sy   출발지의 경도
     * @param ex   도착지의 위도
     * @param ey   도착지의 경도
     * @param mode 이동 방법 [TRANSIT_MODE_DRIVE, TRANSIT_MODE_WALK, TRANSIT_MODE_BICYCLE, TRANSIT_MODE_TRANSIT]
     */
    public GoogleDirection(double sx, double sy, double ex, double ey, int mode) {
        urlBuilder = new StringBuilder();
        urlBuilder.append("https://maps.googleapis.com/maps/api/directions/json?origin=");
        urlBuilder.append(sx);
        urlBuilder.append(", ");
        urlBuilder.append(sy);
        urlBuilder.append("&destination=");
        urlBuilder.append(ex);
        urlBuilder.append(", ");
        urlBuilder.append(ey);
        urlBuilder.append("&key=");
        urlBuilder.append(R.string.google_maps_key);
        urlBuilder.append("&mode=");
        urlBuilder.append(TRANSIT_PARSE[mode]);
    }

    public void addAttributes(String key, String value) {
        urlBuilder.append("&");
        urlBuilder.append(key);
        urlBuilder.append("=");
        urlBuilder.append(value);
    }

    public void editAttributes(String key, String value) {
        int start = urlBuilder.lastIndexOf(key);
        if (start == -1) addAttributes(key, value);
        int end = urlBuilder.lastIndexOf("&", start);
        urlBuilder.replace(start + key.length() + 1, end - 1, value);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        returnBuilder = new StringBuilder();
    }

    protected ArrayList<LatLng> doInBackground(String... strings) {
        if (android.os.Debug.isDebuggerConnected())
            android.os.Debug.waitForDebugger();
        try {
            URL url = new URL(urlBuilder.toString());
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e("Http connection error", Integer.toString(connection.getResponseCode()) + " : " + connection.getResponseMessage());
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
                String line = br.readLine();
                Log.e("", line);
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                returnBuilder.append(line);
            }
        } catch (MalformedURLException e) {
            Log.e("URL 변환 에러", "In GoogleDirection url declaration");
            return null;
        } catch (IOException e) {
            Log.e("IOException", "In GoogleDirection service connection");
            return null;
        }
        ArrayList<LatLng> returns = new ArrayList<>();
        try {
            JSONObject response = new JSONObject(returnBuilder.toString());
            JSONObject routes = response.getJSONArray("routes").getJSONObject(1);
            distance = routes.getJSONObject("distance").getInt("value");
            duration = routes.getJSONObject("duration").getInt("value");
            JSONArray legs = routes.getJSONArray("legs");
            for (int i = 0; i < legs.length(); i++) {
                JSONArray steps = legs.getJSONObject(i).getJSONArray("steps");
                for (int j = 0; j < steps.length(); j++) {
                    JSONObject temp = steps.getJSONObject(j);
                    returns.add(new LatLng(temp.getJSONObject("start_location").getDouble("lat"), temp.getJSONObject("start_location").getDouble("lng")));
                    returns.add(new LatLng(temp.getJSONObject("end_location").getDouble("lat"), temp.getJSONObject("end_location").getDouble("lng")));
                }
            }
        } catch (JSONException e) {
            Log.e("Error", "JSONException at GoogleDirection Response");
            return null;
        }
        return returns;
    }
}
