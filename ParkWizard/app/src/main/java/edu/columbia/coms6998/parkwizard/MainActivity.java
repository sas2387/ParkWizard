package edu.columbia.coms6998.parkwizard;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    final String TAG = "MainActivity";
    View headerView;
    TextView tvName, tvEmail, tvPoints;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        tvName = (TextView) headerView.findViewById(R.id.tvName);
        tvEmail = (TextView) headerView.findViewById(R.id.tvEmail);
        tvPoints = (TextView) headerView.findViewById(R.id.tvPoints);

        SharedPreferences sp = getSharedPreferences("USER_PROFILE", MODE_PRIVATE);
        tvName.setText(sp.getString("name", ""));
        tvEmail.setText(sp.getString("email", ""));

        updatePoints(sp.getString("userid", ""));

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        FragmentManager fm = getSupportFragmentManager();
        if (id == R.id.search_parking) {
            Log.d(TAG, "SEARCH PARKING");
            SearchParkingFragment searchParkingFragment = new SearchParkingFragment();
            fm.beginTransaction().replace(R.id.content_view, searchParkingFragment).commit();
        } else if (id == R.id.report_parking) {
            Log.d(TAG, "ADD PARKING");
            ReportParkingFragment reportParkingFragment = new ReportParkingFragment();
            fm.beginTransaction().replace(R.id.content_view, reportParkingFragment).commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
