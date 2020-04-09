package com.example.ver02;


import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;


public class GetNearbyPlacesData extends AsyncTask<Object, String, String> {

    String googlePlacesData;
    GoogleMap mMap;
    String url;
    LatLng currentPosition;
    ProgressDialog pd;
    int RADIUS;
    String searchType;



    @Override
    protected void onPreExecute() {
        pd = ProgressDialog.show(MainActivity.main, "주변 검색 중...", "초당 API 요청량 초과 시\n2~3분이 소요됩니다.");
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Object... params) {
        try {
            Log.d("GetNearbyPlacesData", "doInBackground entered");

            mMap = (GoogleMap) params[0];
            url = (String) params[1];
            currentPosition = (LatLng) params[2];
            searchType = (String) params[3];

            queryManager q = new queryManager(url);
            googlePlacesData = q.execute();
            RADIUS = q.RADIUS;

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
            pd.dismiss();
            ShowNearbyPlaces(nearbyPlacesList);
        }
        else{
            pd.dismiss();
            Toast.makeText(MainActivity.main, "검색 가능한 지점이 없습니다.", Toast.LENGTH_LONG).show();
        }
        Log.d("GooglePlacesReadTask", "onPostExecute Exit");

    }

    private void ShowNearbyPlaces(List<HashMap<String, String>> nearbyPlacesList) {
            for (int i = 0; i < nearbyPlacesList.size(); i++) {
                Log.d("onPostExecute", "Entered into showing locations");
                MarkerOptions markerOptions = new MarkerOptions();
                HashMap<String, String> googlePlace = nearbyPlacesList.get(i);
                double lat = Double.parseDouble(googlePlace.get("lat"));
                double lng = Double.parseDouble(googlePlace.get("lng"));
                String placeName = googlePlace.get("place_name");
                String vicinity = googlePlace.get("vicinity");
                LatLng latLng = new LatLng(lat, lng);
                markerOptions.position(latLng);
                markerOptions.title(placeName);
                markerOptions.snippet(vicinity);
                markerOptions.alpha((float) 0.9);

                switch(searchType) {
                    case "cafe":
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.cafe));
                        break;
                    case "restaurant":
                        BitmapDrawable bd=(BitmapDrawable)MainActivity.main.getResources().getDrawable(R.drawable.res3);
                        Bitmap b= bd.getBitmap();
                        Bitmap smallMarker = Bitmap.createScaledBitmap(b, 130, 130, false);
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));
                        break;
                    case "store":
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.store));
                        break;
                    case "subway_station":
                        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.subway));
                        break;
                }

                mMap.addMarker(markerOptions);

                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
            }

            CircleOptions circle1KM = new CircleOptions().center(currentPosition) //원점
                .radius(RADIUS)
                .strokeWidth(0f)
                .fillColor(Color.parseColor("#22005017"));


            mMap.addCircle(circle1KM);
    }
}
