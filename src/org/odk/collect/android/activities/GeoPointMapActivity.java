
package org.odk.collect.android.activities;

import com.radicaldynamic.groupinform.R;
import org.odk.collect.android.widgets.GeoPointWidget;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

public class GeoPointMapActivity extends MapActivity implements LocationListener {

    private MapView mMapView;
    private TextView mLocationStatus;

    private MapController mMapController;
    private LocationManager mLocationManager;
    private Overlay mLocationOverlay;
    private Overlay mGeoPointOverlay;

    private GeoPoint mGeoPoint;
    private Location mLocation;
    private Button mAcceptLocation;
    private Button mCancelLocation;

    private boolean mCaptureLocation = true;
    private Button mShowLocation;

    private boolean mGPSOn = false;
    private boolean mNetworkOn = false;
    
    private static double LOCATION_ACCURACY = 5;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.geopoint_layout);

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            double[] location = intent.getDoubleArrayExtra(GeoPointWidget.LOCATION);
            mGeoPoint = new GeoPoint((int) (location[0] * 1E6), (int) (location[1] * 1E6));
            mCaptureLocation = false;
        }

        mMapView = (MapView) findViewById(R.id.mapview);
        mCancelLocation = (Button) findViewById(R.id.cancel_location);
        mCancelLocation.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mMapController = mMapView.getController();
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mMapView.setBuiltInZoomControls(true);
        mMapView.setSatellite(false);
        mMapController.setZoom(16);

        // make sure we have a good location provider before continuing
        List<String> providers = mLocationManager.getProviders(true);        
        for (String provider : providers) {
            if (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
                mGPSOn = true;
            }
            if (provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
                mNetworkOn = true;
            }
        }
        if (!mGPSOn && !mNetworkOn) {
            Toast.makeText(getBaseContext(), getString(R.string.provider_disabled_error),
                Toast.LENGTH_SHORT).show();
            finish();
        }

        mLocationOverlay = new MyLocationOverlay(this, mMapView);
        mMapView.getOverlays().add(mLocationOverlay);

        if (mCaptureLocation) {
            mLocationStatus = (TextView) findViewById(R.id.location_status);
            mAcceptLocation = (Button) findViewById(R.id.accept_location);
            mAcceptLocation.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    returnLocation();
                }
            });

        } else {

            mGeoPointOverlay = new Marker(mGeoPoint);
            mMapView.getOverlays().add(mGeoPointOverlay);

            ((Button) findViewById(R.id.accept_location)).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.location_status)).setVisibility(View.GONE);
            mShowLocation = ((Button) findViewById(R.id.show_location));
            mShowLocation.setVisibility(View.VISIBLE);
            mShowLocation.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    mMapController.animateTo(mGeoPoint);
                }
            });

        }
    }


    private void returnLocation() {
        if (mLocation != null) {
            Intent i = new Intent();
            i.putExtra(
                    com.radicaldynamic.groupinform.activities.FormEntryActivity.LOCATION_RESULT,
                mLocation.getLatitude() + " " + mLocation.getLongitude() + " "
                        + mLocation.getAltitude() + " " + mLocation.getAccuracy());
            setResult(RESULT_OK, i);
        }
        finish();
    }


    private String truncateFloat(float f) {
        return new DecimalFormat("#.##").format(f);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
        ((MyLocationOverlay) mLocationOverlay).disableMyLocation();

    }


    @Override
    protected void onResume() {
        super.onResume();

        ((MyLocationOverlay) mLocationOverlay).enableMyLocation();
        if (mGPSOn) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
        if (mNetworkOn) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        }

    }


    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }


    @Override
    public void onLocationChanged(Location location) {
        if (mCaptureLocation) {
            mLocation = location;
            if (mLocation != null) {
                mLocationStatus.setText(getString(R.string.location_provider_accuracy,
                    mLocation.getProvider(), truncateFloat(mLocation.getAccuracy())));
                mGeoPoint =
                    new GeoPoint((int) (mLocation.getLatitude() * 1E6),
                            (int) (mLocation.getLongitude() * 1E6));

                mMapController.animateTo(mGeoPoint);

                if (mLocation.getAccuracy() <= LOCATION_ACCURACY) {
                    returnLocation();
                }
            }
        }
    }


    @Override
    public void onProviderDisabled(String provider) {
    }


    @Override
    public void onProviderEnabled(String provider) {
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    class Marker extends Overlay {
        GeoPoint gp = null;


        public Marker(GeoPoint gp) {
            super();
            this.gp = gp;
        }


        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            super.draw(canvas, mapView, shadow);
            Point screenPoint = new Point();
            mMapView.getProjection().toPixels(gp, screenPoint);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_maps_indicator_current_position), screenPoint.x, screenPoint.y - 8,
                null); // -8 as image is 16px high
        }
    }

}
