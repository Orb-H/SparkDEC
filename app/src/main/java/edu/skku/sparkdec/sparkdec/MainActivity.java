package edu.skku.sparkdec.sparkdec;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";
    private Context mContext;

    private ActivityRecognitionClient mActivityRecognitionClient;

    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mContext = this;
        mActivityRecognitionClient = new ActivityRecognitionClient(this);

        //Fitness API Initialize
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        startTime = Calendar.getInstance().getTimeInMillis();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    R.string.oauth_key,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        }

        try {
            ArrayList<Double[]> d = pedestrianPath("126.9700634", "37.3001989", "126.9732337", "37.2939288", "성균관대역", "성균관대학교 반도체관");
            updateText1(d.get(0)[1] + "");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void accessGoogleFit() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        long endTime = cal.getTimeInMillis();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();


        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        Log.d("Temp", "onSuccess");
                        updateText2(dataReadResponse.getDataSet(DataType.TYPE_STEP_COUNT_CUMULATIVE) + " steps");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Temp", "onFailure()", e);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        Log.d("Temp", "onComplete()");
                    }
                });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_map) {
            // Handle the camera action
        } else if (id == R.id.nav_statistics) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /*
    public void test(View v) throws UnsupportedEncodingException {
        if (android.os.Debug.isDebuggerConnected())
            android.os.Debug.waitForDebugger();
        ArrayList<Double[]> result = pedestrianPath("1", "1", "1", "1", "출발지", "도착지");
        String a = Double.toString(result.get(0)[0]) + " " + Double.toString(result.get(0)[1]) + "\n";
        for (int i = 1; i < result.size(); i++) {
            a += Double.toString(result.get(i)[0]) + " " + Double.toString(result.get(i)[1]) + "\n";
        }
        Toast.makeText(this, a, Toast.LENGTH_LONG);
        System.out.println(a);
    }
    */
    public ArrayList<Double[]> pedestrianPath(String sx, String sy, String ex, String ey, String startName, String endName) throws UnsupportedEncodingException {
        final String coordinate = "WGS84GEO";
        final String option = "0";
        Long epochTime = System.currentTimeMillis() / 1000;
        epochTime -= 31556926 * 33;
        final String gpsTime = epochTime.toString();
        final String requestValue = "startX=" + sx + "&startY=" + sy + "&endX=" + ex + "&endY=" + ey + "&reqCoordType=" + coordinate +
                "&startName=" + URLEncoder.encode("출발지", "UTF-8") + "&endName=" + URLEncoder.encode("도착지", "UTF-8") + "&searchOption=" + option + "&resCoordType=" + coordinate;

//        final String requestValue = "startX=" + sx + "&startY=" + sy + "&endX=" + ex + "&endY=" + ey + "&reqCoordType=" + coordinate +
//                "&startName=" + URLEncoder.encode(startName, "UTF-8") + "&endName=" + URLEncoder.encode(endName, "UTF-8") + "&searchOption=" + option + "&resCoordType=" + coordinate;

        System.out.println(requestValue);
        try {
            String returnString = new TmapPedestrian().execute(requestValue).get();
            ArrayList<Double[]> returnValue = new ArrayList<>();
            JSONObject jParser = new JSONObject(returnString);
            JSONArray jArray = jParser.getJSONArray("features");
            boolean flag = true;
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject obj = jArray.getJSONObject(i);

                if (obj.getJSONObject("geometry").getString("type").equals("Point")) {
                    JSONArray innerArray = obj.getJSONObject("geometry").getJSONArray("coordinates");
                    if (flag && obj.getJSONObject("properties").getString("pointType").equals("SP")) {
                        flag = false;
                        Double[] doubleArray = {obj.getJSONObject("properties").getDouble("totalDistance"), obj.getJSONObject("properties").getDouble("totalTime")};
                        returnValue.add(doubleArray);
                    }
                    Double[] doubleArray = {innerArray.getDouble(0), innerArray.getDouble(1)};
                    returnValue.add(doubleArray);

                }
            }
            return returnValue;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void requestUpdatesHandler(View view) {
//Set the activity detection interval. I’m using 3 seconds//
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                3000,
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateDetectedActivitiesList();
            }
        });
    }

    public void startRecord() {
        startTime = Calendar.getInstance().getTimeInMillis();
    }

    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();
    }

    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    protected void updateDetectedActivitiesList() {
        accessGoogleFit();
        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString(DETECTED_ACTIVITY, ""));

        HashMap<Integer, Integer> detectedActivitiesMap = new HashMap<>();

        StringBuilder sb = new StringBuilder();
        for (DetectedActivity activity : detectedActivities) {
            sb.append(ActivityIntentService.getActivityString(this, activity.getType()) + "(" + activity.getConfidence() + "%),");
        }

        updateText3(sb.toString());
    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, ActivityIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(DETECTED_ACTIVITY)) {
            updateDetectedActivitiesList();
        }
    }

    private void updateText1(String s) {
        TextView t = (TextView) findViewById(R.id.textView10);
        t.setText(s);
    }

    private void updateText2(String s) {
        TextView t = (TextView) findViewById(R.id.textView12);
        t.setText(s);
    }

    private void updateText3(String s) {
        TextView t = (TextView) findViewById(R.id.textView13);
        t.setText(s);
    }
}

