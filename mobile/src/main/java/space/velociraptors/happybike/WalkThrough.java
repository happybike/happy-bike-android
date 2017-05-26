package space.velociraptors.happybike;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;

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
                    walkthroughImage.setImageResource(walkthroughImages[currentImage]);
                    return true;
                case R.id.walkthrough_previous:
                    currentImage = currentImage > 0 ? currentImage - 1 : currentImage;
                    walkthroughImage.setImageResource(walkthroughImages[currentImage]);
                    return true;
            }
            return false;
        }
    };

    private void gotoMapsActivity() {
        // TODO
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_through);

        walkthroughImage = (ImageView) findViewById(R.id.walkthrough_image);
        walkthroughImage.setImageResource(walkthroughImages[currentImage]);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }
}
