package space.velociraptors.happybike;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RemoteViews;

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

    private Notification bigPicture(PendingIntent goBike, PendingIntent goPublicTransport) {
        String title = this.getString(R.string.notif_title_good);
        String contentText = this.getString(R.string.notif_text_good);

        Bitmap iconBike = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_action_bike);
        Bitmap picBike = BitmapFactory.decodeResource(getResources(),
                R.drawable.bikeway);

        NotificationCompat.Style notifStyle = new NotificationCompat.BigPictureStyle()
                .bigLargeIcon(iconBike)
                .bigPicture(picBike)
                .setBigContentTitle(title)
                .setSummaryText(contentText);


        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(notifStyle)
                .addAction (R.drawable.ic_action_bike,
                        getString(R.string.go_bike), goBike)
                .addAction (R.drawable.ic_action_tram,
                        getString(R.string.go_tram), goPublicTransport)
                .setContentIntent(goBike)
                .build();
    }

    private Notification bigCustom(PendingIntent goBike, PendingIntent goPublicTransport) {
        String title = this.getString(R.string.notif_title_good);
        String contentText = this.getString(R.string.notif_text_good);

        RemoteViews expandedView = new RemoteViews(this.getPackageName(),
                R.layout.notification_morning);
        expandedView.setTextViewText(R.id.temperature, "25°C");
        expandedView.setTextViewText(R.id.location, "Timisoara");

        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(contentText)
                .setCustomBigContentView(expandedView)
                .addAction (R.drawable.ic_action_bike,
                        getString(R.string.go_bike), goBike)
                .addAction (R.drawable.ic_action_tram,
                        getString(R.string.go_tram), goPublicTransport)
                .setContentIntent(goBike)
                .build();
    }


    private void makeNotification() {
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(Const.BIKE_TO, Const.LOC_WORK);
        resultIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent piResult = PendingIntent.getActivity(
                this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notifView = bigCustom(piResult, piResult);

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(MORNING_NOTIFICATION, notifView);
    }

    private void updateWalkthrough() {
        walkthroughImage.setImageResource(walkthroughImages[currentImage]);
        if (currentImage == 2) {
            makeNotification();
        }
    }

    private void gotoMapsActivity() {
        startActivity(new Intent(this, MainActivity.class));
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
