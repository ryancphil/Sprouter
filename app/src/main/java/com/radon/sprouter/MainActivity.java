package com.radon.sprouter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frag = (TweetFragment) getFragmentManager().findFragmentById(R.id.tweet_frag);
        alertDialog = new AlertDialog.Builder(this).create();
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

    }

    public void prompt(Tweet tweet){
        // Display Author ID, Tweet ID, and Text in Dialog
        alertDialog.setTitle("Reply:");
        alertDialog.setMessage("Author ID: " + tweet.user_id + "\n\n" + "Tweet ID: " + tweet.tweet_id + "\n\n" + "Text: " + tweet.text);

        alertDialog.setButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        alertDialog.dismiss();
    }
}
