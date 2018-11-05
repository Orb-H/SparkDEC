package edu.skku.sparkdec.sparkdec;

import android.app.IntentService;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

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
            if (!MainActivity.check)
                return;

//If data is available, then extract the ActivityRecognitionResult from the Intent//
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

//Get an array of DetectedActivity objects//
            ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

            long s = PreferenceManager.getDefaultSharedPreferences(this).getLong(MainActivity.DETECTED_ACTIVITY, 0);

            tempTime = PreferenceManager.getDefaultSharedPreferences(this).getLong(MainActivity.STANDARD_TIME, System.currentTimeMillis());
            long l, temp = System.currentTimeMillis();

            for (DetectedActivity da : detectedActivities) {
                if (da.getConfidence() >= THRESHOLD) {
                    switch (da.getType()) {
                        case DetectedActivity.ON_FOOT:
                            l = s;
                            l += temp - tempTime;
                            s = l;
                            break;
                    }
                }
            }

            tempTime = temp;

            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putLong(MainActivity.DETECTED_ACTIVITY, s)
                    .putLong(MainActivity.STANDARD_TIME, tempTime)
                    .apply();

        }
    }
}