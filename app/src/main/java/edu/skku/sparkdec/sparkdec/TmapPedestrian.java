package edu.skku.sparkdec.sparkdec;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

class TmapPedestrian extends AsyncTask<String, Void, String> {
    String strUrl;
    StringBuilder returnBuilder;
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        returnBuilder = new StringBuilder();
        strUrl = "https://api2.sktelecom.com/tmap/routes/pedestrian?version=1";
    }
    @Override
    protected String doInBackground(String... strings) {
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
                System.out.println(Integer.toString(conn.getResponseCode()) + " : " + conn.getResponseMessage());
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                String line = br.readLine();
                System.out.println(line);
                return Integer.toString(conn.getResponseCode()) + " : " + conn.getResponseMessage();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                returnBuilder.append(line);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnBuilder.toString();
    }
}
