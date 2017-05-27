package space.velociraptors.happybike;

/**
 * Created by rpadurariu on 27.05.2017.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    public void onAlert(View view) {
        switch (view.getId()) {
            case R.id.alert_accident:
                return;
            case R.id.alert_broken_bike:
                return;
            case R.id.alert_stolen_bike:
                return;
        }
    }
}