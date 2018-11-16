package edu.skku.sparkdec.sparkdec;

import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

/**
 *  How to use?
 *                           | Return Type |
 *  private static ArrayList<HashMap<String,String>> destData = null;
 *                                  | Call Type |
 *  var = new RestAPISample().execute(String...)).get();
 *
 *
 *                                         |Call Type| |Progress Type| | Return Type |
 */
public class RestAPISample extends AsyncTask<String, Integer, ArrayList<HashMap<String,String>>> {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected ArrayList<HashMap<String,String>> doInBackground(String... params)
    {
        ArrayList<HashMap<String,String>> result = new ArrayList<HashMap<String, String>>(20);

        try
        {
            // URL Setting
            URL naverAPIServer = new URL("https://openapi.naver.com/v1/search/local.json?query="+params[0]+"&display=20&start=1&sort=random");
            HttpsURLConnection connection = (HttpsURLConnection) naverAPIServer.openConnection();
            // API Key Setting
            connection.setRequestProperty("X-Naver-Client-Id",params[1]);
            connection.setRequestProperty("X-Naver-Client-Secret",params[2]);

            if (connection.getResponseCode() == 200)
            {

                InputStream responseBody = connection.getInputStream();
                InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");

                JsonReader jsonReader = new JsonReader(responseBodyReader);

                // Parsing Start
                jsonReader.beginObject();
                while(jsonReader.hasNext())
                {
                    String name = jsonReader.nextName();
                    if(name.equals("items"))
                        break;
                    else
                        jsonReader.skipValue();
                }

                jsonReader.beginArray();

                while(jsonReader.hasNext())
                {
                    jsonReader.beginObject();
                    HashMap<String,String> InputData = new HashMap<>();
                    while(jsonReader.hasNext())
                    {
                        String name = jsonReader.nextName();
                        if(name.equals("title"))
                            InputData.put("title",jsonReader.nextString().replace("<b>","").replace("</b>",""));
                        else if(name.equals("address"))
                            InputData.put("address",jsonReader.nextString());
                        else if(name.equals("mapx"))
                            InputData.put("mapx",jsonReader.nextString());
                        else if(name.equals("mapy"))
                            InputData.put("mapy",jsonReader.nextString());
                        else
                            jsonReader.skipValue();
                    }

                    jsonReader.endObject();
                    result.add(InputData);
                }
                jsonReader.endArray();
                jsonReader.endObject();
                // Parsing End
                jsonReader.close();
            }
            connection.disconnect();
        }
        catch(MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Integer... params) {

    }

    @Override
    protected void onPostExecute(ArrayList<HashMap<String,String>> result) {
        super.onPostExecute(result);
    }

}