package edu.skku.sparkdec.sparkdec;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.maps.SupportMapFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";
    private Context mContext;

    private final int RC_SIGN_IN = 100;

    private ActivityRecognitionClient mActivityRecognitionClient;

    private long startTime = Calendar.getInstance().getTimeInMillis();

    private GoogleSignInAccount mAccount;
    private boolean accountVerified = false;

    private GoogleApiClient mClient;

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
        findViewById(R.id.nav_statistics).setSelected(true);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync((OnMapReadyCallback) this);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mContext = this;
        mActivityRecognitionClient = new ActivityRecognitionClient(this);

        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.RECORDING_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .build();

        requestUpdatesHandler();

        requestGoogleSignIn();
    }

    private class StepCounter extends AsyncTask<Long, Void, Void> {
        public Void doInBackground(Long... params) {
            long endTime = params[1];
            //long startTime = params[0];
            Calendar cal=Calendar.getInstance();//FIXME
            cal.add(Calendar.YEAR,-1);//FIXME
            long startTime=cal.getTimeInMillis();//FIXME
            Log.e("TEMP", startTime + " " + endTime);

//Check how many steps were walked and recorded in the last 7 days
            DataReadRequest readRequest = new DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build();

            DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);

            long count = 0;

            if (dataReadResult.getBuckets().size() > 0) {
                Log.e("History", "Number of buckets: " + dataReadResult.getBuckets().size());
                for (Bucket bucket : dataReadResult.getBuckets()) {
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        if (dataSet.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA)) {
                            if(!dataSet.isEmpty()){
                                count += dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                            }
                            //count += dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                            Log.e("TEMP", dataSet.toString());
                        }
                    }
                }
            }

            updateText2(count + " steps");
            return null;
        }
    }

    public void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.e("TEMP", "Existing subscription for activity detected.");
                            } else {
                                Log.e("TEMP", "Successfully subscribed!");
                            }
                        } else {
                            Log.e("TEMP", "There was a problem subscribing.");
                        }
                    }
                });
    }

    public void onConnectionSuspended(int i) {
        Log.e("HistoryAPI", "onConnectionSuspended");
    }

    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("HistoryAPI", "onConnectionFailed");
    }

    public void onConnected(@Nullable Bundle bundle) {
        Log.e("HistoryAPI", "onConnected");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            if (resultCode == 0 || resultCode == -1) {
                Log.e("TEMP", resultCode + "");
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                mAccount = task.getResult();
                accountVerified = true;
                initGoogleFit();
            } else {
                Toast.makeText(this, "Due to unknown reason, google login failed. Please retry.", Toast.LENGTH_LONG).show();
                requestGoogleSignIn();
            }
        }
    }

    private void requestGoogleSignIn() {
        mAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (mAccount == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestServerAuthCode(getResources().getString(R.string.oauth_key_debug_web))
                    .requestEmail()
                    .build();
            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } else {
            accountVerified = true;
            initGoogleFit();
        }
    }

    private void initGoogleFit() {
        //Fitness API Initialize
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        startRecord();

        if (!GoogleSignIn.hasPermissions(mAccount, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    0,
                    mAccount,
                    fitnessOptions);
        }

        subscribe();

        try {
            ArrayList<Double[]> d = pedestrianPath("126.9700634", "37.3001989", "126.9732337", "37.2939288", "성균관대역", "성균관대학교 반도체관");
            updateText1(String.format("%.0f m", d.get(0)[1]) + "");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void accessGoogleFit() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        long endTime = cal.getTimeInMillis();

        if (accountVerified) {
            new StepCounter().execute(startTime, endTime);
        }
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

    public void requestUpdatesHandler() {
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

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();
    }

    @Override
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
        TextView t = findViewById(R.id.textView10);
        t.setText(s);
    }

    private void updateText2(String s) {
        TextView t = findViewById(R.id.textView12);
        t.setText(s);
    }

    private void updateText3(String s) {
        TextView t = findViewById(R.id.textView13);
        t.setText(s);
    }
}

