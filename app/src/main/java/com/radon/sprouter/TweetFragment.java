package com.radon.sprouter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * TweetFragment handles the tweets from the API call.
 *
 * @author Ryan Phillips
 *  Created on 2.16.2015
 */
public class TweetFragment extends ListFragment implements LoaderManager.LoaderCallbacks<ArrayList<Tweet>> {

    //Location variables to get the users GPS location
    LocationManager lm;
    Location location;
    String latitude;
    String longitude;

    //ListView variables
    View footer;
    boolean moreTweets = true;
    ListView tweets;
    ArrayList<Tweet> data = new ArrayList<>();
    TweetAdapter adapter;

    //Twitter API variables
    TweetLoader loader;
    String keyword = "";
    String url = "https://api.twitter.com/1.1/search/tweets.json?q=" + keyword + "&geocode=" + latitude + "," + longitude + ",5mi";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Inflate the fragment
        View view =  inflater.inflate(R.layout.fragment_tweet, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Inflate the view used for pagination loader progressBar
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        footer = inflater.inflate(R.layout.listview_loader, tweets, false);

        tweets = getListView();
        tweets.addFooterView(footer);

        //Get the users Last known location for the default search
        lm = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
        location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null) {
            longitude = Double.toString(location.getLongitude());
            latitude = Double.toString(location.getLatitude());
            url = "https://api.twitter.com/1.1/search/tweets.json?q=" + keyword + "&geocode=" + latitude + "," + longitude + ",5mi";
        }

        //LocationListener updates the users location when it detects a change
        final LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                longitude = Double.toString(location.getLongitude());
                latitude = Double.toString(location.getLatitude());
                url = "https://api.twitter.com/1.1/search/tweets.json?q=" + keyword + "&geocode=" + latitude + "," + longitude + ",5mi";
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //Do nothing
            }

            @Override
            public void onProviderEnabled(String provider) {
                //Do nothing
            }

            @Override
            public void onProviderDisabled(String provider) {
                //Do nothing
            }
        };

        //Request location updates every 10 seconds. Probably not good for device battery life, but nice for testing purposes.
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener);

        //Initialize adapter
        adapter = new TweetAdapter(getActivity(), android.R.layout.simple_list_item_1, data);
        //If no loader exists, initialize a new one
        if(savedInstanceState == null) {
            //Log.e("INIT LOADER: ", "ON CREATE");
            //Lock the orientaion of device when loader is executing to prevent losing an AsyncTask into the abyss.
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            getLoaderManager().initLoader(0, null, TweetFragment.this).forceLoad();
        }else{
            //Log.e("ROTATION: ", "SAVED STATE");
            //A loader exists so repopulate the listview from savedInstanceState
            data.clear();
            data = savedInstanceState.getParcelableArrayList("key");
            adapter = new TweetAdapter(getActivity(), android.R.layout.simple_list_item_1, data);
            adapter.notifyDataSetChanged();
        }
        //Set listview divider and adapter
        tweets.setDividerHeight(5);
        tweets.setAdapter(adapter);

        //Implement a scroll listener to achieve pagination of results
        tweets.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //When the user stops scrolling and the last listView item is visible...
                if (scrollState == SCROLL_STATE_IDLE) {
                    if (view.getLastVisiblePosition() >= view.getCount() - 1) {
                        //load more tweets!
                        moreTweets();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                //Do nothing
            }

        });


    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<ArrayList<Tweet>> onCreateLoader(int i, Bundle bundle) {
        //Create a new loader
        loader = new TweetLoader(this.getActivity(), url);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Tweet>> loader, ArrayList<Tweet> data) {
        //if data gets too large, drop older tweets in order to prevent OutOfMemoryError
        if(this.data.size() > 45){
            //Remove first 15 tweets
            for(int i = 0; i < 15; i++){
                this.data.remove(0);
            }
        //If NO new tweets were retrieved, prevent firing the loader until a new keyword is searched
        }else if(data.size() == 0) {
            //Modify the listView footer to notify the user that there are no more new tweets
            TextView textView = (TextView) footer.findViewById(R.id.footer_text);
            textView.setText("No more tweets.");
            footer.findViewById(R.id.progressBar1).setVisibility(View.INVISIBLE);
            //Set flag
            moreTweets = false;
        }
        //update the data array with newly fetched items
        for(int i = 0; i < data.size();i++) {
            this.data.add(data.get(i));
        }
        adapter.notifyDataSetChanged();

        //Unlock the screen orientation once the loader is finished
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        ((MainActivity)getActivity()).searchView.clearFocus();

        //Destroy the loader when it's done its job.
        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Tweet>> loader) {
        //Do nothing
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save state so orientation changes don't meddle with results
        outState.putParcelableArrayList("key", data);
    }

    //Method is called when the user searches keywords
    public void reloadData(String keyword){
        //Set flag
        moreTweets = true;

        //Update footer to progress bar
        footer.findViewById(R.id.progressBar1).setVisibility(View.VISIBLE);
        ((TextView)footer.findViewById(R.id.footer_text)).setText("Loading...");

        //Clear the data set
        this.data.clear();

        //Format the user's input - Handle leading/trailing spaces and replace spaces with commas
        keyword = keyword.trim();
        keyword = keyword.replaceAll(" ",",");

        //Update global keyword variable
        this.keyword = keyword;

        //Reset the url to make a new api call including the users input keywords
        url = "https://api.twitter.com/1.1/search/tweets.json?q=" + keyword + "&geocode=" + latitude + "," + longitude + ",5mi";

        //Restart the loader and lock orientation
        //Log.e("LOAD1: ", "RELOAD DATA");
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        getLoaderManager().restartLoader(0,null,TweetFragment.this).forceLoad();
    }

    //This method ADDS tweets to an existing data set for pagination
    public void moreTweets(){
        //If there are no new tweets, do nothing
        if(!moreTweets){
            return;
        }

        //Get id of last tweet in data set
        String id = "";
        if(data.size() > 0) {
            //Use id to get OLDER tweets
            id = "&max_id=" + data.get(data.size()-1).tweet_id;
        }

        //Reset the url to make a new api call including the users input keywords
        url = "https://api.twitter.com/1.1/search/tweets.json?q=" + keyword + "&geocode=" + latitude + "," + longitude + ",5mi" + id;

        //Restart the loader and lock orientation
        //Log.e("LOAD1: ", "MORE TWEETS");
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        getLoaderManager().restartLoader(0,null,TweetFragment.this).forceLoad();
    }

}
