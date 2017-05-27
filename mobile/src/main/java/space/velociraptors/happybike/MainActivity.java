package space.velociraptors.happybike;

/**
 * Created by rpadurariu on 27.05.2017.
 */

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements ValueEventListener {
    private Data data;
    private JSONArray alerts = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.data = new Data();
        this.data.get(Const.ALERT_KEY, this);

        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                findViewById(R.id.navigation);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);

        bottomNavigationView.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        Fragment selectedFragment = null;
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
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, MapsFragment.newInstance());
        transaction.commit();

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
                a.put("lon", "21.15");
                a.put("lat", "21.15");
                alerts.put(a);
                data.put(Const.ALERT_KEY, alerts.toString());
            } catch (JSONException e) {
                // I don't care
            }
        }
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        try {
            this.alerts = new JSONArray((String)dataSnapshot.getValue());
        } catch (JSONException e) {
            // I don't care
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        this.alerts = new JSONArray();
    }
}