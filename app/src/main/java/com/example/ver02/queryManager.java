package com.example.ver02;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class queryManager {

    final int LIMIT_TIME = 180000;
    final int LIMIT_COUNT = 3;
    String Url;
    String googlePlacesData;
    int sleepTime;
    boolean requestSuccess;
    int RADIUS;

    public queryManager(String Url){
        this.Url = Url;
        requestSuccess = false;
        sleepTime = 5000;
        RADIUS = MainActivity.RADIUS;
    }

    public String execute(){
        int searchCount = 0;
        sleep();
        while(!requestSuccess) {
            try {
                searchCount++;
                DownloadUrl downloadUrl = new DownloadUrl();
                googlePlacesData = downloadUrl.readUrl(Url);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isQueryError(googlePlacesData);
            Log.d("요청 검사", requestSuccess + " ");

            if( (sleepTime >= LIMIT_TIME) || searchCount > LIMIT_COUNT){
                googlePlacesData = null;
                break;
            }
        }
        return googlePlacesData;
    }

    public void isQueryError(String jsonData){
        JSONObject jsonObject;
        try {
            Log.d("Query", "요청 검사");
            jsonObject = new JSONObject((String) jsonData);
            //"status" : "OVER_QUERY_LIMIT", "status" : "ZERO_RESULTS"
            String status = jsonObject.getString("status");
            Log.d("status", status);
            if(status.equals("OVER_QUERY_LIMIT")){
                requestSuccess = false;
                sleep();

                Log.d("할당 초과", "할당 초과 실행중");
            }
            else if(status.equals("ZERO_RESULTS")){
                requestSuccess = false;
                repairUrl();
            }
            else{
                requestSuccess = true;
            }
        } catch (JSONException e) {
            Log.d("Places", "parse error");
            e.printStackTrace();
        }
    }

    public void sleep(){
        Log.d("대기 시간", sleepTime/1000 + "초");
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sleepTime += 50000;
    }

    public void repairUrl(){
        RADIUS += 2000;
        String newUrl = Url.replace("radius=","");
        newUrl += "&radius=" + RADIUS;
        Url = newUrl;
        Log.d("repairUrl", Url);
    }
}
