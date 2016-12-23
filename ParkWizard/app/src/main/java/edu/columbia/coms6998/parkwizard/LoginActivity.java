package edu.columbia.coms6998.parkwizard;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by Siddharth on 11/21/2016.
 */
public class LoginActivity extends AppCompatActivity implements FacebookCallback<LoginResult> {

    private static final String TAG = "LoginActivity";
    final int MY_PERMISSIONS_REQUEST_INTERNET = 201;

    CallbackManager callbackManager;
    LoginButton loginButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);
        AppEventsLogger.activateApp(this);
        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList("public_profile"));
        loginButton.setReadPermissions("email");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("SUCCESS", "SUCCESS");
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.v("LoginActivity", response.toString());
                                try {
                                    // Application code
                                    String name = object.getString("name");
                                    String userid = object.getString("id");
                                    SharedPreferences sp = getSharedPreferences("USER_PROFILE", MODE_PRIVATE);
                                    sp.edit().putString("name", name).putString("userid", userid).commit();
                                    sendToBackEnd(name, userid);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                request.executeAsync();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });

        callbackManager = CallbackManager.Factory.create();

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken != null){
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));
            LoginManager.getInstance().registerCallback(callbackManager, this);
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET},
                    MY_PERMISSIONS_REQUEST_INTERNET);
        } else {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_INTERNET: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));
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
    public void onSuccess(LoginResult loginResult) {
        Log.d("SUCCESS", "SUCCESS");
        GraphRequest request = GraphRequest.newMeRequest(
                loginResult.getAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.v("LoginActivity", response.toString());
                        try {
                            // Application code
                            String name = object.getString("name");
                            String userid = object.getString("id");
                            SharedPreferences sp = getSharedPreferences("USER_PROFILE", MODE_PRIVATE);
                            sp.edit().putString("name", name).putString("userid", userid).commit();
                            sendToBackEnd(name, userid);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
        request.executeAsync();

    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onError(FacebookException error) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    void sendToBackEnd(final String name, final String userID) {

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                //send details to backend
                StringBuffer response = new StringBuffer();
                int responseCode = 500;

                try {
                    String searchpageurl = getString(R.string.adduserurl);
                    //call to backend
                    String parameters = "id=" + userID + "&name=" + name;
                    URL url = new URL(searchpageurl);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    // optional default is GET
                    con.setRequestMethod("POST");
                    byte[] postData = parameters.getBytes(Charset.forName("UTF-8"));

                    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    wr.write(postData);

                    responseCode = con.getResponseCode();

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

                if (responseCode == 200) {
                    return response.toString();
                } else {
                    return "";
                }

            }

            @Override
            protected void onPostExecute(String response) {
                Log.d("START", "MAIN");

                //handle backend response
                Log.d("LOGIN RESPONSE", response);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean login = jsonObject.getBoolean("success");

                    if (login) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        // move to new screen
                    } else {
                        // same activity
                        Toast.makeText(LoginActivity.this, "Sign In Failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();

    }

}
