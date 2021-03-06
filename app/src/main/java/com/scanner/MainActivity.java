package com.scanner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //Initialize variable
    Button btScan;
    FusedLocationProviderClient fusedLocationProviderClient;
    String location_string;
    double latitude;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Assign variable
        btScan = findViewById(R.id.bt_scan);

        //Initialize fusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        btScan.setOnClickListener(view -> {

            //Check location permissions
            if(ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                getLocation();
            } else {
                //Permission denied
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
            }


            //Initialize intent integrator
            IntentIntegrator intentIntegrator = new IntentIntegrator(
                    MainActivity.this
            );

            //Set prompt text
            intentIntegrator.setPrompt("For flash use volume up key.");

            //Set beep
            intentIntegrator.setBeepEnabled(true);

            //Locked orientation
            intentIntegrator.setOrientationLocked(true);

            //Set capture activity
            intentIntegrator.setCaptureActivity(Capture.class);

            //Initiate scan
            intentIntegrator.initiateScan();
        });
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
            //Initialize location
            Location location = task.getResult();
            if (location != null) {
                //Initialize geoCoder
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                //Initialize address list
                try {
                    List<Address> addresses = geocoder.getFromLocation(
                            location.getLatitude(), location.getLongitude(), 1);
                    location_string = addresses.get(0).getLatitude() +
                            " " + addresses.get(0).getLongitude() +
                            "\nCountry: " + addresses.get(0).getCountryName() +
                            "\nLocality: " + addresses.get(0).getLocality();
                    latitude = addresses.get(0).getLatitude();
                    longitude = addresses.get(0).getLongitude();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint({"MissingPermission", "HardwareIds"})
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Initialize intent result
        IntentResult intentResult = IntentIntegrator.parseActivityResult(
                requestCode,resultCode,data
        );

        //Check condition
        if(intentResult.getContents() != null){

            String epc_sgtin = null;

            //Convert EPC to SGTIN
            if(intentResult.getFormatName().equals("UPC_A")){

                String prefix;
                String ref;
                String indic = "0";

                prefix = "0"+intentResult.getContents().substring(0,6);
                ref = intentResult.getContents().substring(6,11);

                //Arbitrary assignment
                //Will need to account for serialization
                String serial = "0001";

                epc_sgtin = "urn:epc:id:sgtin:"+prefix+"."+indic+ref+"."+serial;
            }


            //When result content is not null
            //Initialize alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    MainActivity.this
            );

            //Set Activity type
            String type = "scanTransaction";

            //Set title
            builder.setTitle("Scan Result");

            JSONObject loc = new JSONObject();
            try {
                loc.put("latitude", latitude);
                loc.put("longitude", longitude);

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            JSONObject obj = new JSONObject();
            try {
                obj.put("symbology", intentResult.getFormatName());
                obj.put("value", intentResult.getContents());
                obj.put("timestamp", Instant.now().toString());
                obj.put("location", loc);
                obj.put("deviceId", getDeviceId(this));
                obj.put("type", type);

            }  catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            //Set message
            try {
                builder.setMessage(obj.toString(2));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //Set positive button
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //Dismiss dialog
                    dialogInterface.dismiss();
                }
            });

            //Show alert dialog
            builder.show();
        } else {
            //When result content is null
            //Display toast
            Toast.makeText(getApplicationContext(),
                    "Failed to scan.",Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {

        return Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}