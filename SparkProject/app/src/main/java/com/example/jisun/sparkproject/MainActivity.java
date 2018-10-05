package com.example.jisun.sparkproject;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private String[] returnPreference(Activity activity)
    {
        String[] data = {"a","b","c"};
        return data;
    }
    private void printLayout(String[] data){
        TextView t1 = (TextView)findViewById(R.id.time_prevrew);
        TextView t2 = (TextView)findViewById(R.id.time_goal);
        TextView t3 = (TextView)findViewById(R.id.distance);
        t1.setText(data[0]);
        t2.setText(data[1]);
        t3.setText(data[2]);
        return;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String[] layoutData = returnPreference(this);
        printLayout(layoutData);
    }
}