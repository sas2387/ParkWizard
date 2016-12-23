package edu.columbia.coms6998.parkwizard;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,GoogleApiClient.OnConnectionFailedListener {

    final String TAG = "MainActivity";
    View headerView;
    TextView tvName, tvEmail, tvPoints;
    final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS=201;
    private String SENDER_ID = "494925756460";
    private String regid;
    SharedPreferences gcmprefs;
    private GoogleCloudMessaging gcm;
    private static String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int RC_SIGN_IN = 9001;
    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        headerView = navigationView.getHeaderView(0);

        onNavigationItemSelected(navigationView.getMenu().getItem(0));

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("494925756460-u3jofjt7nn57pnb612vjnq4rgo29ocgi.apps.googleusercontent.com")
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
        tvName = (TextView) headerView.findViewById(R.id.tvName);
        tvEmail = (TextView) headerView.findViewById(R.id.tvEmail);
        tvPoints = (TextView) headerView.findViewById(R.id.tvPoints);

        SharedPreferences sp = getSharedPreferences("USER_PROFILE", MODE_PRIVATE);
        tvName.setText(sp.getString("name", ""));
        tvEmail.setText(sp.getString("email", ""));

        updatePoints(sp.getString("userid", ""));
        createAWSCredentials();
        doGCMStuff();
    }

    void doGCMStuff() {
        gcmprefs = getSharedPreferences("GCM",MODE_PRIVATE);

        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId();
            Log.d("STORED REGID",regid);
            if (regid.isEmpty()) {
                if (ConnectionDetector.checkConnection(this)) {
                    registerInBackground();
                }
            }
        }else{
            Log.d("GCM","NO PLAY SERVICES");
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                //Log.i("GCM", "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private String getRegistrationId() {
        String registrationId = gcmprefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = gcmprefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(getBaseContext());
        if (registeredVersion != currentVersion) {
            //unregisterGCM(registrationId);
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private void registerInBackground() {

        new AsyncTask<Void,Void,String>() {

            private ProgressDialog pDialog;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                pDialog = new ProgressDialog(MainActivity.this);
                pDialog.setMessage("Registering your device...\nPlease wait...");
                pDialog.setIndeterminate(false);
                pDialog.setCancelable(false);
                pDialog.show();
            }


            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(getBaseContext());
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid+"\n";
                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.

                    //sendRegistrationIdToBackend(regid);

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(getBaseContext(), regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                pDialog.dismiss();
                Log.d("GOT FROM GOOGLE REGID",regid);
            }
        }.execute();
    }

    private void storeRegistrationId(Context context, String regId) {
        int appVersion = getAppVersion(context);
        //Log.i("GCM", "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = gcmprefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        FragmentManager fm = getSupportFragmentManager();
        if (id == R.id.search_parking) {
            Log.d(TAG, "SEARCH PARKING");
            setTitle("Search Parking Location");
            SearchParkingFragment searchParkingFragment = new SearchParkingFragment();
            fm.beginTransaction().replace(R.id.content_view, searchParkingFragment).commit();
        } else if (id == R.id.report_parking) {
            Log.d(TAG, "ADD PARKING");
            setTitle("Report Parking Location");
            ReportParkingFragment reportParkingFragment = new ReportParkingFragment();
            fm.beginTransaction().replace(R.id.content_view, reportParkingFragment).commit();
        } else if(id == R.id.update_parking_menu) {
            Log.d(TAG, "UPDATE PARKING");
            setTitle("Update Parking Location");
            UpdateParkingFragment updateParkingFragment = new UpdateParkingFragment();
            fm.beginTransaction().replace(R.id.content_view, updateParkingFragment).commit();
        } else if(id == R.id.signout_menu) {

            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            SharedPreferences sp = getSharedPreferences("USER_PROFILE",MODE_PRIVATE);
                            sp.edit().clear().commit();
                            finish();
                        }
                    });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,"in result");
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            String token = result.getSignInAccount().getIdToken();

            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... voids) {
                    Log.d("COGNITO","Getting credentials");
                    CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                            getApplicationContext(),
                            "us-east-1:23ace8aa-c8e6-4a67-ae5c-3e463343d6e6", // Identity Pool ID
                            Regions.US_EAST_1 // Region
                    );

                    if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.GET_ACCOUNTS)
                            != PackageManager.PERMISSION_GRANTED ){
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{android.Manifest.permission.GET_ACCOUNTS},
                                MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
                    } else {
                        try {
                            Map<String, String> logins = new HashMap<String, String>();
                            logins.put("accounts.google.com", token);
                            Log.d(TAG, logins.toString());
                            credentialsProvider.setLogins(logins);
                            Log.d("COGNITO","Stored:"+credentialsProvider.getLogins().toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (GoogleAuthException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
            }.execute();
        }
    }

    void createAWSCredentials() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);



    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_GET_ACCOUNTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.GET_ACCOUNTS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{android.Manifest.permission.GET_ACCOUNTS},
                                MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
                    } else {
                        createAWSCredentials();
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

    private void updatePoints(final String userid) {

        new AsyncTask<Void, Void, String>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(Void... voids) {
                StringBuffer response = new StringBuffer();

                try {
                    String getpointsurl = getString(R.string.getpointsurl);

                    //call to backend
                    String parameters = "?id=" + userid;
                    URL url = new URL(getpointsurl + parameters);
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
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    boolean success = jsonObject.getBoolean("success");
                    if (success) {
                        int points = jsonObject.getInt("score");
                        tvPoints.setText("Points: " + points);
                    } else {
                        tvPoints.setText("Points: N/A");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();

    }

}
