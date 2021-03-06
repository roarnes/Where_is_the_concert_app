package com.example.nilss.whenistheconcert;

import android.os.AsyncTask;
import android.util.Log;

import com.example.nilss.whenistheconcert.BandPlayingActivityClasses.BandPlayingActivity;
import com.example.nilss.whenistheconcert.MapActivityClasses.MapActivity;
import com.example.nilss.whenistheconcert.Pojos.DetailedEvent;
import com.example.nilss.whenistheconcert.Pojos.SimpleEvent;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class TicketMasterHandler {
    private static final String TAG = "TicketMasterHandler";
    private final String rootURL = "https://app.ticketmaster.com/discovery/v2/";
    private final String tmAPIKey = "XztmbuZ4nUUAx2Ki3rG7gDVZzHfGAAbw";

    private static final String REQUEST_TYPE_GET_EVENTS = "0";
    private static final String REQUEST_TYPE_GET_EVENT_INFO = "1";

    private static final String NAME_KEY = "name";
    private static final String ID_KEY = "id";
    private static final String LOCATION_KEY = "location";
    private static final String LATITUDE_KEY = "latitude";
    private static final String LONGITUDE_KEY = "longitude";
    private static final String TICKETS_URL = "url";
    private static final String IMAGE_URL = "url";


    /**
     *
     * @param mapActivity
     * @param cityName, city to search for events within.
     * @param countryCode, Used to only search within a certain country.
     * @param dateFrom, from?
     * @param dateTo, to?
     */
    public void requestAllEvents(MapActivity mapActivity, String cityName, String countryCode, String dateFrom, String dateTo){
        //String URL = rootURL + "events.json?" + "classificationName=music"+ "&city=" + cityName + "&apikey="+ tmAPIKey;
        String dateCriteria = "&startDateTime="+dateFrom+"T00:00:00Z&endDateTime=" + dateTo + "T23:59:59Z";
        String URL;
        if(countryCode.equals("")){
            URL = rootURL + "events.json?" + "classificationName=music" + dateCriteria + "&city=" + cityName + "&size=199" + "&apikey="+ tmAPIKey;
        }
        else{
            URL = rootURL + "events.json?" + "classificationName=music" + dateCriteria + "&countryCode=" + countryCode + "&city=" + cityName + "&size=199" + "&apikey="+ tmAPIKey;
        }
        TMRequester tmRequester = new TMRequester(new AsyncResponse() {
            @Override
            public void processFinish(JSONArray result) {
                ArrayList<SimpleEvent> foundEvents = new ArrayList<>();
                for (int i = 0; i < result.length(); i++) {
                    JSONObject temp = null;
                    try {
                        temp = result.getJSONObject(i);
                        String name = temp.getString(NAME_KEY);
                        //Log.d(TAG, "processFinish: event: "+ String.valueOf(i) + " "+ name);
                        String id = temp.getString(ID_KEY);
                        JSONObject JSONlocation = temp.getJSONObject("_embedded").getJSONArray("venues").getJSONObject(0).getJSONObject(LOCATION_KEY);
                        LatLng latLng = new LatLng(JSONlocation.getDouble(LATITUDE_KEY), JSONlocation.getDouble(LONGITUDE_KEY));
                        foundEvents.add(new SimpleEvent(name, id, latLng));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                mapActivity.updateEventList(foundEvents);
                Log.d(TAG, "processFinish: nbrOfEvents: " + String.valueOf(foundEvents.size()));
            }
        });
        tmRequester.execute(URL, REQUEST_TYPE_GET_EVENTS);
    }

    /**
     *
     * @param bandPlayingActivity
     * @param eventID, id of a certain event.
     */
    public void getEventInfo(BandPlayingActivity bandPlayingActivity, String eventID){
        //Do something
        String URL = rootURL + "events/" + eventID + ".json?apikey=" + tmAPIKey ;
        Log.d(TAG, "getEventInfo: URL: "+ URL);
        TMRequester tmRequester = new TMRequester(new AsyncResponse() {
            @Override
            public void processFinish(JSONArray result) {
                JSONObject temp = null;
                try{
                    temp = result.getJSONObject(0);
                    String name = temp.getString(NAME_KEY);
                    String ticketUrl = temp.getString(TICKETS_URL);
                    String imageUrl = temp.getJSONArray("images").getJSONObject(0).getString(IMAGE_URL);
                    String venue = temp.getJSONObject("_embedded").getJSONArray("venues").getJSONObject(0).getString(NAME_KEY);
                    String date = temp.getJSONObject("dates").getJSONObject("start").getString("localDate");
                    Log.d(TAG, "processFinish: Name: "+name);
                    Log.d(TAG, "processFinish: ticket: "+ticketUrl);
                    Log.d(TAG, "processFinish: image:"+imageUrl);
                    Log.d(TAG, "processFinish: Venue: "+venue);
                    Log.d(TAG, "processFinish: Date:"+date);
                    DetailedEvent event = new DetailedEvent(name, venue, date, ticketUrl, imageUrl);
                    bandPlayingActivity.getEventDetails(event);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        tmRequester.execute(URL, REQUEST_TYPE_GET_EVENT_INFO);

    }

    /**
     * Inner class that extends AsyncTask.
     * Used to download JSON content from a given URL.
     * When calling .execute you'll have to pass a URL as a string as well as what type of request it is.
     */
    public class TMRequester extends AsyncTask<String,Void,String>{
        private AsyncResponse delegate = null;
        private String requestType;


        public TMRequester(AsyncResponse delegate) {
            this.delegate = delegate;
        }

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(params[0]);
                requestType = params[1];
                Log.d(TAG, "doInBackground: URL"+ url);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                }
                return buffer.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                //Request all events.
                if(requestType.equals(REQUEST_TYPE_GET_EVENTS)) {
                    JSONObject jsonObject = new JSONObject(s);
                    Log.d(TAG, "onPostExecute: nbrOfEventsALALA: " + String.valueOf(jsonObject.getJSONObject("page").getInt("totalElements")));
                    if(jsonObject.getJSONObject("page").getInt("totalElements")==0){
                        delegate.processFinish(new JSONArray());
                        return;
                    }
                    JSONArray jsonArray = jsonObject.getJSONObject("_embedded").getJSONArray("events");
                    if (jsonArray != null) {
                        delegate.processFinish(jsonArray);
                    }
                }
                //Otherwise it is a eventInfo request!
                else{
                    JSONObject jsonObject = new JSONObject(s);
                    JSONArray jsonArray = covertJsonObjectToJsonArray(jsonObject);
                    if(jsonArray!=null) {
                        delegate.processFinish(jsonArray);
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private  JSONArray covertJsonObjectToJsonArray(Object InsideArray) {
        JSONArray jsonArray;
        if (InsideArray instanceof JSONArray) {
            jsonArray = (JSONArray) InsideArray;
        } else {
            jsonArray = new JSONArray();
            jsonArray.put((JSONObject) InsideArray);
        }
        return jsonArray;
    }
}
