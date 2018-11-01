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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

class TmapPedestrian extends AsyncTask<String, Void, ArrayList<LatLng>> {
    private String strUrl;
    private StringBuilder returnBuilder;

    public int distance = 0;
    public int duration = 0;


    public TmapPedestrian(String sLat, String sLng, String eLat, String eLng, String startName, String endName) {
        final String coordinate = "WGS84GEO";
        final String option = "0";
        Long epochTime = System.currentTimeMillis() / 1000;
        epochTime -= 31556926 * 33;
        final String gpsTime = epochTime.toString();
        try {
            final String requestValue = "startX=" + sLng + "&startY=" + sLat + "&endX=" + eLng + "&endY=" + eLat + "&reqCoordType=" + coordinate +
                    "&startName=" + URLEncoder.encode("출발지", "UTF-8") + "&endName=" + URLEncoder.encode("도착지", "UTF-8") + "&searchOption=" + option + "&resCoordType=" + coordinate;
        } catch (Exception e) {
            Log.e("At Tmap Constructor", "URL Form Has an Exception");
            e.printStackTrace();
        }

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        returnBuilder = new StringBuilder();
        strUrl = "https://api2.sktelecom.com/tmap/routes/pedestrian?version=1";
    }

    @Override
    protected ArrayList<LatLng> doInBackground(String... strings) {
        if (android.os.Debug.isDebuggerConnected())
            android.os.Debug.waitForDebugger();
        try {
            URL Url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) Url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Host", "api2.sktelecom.com");
            conn.setRequestProperty("appKey", "f8a993d2-9a13-4d70-90cf-2be31ca063e4");
            conn.setRequestProperty("Accept-Language", "ko");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            OutputStream os = conn.getOutputStream();
            os.write(strings[0].getBytes("UTF-8"));
            os.flush();
            os.close();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                String line = br.readLine();
                System.out.println(line);
                Log.e("Tmap", Integer.toString(conn.getResponseCode()) + " : " + conn.getResponseMessage());
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                returnBuilder.append(line);
            }
            br.close();
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //parsing.
        ArrayList<LatLng> returnValue = new ArrayList<>();
        try {
            JSONObject jParser = new JSONObject(returnBuilder.toString());
            JSONArray jArray = jParser.getJSONArray("features");
            boolean flag = true;
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject obj = jArray.getJSONObject(i);

                if (obj.getJSONObject("geometry").getString("type").equals("Point")) {
                    JSONArray innerArray = obj.getJSONObject("geometry").getJSONArray("coordinates");
                    if (flag && obj.getJSONObject("properties").getString("pointType").equals("SP")) {
                        flag = false;
                        distance = (int) obj.getJSONObject("properties").getDouble("totalDistance");
                        duration = (int) obj.getJSONObject("properties").getDouble("totalTime");
                    }
                    LatLng latlng = new LatLng(innerArray.getDouble(0), innerArray.getDouble(1));
                    returnValue.add(latlng);

                }
            }
        } catch (JSONException e) {
            Log.e("At Tmap JSON Parsing", "JSON Exception Occured");
            e.printStackTrace();
            return null;
        }
        return returnValue;
    }
}
