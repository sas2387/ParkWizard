package edu.columbia.coms6998.parkwizard;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.location.places.ui.PlaceAutocomplete;

/**
 * Created by Siddharth on 11/21/2016.
 */
public class SearchParkingFragment extends Fragment {
    Button searchButton;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View rootView = layoutInflater.inflate(R.layout.fragment_searchdestination,viewGroup , false);
        searchButton = (Button)rootView.findViewById(R.id.btSearchDestination);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("START","start called");
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN).build(getActivity());
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("REQUEST",""+resultCode);
        Log.d("RESULT",""+requestCode);
        Log.d("DATA",data.toString());
    }
}
