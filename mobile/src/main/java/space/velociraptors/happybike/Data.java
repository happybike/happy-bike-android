package space.velociraptors.happybike;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by Mihai Balint on 5/27/17.
 */

class SyncValue implements ValueEventListener {
    private DataSnapshot data;
    private DatabaseError err;
    private boolean complete = false;

    public boolean isComplete() {
        return complete;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        this.data = dataSnapshot;
        complete = true;
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        this.err = databaseError;
        complete = true;
    }

    public DataSnapshot get() {
        while(!this.isComplete()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // meh
            }
        }
        if (this.err != null) {
            throw new RuntimeException(this.err.getMessage());
        }
        return data;
    }
}

public class Data {
    private FirebaseDatabase database;

    public Data() {
        database = FirebaseDatabase.getInstance();
    }

    public void put(String key, String value) {
        DatabaseReference myRef = database.getReference(key);
        myRef.setValue(value);
    }

    public DataSnapshot get(String key) {
        SyncValue value = new SyncValue();
        get(key, value);
        return value.get();
    }

    public void get(String key, ValueEventListener listener) {
        DatabaseReference myRef = database.getReference(key);
        myRef.addValueEventListener(listener);
    }
}
