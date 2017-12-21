package uk.co.sundroid.activity.location;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import uk.co.sundroid.util.geo.Geocoder;
import uk.co.sundroid.R;
import uk.co.sundroid.R.drawable;
import uk.co.sundroid.R.id;
import uk.co.sundroid.R.layout;
import uk.co.sundroid.NavItem;
import uk.co.sundroid.domain.LocationDetails;
import uk.co.sundroid.util.geo.LatitudeLongitude;
import uk.co.sundroid.util.SharedPrefsHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static uk.co.sundroid.NavItem.NavItemLocation.*;

public class MapActivity extends AbstractLocationActivity implements OnMapClickListener, OnInfoWindowClickListener {
	
    private static final int DIALOG_MAPVIEW = 39879;

    private Handler handler = new Handler();

    private GoogleMap map;
    private LatLng mapCentre;
    private float mapZoom;
    private Marker mapMarker;
    private LatitudeLongitude mapLocation;
    private LocationDetails mapLocationDetails;
	
	@Override
	protected int getLayout() {
		return layout.loc_mapv2;
	}

	@Override
	protected String getViewTitle() {
		return "Map";
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        restoreInstanceState(savedInstanceState);
        setNavItems(Collections.singletonList(
                new NavItem("Page settings", drawable.icn_bar_viewsettings, HEADER, 0)
        ));
        setUpMapIfNeeded();
   	}

    @Override
    protected void onSaveInstanceState(Bundle state) {
        if (state != null && map != null && map.getCameraPosition() != null) {
            state.putDouble("mapCentreLat", map.getCameraPosition().target.latitude);
            state.putDouble("mapCentreLon", map.getCameraPosition().target.longitude);
            state.putFloat("mapZoom", map.getCameraPosition().zoom);
            state.putSerializable("mapLocation", mapLocation);
            state.putSerializable("mapLocationDetails", mapLocationDetails);
        }
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        restoreInstanceState(state);
        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onStop() {
        this.map = null;
        this.mapMarker = null;
        this.mapCentre = null;
        this.mapLocation = null;
        this.mapLocationDetails = null;
        super.onStop();
    }

    private void restoreInstanceState(Bundle state) {
        if (state != null) {
            try {
                mapCentre = new LatLng(state.getDouble("mapCentreLat"), state.getDouble("mapCentreLon"));
                mapZoom = state.getFloat("mapZoom");
                mapLocation = (LatitudeLongitude)state.getSerializable("mapLocation");
                mapLocationDetails = (LocationDetails)state.getSerializable("mapLocationDetails");
            } catch (Exception e) {
                // Default map
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        if (map == null) {
            ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMapAsync(googleMap -> {
                map = googleMap;
                setUpMap();
            });
        }
    }

    private void setUpMap() {

        LocationDetails location = SharedPrefsHelper.getSelectedLocation(this);

        // Hide the zoom controls as the button panel will cover it.
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);

        map.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        map.setOnInfoWindowClickListener(this);
        map.setOnMapClickListener(this);
        map.clear();

        String mapMode = SharedPrefsHelper.getLocMapMode(getApplicationContext());
        switch (mapMode) {
            case "normal":
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case "satellite":
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case "terrain":
                map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            case "hybrid":
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
        }

        if (mapCentre != null) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mapCentre, mapZoom);
            map.moveCamera(cameraUpdate);
        } else if (location != null) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLocation().getLatitude().getDoubleValue(), location.getLocation().getLongitude().getDoubleValue()), 6);
            map.moveCamera(cameraUpdate);
        }

        if (mapLocation != null) {
            addMarker();
        }

    }

    @Override
    public void onMapClick(LatLng latLng) {
        mapLocation = new LatitudeLongitude(latLng.latitude, latLng.longitude);
        mapLocationDetails = null;
        addMarker();
        startPointLookup(mapLocation);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(latLng);
        map.animateCamera(cameraUpdate);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (mapLocation != null && mapLocationDetails != null) {
            SharedPrefsHelper.saveSelectedLocation(this, mapLocationDetails);
            if (mapLocationDetails.getTimeZone() == null) {
                Intent intent = new Intent(getApplicationContext(), TimeZonePickerActivity.class);
                intent.putExtra(TimeZonePickerActivity.INTENT_MODE, TimeZonePickerActivity.MODE_SELECT);
                startActivityForResult(intent, TimeZonePickerActivity.REQUEST_TIMEZONE);
            } else {
                setResult(LocationSelectActivity.RESULT_LOCATION_SELECTED);
                finish();
            }
        }
    }

    private void addMarker() {
        map.clear();
        mapMarker = map.addMarker(new MarkerOptions()
                .position(new LatLng(mapLocation.getLatitude().getDoubleValue(), mapLocation.getLongitude().getDoubleValue()))
                .title("Fetching location")
                .icon(BitmapDescriptorFactory.fromResource(drawable.pixel)));
        mapMarker.showInfoWindow();
    }

    private void startPointLookup(final LatitudeLongitude location) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                updateLocationDetails(Geocoder.getLocationDetails(location, getApplicationContext()));
            }
        };
        thread.start();
    }

    private void updateLocationDetails(final LocationDetails locationDetails) {

        // Details may be null after rotate.
        if (locationDetails == null) {
            return;
        }

        // Data may be received after a new point has been tapped. Discard if so.
        if (mapLocation == null || !mapLocation.getAbbreviatedValue().equals(locationDetails.getLocation().getAbbreviatedValue())) {
            return;
        }

        mapLocationDetails = locationDetails;
        if (mapMarker != null) {
            handler.post(() -> mapMarker.showInfoWindow());
        }

    }

    class CustomInfoWindowAdapter implements InfoWindowAdapter {

        private final View window;
        private final View contents;

        CustomInfoWindowAdapter() {
            window = getLayoutInflater().inflate(layout.loc_mapv2_infowindow, null);
            contents = getLayoutInflater().inflate(layout.loc_mapv2_infowindow, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            render(window);
            return window;
        }

        @Override
        public View getInfoContents(Marker marker) {
            render(contents);
            return contents;
        }

        private void render(View view) {
            TextView title = view.findViewById(id.title);
            View button = view.findViewById(id.button);
            if (mapLocationDetails != null) {
                title.setText(mapLocationDetails.getDisplayName());
                button.setVisibility(View.VISIBLE);
            } else {
                title.setText("Loading...");
                button.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onNavItemSelected(int navItemAction) {
        showDialog(DIALOG_MAPVIEW);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_MAPVIEW) {
            List<String> names = Arrays.asList("Map", "Satellite", "Terrain", "Hybrid");
            int selectedIndex;
            String currentMapMode = SharedPrefsHelper.getLocMapMode(getApplicationContext());
            if (currentMapMode.equals("normal")) {
                selectedIndex = 0;
            } else if (currentMapMode.equals("satellite")) {
                selectedIndex = 1;
            } else if (currentMapMode.equals("terrain")) {
                selectedIndex = 2;
            } else if (currentMapMode.equals("hybrid")) {
                selectedIndex = 3;
            } else {
                selectedIndex = 4;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Map view");

            builder.setSingleChoiceItems(names.toArray(new CharSequence[names.size()]), selectedIndex, null);
            builder.setNegativeButton("Cancel", (dialog, id1) -> removeDialog(DIALOG_MAPVIEW));
            builder.setPositiveButton("OK", (dialog, id12) -> {
                if (dialog instanceof AlertDialog) {
                    int selectedItem = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                    if (selectedItem == 0) {
                        SharedPrefsHelper.setLocMapMode(getApplicationContext(), "normal");
                        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    } else if (selectedItem == 1) {
                        SharedPrefsHelper.setLocMapMode(getApplicationContext(), "satellite");
                        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    } else if (selectedItem == 2) {
                        SharedPrefsHelper.setLocMapMode(getApplicationContext(), "terrain");
                        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    } else if (selectedItem == 3) {
                        SharedPrefsHelper.setLocMapMode(getApplicationContext(), "hybrid");
                        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    }
                }
                removeDialog(DIALOG_MAPVIEW);
            });
            return builder.create();
        }
        return super.onCreateDialog(id);
    }

}