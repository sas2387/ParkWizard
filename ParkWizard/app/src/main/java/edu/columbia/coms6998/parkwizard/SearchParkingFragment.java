package edu.columbia.coms6998.parkwizard;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Siddharth on 11/21/2016.
 */
public class SearchParkingFragment extends Fragment {
    Button searchDestinationButton, searchParkingButton;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    final int RESULT_OK = -1;
    final int RESULT_CANCEL = 0;
    final String TAG = "SearchParkingFragment";
    final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 201;
    Location userLocation;
    LatLng destLatLng;
    MapView mMapView;
    private GoogleMap googleMap;


    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View rootView = layoutInflater.inflate(R.layout.fragment_searchdestination, viewGroup, false);
        searchDestinationButton = (Button) rootView.findViewById(R.id.btSearchDestination);
        searchParkingButton = (Button) rootView.findViewById(R.id.btSearchParking);
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

                // For showing a move to my location button
                if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
                } else {
                    googleMap.setMyLocationEnabled(true);
                    LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    userLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                    if (userLocation != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), 13));
                    }
                }
            }
        });

        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    } else {
                        googleMap.setMyLocationEnabled(true);
                        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                        Criteria criteria = new Criteria();
                        userLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                        if (userLocation != null) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), 20));
                        }
                    }
                } else {
                    Log.d(TAG, "Permission not granted");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("START", "start called");
        searchDestinationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build(getActivity());
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        searchParkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (destLatLng != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage("This will deduct 1 point from your account");
                    // Add the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                            Intent intent = new Intent(getActivity(), AvailableParkingActivity.class);
                            intent.putExtra("destLatLng", destLatLng);
                            startActivity(intent);
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
                    // Set other dialog properties

                    // Create the AlertDialog
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    Toast.makeText(getContext(), "Please select destination", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(getActivity(), data);
                destLatLng = place.getLatLng();
                Log.d(TAG, "lat:" + destLatLng.latitude);
                Log.d(TAG, "lng:" + destLatLng.longitude);
                Log.i(TAG, "Place: " + place.getName());
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions()
                        .position(destLatLng)
                        .title(place.getName().toString()));
                if (userLocation != null) {
                    LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                    LatLngBounds RANGE;
                    if (destLatLng.latitude > userLatLng.latitude) {
                        RANGE = new LatLngBounds(userLatLng, destLatLng);
                    } else {
                        RANGE = new LatLngBounds(destLatLng, userLatLng);
                    }
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(RANGE, 100));
                } else {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 20));
                }

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(getActivity(), data);
                // TODO: Handle the error.
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCEL) {
                Log.d(TAG, "CANCEL");
                // The user canceled the operation.
            }
        }
    }
}
