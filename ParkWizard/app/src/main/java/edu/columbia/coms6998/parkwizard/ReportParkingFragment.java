package edu.columbia.coms6998.parkwizard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.util.List;

/**
 * Created by Siddharth on 12/21/2016.
 */

public class ReportParkingFragment extends Fragment{

    Button addParkingButton;
    MapView mMapView;
    private GoogleMap googleMap;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    final int RESULT_OK = -1;
    final int RESULT_CANCEL = 0;
    final String TAG = "ReportParkingFragment";
    Location userLocation;
    final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 201;
    final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 202;
    private static String PROPERTY_REG_ID = "registration_id";
    LatLng selectedLatLng;
    CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Log.d("DEBUG","oncreate called");
        View rootView = layoutInflater.inflate(R.layout.fragment_reportparking, viewGroup, false);
        addParkingButton = (Button) rootView.findViewById(R.id.btAddParking);
        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(bundle);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

                    @Override
                    public void onMapClick(LatLng latLng) {
                        Log.d(TAG,latLng.toString());
                        // TODO Auto-generated method stub
                        String address=null;
                        try {
                            //fetch corresponding address
                            Geocoder geoCoder = new Geocoder(getActivity());
                            List<Address> matches = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                            Address bestMatch = (matches.isEmpty() ? null : matches.get(0));
                            if(bestMatch != null) {
                                address = bestMatch.getThoroughfare();
                            }
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                        //remove all markers
                        googleMap.clear();

                        selectedLatLng = latLng;

                        //add marker
                        googleMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(address));
                    }
                });

                // For showing a move to my location button
                if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
                } else {
                    googleMap.setMyLocationEnabled(true);
                    LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    userLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                    if (userLocation != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), 18));
                    }
                }
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("COGNITO","Cached credentials should be present");
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity().getApplicationContext(),
                "us-east-1:23ace8aa-c8e6-4a67-ae5c-3e463343d6e6", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        addParkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedLatLng!=null) {
                    //make a dialog
                    DialogFragment newFragment = new ReportParkingDialogFragment();
                    newFragment.setTargetFragment(ReportParkingFragment.this,200);
                    newFragment.show(getFragmentManager(), "report");
                }else{
                    Toast.makeText(getActivity(),"Please select a location",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onDialogPositiveClick(DialogFragment dialog,final String location, final int spots){
        dialog.getDialog().dismiss();

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                //get data and send to sqs
                AmazonSQSClient sqs = new AmazonSQSClient(credentialsProvider);
                Region usEast1 = Region.getRegion(Regions.US_EAST_1);
                sqs.setRegion(usEast1);
                String queueUrl = sqs.listQueues("parkinglocations").getQueueUrls().get(0);

                SharedPreferences sp = getActivity().getSharedPreferences("USER_PROFILE",Context.MODE_PRIVATE);
                String userid = sp.getString("userid","");
                SharedPreferences gcmprefs = getActivity().getSharedPreferences("GCM",Context.MODE_PRIVATE);
                String regid = gcmprefs.getString(PROPERTY_REG_ID, "");

                ParkingLocation pl = new ParkingLocation();
                pl.id = userid;
                pl.type = "report";
                pl.name = location;
                pl.spots = spots;
                pl.regid = regid;
                pl.lat = String.valueOf(selectedLatLng.latitude);
                pl.lon = String.valueOf(selectedLatLng.longitude);
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
                Toast.makeText(getActivity(),"Parking Reported",Toast.LENGTH_SHORT).show();
            }
        }.execute();

    }

    public void onDialogNegativeClick(DialogFragment dialog){
        dialog.getDialog().dismiss();
    }

}
