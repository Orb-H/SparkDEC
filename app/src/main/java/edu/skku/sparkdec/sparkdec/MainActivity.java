package edu.skku.sparkdec.sparkdec;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";// Preference 데이터 Key
    public static final String STANDARD_TIME = ".STANDARD_TIME";
    private static final int REQUEST_FINE_LOCATION = 101;// FINE_LOCATION Request code
    private static final int REQUEST_INTERNET = 102;// INTERNET Request code
    private static final int REQUEST_ACTIVITY_RECOGNITION = 103;// ACTIVITY_RECOGNITION Request code
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 104;//WRITE_EXTERNAL_STORAGE Request code
    private int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    private Context mContext;// 이 Activity의 Context
    private final int RC_SIGN_IN = 100;// 구글 로그인 Intent request code

    private ActivityRecognitionClient mActivityRecognitionClient;

    private long startTime = Calendar.getInstance().getTimeInMillis();// 특정 경로에 대해 도보 시작 시간 저장

    private GoogleSignInAccount mAccount;// 구글 계정 저장용 변수
    private boolean accountVerified = false;// 구글 계정으로 로그인 되어있는 상태인지 판별

    private GoogleApiClient mClient;
    private GoogleMap googleMap;

    private GPSInfo gpsInfo;
    protected GeoDataClient geoDataClient;
    protected PlaceDetectionClient placeDetectionClient;
    private Activity mainActivity;
    private LatLng sLatLng = null;
    private LatLng dLatLng = null;
    private int lastCalled = 0;

    View.OnClickListener searchBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            int callId = v.getId();

            if (sLatLng != null && dLatLng != null && lastCalled == callId) {
                ArrayList<LatLng> pathData = findSpecPath(
                        Double.toString(sLatLng.latitude),
                        Double.toString(sLatLng.longitude),
                        Double.toString(dLatLng.latitude),
                        Double.toString(dLatLng.longitude),
                        GoogleDirection.TRANSIT_MODE_TRANSIT,
                        getResources().getString(R.string.google_maps_key));

                drawPathToMap(pathData);
                return;
            }

            try {
                    Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build(mainActivity);
                lastCalled = callId;
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);

                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
    };

    /**
     * Google Fit으로부터 특정 시간동안의 걸음 수 요청하는 AsyncTask
     */
    private class StepCounter extends AsyncTask<Long, Void, Void> {
        public Void doInBackground(Long... params) {
            long endTime = params[1];
            long startTime = params[0];

            DataReadRequest readRequest = new DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build();

            DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);

            long count = 0;

            if (dataReadResult.getBuckets().size() > 0) {
                for (Bucket bucket : dataReadResult.getBuckets()) {
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        if (dataSet.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA)) {
                            if (!dataSet.isEmpty()) {
                                count += dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                            }
                            //count += dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                        }
                    }
                }
            }

            readRequest = new DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build();

            dataReadResult = Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);

            long distance = 0;

            if (dataReadResult.getBuckets().size() > 0) {
                for (Bucket bucket : dataReadResult.getBuckets()) {
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        if (dataSet.getDataType().equals(DataType.TYPE_DISTANCE_DELTA)) {
                            if (!dataSet.isEmpty()) {
                                distance += dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                            }
                            //count += dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                        }
                    }
                }
            }

            updateText2(count + " steps / " + distance + "m");
            return null;
        }
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        initializeMap(map);
        googleMap = map;


        /*GoogleDirection googleDirection = new GoogleDirection("37.3001989", "126.9700634", "37.2939288", "126.9732337", GoogleDirection.TRANSIT_MODE_TRANSIT, getResources().getString(R.string.google_maps_key));
        ArrayList<LatLng> latLngArrayList;
        try {
            latLngArrayList = googleDirection.execute().get();
            for (LatLng element : latLngArrayList) System.out.println(element.toString());
            drawPolyLine(latLngArrayList);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

    }

    /**
     * Google Fit API에 걸음 수 및 거리 데이터 구독 요청
     */
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
                            } else {
                            }
                        } else {
                        }
                    }
                });
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_DISTANCE_CUMULATIVE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                            } else {
                            }
                        } else {
                        }
                    }
                });
        Fitness.RecordingApi.subscribe(mClient, DataType.AGGREGATE_SPEED_SUMMARY)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                            } else {
                            }
                        } else {
                        }
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //findViewById(R.id.nav_statistics).setSelected(true);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //구글 지도 만드는 코드
        MapsInitializer.initialize(getApplicationContext());
        gpsInfo = new GPSInfo(getApplicationContext());
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Construct a GeoDataClient.
        geoDataClient = Places.getGeoDataClient(this);

        // Construct a PlaceDetectionClient.
        placeDetectionClient = Places.getPlaceDetectionClient(this);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mContext = this;
        mActivityRecognitionClient = new ActivityRecognitionClient(this);

        requestPerm();

        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.RECORDING_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .build();

        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(MainActivity.DETECTED_ACTIVITY, "0,0,0,0").apply();


        findViewById(R.id.destPosText).setOnClickListener(searchBtnListener);
        findViewById(R.id.startPosText).setOnClickListener(searchBtnListener);
        /*
        GoogleDirection googleDirection = new GoogleDirection("37.3001989","126.9700634", "37.2939288","126.9732337", GoogleDirection.TRANSIT_MODE_TRANSIT, getResources().getString(R.string.google_maps_key));
        googleDirection.execute();
        ArrayList<LatLng> latLngArrayList;
        try{
            latLngArrayList = googleDirection.get();
            for(LatLng element : latLngArrayList)System.out.println(element.toString());
            drawPolyLine(latLngArrayList);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        */
        requestUpdatesHandler();

        requestGoogleSignIn();
    }

    /*
    Map이 처음
     */
    private void initializeMap(final GoogleMap googleMap) {
        LatLng nowWhere = new LatLng(gpsInfo.getLatitude(), gpsInfo.getLongitude());
        googleMap.addMarker(new MarkerOptions().position(nowWhere).title("현재 위치"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(nowWhere));
    }

    public void onConnectionSuspended(int i) {
    }

    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    public void onConnected(@Nullable Bundle bundle) {

    }

    /**
     * Google Fit과 관련 변수 초기화
     */
    private void initGoogleFit() {
        //Fitness API Initialize
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_SPEED_SUMMARY, FitnessOptions.ACCESS_READ)
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

        /*Calendar cal = Calendar.getInstance();
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.YEAR, -1);
        long startTime = cal.getTimeInMillis();*/
    }

    /**
     * 구글 로그인 요청
     */
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

    /**
     * 전체 Request 체크
     */
    private void requestPerm() {
        requestPerm(Manifest.permission.ACCESS_FINE_LOCATION, 101);
        String[] request = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET, "com.google.android.gms.permission.ACTIVITY_RECOGNITION", Manifest.permission.WRITE_EXTERNAL_STORAGE};

        for (int i = 0; i < 4; i++) {
            requestPerm(request[i], i + 101);
        }
    }

    /**
     * Request 체크해서 없으면 권한 요청
     */
    private void requestPerm(String request, int requestId) {
        int p = ContextCompat.checkSelfPermission(mContext, request);
        if (p != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, request)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{request}, requestId);
            }
        }
    }

    /**
     * Request 요청 답
     */
    public void onRequestPermissionsResult(int requestCode, String permission[], int[] grantResults) {
        switch (requestCode) {
            case 101:
                requestPerm(Manifest.permission.INTERNET, 102);
                break;
            case 102:
                requestPerm("com.google.android.gms.permission.ACTIVITY_RECOGNITION", 103);
                break;
            case 103:
                requestPerm(Manifest.permission.WRITE_EXTERNAL_STORAGE, 104);
                break;
        }
    }

    private class Speed extends AsyncTask<Void, Void, Float> {
        public Float doInBackground(Void... params) {
            float f = 0f;
            Calendar c = Calendar.getInstance();
            long l1 = c.getTimeInMillis();
            c.add(Calendar.DATE, -7);
            long l2 = c.getTimeInMillis();
            DataReadRequest drr = new DataReadRequest.Builder()
                    .read(DataType.AGGREGATE_SPEED_SUMMARY)
                    .setTimeRange(l2, l1, TimeUnit.MILLISECONDS)
                    .bucketByTime(7, TimeUnit.DAYS)
                    .build();

            DataReadResult dr = Fitness.HistoryApi.readData(mClient, drr).await(5, TimeUnit.SECONDS);

            double speed;

            if (dr.getBuckets().size() > 0) {
                for (Bucket bucket : dr.getBuckets()) {
                    List<DataSet> ds = bucket.getDataSets();
                    for (DataSet set : ds) {
                        if (set.getDataType().equals(DataType.AGGREGATE_SPEED_SUMMARY)) {
                            List<DataPoint> l = set.getDataPoints();
                            for (DataPoint dp : l) {
                            }
                        }
                    }
                }
            }
            return 0f;
        }
    }

    /**
     * Google Fit 사용해서 걸음 수 업데이트하는 AsyncTask 실행
     */
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

    /**
     * @param positions 위도와 경도를 나타내는 클래스 LatLng로 이루어진 ArrayList. 그러니까 그릴 좌표
     * @return 그렸다면 polyline 객체, 좌표가 1개 이하이면 그리지 못하고 null 반환.
     */

    public Polyline drawPolyLine(ArrayList<LatLng> positions) {
        if (positions.size() < 2) return null;
        Polyline polyline = googleMap.addPolyline(new PolylineOptions().addAll(positions));
        return polyline;
    }

    /**
     * Activity Recognition API에 3초마다 체크 요청
     */
    public void requestUpdatesHandler() {
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(3000, getActivityDetectionPendingIntent());// 3초 간격으로 Activity 체크
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateDetectedActivitiesList();
            }
        });
    }

    /**
     * 도보 수 체크 시작 시간 설정
     */
    public void startRecord() {
        startTime = Calendar.getInstance().getTimeInMillis();
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putLong(STANDARD_TIME, startTime).apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();
    }

    /**
     * 바뀐 Activity에 대해 데이터 업데이트
     */
    protected void updateDetectedActivitiesList() {
        accessGoogleFit();
        String s = PreferenceManager.getDefaultSharedPreferences(mContext).getString(DETECTED_ACTIVITY, "0,0,0,0");
        String[] sp = s.split(",");

        String sb = "";
        try {
            sb = "S: " + sp[0] + "ms\n" +        //TODO: Here Is Error. IndexOutBoundsException.
                    "W: " + sp[1] + "ms\n" +
                    "R: " + sp[2] + "ms\n" +
                    "Other: " + sp[3] + "ms";
        } catch (Exception e) {
            sb = "S: 0ms\nW: 0ms\nR: 0ms\nOther: 0ms";
        }
        updateText3(sb);
    }

    /**
     * Activity Recognition 서비스의 Intent 반환
     *
     * @return Activity Recognition 서비스의 Intent
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, ActivityIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Preference 데이터 변환 Listener
     *
     * @param sharedPreferences
     * @param s                 이 값이 {@link #DETECTED_ACTIVITY}일 경우만 처리
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {// Preference 데이터 변화 Listener
        if (s.equals(DETECTED_ACTIVITY)) {
            updateDetectedActivitiesList();
        }
    }

    /**
     * 첫 번째 TextView 내용 삽입
     *
     * @param s 해당 내용
     */
    private void updateText1(String s) {// 첫 번째 TextView 내용 삽입
        TextView t = findViewById(R.id.textView10);
        t.setText(s);
    }

    /**
     * 두 번째 TextView 내용 삽입
     *
     * @param s 해당 내용
     */
    private void updateText2(String s) {//두 번째 TextView 내용 삽입
        TextView t = findViewById(R.id.textView12);
        t.setText(s);
    }

    /**
     * 세 번째 TextView 내용 삽입
     *
     * @param s 해당 내용
     */
    private void updateText3(String s) {
        TextView t = findViewById(R.id.textView13);
        t.setText(s);
    }

    private String getText3() {
        return ((TextView) findViewById(R.id.textView13)).getText().toString();
    }

    // Find Path
    private void drawPathToMap(ArrayList<LatLng> pathData) {
        try {
            drawPolyLine(pathData);
            /*DataReadRequest readRequest = new DataReadRequest.Builder()
                    .aggregate(DataType.TYPE_SPEED, DataType.AGGREGATE_SPEED_SUMMARY)
                    .bucketByTime(10000, TimeUnit.DAYS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build();

            DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);*/
            long count = 0;

            /*if (dataReadResult.getBuckets().size() > 0) {
                Log.e("History", "Number of buckets: " + dataReadResult.getBuckets().size());
                for (Bucket bucket : dataReadResult.getBuckets()) {
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        if (dataSet.getDataType().equals(DataType.AGGREGATE_SPEED_SUMMARY)) {
                            if (!dataSet.isEmpty()) {
                                count += dataSet.getDataPoints().get(0).getValue(Field.FIELD_SPEED).asInt();
                            }
                            //count += dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                            Log.e("TEMP", dataSet.toString());
                        }
                    }
                }
            }*/

            float s = new Speed().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
            if (s >= 2 && s < 10) {
                //    updateText1(String.format("%.0f m", pf.dis) + " / " + String.format("%.0f s", pf.dur) + " (Expected: " + String.format("%.0f s", pf.dis / s) + ")");
            } else {
                //    updateText1(String.format("%.0f m", pf.dis) + " / " + String.format("%.0f s", pf.dur) + " (Expected: " + String.format("%.0f s", pf.dis / 1.25) + ")");
            }//FIXME: 배열로 고치기

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<LatLng> findSpecPath(String sLat, String sLng, String eLat, String eLng, int mode, String key) {

        GoogleDirection googleDirection = new GoogleDirection(sLat, sLng, eLat, eLng, mode, key);
        TmapPedestrian pedestrian = null;

        ArrayList<Integer> duration = new ArrayList<>();
        ArrayList<Integer> distance = new ArrayList<>();
        int dur = 0;
        int dis = 0;

        ArrayList<LatLng> retList = new ArrayList<>();

        ArrayList<LatLng> googleDir;

        try {
            googleDir = googleDirection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }


        for (int i = 0; i < googleDirection.transits.size(); i++) {
            ArrayList<LatLng> ped = null;

            if (googleDirection.transits.get(i).equals("WALKING")) {
                SystemClock.sleep(300);
                pedestrian = new TmapPedestrian(
                        Double.toString(googleDir.get(i * 2).latitude),
                        Double.toString(googleDir.get(i * 2).longitude),
                        Double.toString(googleDir.get(i * 2 + 1).latitude),
                        Double.toString(googleDir.get(i * 2 + 1).longitude),
                        "출발지",
                        "도착지");
                try {
                    ped = pedestrian.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    return null;
                } catch (InterruptedException e) {
                    return null;
                }
            }

            if (ped != null) {
                for (int j = 0; j < ped.size() - 1; j++) {
                    retList.add(ped.get(j));
                    retList.add(ped.get(j + 1));
                }
                //TODO : 여기를 그 그 사용자별 예상 시간을 넣으셈
                distance.add(pedestrian.distance);
                duration.add(pedestrian.duration);

                dis += pedestrian.distance;
                dur += pedestrian.duration;
            } else {
                retList.add(googleDir.get(i * 2));
                retList.add(googleDir.get(i * 2 + 1));
                distance.add(googleDirection.distance.get(i));
                duration.add(googleDirection.duration.get(i));
                dis += googleDirection.distance.get(i);
                dur += googleDirection.duration.get(i);
            }
        }
        return retList;
    }


    /**
     * 구글 로그인 Activity에 대해서만 작동
     *
     * @param requestCode RC_SIGN_IN인 경우만 체크
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            if (resultCode == 0 || resultCode == -1) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                mAccount = task.getResult();
                accountVerified = true;
                initGoogleFit();
            } else {
                Toast.makeText(this, "Due to unknown reason, google login failed. Please retry.", Toast.LENGTH_LONG).show();
                requestGoogleSignIn();
            }
        }

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                if (lastCalled == R.id.startPosText) {
                    ((TextView) findViewById(R.id.startPosText)).setText(place.getName());
                    sLatLng = place.getLatLng();
                } else {
                    ((TextView) findViewById(R.id.destPosText)).setText(place.getName());
                    dLatLng = place.getLatLng();
                }

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }
}

