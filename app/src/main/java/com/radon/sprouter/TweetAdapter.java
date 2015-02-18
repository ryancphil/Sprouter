package com.radon.sprouter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

        //Take advantage of the wonderful ViewHolder Design Pattern
        ViewHolder holder = new ViewHolder();

        if(convertView == null){
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate(R.layout.tweet_view, null);


            holder.avatar = (ImageView) view.findViewById(R.id.avatar);
            holder.author = (TextView) view.findViewById(R.id.author);
            holder.timestamp = (TextView) view.findViewById(R.id.timestamp);
            holder.text = (TextView) view.findViewById(R.id.tweet);
            holder.photo = (ImageView) view.findViewById(R.id.photo);
            holder.location = (TextView) view.findViewById(R.id.location);
            view.setTag(holder);
        }else{
            holder = (ViewHolder) view.getTag();
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
                ((MainActivity)context).prompt(tweet);
            }

        });


        if(tweet != null) {

            //If the Bitmaps aren't saved in Tweet object, load them asynchronously
            //else, just set them from the Tweet object
            if (tweet.avatar_pic == null){
                new LoadPictures(view, holder.avatar, holder.photo).execute(tweet);
            }else{
                holder.avatar.setImageBitmap(tweet.avatar_pic);
                holder.photo.setImageBitmap(tweet.photo_pic);
            }


            if(holder.author != null){
                //set author
                holder.author.setText(tweet.author);
            }

            if(holder.timestamp != null){
                //set timestamp
                holder.timestamp.setText(tweet.timestamp);
            }

            if(holder.text != null){
                //set text
                holder.text.setText(tweet.text);
            }

            if(holder.location != null){
                //set location
                holder.location.setText(tweet.location);
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
                if(!params[0].photo.equals("none")) {
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

    static class ViewHolder {
        ImageView avatar;
        TextView author;
        TextView timestamp;
        TextView text;
        ImageView photo;
        TextView location;
    }

}
