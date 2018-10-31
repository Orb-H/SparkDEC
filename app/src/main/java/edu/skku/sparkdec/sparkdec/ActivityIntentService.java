package edu.skku.sparkdec.sparkdec;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

//Extend IntentService//
public class ActivityIntentService extends IntentService {
    protected static final String TAG = "Activity";

    /**
     * Activity를 확정하기 위한 Confidence의 최소값
     */
    private final int THRESHOLD = 70;
    private long tempTime;// Activity Transition 사이의 시간 계산용 변수

    //Call the super IntentService constructor with the name for the worker thread//
    public ActivityIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
//Define an onHandleIntent() method, which will be called whenever an activity detection update is available//

    @Override
    protected void onHandleIntent(Intent intent) {
//Check whether the Intent contains activity recognition data//
        if (ActivityRecognitionResult.hasResult(intent)) {

//If data is available, then extract the ActivityRecognitionResult from the Intent//
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

//Get an array of DetectedActivity objects//
            ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

            String s = PreferenceManager.getDefaultSharedPreferences(this).getString(MainActivity.DETECTED_ACTIVITY, "0,0,0,0");
            String[] sp = s.split(",");

            tempTime = PreferenceManager.getDefaultSharedPreferences(this).getLong(MainActivity.STANDARD_TIME, 0);
            long l, temp = System.currentTimeMillis();

            for (DetectedActivity da : detectedActivities) {
                if (da.getConfidence() >= THRESHOLD) {
                    switch (da.getType()) {
                        case DetectedActivity.STILL:
                            l = Long.parseLong(sp[0]);
                            l += temp - tempTime;
                            sp[0] = Long.toString(l);
                            break;
                        case DetectedActivity.WALKING:
                            l = Long.parseLong(sp[1]);
                            l += temp - tempTime;
                            sp[1] = Long.toString(l);
                            break;
                        case DetectedActivity.RUNNING:
                            l = Long.parseLong(sp[2]);
                            l += temp - tempTime;
                            sp[2] = Long.toString(l);
                            break;
                        default:
                            l = Long.parseLong(sp[3]);
                            l += temp - tempTime;
                            sp[3] = Long.toString(l);
                            break;
                    }
                }
            }

            tempTime = temp;

            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(MainActivity.DETECTED_ACTIVITY,
                            sp[0] + "," + sp[1] + "," + sp[2] + "," + sp[3]).putLong(MainActivity.STANDARD_TIME, tempTime)
                    .apply();

        }
    }
}