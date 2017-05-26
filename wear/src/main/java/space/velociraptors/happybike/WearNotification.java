package space.velociraptors.happybike;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class WearNotification extends Activity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_notification);
        mTextView = (TextView) findViewById(R.id.text);
    }
}
