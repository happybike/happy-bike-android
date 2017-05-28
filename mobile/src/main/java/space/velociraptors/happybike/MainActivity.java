package space.velociraptors.happybike;

/**
 * Created by rpadurariu on 27.05.2017.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static space.velociraptors.happybike.Const.MORNING_NOTIFICATION;

public class MainActivity extends AppCompatActivity implements
        ValueEventListener, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private Data data;
    private JSONArray alerts = new JSONArray();

    private Fragment selectedFragment;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest = createLocationRequest();
    private double currentLatitude;
    private double currentLongitude;


    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    public LatLng getMyLocation() {
        return new LatLng(currentLatitude, currentLongitude);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();

        this.data = new Data();
        this.data.get(Const.ALERTS_KEY, this);
        this.data.get(Const.ALERT_LATEST_KEY, new NotificationOnAlert());
        this.currentLatitude = 45.7481971;
        this.currentLongitude = 21.2401086;

        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                findViewById(R.id.navigation);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);

        bottomNavigationView.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.map_action:
                                selectedFragment = MapsFragment.newInstance();
                                break;
                            case R.id.rate_action:
                                selectedFragment = RateFragment.newInstance();
                                break;
                            case R.id.help_action:
                                selectedFragment = HelpFragment.newInstance();
                                break;
                            case R.id.add_action:
                                selectedFragment = AddFragment.newInstance();
                                break;
                            case R.id.scan_action:
                                selectedFragment = ScanFragment.newInstance();
                                break;
                        }
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.frame_layout, selectedFragment);
                        transaction.commit();
                        return true;
                    }
                });

        //Manually displaying the first fragment - one time only

        if (!getIntent().hasExtra(Const.NOTIF_CMD)) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_layout, MapsFragment.newInstance());
            transaction.commit();
        } else {
            String cmd = getIntent().getExtras().getString(Const.NOTIF_CMD, "");
            if (cmd.equals(Const.CMD_ALERT)) {
                String text = getIntent().getExtras().getString(Const.ALERT_TEXT);
                double lon = getIntent().getExtras().getDouble(Const.ALERT_TEXT);
                double lat = getIntent().getExtras().getDouble(Const.ALERT_TEXT);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                MapsFragment mf = MapsFragment.newInstance();
                mf.setAlert(text, lon, lat);
                transaction.replace(R.id.frame_layout, mf);
                transaction.commit();
            }
        }

        //Used to select an item programmatically
        //bottomNavigationView.getMenu().getItem(2).setChecked(true);
    }

    public void daLocatia() {

    }

    public void onAlert(View view) {
        String alert = null;
        switch (view.getId()) {
            case R.id.alert_accident:
                alert = "Help! I've had an accident!";
                break;
            case R.id.alert_broken_bike:
                alert = "Help! I've broken my bike!";
                break;
            case R.id.alert_stolen_bike:
                alert = "Help! My bike was stolen!";
                break;
        }
        if (alert != null) {
            if (alerts.length() >= 10 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alerts.remove(0);
            }

            JSONObject a = new JSONObject();
            try {
                a.put("text", alert);
                a.put("lon", Double.toString(currentLongitude));
                a.put("lat", Double.toString(currentLatitude));
                alerts.put(a);
                data.put(Const.ALERTS_KEY, alerts.toString());
                data.put(Const.ALERT_LATEST_KEY, a.toString());
            } catch (JSONException e) {
                // I don't care
            }
        }
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        try {
            String value = (String)dataSnapshot.getValue();
            if (value == null) {
                value = "[]";
            }
            this.alerts = new JSONArray(value);
        } catch (JSONException e) {
            // I don't care
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        this.alerts = new JSONArray();
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        if (selectedFragment != null && selectedFragment instanceof LocationListener) {
            ((LocationListener)selectedFragment).onLocationChanged(location);
        }
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                currentLatitude = mLastLocation.getLatitude();
                currentLongitude = mLastLocation.getLongitude();
            }
            startLocationUpdates();
        }
    }
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onStart() {
        System.out.println("In start");
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        System.out.println("In stop");
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    public class NotificationOnAlert implements ValueEventListener {

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            try {
                String value = (String)dataSnapshot.getValue();
                if (value == null) {
                    value = "[]";
                }
                JSONObject jv = new JSONObject(value);
                String text = jv.getString("text");
                double lon = Double.parseDouble(jv.getString("lon"));
                double lat = Double.parseDouble(jv.getString("lat"));

                NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(Const.ALERT_NOTIFICATION, alertNotification(text, lon, lat));
            } catch (JSONException e) {
                // I don't care
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }

        private Notification alertNotification(String text, double lon, double lat) {
            Intent resultIntent = new Intent(MainActivity.this, MainActivity.class);
            resultIntent.putExtra(Const.NOTIF_CMD, Const.CMD_ALERT);
            resultIntent.putExtra(Const.ALERT_LON, lon);
            resultIntent.putExtra(Const.ALERT_LAT, lat);
            resultIntent.putExtra(Const.ALERT_TEXT, text);

            resultIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent piResult = PendingIntent.getActivity(
                    MainActivity.this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);


            NotificationCompat.Style notifStyle = new NotificationCompat.BigTextStyle()
                    .setBigContentTitle("Alert")
                    .setSummaryText(text);


            return new NotificationCompat.Builder(MainActivity.this)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Alert")
                    .setContentText(text)
                    .setStyle(notifStyle)
                    .setContentIntent(piResult)
                    .build();
        }
    }
}