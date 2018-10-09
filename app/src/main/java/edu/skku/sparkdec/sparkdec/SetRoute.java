package edu.skku.sparkdec.sparkdec;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;


public class SetRoute extends AppCompatActivity {


    private static ArrayList<HashMap<String,String>> startData = null;
    private static ArrayList<HashMap<String,String>> destData = null;
    private static String[] position = new String[]{null,null,null,null};
    private static final int REQUEST_ACCESS_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.startButton).setOnClickListener(sLButtonListener);
        findViewById(R.id.destButton).setOnClickListener(dLButtonListener);
        findViewById(R.id.endButton).setOnClickListener(eLButtonListener);
        checkPermissionVaild();
    }


    Button.OnClickListener sLButtonListener = new View.OnClickListener() {
        public void onClick(View v) {

            Switch currentLocation = findViewById(R.id.currentLocation);

            if(currentLocation.isChecked())
            {
                GPSInfo gpsInfo = new GPSInfo(SetRoute.this);

                if(!gpsInfo.isGetLocation())
                {
                    gpsInfo.showSettingsAlert();
                    return;
                }
                Double[] posi = new Double[]{gpsInfo.getLongitude(),gpsInfo.getLatitude()};
                try
                {
                    startData = new RestAPISample().execute("Sample", "Sample 2").get();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                catch (ExecutionException e)
                {
                    e.printStackTrace();
                }
                gpsInfo.stopUsingGPS();
            }
            else
            {
                TextView startLocation = findViewById(R.id.startLocation);
                String path = startLocation.getText().toString();
                try
                {
                    path = URLEncoder.encode(path, "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                    return;
                }

                try
                {
                    startData = new RestAPISample().execute("Sample", "Sample 2").get();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                catch (ExecutionException e)
                {
                    e.printStackTrace();
                }
            }

            ListView listView = findViewById(R.id.startLocationList);

            SimpleAdapter simpleAdapter = new SimpleAdapter(SetRoute.this,startData,android.R.layout.simple_list_item_2,new String[]{"title","address"},new int[]{android.R.id.text1,android.R.id.text2});
            listView.setAdapter(simpleAdapter);
            listView.setOnItemClickListener(sListViewListener);

        }
    };

    private void checkPermissionVaild()
    {
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(SetRoute.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_LOCATION);

    }

    Button.OnClickListener dLButtonListener = new View.OnClickListener() {
        public void onClick(View v) {

            TextView destLocation = findViewById(R.id.destLocation);
            String path = destLocation.getText().toString();
            try
            {
                path = URLEncoder.encode(path, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
                return;
            }

            try
            {
                destData = new RestAPISample().execute("Sample", "Sample 2").get();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            catch (ExecutionException e)
            {
                e.printStackTrace();
            }

            ListView listView = findViewById(R.id.destLocationList);

            SimpleAdapter simpleAdapter = new SimpleAdapter(SetRoute.this,destData,android.R.layout.simple_list_item_2,new String[]{"title","address"},new int[]{android.R.id.text1,android.R.id.text2});
            listView.setAdapter(simpleAdapter);
            listView.setOnItemClickListener(dListViewListener);

        }
    };

    Button.OnClickListener eLButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(SetRoute.this, MainActivity.class);
            intent.putExtra("sx",position[0]);
            intent.putExtra("sy",position[1]);
            intent.putExtra("ex", position[2]);
            intent.putExtra("ey", position[3]);
            startActivity(intent);
        }
    };

    AdapterView.OnItemClickListener sListViewListener =  new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int posi, long id) {
            HashMap<String,String> startdata = startData.get(posi);

            if(((Switch)findViewById(R.id.currentLocation)).isChecked())
            {
                position[0] = startdata.get("mapx");
                position[1] = startdata.get("mapy");
            }
            else
            {
                Double[] posidata = convertToGIS(startdata.get("mapx"), startdata.get("mapy"));
                position[0] = posidata[0].toString();
                position[1] = posidata[1].toString();
            }

            Toast.makeText(SetRoute.this,startdata.get("title")+"("+position[0]+","+position[1]+") 이 선택되었습니다.",Toast.LENGTH_SHORT).show();
        }
    };

    AdapterView.OnItemClickListener dListViewListener =  new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int posi, long id) {
            HashMap<String,String> destdata = destData.get(posi);
            Double[] posidata = convertToGIS(destdata.get("mapx"),destdata.get("mapy"));
            position[2] = posidata[0].toString();
            position[3] = posidata[1].toString();
            Toast.makeText(SetRoute.this,destdata.get("title")+"("+position[2]+","+position[3]+") 이 선택되었습니다.",Toast.LENGTH_SHORT).show();
        }
    };

    protected Double[] convertToGIS(String X,String Y)
    {
        GeoTransPoint oKA = new GeoTransPoint(Double.parseDouble(X),Double.parseDouble(Y));
        GeoTransPoint oGeo = GeoTrans.convert(GeoTrans.KATEC, GeoTrans.GEO, oKA);
        Double[] p = new Double[]{oGeo.getX(),oGeo.getY()};
        return p;
    }
}