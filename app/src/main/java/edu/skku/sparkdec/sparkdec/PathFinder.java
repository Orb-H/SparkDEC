package edu.skku.sparkdec.sparkdec;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * usage
 * PathFinder pf = new PathFinder(String sLat, String sLng, String eLat, String eLng, int mode(at GoogleDirection static values), String key);
 * pf.execute()
 * try{ pf.get() }catch(Exception e){}
 * duration = pf.duration;
 * distance = pf.distance;
 */

public class PathFinder extends AsyncTask<String, Void, ArrayList<LatLng>> {
    GoogleDirection googleDirection;
    TmapPedestrian pedestrian;
    int duration;
    int distance;

    public PathFinder(String sLat, String sLng, String eLat, String eLng, int mode, String key) {
        googleDirection = new GoogleDirection(sLat, sLng, eLat, eLng, mode, key);
    }

    @Override
    protected ArrayList<LatLng> doInBackground(String... strings) {
        googleDirection.execute();

        ArrayList<LatLng> retList = new ArrayList<>();
        ArrayList<LatLng> googleDir;
        try {
            googleDir = googleDirection.get();
        } catch (ExecutionException e) {
            Log.e("At PathFinder Execution", "Can't Execute GoogleDirection");
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            Log.d("At PathFinder Execution", "Interrupted");
            return null;
        }
        for (int i = 0; i < googleDirection.transits.size(); i++) {
            if (googleDirection.transits.get(i).equals("WALKING")) {
                pedestrian = new TmapPedestrian(Double.toString(googleDir.get(i).latitude), Double.toString(googleDir.get(i).longitude), Double.toString(googleDir.get(i + 1).latitude), Double.toString(googleDir.get(i + 1).longitude), "", "");
                pedestrian.execute();
                try {
                    ArrayList<LatLng> ped = pedestrian.get();

                    for (int j = 0; j < ped.size() - 1; j++) {
                        retList.add(ped.get(j));
                        retList.add(ped.get(j + 1));
                    }
                    //TODO : 여기를 그 그 사용자별 예상 시간을 넣으셈
                    distance += pedestrian.distance;
                    duration += pedestrian.duration;
                } catch (ExecutionException e) {
                    Log.e("At PathFinder Execution", "Can't Execute TmapPedestrian");
                    e.printStackTrace();
                    return null;
                } catch (InterruptedException e) {
                    Log.d("At PathFinder Execution", "Interrupted");
                    return null;
                }
            } else {
                retList.add(googleDir.get(i * 2));
                retList.add(googleDir.get(i * 2 + 1));
                distance += googleDirection.distance.get(i);
                duration += googleDirection.duration.get(i);
            }
        }
        return retList;
    }
}
