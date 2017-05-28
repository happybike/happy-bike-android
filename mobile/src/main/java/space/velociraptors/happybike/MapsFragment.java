package space.velociraptors.happybike;

import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.data.kml.KmlLayer;


import android.support.v7.app.AlertDialog;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import okhttp3.MediaType;


public class MapsFragment extends Fragment implements DownloadCompleteListener, LocationListener {

    MapView mMapView;
    private GoogleMap googleMap;
    private ProgressDialog mProgressDialog;
    private Marker currLocationMarker;
    private String alertText = null;
    private double alertLon, alertLat;

    public static MapsFragment newInstance() {
        MapsFragment fragment = new MapsFragment();
        return fragment;
    }

    public void setAlert(String text, double lon, double lat) {
        this.alertLat = lat;
        this.alertLon = lon;
        this.alertText = text;
    }

    private void addAlertMarker() {
        if (alertText == null)
            return;
        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(alertLat, alertLon))
                .title(alertText));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        mMapView = (MapView) rootView.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {

                googleMap = mMap;

                try {
                    KmlLayer layer = new KmlLayer(mMap, R.raw.pistebici, getContext());
                    layer.addLayerToMap();
                    new KmlLayer(mMap, R.raw.places_to_visit, getContext()).addLayerToMap();
//                    new KmlLayer(mMap, R.raw.bicycle_danger_zones_update, getContext()).addLayerToMap();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                addAlertMarker();
                // For showing a move to my location button
                if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
//                googleMap.setMyLocationEnabled(true);
                if (isNetworkConnected()) {
                    mProgressDialog = new ProgressDialog(getActivity());
                    mProgressDialog.setMessage("Please wait...");
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.show();

                    startDownload();
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("No Internet Connection")
                            .setMessage("It looks like your internet connection is off. Please turn it " +
                                    "on and try again")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                }
                drawMarker();
            }
        });

        return rootView;
    }

    private MainActivity getMainActivity() {
        return (MainActivity)getActivity();
    }

    private void drawMarker() {
        if(currLocationMarker != null) {
            currLocationMarker.remove();
        }

        LatLng myLocation = getMainActivity().getMyLocation();
        Drawable drawable = getActivity().getResources().getDrawable(R.drawable.pinhappy);
        MarkerOptions markerOptions = new MarkerOptions().position(myLocation).title("I am here")
                .icon(BitmapDescriptorFactory.fromBitmap(((BitmapDrawable)drawable).getBitmap()));
        currLocationMarker = googleMap.addMarker(markerOptions);

        // For zooming automatically to the location of the marker
        CameraPosition cameraPosition = new CameraPosition.Builder().target(myLocation).zoom(16).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


    private boolean isNetworkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }


    private void startDownload() {
        getStations("http://www.velotm.ro/Station/Read");

        if (mProgressDialog != null) {
            mProgressDialog.hide();
        }

    }

    private JSONObject station;
    private double latitude;
    private double longitude;
    private String title;
    private int noBikes;

    private Drawable getStationPin(int noBikes) {
        int pin = R.drawable.pinyellow;
        if (noBikes < 5) {
            pin = R.drawable.pingray;
        } else if (noBikes < 15) {
            pin = R.drawable.pinblue;
        }

        //noinspection ResourceType
        return getActivity().getResources().getDrawable(pin);
    }

    @Override
    public void downloadComplete(JSONArray stations) {

        for(int i = 0 ; i < stations.length() ; i++ ) {
            try {
                station = stations.getJSONObject(i);
                latitude = station.getDouble("Latitude");
                longitude = station.getDouble("Longitude");
                title = station.getString("StationName");
                noBikes = station.getInt("EmptySpots");

            } catch (JSONException e) {
                e.printStackTrace();
            }
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .title(title)
                    .icon(BitmapDescriptorFactory.fromBitmap(((BitmapDrawable)getStationPin(noBikes)).getBitmap())));
        }
    }

    public String postBody = "{}";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private void getStations(String url) {

        try {
            JSONObject jsonObj = new JSONObject("{\n" +
                    "  \"Data\": [\n" +
                    "    {\n" +
                    "      \"StationName\": \"Regele Ferdinand\",\n" +
                    "      \"Address\": \"Str. Regele Ferdinand\",\n" +
                    "      \"OcuppiedSpots\": 9,\n" +
                    "      \"EmptySpots\": 11,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.751129,\n" +
                    "      \"Longitude\": 21.223758,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 1\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Torontanului Bucovinei\",\n" +
                    "      \"Address\": \"Torontanului cu Bucovinei\",\n" +
                    "      \"OcuppiedSpots\": 9,\n" +
                    "      \"EmptySpots\": 11,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.772036,\n" +
                    "      \"Longitude\": 21.216819,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 2\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Liege\",\n" +
                    "      \"Address\": \"Liege\",\n" +
                    "      \"OcuppiedSpots\": 11,\n" +
                    "      \"EmptySpots\": 9,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.774997,\n" +
                    "      \"Longitude\": 21.221271,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 3\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Ana Ipatescu\",\n" +
                    "      \"Address\": \"Ana Ipatescu\",\n" +
                    "      \"OcuppiedSpots\": 13,\n" +
                    "      \"EmptySpots\": 7,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.728312,\n" +
                    "      \"Longitude\": 21.204948,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 4\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Kogalniceanu\",\n" +
                    "      \"Address\": \"Kogalniceanu\",\n" +
                    "      \"OcuppiedSpots\": 7,\n" +
                    "      \"EmptySpots\": 12,\n" +
                    "      \"MaximumNumberOfBikes\": 19,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.762782,\n" +
                    "      \"Longitude\": 21.247635,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 5\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Carol 1\",\n" +
                    "      \"Address\": \"Carol 1 cu Dragalina\",\n" +
                    "      \"OcuppiedSpots\": 4,\n" +
                    "      \"EmptySpots\": 14,\n" +
                    "      \"MaximumNumberOfBikes\": 18,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.744746,\n" +
                    "      \"Longitude\": 21.210786,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 6\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Profi\",\n" +
                    "      \"Address\": \"Profi - Str Sagului cu Str Rebreanu\",\n" +
                    "      \"OcuppiedSpots\": 6,\n" +
                    "      \"EmptySpots\": 13,\n" +
                    "      \"MaximumNumberOfBikes\": 19,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.732855,\n" +
                    "      \"Longitude\": 21.209021,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 7\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Take Ionescu\",\n" +
                    "      \"Address\": \"Take Ionescu\",\n" +
                    "      \"OcuppiedSpots\": 8,\n" +
                    "      \"EmptySpots\": 11,\n" +
                    "      \"MaximumNumberOfBikes\": 19,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.757976,\n" +
                    "      \"Longitude\": 21.234135,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 8\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Mihai Viteazu\",\n" +
                    "      \"Address\": \"Mihai Viteazu\",\n" +
                    "      \"OcuppiedSpots\": 9,\n" +
                    "      \"EmptySpots\": 11,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.744931,\n" +
                    "      \"Longitude\": 21.225744,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 11\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Piata Marasti\",\n" +
                    "      \"Address\": \"Piata Marasti\",\n" +
                    "      \"OcuppiedSpots\": 9,\n" +
                    "      \"EmptySpots\": 10,\n" +
                    "      \"MaximumNumberOfBikes\": 19,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.759353,\n" +
                    "      \"Longitude\": 21.228378,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 12\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Ripensia\",\n" +
                    "      \"Address\": \"Sala Olimpia\",\n" +
                    "      \"OcuppiedSpots\": 3,\n" +
                    "      \"EmptySpots\": 17,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.745247,\n" +
                    "      \"Longitude\": 21.241633,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 13\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Aries\",\n" +
                    "      \"Address\": \"Bulbuca cu Aries\",\n" +
                    "      \"OcuppiedSpots\": 3,\n" +
                    "      \"EmptySpots\": 17,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.738001,\n" +
                    "      \"Longitude\": 21.242053,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 14\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Kaufland\",\n" +
                    "      \"Address\": \"Kaufland(Gh Lazar)\",\n" +
                    "      \"OcuppiedSpots\": 7,\n" +
                    "      \"EmptySpots\": 12,\n" +
                    "      \"MaximumNumberOfBikes\": 19,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.760539,\n" +
                    "      \"Longitude\": 21.218665,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 15\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Armoniei\",\n" +
                    "      \"Address\": \"Armoniei\",\n" +
                    "      \"OcuppiedSpots\": 8,\n" +
                    "      \"EmptySpots\": 32,\n" +
                    "      \"MaximumNumberOfBikes\": 40,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.77989,\n" +
                    "      \"Longitude\": 21.23454,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 16\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Lct. Ovidiu Balea\",\n" +
                    "      \"Address\": \"Lct Ovidiu Balea\",\n" +
                    "      \"OcuppiedSpots\": 6,\n" +
                    "      \"EmptySpots\": 34,\n" +
                    "      \"MaximumNumberOfBikes\": 40,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.76647,\n" +
                    "      \"Longitude\": 21.19393,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 17\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Posta Centrala\",\n" +
                    "      \"Address\": \"Posta Centrala\",\n" +
                    "      \"OcuppiedSpots\": 7,\n" +
                    "      \"EmptySpots\": 13,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.754707,\n" +
                    "      \"Longitude\": 21.233771,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 18\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Piata Virgil Economu\",\n" +
                    "      \"Address\": \"Piata Virgil Economu\",\n" +
                    "      \"OcuppiedSpots\": 16,\n" +
                    "      \"EmptySpots\": 24,\n" +
                    "      \"MaximumNumberOfBikes\": 40,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.766022,\n" +
                    "      \"Longitude\": 21.261308,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 19\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Felix\",\n" +
                    "      \"Address\": \"Felix\",\n" +
                    "      \"OcuppiedSpots\": 10,\n" +
                    "      \"EmptySpots\": 30,\n" +
                    "      \"MaximumNumberOfBikes\": 40,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.777772,\n" +
                    "      \"Longitude\": 21.220477,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 20\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Piata Mocioni\",\n" +
                    "      \"Address\": \"Piata Mocioni\",\n" +
                    "      \"OcuppiedSpots\": 5,\n" +
                    "      \"EmptySpots\": 15,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.746137,\n" +
                    "      \"Longitude\": 21.215054,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 21\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Piata 700\",\n" +
                    "      \"Address\": \"Piata 700\",\n" +
                    "      \"OcuppiedSpots\": 6,\n" +
                    "      \"EmptySpots\": 14,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.75651,\n" +
                    "      \"Longitude\": 21.2233,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 22\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Cons. Europei\",\n" +
                    "      \"Address\": \"Piata Consiliul Europei\",\n" +
                    "      \"OcuppiedSpots\": 13,\n" +
                    "      \"EmptySpots\": 7,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.765616,\n" +
                    "      \"Longitude\": 21.22593,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 23\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Divizia 9 Cavalerie\",\n" +
                    "      \"Address\": \"Divizia 9 Cavalerie\",\n" +
                    "      \"OcuppiedSpots\": 9,\n" +
                    "      \"EmptySpots\": 11,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.769026,\n" +
                    "      \"Longitude\": 21.229961,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 24\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Piata Leonardo DaVinci\",\n" +
                    "      \"Address\": \"Piata Leonardo DaVinci\",\n" +
                    "      \"OcuppiedSpots\": 10,\n" +
                    "      \"EmptySpots\": 8,\n" +
                    "      \"MaximumNumberOfBikes\": 18,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.748328,\n" +
                    "      \"Longitude\": 21.235671,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 25\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Domasneanu\",\n" +
                    "      \"Address\": \"Domasneanu\",\n" +
                    "      \"OcuppiedSpots\": 7,\n" +
                    "      \"EmptySpots\": 31,\n" +
                    "      \"MaximumNumberOfBikes\": 38,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.732791,\n" +
                    "      \"Longitude\": 21.258271,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 26\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Libertatii\",\n" +
                    "      \"Address\": \"Libertatii\",\n" +
                    "      \"OcuppiedSpots\": 10,\n" +
                    "      \"EmptySpots\": 10,\n" +
                    "      \"MaximumNumberOfBikes\": 20,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.75596,\n" +
                    "      \"Longitude\": 21.22676,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 27\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Statie Virtuala\",\n" +
                    "      \"Address\": \"asdasd\",\n" +
                    "      \"OcuppiedSpots\": 0,\n" +
                    "      \"EmptySpots\": 0,\n" +
                    "      \"MaximumNumberOfBikes\": 0,\n" +
                    "      \"LastSyncDate\": \"01.01.2013 00:00\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Offline\",\n" +
                    "      \"Latitude\": 0,\n" +
                    "      \"Longitude\": 0,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 28\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Statia7-Dragalina\",\n" +
                    "      \"Address\": \"Pod Dragalina\",\n" +
                    "      \"OcuppiedSpots\": 5,\n" +
                    "      \"EmptySpots\": 14,\n" +
                    "      \"MaximumNumberOfBikes\": 19,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.74702,\n" +
                    "      \"Longitude\": 21.20929,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 29\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Statia2 - Badea Cartan\",\n" +
                    "      \"Address\": \"statia 2 badea cartan\",\n" +
                    "      \"OcuppiedSpots\": 2,\n" +
                    "      \"EmptySpots\": 16,\n" +
                    "      \"MaximumNumberOfBikes\": 18,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.76063,\n" +
                    "      \"Longitude\": 21.24929,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 30\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"3 - Mocioni Prefectura\",\n" +
                    "      \"Address\": \"statia 3 mocioni prefectura \",\n" +
                    "      \"OcuppiedSpots\": 1,\n" +
                    "      \"EmptySpots\": 17,\n" +
                    "      \"MaximumNumberOfBikes\": 18,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.75676,\n" +
                    "      \"Longitude\": 21.24095,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 31\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"4 - Pod DaVinci\",\n" +
                    "      \"Address\": \"Statia4 - Pod DaVinci\",\n" +
                    "      \"OcuppiedSpots\": 2,\n" +
                    "      \"EmptySpots\": 37,\n" +
                    "      \"MaximumNumberOfBikes\": 39,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.75032,\n" +
                    "      \"Longitude\": 21.23538,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 32\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"8 - Golf\",\n" +
                    "      \"Address\": \"Statia 8 - Golf\",\n" +
                    "      \"OcuppiedSpots\": 1,\n" +
                    "      \"EmptySpots\": 18,\n" +
                    "      \"MaximumNumberOfBikes\": 19,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.74241,\n" +
                    "      \"Longitude\": 21.19629,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 33\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"1 - Modern\",\n" +
                    "      \"Address\": \"Statia 1 - Modern\",\n" +
                    "      \"OcuppiedSpots\": 0,\n" +
                    "      \"EmptySpots\": 39,\n" +
                    "      \"MaximumNumberOfBikes\": 39,\n" +
                    "      \"LastSyncDate\": \"26.05.2017 14:33\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Offline\",\n" +
                    "      \"Latitude\": 45.76042,\n" +
                    "      \"Longitude\": 21.25849,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 34\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Statia5 - Savoy\",\n" +
                    "      \"Address\": \"Statia5 - Savoy\",\n" +
                    "      \"OcuppiedSpots\": 10,\n" +
                    "      \"EmptySpots\": 30,\n" +
                    "      \"MaximumNumberOfBikes\": 40,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.74858,\n" +
                    "      \"Longitude\": 21.22507,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 35\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"Statia9 - Pod Modos\",\n" +
                    "      \"Address\": \"Statia9 - Pod Modos\",\n" +
                    "      \"OcuppiedSpots\": 2,\n" +
                    "      \"EmptySpots\": 37,\n" +
                    "      \"MaximumNumberOfBikes\": 39,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.7389,\n" +
                    "      \"Longitude\": 21.18542,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 36\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"StationName\": \"6 - Pod Traian\",\n" +
                    "      \"Address\": \"Pod Traian\",\n" +
                    "      \"OcuppiedSpots\": 3,\n" +
                    "      \"EmptySpots\": 35,\n" +
                    "      \"MaximumNumberOfBikes\": 38,\n" +
                    "      \"LastSyncDate\": \"27.05.2017 13:01\",\n" +
                    "      \"IdStatus\": 1,\n" +
                    "      \"Status\": \"Functionala\",\n" +
                    "      \"StatusType\": \"Subpopulated\",\n" +
                    "      \"Latitude\": 45.74965,\n" +
                    "      \"Longitude\": 21.22027,\n" +
                    "      \"IsValid\": true,\n" +
                    "      \"CustomIsValid\": false,\n" +
                    "      \"Notifies\": [],\n" +
                    "      \"Id\": 37\n" +
                    "    }\n" +
                    "  ]}");
            JSONArray stations = jsonObj.getJSONArray("Data");

            downloadComplete(stations);
        } catch (JSONException e) {
            e.printStackTrace();
        }

//        OkHttpClient client = new OkHttpClient();
//        RequestBody body = RequestBody.create(JSON, postBody);
//        okhttp3.Request request = new okhttp3.Request.Builder().url(url).post(body).build();
//
//        client.newCall(request).enqueue(new okhttp3.Callback() {
//            @Override
//            public void onFailure(okhttp3.Call call, IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(okhttp3.Call call, okhttp3.Response response)
//                    throws IOException {
//                final String result = response.body().string();
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//
//                    }
//                });
//            }
//        });
    }

    @Override
    public void onLocationChanged(Location location) {
        drawMarker();
    }

}