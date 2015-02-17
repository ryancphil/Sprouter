package com.radon.sprouter;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * AsyncTaskLoader to fetch tweets using Twitter API
 */
public class TweetLoader extends AsyncTaskLoader<ArrayList<Tweet>> {

    //Twitter API variables
    final static String CONSUMER_KEY = "PUT_YOUR_KEY_HERE";
    final static String CONSUMER_SECRET = "PUT_YOUR_SECRET_HERE";
    final static String TwitterTokenURL = "https://api.twitter.com/oauth2/token";
    String TwitterStreamURL = "https://api.twitter.com/1.1/search/tweets.json?q=";

    //Default Constructor
    public TweetLoader(Context context, String url) {
        super(context);
        TwitterStreamURL = url;
    }

    //This method takes in the GET request and returns a string of the JSON object using a stringBuilder
    private String getResponseBody(HttpRequestBase request) {
        StringBuilder sb = new StringBuilder();
        try {

            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String reason = response.getStatusLine().getReasonPhrase();

            if (statusCode == 200) {

                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();

                BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                String line = null;
                while ((line = bReader.readLine()) != null) {
                    sb.append(line);
                }
            } else {
                sb.append(reason);
            }
        } catch (UnsupportedEncodingException ex) {
        } catch (ClientProtocolException ex1) {
        } catch (IOException ex2) {
        }
        return sb.toString();
    }

    @Override
    public ArrayList<Tweet> loadInBackground() {
        ArrayList<Tweet> result = null;

        //Encode consumer key and secret
        try {
            // URL encode the consumer key and secret
            String urlApiKey = URLEncoder.encode(CONSUMER_KEY, "UTF-8");
            String urlApiSecret = URLEncoder.encode(CONSUMER_SECRET, "UTF-8");

            //Concatenate the encoded consumer key, a colon character, and the
            //encoded consumer secret
            String combined = urlApiKey + ":" + urlApiSecret;

            //Base64 encode the string
            String base64Encoded = Base64.encodeToString(combined.getBytes(), Base64.NO_WRAP);

            //Obtain a bearer token
            HttpPost httpPost = new HttpPost(TwitterTokenURL);
            httpPost.setHeader("Authorization", "Basic " + base64Encoded);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            httpPost.setEntity(new StringEntity("grant_type=client_credentials"));
            String rawAuthorization = getResponseBody(httpPost);
            //Log.e("RAW: ", rawAuthorization);
            JSONObject raw = new JSONObject(rawAuthorization);

            // Applications should verify that the value associated with the
            // token_type key of the returned object is bearer
            if (raw.getString("token_type").equals("bearer")) {

                //Authenticate API requests with bearer token
                HttpGet httpGet = new HttpGet(TwitterStreamURL);

                // construct a normal HTTPS request and include an Authorization
                // header with the value of Bearer <>
                httpGet.setHeader("Authorization", "Bearer " + raw.getString("access_token"));
                httpGet.setHeader("Content-Type", "application/json");
                // update the results with the body of the response
                String results = getResponseBody(httpGet);
                //Log.e("Tweets: ", results);

                //Parse the String results and return an ArrayList of Tweet objects
                result = parse(results);

            }

        } catch (UnsupportedEncodingException ex) {
        } catch (IllegalStateException ex1) {
        }catch (JSONException ex2){

        }
        return result;
    }

    //PARSE method for the JSON object of tweets and convert the String to ArrayList<Tweet>
    public ArrayList<Tweet> parse(String data){
        //Log.e("Parse: ", "entered.");
        String avatar;
        String author;
        String text;
        String location;
        String photo;
        String timestamp;
        String tweet_id;
        String user_id;

        ArrayList<Tweet> results = new ArrayList<>();

        try{
            //Convert string data to JSON object for easy parsing
            JSONObject json = new JSONObject(data);
            JSONArray jsonArray = json.getJSONArray("statuses");
            //Log.e("statuses: ", Integer.toString(jsonArray.length()));

            for(int i = 0; i < jsonArray.length(); i++){
                text = jsonArray.getJSONObject(i).getString("text");
                //Log.e("text: ", text);

                //Remove _normal to get full size images
                avatar = jsonArray.getJSONObject(i).getJSONObject("user").getString("profile_image_url");
                avatar = avatar.replace("_normal","");
                //Log.e("avatar: ", avatar);

                author = jsonArray.getJSONObject(i).getJSONObject("user").getString("screen_name");
                //Log.e("author: ", author);

                location = jsonArray.getJSONObject(i).getJSONObject("user").getString("location");
                //Log.e("location: ", location);

                if (jsonArray.getJSONObject(i).getString("entities").contains("media")){
                    photo = jsonArray.getJSONObject(i).getJSONObject("entities").getJSONArray("media").getJSONObject(0).getString("media_url");
                    //Log.e("photo: ", photo);
                }else{
                    //Log.e("ENTITIES: ", jsonArray.getJSONObject(i).getString("entities"));
                    photo = null;
                }

                timestamp = jsonArray.getJSONObject(i).getString("created_at");
                //Log.e("timestamp: ", timestamp);
                timestamp = dateDifference(timestamp);

                tweet_id = jsonArray.getJSONObject(i).getString("id_str");
                //Log.e("IDs: " ,  id);

                user_id = jsonArray.getJSONObject(i).getJSONObject("user").getString("id_str");


                //create a new Tweet object using data retrieved and add it to arrayList results
                results.add(new Tweet(avatar,author,text,location,photo, timestamp, tweet_id, user_id));
            }
        }catch(JSONException e){
            e.printStackTrace();
        }

        //Return arraylist of Tweet objects
        return results;
    }

    //This method calculates the timestamp value of each tweet.
    public String dateDifference(String tweetTime){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd kk:mm:ss Z yyyy");
        String current = simpleDateFormat.format(new Date());
        Date date1;
        Date date2;
        try {
            date2 = simpleDateFormat.parse(tweetTime);
            date1 = simpleDateFormat.parse(current);

            //milliseconds
            long different = Math.abs(date1.getTime() - date2.getTime());
            long secondsInMilli = 1000;
            long minutesInMilli = secondsInMilli * 60;
            long hoursInMilli = minutesInMilli * 60;
            long daysInMilli = hoursInMilli * 24;

            long elapsedDays = different / daysInMilli;
            different = different % daysInMilli;

            long elapsedHours = different / hoursInMilli;
            different = different % hoursInMilli;

            long elapsedMinutes = different / minutesInMilli;
            different = different % minutesInMilli;

            long elapsedSeconds = different / secondsInMilli;

            if (elapsedDays > 0){
                return elapsedDays + "d ";
            }else if(elapsedHours > 0){
                return elapsedHours + "h ";
            }else if(elapsedMinutes > 0){
                return elapsedMinutes + "m ";
            }else{
                return elapsedSeconds + "s ";
            }
        }catch(ParseException e){
            Log.e("CATCH: ", e.toString());
        }
        return "Date Error";
    }
}
