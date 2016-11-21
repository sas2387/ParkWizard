package edu.columbia.coms6998.parkwizard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Siddharth on 11/21/2016.
 */
public class AvailableParkingActivity extends FragmentActivity {

    LatLng destLatLng;
    MapView mMapView;
    private GoogleMap googleMap;
    final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 201;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_availableparking);
        mMapView = (MapView) findViewById(R.id.parkingMapView);
        mMapView.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        destLatLng = getIntent().getParcelableExtra("destLatLng");
        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                loadParkingLocations(destLatLng);
            }
        });
    }

    public void loadParkingLocations(LatLng latLng) {

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                //call to backend

                String jsonString = "{\n" +
                        "  \"locations\": [\n" +
                        "    {\n" +
                        "      \"available\": 5,\n" +
                        "      \"location\": {\n" +
                        "        \"lat\": 40.787109,\n" +
                        "        \"lng\": -73.972200\n" +
                        "      },\n" +
                        "      \"name\": \"120th Street\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"available\": 5,\n" +
                        "      \"location\": {\n" +
                        "        \"lat\": 40.78823,\n" +
                        "        \"lng\": -73.976234\n" +
                        "      },\n" +
                        "      \"name\": \"121th Street\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

                return jsonString;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    JSONArray jsonArray = jsonObject.getJSONArray("locations");

                    googleMap.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject outerjo = jsonArray.getJSONObject(i);
                        JSONObject innerjo = outerjo.getJSONObject("location");
                        LatLng latLng = new LatLng(innerjo.getDouble("lat"), innerjo.getDouble("lng"));

                        googleMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(outerjo.getString("name")));
                    }
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 13));
                    if (ContextCompat.checkSelfPermission(AvailableParkingActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(AvailableParkingActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(AvailableParkingActivity.this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
                    } else {
                        googleMap.setMyLocationEnabled(true);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    } else {
                        googleMap.setMyLocationEnabled(true);
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        Criteria criteria = new Criteria();
                    }
                } else {
                    //Log.d(TAG, "Permission not granted");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}