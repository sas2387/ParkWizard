package edu.columbia.coms6998.parkwizard;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.facebook.AccessToken;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Siddharth on 12/22/2016.
 */

public class UpdateParkingFragment extends Fragment {

    ListView listView;
    Button updateBtn;
    Location userLocation;
    final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 201;
    ArrayList<Info> parkingLocationInfos = new ArrayList<>();
    boolean loaded = false;
    private static String PROPERTY_REG_ID = "registration_id";
    CognitoCachingCredentialsProvider credentialsProvider;
    CustomAdapter adapter;

    int selectedPosition = -1;

    class Info {
        String locid;
        String name;
        int available;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View rootView = layoutInflater.inflate(R.layout.fragment_updateparking, viewGroup, false);
        listView = (ListView) rootView.findViewById(R.id.lvParkingLocations);
        updateBtn = (Button) rootView.findViewById(R.id.btnUpdate);

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity().getApplicationContext(),
                "us-east-1:23ace8aa-c8e6-4a67-ae5c-3e463343d6e6", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        Map<String, String> logins = new HashMap<String, String>();
        logins.put("graph.facebook.com", AccessToken.getCurrentAccessToken().getToken());
        credentialsProvider.setLogins(logins);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        getUserLocation();
        if (!loaded) {
            if (userLocation != null) {
                loadParkingLocations();
            } else {
                Toast.makeText(getActivity(), "We need your location", Toast.LENGTH_SHORT).show();
            }
            loaded = true;
        }
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateLocationData();
            }
        });
    }

    void updateLocationData() {
        SharedPreferences sp1 = getActivity().getSharedPreferences("USER_PROFILE", Context.MODE_PRIVATE);
        String userid = sp1.getString("userid", "");
        SharedPreferences sp2 = getActivity().getSharedPreferences("updates_" + userid, Context.MODE_PRIVATE);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        int times = sp2.getInt(sdf.format(new Date()), 0);
        Log.d("TIMES", "" + times);
        if (times > 4) {
            Toast.makeText(getActivity(), "You can submit only 5 updates per day", Toast.LENGTH_SHORT).show();
            selectedPosition = -1;
            adapter.notifyDataSetChanged();
            return;
        }
        if (selectedPosition != -1) {
            DialogFragment newFragment = new UpdateDialogFragment();
            newFragment.setTargetFragment(UpdateParkingFragment.this, 200);
            newFragment.show(getFragmentManager(), "update");
        }
    }

    void getUserLocation() {
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
        } else {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            userLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        }
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
                        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                        Criteria criteria = new Criteria();
                        userLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
                    }
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    void loadParkingLocations() {

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {

                StringBuffer response = new StringBuffer();

                try {
                    String searchpageurl = getString(R.string.getupdatelocationsurl);

                    // Read user id from the config file
                    SharedPreferences sp = getActivity().getSharedPreferences("USER_PROFILE", Context.MODE_PRIVATE);
                    String userid = sp.getString("userid", "");

                    //call to backend
                    String parameters = "?id=" + userid + "&lat=" + userLocation.getLatitude() + "&lon=" + userLocation.getLongitude();
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
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                //update recyclerview
                Log.d("JSON", s);
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    JSONArray jsonArray = jsonObject.getJSONArray("parkings");

                    if (jsonArray.length() == 0) {
                        Toast.makeText(getActivity().getApplicationContext(), jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                    }

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject outerjo = jsonArray.getJSONObject(i);
                        JSONObject innerjo = outerjo.getJSONObject("location");
                        //LatLng latLng = new LatLng(innerjo.getDouble("lat"), innerjo.getDouble("lon"));
                        String id = outerjo.getString("locid");
                        String name = outerjo.getString("name");

                        UpdateParkingFragment.Info information = new Info();
                        information.name = name;
                        information.locid = id;
                        parkingLocationInfos.add(information);
                    }

                    adapter = new CustomAdapter(getActivity(), R.layout.card_parking);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            selectedPosition = i;
                            adapter.notifyDataSetChanged();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    public class CustomAdapter extends BaseAdapter {
        // store the context (as an inflated layout)
        private LayoutInflater inflater;
        // store the resource (typically list_item.xml)
        private int resource;

        public CustomAdapter(Context context, int resource) {
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.resource = resource;
        }

        public int getCount() {
            return parkingLocationInfos.size();
        }

        public Object getItem(int position) {
            return parkingLocationInfos.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // reuse a given view, or inflate a new one from the xml
            View view;

            if (convertView == null) {
                view = this.inflater.inflate(resource, parent, false);
            } else {
                view = convertView;
            }

            return this.bindData(view, position);
        }

        public View bindData(View view, int position) {
            // make sure it's worth drawing the view
            if (parkingLocationInfos.get(position) == null) {
                return view;
            }
            Info item = parkingLocationInfos.get(position);
            TextView tv = (TextView) view.findViewById(R.id.tv_parking);
            tv.setText(item.name);
            view.setClickable(false);
            if (position == selectedPosition) {
                view.setBackgroundColor(Color.parseColor("#2B60DE"));
                tv.setTextColor(Color.WHITE);
            } else {
                view.setBackgroundColor(Color.WHITE);
                tv.setTextColor(Color.BLACK);
            }

            return view;
        }
    }

    public void onDialogPositiveClick(DialogFragment dialog, final int available) {
        dialog.getDialog().dismiss();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                //get data and send to sqs
                AmazonSQSClient sqs = new AmazonSQSClient(credentialsProvider);
                Region usEast1 = Region.getRegion(Regions.US_EAST_1);
                sqs.setRegion(usEast1);
                String queueUrl = sqs.listQueues("parkinglocations").getQueueUrls().get(0);

                SharedPreferences sp = getActivity().getSharedPreferences("USER_PROFILE", Context.MODE_PRIVATE);
                String userid = sp.getString("userid", "");
                SharedPreferences gcmprefs = getActivity().getSharedPreferences("GCM", Context.MODE_PRIVATE);
                String regid = gcmprefs.getString(PROPERTY_REG_ID, "");

                ParkingLocation pl = new ParkingLocation();
                pl.id = userid;
                pl.locid = parkingLocationInfos.get(selectedPosition).locid;
                pl.type = "update";
                pl.available = available;
                pl.regid = regid;
                try {
                    Gson gson = new Gson();
                    String jsonInString = gson.toJson(pl);
                    sqs.sendMessage(new SendMessageRequest(queueUrl, jsonInString));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                selectedPosition = -1;
                adapter.notifyDataSetChanged();
                Toast.makeText(getActivity(), "Parking Updated", Toast.LENGTH_SHORT).show();
                SharedPreferences sp1 = getActivity().getSharedPreferences("USER_PROFILE", Context.MODE_PRIVATE);
                String userid = sp1.getString("userid", "");
                SharedPreferences sp2 = getActivity().getSharedPreferences("updates_" + userid, Context.MODE_PRIVATE);
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                String today = sdf.format(new Date());
                if (!sp2.contains(today)) {
                    sp2.edit().clear().putInt(today, 1).commit();
                } else {
                    int times = sp2.getInt(today, 0);
                    sp2.edit().putInt(today, ++times).commit();
                }
            }
        }.execute();
    }

    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.getDialog().dismiss();
    }
}
