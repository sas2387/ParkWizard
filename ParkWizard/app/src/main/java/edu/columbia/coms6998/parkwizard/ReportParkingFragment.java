package edu.columbia.coms6998.parkwizard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
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
    LatLng selectedLatLng;
    CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
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
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), 15));
                    }
                }
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity().getApplicationContext(),
                "us-west-2:955e3ceb-26d4-4080-92b2-42a0edc2bf7f", // Identity Pool ID
                Regions.US_WEST_2 // Region
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

    public void onDialogPositiveClick(DialogFragment dialog,String location, String spots){
        dialog.getDialog().dismiss();

        //get data and send to sqs
        AmazonSQSClient sqs = new AmazonSQSClient(credentialsProvider);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        sqs.setRegion(usWest2);
        String queueUrl = sqs.listQueues("parkinglocations").getQueueUrls().get(0);

        ParkingLocation pl = new ParkingLocation();
        pl.name = location;
        pl.spots = spots;
        pl.lat = String.valueOf(selectedLatLng.latitude);
        pl.lng = String.valueOf(selectedLatLng.longitude);

        sqs.sendMessage(new SendMessageRequest(queueUrl, pl.toString()));

        Toast.makeText(getActivity(),"Parking Reported",Toast.LENGTH_SHORT).show();
    }

    public void onDialogNegativeClick(DialogFragment dialog){
        dialog.getDialog().dismiss();
    }

}
