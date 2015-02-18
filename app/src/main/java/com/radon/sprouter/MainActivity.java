package com.radon.sprouter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

/**
 *  Welcome to Sprouter!
 *  Get tweets using keyword searches within a 5 mile radius of your location.
 *
 *  @author Ryan Phillips
 *  Created on 2.16.2015
 **/

public class MainActivity extends FragmentActivity {
    TweetFragment frag;
    SearchView searchView;
    AlertDialog alertDialog;

    //Location variables
    LocationManager lm;
    LocationListener locationListener;
    Location location;
    AlertDialog gpsAlert;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frag = (TweetFragment) getFragmentManager().findFragmentById(R.id.tweet_frag);
        //Replay button dialog
        alertDialog = new AlertDialog.Builder(this).create();
        //GPS alert dialog
        gpsAlert = new AlertDialog.Builder(this).create();
        //SearchView box that lets users search twitter by keywords
        searchView = (SearchView)findViewById(R.id.search);

        //Prevent keyboard from auto appearing
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //restart loader in the TweetFragment using users input
                frag.reloadData(query);
                //hide keyboard when user presses done/go/submit
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        /************************
         * Location Services
         *************************/
        //Get the users Last known location for the default search
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        //Check that location services are in fact enabled.
        if ( !lm.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            gpsAlert();
        }
        location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null) {
            frag.longitude = Double.toString(location.getLongitude());
            frag.latitude = Double.toString(location.getLatitude());
            frag.url = "https://api.twitter.com/1.1/search/tweets.json?q=" + frag.keyword + "&geocode=" + frag.latitude + "," + frag.longitude + ",5mi";
        }

        //LocationListener updates the users location when it detects a change
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                frag.longitude = Double.toString(location.getLongitude());
                frag.latitude = Double.toString(location.getLatitude());
                frag.url = "https://api.twitter.com/1.1/search/tweets.json?q=" + frag.keyword + "&geocode=" + frag.latitude + "," + frag.longitude + ",5mi";
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //Do nothing
            }

            @Override
            public void onProviderEnabled(String provider) {
                frag.reloadData(frag.keyword);
            }

            @Override
            public void onProviderDisabled(String provider) {
                //fire location alert
                gpsAlert();
            }
        };

        //Request location updates every 10 seconds. Probably not good for device battery life, but nice for testing purposes.
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener);

    }

    public void prompt(Tweet tweet){
        // Display Author ID, Tweet ID, and Text in Dialog
        alertDialog.setTitle("Reply:");
        alertDialog.setMessage("Author ID: " + tweet.user_id + "\n\n" + "Tweet ID: " + tweet.tweet_id + "\n\n" + "Text: " + tweet.text);

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,"Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    private void gpsAlert() {
        gpsAlert.setMessage("Enable location services to continue.");
        gpsAlert.setButton(DialogInterface.BUTTON_POSITIVE,"Enable",new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                dialog.cancel();
                while (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    //This seems very hacky. Want application to wait until location services are enabled
                    //to restart application.
                }
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                finish();
                startActivity(i);
            }
        });
        gpsAlert.setButton(DialogInterface.BUTTON_NEGATIVE,"Cancel",new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                dialog.cancel();
                finish();
                System.exit(0);
            }
        });

        gpsAlert.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(gpsAlert != null) {
            gpsAlert.dismiss();
        }
        if(alertDialog != null) {
            alertDialog.dismiss();
        }
        //Prevent constant location updates if application is in background.
        lm.removeUpdates(locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //if user disables them while app is in background, prompt onResume
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            gpsAlert();
        }
    }
}
