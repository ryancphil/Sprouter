package com.radon.sprouter;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Object class to hold all of the data of a single tweet.
 */
public class Tweet implements Parcelable{

    //Variables
    String avatar;
    String author;
    String text;
    String location;
    String photo;
    String timestamp;
    String tweet_id;
    String user_id;

    Bitmap avatar_pic;
    Bitmap photo_pic;

    //Constructor
    Tweet(String avatar, String author, String text, String location, String photo, String timestamp, String tweet_id, String user_id){
        this.avatar = avatar;
        this.author = author;
        this.text = text;
        this.location = location;
        if(photo != null) {
            this.photo = photo;
        }
        this.timestamp = timestamp;
        this.tweet_id = tweet_id;
        this.user_id = user_id;
    }

    //Bitmap Setters
    public void setAvatar_pic(Bitmap pic){
        this.avatar_pic = pic;
    }

    public void setPhoto_pic(Bitmap pic){
        this.photo_pic = pic;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(avatar);
        dest.writeString(author);
        dest.writeString(text);
        dest.writeString(location);
        if(photo != null) {
            dest.writeString(photo);
        }
        dest.writeString(timestamp);
        dest.writeString(tweet_id);
        dest.writeString(user_id);
    }
}
