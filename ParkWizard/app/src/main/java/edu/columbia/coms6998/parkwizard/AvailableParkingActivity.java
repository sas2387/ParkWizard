package edu.columbia.coms6998.parkwizard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by Siddharth on 11/21/2016.
 */
public class AvailableParkingActivity extends AppCompatActivity implements GoogleMap.OnMarkerClickListener {

    LatLng destLatLng;
    MapView mMapView;
    private GoogleMap googleMap;
    final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 201;
    HashMap<Marker,Info> markerInfos = new HashMap();
    Marker currentMarker;
    Button btUsingThis;
    CognitoCachingCredentialsProvider credentialsProvider;

    class Info{
        String id;
        int available;
        double lat;
        double lng;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_availableparking);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Available Parking Spots");
        setSupportActionBar(toolbar);
        mMapView = (MapView) findViewById(R.id.parkingMapView);
        mMapView.onCreate(savedInstanceState);

        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                // ProjectsActivity is my 'home' activity
                finish();
                return true;
        }
        return (super.onOptionsItemSelected(menuItem));
    }

    @Override
    protected void onStart() {
        super.onStart();
        setTitle("Available Parking Spots");
        btUsingThis = (Button) findViewById(R.id.btUsingThis);
        destLatLng = getIntent().getParcelableExtra("destLatLng");
        mMapView.onResume(); // needed to get the map to display immediately

        btUsingThis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentMarker != null){
                    Info information = markerInfos.get(currentMarker);
                    updateParking(information);
                }else{
                    Toast.makeText(AvailableParkingActivity.this,"Please select a location",Toast.LENGTH_SHORT).show();
                }
            }
        });

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:23ace8aa-c8e6-4a67-ae5c-3e463343d6e6", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        try {
            MapsInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                loadParkingLocations(destLatLng);

                mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                    @Override
                    public View getInfoWindow(Marker arg0) {
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {

                        LinearLayout info = new LinearLayout(AvailableParkingActivity.this);
                        info.setOrientation(LinearLayout.VERTICAL);

                        TextView title = new TextView(AvailableParkingActivity.this);
                        title.setTextColor(Color.BLACK);
                        title.setGravity(Gravity.CENTER);
                        title.setTypeface(null, Typeface.BOLD);
                        title.setText(marker.getTitle());

                        TextView snippet = new TextView(AvailableParkingActivity.this);
                        snippet.setTextColor(Color.GRAY);
                        snippet.setText(marker.getSnippet());

                        info.addView(title);
                        info.addView(snippet);

                        return info;
                    }
                });

            }


        });
    }

    public void loadParkingLocations(final LatLng latLng) {

        new AsyncTask<Void, Void, String>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(Void... voids) {
                StringBuffer response = new StringBuffer();

                try {
                    String searchpageurl = getString(R.string.searchparkingurl);

                    // Read user id from the config file
                    SharedPreferences sp = getSharedPreferences("USER_PROFILE", MODE_PRIVATE);
                    String userid = sp.getString("userid", "");

                    //call to backend
                    String parameters = "?id=" + userid + "&lat=" + latLng.latitude + "&lon=" + latLng.longitude;
                    URL url = new URL(searchpageurl + parameters);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    // optional default is GET
                    con.setRequestMethod("GET");

                    int responseCode = con.getResponseCode();

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return response.toString();
                //return jsonString;
            }

            @Override
            protected void onPostExecute(String s) {
                Log.d("SEARCH PARKING", s);
                super.onPostExecute(s);
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    JSONArray jsonArray = jsonObject.getJSONArray("parkings");

                    if(jsonArray.length() == 0){
                        finish();
                        Toast.makeText(getApplicationContext(), jsonObject.getString("message"),Toast.LENGTH_SHORT).show();
                    }

                    googleMap.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject outerjo = jsonArray.getJSONObject(i);
                        JSONObject innerjo = outerjo.getJSONObject("location");
                        LatLng latLng = new LatLng(innerjo.getDouble("lat"), innerjo.getDouble("lon"));
                        String  id = outerjo.getString("locid");
                        String name = outerjo.getString("name");
                        int spots = outerjo.getInt("spots");
                        int available = outerjo.getInt("available");
                        Info information = new Info();
                        information.id = id;
                        information.available = available;
                        information.lat = latLng.latitude;
                        information.lng = latLng.longitude;

                        googleMap.setOnMarkerClickListener(AvailableParkingActivity.this);

                        Marker marker = googleMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(name)
                                .snippet("Spots:" + spots + "\nAvailable:" + available));

                        markerInfos.put(marker,information);
                    }
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 18));
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

    @Override
    public boolean onMarkerClick(Marker marker) {
        currentMarker = marker;
        return false;
    }

    void updateParking(final Info info){

        new AsyncTask<Void, Void, Void>(){

            protected Void doInBackground(Void... voids) {
                //get data and send to sqs
                AmazonSQSClient sqs = new AmazonSQSClient(credentialsProvider);
                Region usEast1 = Region.getRegion(Regions.US_EAST_1);
                sqs.setRegion(usEast1);
                String queueUrl = sqs.listQueues("parkinglocations").getQueueUrls().get(0);

                SharedPreferences sp = getSharedPreferences("USER_PROFILE",Context.MODE_PRIVATE);
                String userid = sp.getString("userid","");

                ParkingLocation pl = new ParkingLocation();
                pl.id = userid;
                pl.type = "use";
                pl.available = --info.available;
                pl.locid = info.id;
                try {
                    Gson gson = new Gson();
                    String jsonInString = gson.toJson(pl);
                    sqs.sendMessage(new SendMessageRequest(queueUrl, jsonInString));
                }catch (Exception e){
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Uri gmmIntentUri = Uri.parse("google.navigation:q="+info.lat+","+info.lng+"&mode=b");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
                finish();
            }
        }.execute();

    }


}