package edu.skku.sparkdec.sparkdec;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;

public class ViewSender
{
    private Context preContext;
    static final Class destClass = DataShowActivity.class;
    private String finalDest;
    private String destTime;
    private String leftTime;
    private String topInfo;
    private String midLeftInfo;
    private String midMidInfo;
    private String midRightInfo;
    private String midBotInfo;
    private String botBotInfo;
    private String userInfo;

    public ViewSender(Context PresentClass)
    {
        if (PresentClass == null)
        {
            Log.d("WidgetDataSender","Context 파라미터가 NULL 입니다");
            return;
        }
        this.preContext = PresentClass;
    }

    public ViewSender(Context PresentClass, @NonNull String[] params)
    {
        if(params.length != 10)
        {
            Log.d("WidgetDataSender","파라미터의 갯수가 10개가 아닙니다");
            return;
        }
        this.preContext = PresentClass;
        this.finalDest = params[0];
        this.destTime = params[1];
        this.leftTime = params[2];
        this.topInfo = params[3];
        this.midLeftInfo = params[4];
        this.midMidInfo = params[5];
        this.midRightInfo = params[6];
        this.midBotInfo = params[7];
        this.botBotInfo = params[8];
        this.userInfo = params[9];
    }

    public ViewSender(Context PresentClass, @NonNull HashMap<String,String> params)
    {
        if(!(PresentClass != null && params.containsKey("finalDest")&&params.containsKey("destTime")&&params.containsKey("leftTime")&&params.containsKey("topInfo")&&params.containsKey("midLeftInfo")&&params.containsKey("midMidInfo")&&params.containsKey("midRightInfo")&&params.containsKey("midBotInfo")&&params.containsKey("botBotInfo")&&params.containsKey("userInfo")))
        {
            Log.d("WidgetDataSender","필요한 데이터가 모두 있지 않습니다");
            return;
        }
        this.preContext = PresentClass;
        this.finalDest = params.get("finalDest");
        this.destTime = params.get("destTime");
        this.leftTime = params.get("leftTime");
        this.topInfo = params.get("topInfo");
        this.midLeftInfo = params.get("midLeftInfo");
        this.midMidInfo = params.get("midMidInfo");
        this.midRightInfo = params.get("midRightInfo");
        this.midBotInfo = params.get("midBotInfo");
        this.botBotInfo = params.get("botBotInfo");
        this.userInfo = params.get("userInfo");
    }

    public void setPreClass(Context preContext) {
        this.preContext = preContext;
    }

    private boolean checkDataVaild()
    {
        return this.preContext != null && this.finalDest != null && this.destTime != null && this.leftTime != null && this.topInfo != null&& this.midLeftInfo != null&& this.midMidInfo != null&& this.midRightInfo != null&& this.midBotInfo != null&& this.botBotInfo != null&& this.userInfo != null;
    }

    public void setDestTime(String destTime) {
        this.destTime = destTime;
    }

    public void setLeftTime(String leftTime) {
        this.leftTime = leftTime;
    }

    public void setTopInfo(String topInfo) {
        this.topInfo = topInfo;
    }

    public void setMidLeftInfo(String midLeftInfo) {
        this.midLeftInfo = midLeftInfo;
    }

    public void setMidMidInfo(String midMidInfo) {
        this.midMidInfo = midMidInfo;
    }

    public void setMidRightInfo(String midRightInfo) {
        this.midRightInfo = midRightInfo;
    }

    public void setMidBotInfo(String midBotInfo) {
        this.midBotInfo = midBotInfo;
    }

    public void setBotBotInfo(String botBotInfo) {
        this.botBotInfo = botBotInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public void setFinalDest(String finalDest)
    {
        this.finalDest = finalDest;
    }

    public void send()
    {
        if(!checkDataVaild())
        {
            Log.d("ViewDataSender","필요한 변수를 모두 설정하지 않았습니다");
            return;
        }
        SharedPreferences sharedPreferences  = preContext.getSharedPreferences("edu.skku.sparkdec.sparkdec.ViewDataTrans", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("type","Server"); // 이게 없으면 위젯이 인식하지 못함.
        // put variable like this.
        editor.putString("finalDest", finalDest);
        editor.putString("destTime", destTime);
        editor.putString("leftTime", leftTime);
        editor.putString("topinfo", topInfo);
        editor.putString("midleftinfo", midLeftInfo);
        editor.putString("midmidinfo", midMidInfo);
        editor.putString("midrightinfo", midRightInfo);
        editor.putString("midbotinfo", midBotInfo);
        editor.putString("botbotinfo", botBotInfo);
        editor.putString("userInfo", userInfo);
        editor.commit();
        Intent intent = new Intent(preContext, destClass);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        preContext.sendBroadcast(intent);
    }



}