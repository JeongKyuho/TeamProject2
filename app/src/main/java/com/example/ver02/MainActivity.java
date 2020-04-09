package com.example.ver02;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.compat.Place;
import com.google.android.libraries.places.compat.ui.PlaceSelectionListener;
import com.google.android.libraries.places.compat.ui.SupportPlaceAutocompleteFragment;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener
{
    public static Activity main;
    public static int RADIUS = 1000;
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
    List<Marker> previousMarker;
    private GoogleMap mMap;
    SupportPlaceAutocompleteFragment placeAutoComplete = null;
    LatLng currentPosition;
    private double peopleCount;
    List<Location> locationForCenter;
    private Marker centerMarker;
    private boolean isShowingNearby;
    private MarkerOptions centerMarkerOption;

    List<String> duration;
    Boolean calCenterState;
    List<Polyline> polylineList;
    List<PatternItem> pattern;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        placeAutoComplete = (SupportPlaceAutocompleteFragment) getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment) ;
        placeAutoComplete.setCountry("KR");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();
        main = this;

        placeAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Location location = new Location("");
                location.setLatitude(place.getLatLng().latitude);
                location.setLongitude(place.getLatLng().longitude);

                setCurrentLocation(location, place.getName().toString(), place.getAddress().toString());
                locationForCenter.add(location);

                //if(peopleCount >=2 )
                //    calCenter();

                Log.d("Maps", "Place selected: " + place.getName());

            }

            @Override
            public void onError(Status status) {
                Log.d("Maps", "An error occurred: " + status);
            }
        });

        ImageButton centerButton = (ImageButton) findViewById(R.id.setCenterLocation);
        centerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isOverOnePlace()){
                    calCenter();
                    //setCenterLocation();
                    //showRoute(locationForCenter, currentPosition);

                }
            }
        });

        ImageButton aroundButton = (ImageButton)findViewById(R.id.searchAroundButton);
        aroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isShowingNearby){
                    hideNearby();
                }
                else if(isOverOnePlace()){
                    final CharSequence[] items = {"카페", "식당", "지하철", "상점"};
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

                    //alertDialogBuilder.setTitle("");
                    alertDialogBuilder.setItems(items,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    switch (id) {
                                        case 0:
                                            showNearby("cafe");
                                            break;
                                        case 1:
                                            showNearby("restaurant");
                                            break;
                                        case 2:
                                            showNearby("subway_station");
                                            break;
                                        case 3:
                                            showNearby("store");
                                            break;
                                    }
                                    dialog.dismiss();
                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        });

        ImageButton resetButton = (ImageButton)findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                init();
                mMap.clear();
            }
        });
    }

    public void init(){
        /* init */
        currentPosition = DEFAULT_LOCATION;

        locationForCenter = new ArrayList<>();
        previousMarker = new ArrayList<>();
        duration = new ArrayList<>();
        polylineList = new ArrayList<>();

        centerMarker = null;
        centerMarkerOption = null;

        isShowingNearby = false;
        calCenterState = false;

        pattern = Arrays.<PatternItem>asList(new Dash(45), new Gap(20), new Dash(45), new Gap(20) );
        peopleCount = 0;
    }

    public void calCenter(){
        double latitudeSum = 0;
        double longitudeSum = 0;
        Location centerLocation = new Location("");
        for (Location location : locationForCenter) {
            latitudeSum += location.getLatitude();
            longitudeSum += location.getLongitude();
        }

        centerLocation.setLatitude(latitudeSum / peopleCount);
        centerLocation.setLongitude(longitudeSum / peopleCount);

        String url = getUrl(centerLocation.getLatitude(), centerLocation.getLongitude(),"bus_station", 1000);
        Object[] DataTransfer = new Object[2];
        DataTransfer[0] = mMap;
        DataTransfer[1] = url;
        Log.d("onClick", url);
        GetCenterPlacesData getCenterPlacesData = new GetCenterPlacesData();
        getCenterPlacesData.execute(DataTransfer);
    }

    public class GetCenterPlacesData extends AsyncTask<Object, String, String> {

        String googlePlacesData;
        GoogleMap mMap;
        String url;
        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(MainActivity.main, "중간 지점 검색 중...", "초당 API 요청량 초과 시\n2~3분이 소요됩니다.");
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d("GetNearbyPlacesData", "doInBackground entered");
                mMap = (GoogleMap) params[0];
                url = (String) params[1];
/*
                DownloadUrl downloadUrl = new DownloadUrl();
                googlePlacesData = downloadUrl.readUrl(url);
*/
                queryManager q = new queryManager(url);
                googlePlacesData = q.execute();

                Log.d("GooglePlacesReadTask", "doInBackground Exit");
            } catch (Exception e) {
                Log.d("GooglePlacesReadTask", e.toString());
            }
            return googlePlacesData;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("GooglePlacesReadTask", "onPostExecute Entered");
            List<HashMap<String, String>> nearbyPlacesList = null;
            DataParser dataParser = new DataParser();

            if(result != null) {
                nearbyPlacesList = dataParser.parse(result);
                searchPlaces(nearbyPlacesList);
            }
            else{
                calCenterState = false;
                Toast.makeText(MainActivity.main, "검색된 지점이 없습니다.\n위치 상 중간 지점을 선택합니다.", Toast.LENGTH_LONG).show();
            }

            setCenterLocation();
            showRoute(locationForCenter, currentPosition);
            pd.dismiss();

            Log.d("GooglePlacesReadTask", "onPostExecute Exit");
        }

        private void searchPlaces(List<HashMap<String, String>> nearbyPlacesList) {
            try{
                Log.d("onPostExecute", "Entered into showing locations");
                MarkerOptions markerOptions = new MarkerOptions();

                HashMap<String, String> googlePlace = nearbyPlacesList.get(calDistance(nearbyPlacesList));

                double lat = Double.parseDouble(googlePlace.get("lat"));
                double lng = Double.parseDouble(googlePlace.get("lng"));
                String placeName = googlePlace.get("place_name");
                String vicinity = googlePlace.get("vicinity");
                LatLng latLng = new LatLng(lat, lng);
                markerOptions.position(latLng);
                markerOptions.title(placeName);
                markerOptions.snippet(vicinity);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getIcon("center")));

                centerMarkerOption = markerOptions;
                calCenterState = true;
            }
            catch(IndexOutOfBoundsException e){
                calCenterState = false;
            }
        }

        private int calDistance(List<HashMap<String,String>> placesList){
            int min_idx = 0;
            double minDispersion=1000;
            List<LatLng> locList = new ArrayList<>();

            try {
                for (int i = 0; i < placesList.size(); i++) {
                    HashMap<String, String> temp = placesList.get(i);
                    double lat = Double.parseDouble(temp.get("lat"));
                    double lng = Double.parseDouble(temp.get("lng"));
                    locList.add(new LatLng(lat, lng));

                    CompareCenter compareCenter = new CompareCenter(locationForCenter, locList.get(i));

                    if (i == 0)
                        minDispersion = compareCenter.dispersion;
                    if (compareCenter.dispersion <= minDispersion) {
                        min_idx = i;
                        Log.d("비교" , i + "번째 : " + compareCenter.dispersion + "와 min_idx : " + minDispersion + " 비교");
                    }
                }
            }catch(IndexOutOfBoundsException e){
                Toast.makeText(MainActivity.this, "거리 편차 계산 오류.", Toast.LENGTH_LONG).show();
                calCenterState = false;
            }

            return min_idx;
        }
    }

    private void hideNearby(){
        mMap.clear();

        for (Marker marker : previousMarker) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(marker.getPosition());
            markerOptions.title(marker.getTitle());
            markerOptions.snippet(marker.getSnippet());
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getIcon("person")));
            mMap.addMarker(markerOptions);
        }
        if (centerMarker != null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(currentPosition);
            markerOptions.title(centerMarker.getTitle());
            markerOptions.snippet(centerMarker.getSnippet());
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getIcon("center")));
            mMap.addMarker(markerOptions);
        }
        for(int i=0;i<polylineList.size();i++){
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.addAll(polylineList.get(i).getPoints());
            polylineOptions.pattern(pattern);
            polylineOptions.width(20);
            polylineOptions.color(Color.rgb(127,127,127));
            polylineList.set(i,mMap.addPolyline(polylineOptions));
        }
        Toast.makeText(MainActivity.this, "표시 안함", Toast.LENGTH_LONG).show();
        isShowingNearby = false;
    }

    private void showNearby(String place){
        if(currentPosition!=null && isOverOnePlace()) {
            if (!isShowingNearby) {
                Log.d("onClick", "Button is Clicked");
                String url = getUrl(currentPosition.latitude, currentPosition.longitude, place, RADIUS);
                Object[] DataTransfer = new Object[4];
                DataTransfer[0] = mMap;
                DataTransfer[1] = url;
                DataTransfer[2] = currentPosition;
                DataTransfer[3] = place;
                Log.d("onClick", url);

                GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
                getNearbyPlacesData.execute(DataTransfer);

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 14));
                isShowingNearby = true;
            }
        }
    }

    /* Route code */

    private void showRoute(List<Location> locationList, LatLng pos){
        duration.clear();
        removePoly();

        for(int i=0;i<locationList.size();i++) {
            LatLng origin = new LatLng(locationList.get(i).getLatitude(),
                    locationList.get(i).getLongitude());
            LatLng dest = pos;

            String url = getRouteUrl(origin, dest);
            Log.d("onMapClick", url.toString());
            FetchUrl fetchUrl = new FetchUrl();

            fetchUrl.execute(url);
        }
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
    private class FetchUrl extends AsyncTask<String, Void, String> {

        ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(MainActivity.main, "중간 지점 검색 중...", "경로를 탐색 중입니다.");
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... url) {
            String data = "";

            try {
                data = downloadRouteUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            RouteParserTask parserTask = new RouteParserTask();

            parserTask.execute(result);

            pd.dismiss();
        }
    }
    private String downloadRouteUrl(String strUrl) throws IOException {
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

    private class RouteParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

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
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(20);
                lineOptions.color(Color.rgb(127,127,127));
                lineOptions.clickable(true);

                lineOptions.pattern(pattern);

                Log.d("onPostExecute","onPostExecute lineoptions decoded");

            }

            if(lineOptions != null) {
                polylineList.add(mMap.addPolyline(lineOptions));
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }

    /* Route code end */

    private String getUrl(double latitude, double longitude, String nearbyPlace, int radius) {

        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        googlePlacesUrl.append("&radius=" + radius);
        googlePlacesUrl.append("&type=" + nearbyPlace);
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + "AIzaSyATuUiZUkEc_UgHuqsBJa1oqaODI-3mLs0");
        Log.d("getUrl", googlePlacesUrl.toString());
        return (googlePlacesUrl.toString());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        UiSettings mapSettings;
        mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);
        mapSettings.setTiltGesturesEnabled(false);

        LatLng SEOUL = new LatLng(37.56, 126.97);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(7));
        mMap.setOnMarkerClickListener(this);
    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {
        if ( location != null) {

            LatLng currentLocation = new LatLng( location.getLatitude(), location.getLongitude());

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(currentLocation);
            markerOptions.title(markerTitle);
            markerOptions.snippet(markerSnippet);
            markerOptions.draggable(true);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getIcon("person")));

            Marker marker = mMap.addMarker(markerOptions);
            previousMarker.add(marker);
            marker.showInfoWindow();

            peopleCount++;
            return;
        }

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        mMap.addMarker(markerOptions);
    }

    public void setCenterLocation() {
        double latitudeSum = 0;
        double longitudeSum = 0;
        Location location = new Location("");
        for (Location loc : locationForCenter) {
            latitudeSum += loc.getLatitude();
            longitudeSum += loc.getLongitude();
        }

        location.setLatitude(latitudeSum / peopleCount);
        location.setLongitude(longitudeSum / peopleCount);

        if(centerMarker != null){
            centerMarker.remove();
            centerMarker = null;
        }

        if ( location != null) {

            if(calCenterState) {
                centerMarker = mMap.addMarker(centerMarkerOption);
                centerMarker.showInfoWindow();
                LatLng currentLocation = new LatLng(centerMarkerOption.getPosition().latitude, centerMarkerOption.getPosition().longitude);
                currentPosition = currentLocation;

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 9));
            }
            else{
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                currentPosition = currentLocation;

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(currentLocation);

                markerOptions.draggable(true);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getIcon("center")));
                centerMarker = mMap.addMarker(markerOptions);

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 9));

                Toast.makeText(MainActivity.this, "찾을 수 있는 중간 지점이 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker){
        if(centerMarker!=null) {
            for (int i = 0; i < previousMarker.size(); i++) {
                previousMarker.get(i).setSnippet(duration.get(i));
            }
        }
        marker.showInfoWindow();

        return true;
    }

    public boolean isOverOnePlace(){
        if(peopleCount < 2) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.setMessage("두 개 이상의 위치가 설정되어야 합니다.");
            alert.show();
            return false;
        }
        else
            return true;
    }

    public void removePoly(){
        for(int i=0;i<polylineList.size();i++) {
            polylineList.get(i).setVisible(false);
        }
    }

    public Bitmap getIcon(String source){
        int id;
        int width;
        int height;

        switch (source){
            case "person" :
                id = R.drawable.person;
                width = 200; height = 200;
                break;
            case "res" :
                id = R.drawable.res;
                width = 100; height = 100;
                break;
            case "center" :
                id = R.drawable.center;
                width = 200; height = 200;
                break;
            default :
                id = R.drawable.person;
                width = 200; height = 200;
                break;
        }

        BitmapDrawable bd=(BitmapDrawable)getResources().getDrawable(id);
        Bitmap b= bd.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        return smallMarker;
    }

}

