package edu.columbia.coms6998.parkwizard;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by Siddharth on 12/21/2016.
 */

public class UpdateDialogFragment extends DialogFragment {

    View view;
    EditText etAvailable, etLocation;
    Button btCancel, btSubmit;

    UpdateParkingFragment targetFragment;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        targetFragment = (UpdateParkingFragment) getTargetFragment();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_reportparking, null);
        etAvailable = (EditText)view.findViewById(R.id.etSpots);
        etAvailable.setHint("Available Spots");
        etLocation = (EditText)view.findViewById(R.id.etLocation);
        etLocation.setVisibility(View.GONE);
        btCancel = (Button)view.findViewById(R.id.btCancel);
        btSubmit = (Button)view.findViewById(R.id.btSubmit);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view);
        btCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                targetFragment.onDialogNegativeClick(UpdateDialogFragment.this);
            }
        });

        btSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(etAvailable.getText().toString().isEmpty()){
                    Toast.makeText(getActivity(), "Please enter valid data",Toast.LENGTH_SHORT).show();
                } else {
                    int available = Integer.parseInt(etAvailable.getText().toString());
                    targetFragment.onDialogPositiveClick(UpdateDialogFragment.this,available);
                }
            }
        });

        return builder.create();
    }
}
