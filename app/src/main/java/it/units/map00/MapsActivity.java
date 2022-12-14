package it.units.map00;

import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.List;


import it.units.map00.databinding.ActivityMapsBinding;
import it.units.map00.entities.Bacaro;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FirebaseFirestore db;
    private Polyline polyline = null;
    private GeoApiContext mGeoApiContext = null;
    private static final String TAG = "MapsActivity";
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng center = new LatLng(45.4341668, 12.3343518);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 13));

        db = FirebaseFirestore.getInstance();
        CollectionReference myRef = db.collection("Bacari");

        myRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                Bacaro bacaro = doc.toObject(Bacaro.class);
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(bacaro.getLat(),bacaro.getLng()))
                        .title(bacaro.getName())
                        .snippet(bacaro.getShortDescription())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }

        });

        mMap.setMyLocationEnabled(true);

        mMap.setOnInfoWindowClickListener(marker -> {
            Intent intent = new Intent(MapsActivity.this, BacaroActivity.class);
            intent.putExtra("name", marker.getTitle());
            startActivity(intent);

        });

        mMap.setOnMarkerClickListener(marker -> {
            calculateDirection(marker);
            return false;
        });

        ApplicationInfo ai = null;
        try {
            ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Bundle bundle = ai.metaData;
        String myApiKey = bundle.getString("com.google.android.geo.API_KEY");

        if (mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(myApiKey)
                    .build();
        }

    }

    @SuppressLint("MissingPermission")
    private void calculateDirection(Marker marker){
        Log.d(TAG, "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        if (location != null) {

                            com.google.maps.model.LatLng origin = new com.google.maps.model.LatLng(location.getLatitude(), location.getLongitude());
                            DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

                            directions.alternatives(true).origin(origin).mode(TravelMode.WALKING);
                            Log.d(TAG, "calculateDirections: origin: " + origin.toString());
                            Log.d(TAG, "calculateDirections: destination: " + destination.toString());
                            directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
                                @Override
                                public void onResult(DirectionsResult result) {
                                    Log.d(TAG, "onResult: routes: " + result.routes[0].toString());
                                    Log.d(TAG, "onResult: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());
                                    addPolylinesToMap(result);
                                }

                                @Override
                                public void onFailure(Throwable e) {
                                    Log.e(TAG, "onFailure: " + e.getMessage() );

                                }
                            });
                        }
                    }
                });

    }

    private void addPolylinesToMap(final DirectionsResult result){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                    DirectionsRoute route = result.routes[0];
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    for(com.google.maps.model.LatLng latLng: decodedPath){

                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));


                    if (polyline != null) {
                        polyline.remove();
                    }
                    polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(Color.BLUE);

                }
            }
        });
    }

}