package com.example.ver02;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class CompareCenter {

    CenterFetchUrl fetchUrl;
    List<String> duration = new ArrayList<>();
    double dispersion;

    public CompareCenter(List<Location> locationList, LatLng pos){


        for(int i=0;i<locationList.size();i++) {
            LatLng origin = new LatLng(locationList.get(i).getLatitude(),
                    locationList.get(i).getLongitude());
            LatLng dest = pos;

            String url = getRouteUrl(origin, dest);
            Log.d("CompareCenter", url.toString());
            fetchUrl = new CenterFetchUrl();

            fetchUrl.execute(url);
        }
        dispersion = calDispersion();
    }

    private class CenterFetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadCenterUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            CenterParserTask parserTask = new CenterParserTask();

            parserTask.execute(result);
        }
    }
    private class CenterParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                RouteDataParser parser = new RouteDataParser();
                Log.d("ParserTask", parser.toString());

                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

                duration.add(parser.duration);

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

        }
    }
    private String downloadCenterUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private String getRouteUrl(LatLng origin, LatLng dest) {

        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "mode=transit&sensor=false";
        String Key = "key=AIzaSyB9JI6AoYt2apJkAazZv-jGrvFCAP3t23s";
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + Key;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters ;

        return url;
    }

    private double calDispersion(){
        int size = duration.size();

        double dispersion=0;
        double sum=0;
        double[] arr = new double[size];

        for(int i=0;i<size;i++) {
            String str = duration.get(0).replaceAll(" ", "");
            int idx = str.indexOf("/");
            str = str.substring(0, idx).replaceAll("[^0-9]", "");
            arr[i] = (Double.parseDouble(str.substring(0, 1)) / 60) + Double.parseDouble(str.substring(1)); //time
            sum += arr[i];
        }
        double avg = sum/size;
        for(int i=0;i<size;i++) {
            arr[i] -= avg;
            dispersion += arr[i]*arr[i];
        }

        return (dispersion/size) ;
    }
}
