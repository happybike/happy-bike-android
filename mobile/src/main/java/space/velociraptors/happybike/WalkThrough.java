package space.velociraptors.happybike;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;

import static space.velociraptors.happybike.Const.MORNING_NOTIFICATION;

public class WalkThrough extends AppCompatActivity {

    private static int[] walkthroughImages = new int[]{R.drawable.bikeway, R.drawable.flowers, R.drawable.river};
    private ImageView walkthroughImage;
    private int currentImage = 0;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.walkthrough_next:
                    if (currentImage >= walkthroughImages.length - 1) {
                        gotoMapsActivity();
                        return true;
                    }
                    currentImage = currentImage + 1;
                    updateWalkthrough();
                    return true;
                case R.id.walkthrough_previous:
                    currentImage = currentImage > 0 ? currentImage - 1 : currentImage;
                    updateWalkthrough();
                    return true;
            }
            return false;
        }
    };

    private void makeNotification() {
        Intent resultIntent = new Intent(this, MapsActivity.class);
        resultIntent.putExtra(Const.BIKE_TO, Const.LOC_WORK);
        resultIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent piResult = PendingIntent.getActivity(
                this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = this.getString(R.string.notif_title_good);
        String contentText = this.getString(R.string.notif_text_good);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(contentText)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(contentText))
                        .addAction (R.drawable.ic_action_bike,
                                getString(R.string.go_bike), piResult)
                        .addAction (R.drawable.ic_action_tram,
                                getString(R.string.go_tram), piResult);
                ;
        mBuilder.setContentIntent(piResult);

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(MORNING_NOTIFICATION, mBuilder.build());
    }

    private void updateWalkthrough() {
        walkthroughImage.setImageResource(walkthroughImages[currentImage]);
        if (currentImage == 2) {
            makeNotification();
        }
    }

    private void gotoMapsActivity() {
        startActivity(new Intent(this, MapsActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_through);

        walkthroughImage = (ImageView) findViewById(R.id.walkthrough_image);
        updateWalkthrough();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }
}
