package edu.columbia.coms6998.parkwizard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Created by Siddharth on 11/21/2016.
 */
public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    GoogleApiClient mGoogleApiClient;
    SignInButton signInButton;
    final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 201;
    String name;
    String email;
    String userID;


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("494925756460-u3jofjt7nn57pnb612vjnq4rgo29ocgi.apps.googleusercontent.com")
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
// options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn(null);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        /*SharedPreferences sp = getSharedPreferences("USER_PROFILE",MODE_PRIVATE);
        String userid = sp.getString("userid",null);
        if(userid != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }*/
    }

    public void signIn(View v) {
        Log.d("TAG","sign called");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,"in result");
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            String token = result.getSignInAccount().getIdToken();
            Log.d("TOKEN",token);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            email = acct.getEmail();
            name = acct.getDisplayName();
            userID = acct.getId();
            sendToBackEnd(name, email, userID);
            //mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            //updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            //updateUI(false);
        }
    }

    void sendToBackEnd(final String name, final String email, final String userID){

        new AsyncTask<Void,Void,String>() {

            @Override
            protected String doInBackground(Void... voids) {
                //send details to backend
                StringBuffer response = new StringBuffer();
                int responseCode = 500;

                try {
                    String searchpageurl = getString(R.string.adduserurl);
                    //call to backend
                    String parameters = "id="+userID+"&name="+name;
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
                }catch (Exception e) {
                    e.printStackTrace();
                }

                if(responseCode == 200) {
                    return response.toString();
                }else {
                    return "";
                }

            }

            @Override
            protected void onPostExecute(String response) {
                //handle backend response
                Log.d("LOGIN RESPONSE",response);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean login = jsonObject.getBoolean("success");

                    if (login) {
                        //save to config file
                        SharedPreferences sp =getSharedPreferences("USER_PROFILE",MODE_PRIVATE);
                        sp.edit().putString("userid",userID).putString("name",name).putString("email",email).commit();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        // move to new screen
                    } else {
                        // same activity
                        Toast.makeText(LoginActivity.this, "Sign In Failed" , Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();

    }

}
