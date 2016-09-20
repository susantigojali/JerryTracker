package sg.pbd;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;

public class MapsActivity extends FragmentActivity {

    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private boolean mLock;
    private double mLatitude=0;
    private double mLongitude=0;
    private Long mValidUntil;
    private long mDuration;
    private String mResult;//untuk mResult dari HTTPpost
    private boolean mLockPost=true;
    private CountDownTimer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLock = true;
        new Task().execute(getApplicationContext());
        while (mLock) { //wait until requested
        }
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        Button buttonScanner = (Button) findViewById(R.id.b_scanner);
        buttonScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //start the scanning activity from the com.google.zxing.client.android.SCAN intent
                Intent intent = new Intent(ACTION_SCAN);
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, 0);
            }
        });
        Button buttonCompass = (Button) findViewById(R.id.b_compass);
        buttonCompass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, CompassActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void updateMap(){
        new Task().execute(getApplicationContext());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     * Membuat timer dan melakukan countdown
     * Menaruh marker pada latitude dan logitude
     *
     */
    private void setUpMap() {
        mTimer=new CountDownTimer(mDuration,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                TextView tv_duration = (TextView) findViewById(R.id.tv_duration);
                tv_duration.setText(millisUntilFinished/1000+" seconds");
            }

            @Override
            public void onFinish() {
                mLock = true;
                updateMap();
                while (mLock) {}
                setUpMap();
            }
        };
        mTimer.start();
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(new LatLng(mLatitude,mLongitude)).title("here"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLatitude, mLongitude), 15));
    }

    /**
     * Mengambil konten yang diberikan oleh barcode activity
     * Lalu melakukan post ke server
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                //get the extras that are returned from the intent
                final String contents = intent.getStringExtra("SCAN_RESULT");
                Toast toast = Toast.makeText(this, "Content:" + contents , Toast.LENGTH_LONG);
                toast.show();

                mLockPost=true;
                new Thread(new Runnable() {
                    public void run() {
                        //send to server
                        HttpClient client = new DefaultHttpClient();
                        HttpPost httppost = new HttpPost("http://167.205.32.46/pbd/api/catch");
                        httppost.setHeader("Content-type", "application/json");

                        //set body request
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("nim", "13512057");
                            jsonObject.put("token", contents);
                        } catch (JSONException e) {

                            e.printStackTrace();
                        }

                        StringEntity se = null;
                        try {
                            se = new StringEntity(jsonObject.toString());
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        httppost.setEntity(se);


                        HttpResponse response;
                        try {
                            response = client.execute(httppost);
                            if (response != null) {

                                // Get the response
                                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                                String line = "";
                                String body="";
                                while ((line = rd.readLine()) != null) {
                                    body += line;
                                }

                                JSONObject jsonObj = new JSONObject(body);
                                String message=jsonObj.getString("message");
                                int code=jsonObj.getInt("code");
                                mResult= code +" "+message;
                                System.out.println("code: "+code);
                                System.out.println("message: "+message);

                            } else {
                                mResult="response null";
                            }
                            mLockPost=false;

                        }catch (Exception e) {
                            e.printStackTrace();
                            mResult="no internet connection";
                            mLockPost=false;
                        }
                    }
                }).start();

                while(mLockPost){}
                Toast.makeText(getApplicationContext(), mResult, Toast.LENGTH_LONG).show();
          }
        }
    }


    public class Task extends AsyncTask<Context, String, String>{

        private Context context;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Context... params) {
            context = params[0];

            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet("http://167.205.32.46/pbd/api/track?nim=13512057");
            HttpResponse response;
            String mResult = "";

            try {
                response = client.execute(request);
                // Get the response
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                while ((line = rd.readLine()) != null) {
                    mResult += line;
                }

                JSONObject jsonObj = new JSONObject(mResult);
                mLatitude=jsonObj.getDouble("lat");
                mLongitude=jsonObj.getDouble("long");
                mValidUntil=jsonObj.getLong("valid_until");
                System.out.println("lat: "+mLatitude+ ", long= "+mLongitude);

                //perhitungan mDuration
                Date date = new Date();
                long epoch = date.getTime();
                System.out.println(epoch);
                mValidUntil=mValidUntil*1000;
                mDuration=mValidUntil-epoch;
                System.out.println("dur "+mDuration+" valid= "+mValidUntil);

                mLock = false;
            } catch (Exception e) {
                mLatitude=0;
                mLongitude=0;
                mDuration=20000;
                mLock = false;

                e.printStackTrace();
                System.out.println("exception");
                System.out.println("lat: "+mLatitude+ ", long= "+mLongitude);
                System.out.println("dur "+mDuration);
            }
            return mResult;
        }
    }
}
