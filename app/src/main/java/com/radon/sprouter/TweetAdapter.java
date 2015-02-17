package com.radon.sprouter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Custom Adapter class to populate listView content.
 * Populates bitmaps Asynchronously
 */
public class TweetAdapter extends ArrayAdapter<Tweet> {
    //Variables
    Context context;

    //Constructor
    public TweetAdapter(Context context, int resource, List<Tweet> objects) {
        super(context, resource, objects);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if(convertView == null){
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate(R.layout.tweet_view, null);
        }

        //Get the Tweet at the current position in the list
        final Tweet tweet = getItem(position);

        //Implement the Reply button with a click listener
        Button reply = (Button) view.findViewById(R.id.reply);
        reply.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Display Author ID, Tweet ID, and Text in Dialog
                AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                alertDialog.setTitle("Reply:");
                alertDialog.setMessage("Author ID: " + tweet.user_id + "\n\n" + "Tweet ID: " + tweet.tweet_id + "\n\n" + "Text: " + tweet.text);

                alertDialog.setButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });

                alertDialog.show();

            }

        });


        if(tweet != null) {
            //Find all views to be populated
            ImageView avatar = (ImageView) view.findViewById(R.id.avatar);
            TextView author = (TextView) view.findViewById(R.id.author);
            TextView timestamp = (TextView) view.findViewById(R.id.timestamp);
            TextView text = (TextView) view.findViewById(R.id.tweet);
            ImageView photo = (ImageView) view.findViewById(R.id.photo);
            TextView location = (TextView) view.findViewById(R.id.location);

            //If the Bitmaps aren't saved in Tweet object, load them asynchronously
            //else, just set them from the Tweet object
            if (tweet.avatar_pic == null){
                new LoadPictures(view, avatar, photo).execute(tweet);
            }else{
                avatar.setImageBitmap(tweet.avatar_pic);
                photo.setImageBitmap(tweet.photo_pic);
            }


            if(author != null){
                //set author
                author.setText(tweet.author);
            }

            if(timestamp != null){
                //set timestamp
                timestamp.setText(tweet.timestamp);
            }

            if(text != null){
                //set text
                text.setText(tweet.text);
            }

            if(location != null){
                //set location
                location.setText(tweet.location);
            }
        }
        return view;
    }

    //AsyncTask to retrieve and store bitmaps
    private class LoadPictures extends AsyncTask<Tweet, Void, Bitmap[]> {

        //Variables
        View view;
        ImageView avatar;
        ImageView photo;

        //Constructor
        LoadPictures(View view, ImageView avatar, ImageView photo){
            this.view = view;
            this.avatar = avatar;
            this.photo = photo;
        }

        @Override
        protected Bitmap[] doInBackground(Tweet... params) {
            //Avatar and inline photo
            Bitmap[] results = new Bitmap[2];

            try {
                //Get the avatar bitmap, every tweet MUST have one
                URL avatar_url = new URL(params[0].avatar);
                Bitmap avatar_bit = BitmapFactory.decodeStream(avatar_url.openConnection().getInputStream());
                results[0] = avatar_bit;

                //There will not always be inline photos so check to see if tweet object actually has one first.
                if(params[0].photo != null) {
                    URL photo_url = new URL(params[0].photo);
                    Bitmap photo_bit = BitmapFactory.decodeStream(photo_url.openConnection().getInputStream());
                    results[1] = photo_bit;

                }else{
                    results[1] = null;
                }

                //Set the Bitmaps to the Tweet object so they don't have to be reloaded.
                params[0].setAvatar_pic(results[0]);
                params[0].setPhoto_pic(results[1]);


            }catch(MalformedURLException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }
            return results;
        }

        @Override
        protected void onPostExecute(Bitmap[] result) {
            //This method puts us back on the main thread, so we can update the views with the newly loaded bitmaps
            avatar.setImageBitmap(result[0]);

            //If inline photo is present, set it.
            if(result[1] != null) {
                photo.setImageBitmap(result[1]);
            }else{
                photo.setImageBitmap(null);
            }
        }

        @Override
        protected void onPreExecute() {
            //Do nothing
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            //Do nothing
        }
    }
}
