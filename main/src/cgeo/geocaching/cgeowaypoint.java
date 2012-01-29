package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class cgeowaypoint extends AbstractActivity {

    private static final int MENU_ID_NAVIGATION = 0;
    private static final int MENU_ID_CACHES_AROUND = 5;
    private static final int MENU_ID_DEFAULT_NAVIGATION = 2;
    private static final int MENU_ID_OPEN_GEOCACHE = 6;
    private cgWaypoint waypoint = null;
    private String geocode = null;
    private int id = -1;
    private ProgressDialog waitDialog = null;
    private cgGeo geo = null;
    private UpdateLocationCallback geoUpdate = new update();
    private Handler loadWaypointHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (waypoint == null) {
                    if (waitDialog != null) {
                        waitDialog.dismiss();
                        waitDialog = null;
                    }

                    showToast(res.getString(R.string.err_waypoint_load_failed));

                    finish();
                    return;
                } else {
                    final TextView identification = (TextView) findViewById(R.id.identification);
                    final TextView coords = (TextView) findViewById(R.id.coordinates);
                    final ImageView compass = (ImageView) findViewById(R.id.compass);
                    final View separator = findViewById(R.id.separator);

                    final View headline = findViewById(R.id.headline);
                    registerNavigationMenu(headline);

                    if (StringUtils.isNotBlank(waypoint.getName())) {
                        setTitle(Html.fromHtml(waypoint.getName().trim()).toString());
                    } else {
                        setTitle(res.getString(R.string.waypoint_title));
                    }

                    if (!waypoint.getPrefix().equalsIgnoreCase("OWN")) {
                        identification.setText(waypoint.getPrefix().trim() + "/" + waypoint.getLookup().trim());
                    } else {
                        identification.setText(res.getString(R.string.waypoint_custom));
                    }
                    registerNavigationMenu(identification);
                    waypoint.setIcon(res, identification);

                    if (waypoint.getCoords() != null) {
                        coords.setText(Html.fromHtml(waypoint.getCoords().toString()), TextView.BufferType.SPANNABLE);
                        compass.setVisibility(View.VISIBLE);
                        separator.setVisibility(View.VISIBLE);
                    } else {
                        coords.setText(res.getString(R.string.waypoint_unknown_coordinates));
                        compass.setVisibility(View.GONE);
                        separator.setVisibility(View.GONE);
                    }
                    registerNavigationMenu(coords);

                    if (StringUtils.isNotBlank(waypoint.getNote())) {
                        final TextView note = (TextView) findViewById(R.id.note);
                        note.setText(Html.fromHtml(waypoint.getNote().trim()), TextView.BufferType.SPANNABLE);
                        registerNavigationMenu(note);
                    }

                    Button buttonEdit = (Button) findViewById(R.id.edit);
                    buttonEdit.setOnClickListener(new editWaypointListener());

                    Button buttonDelete = (Button) findViewById(R.id.delete);
                    if (waypoint.isUserDefined()) {
                        buttonDelete.setOnClickListener(new deleteWaypointListener());
                        buttonDelete.setVisibility(View.VISIBLE);
                    }

                    if (waitDialog != null) {
                        waitDialog.dismiss();
                        waitDialog = null;
                    }
                }
            } catch (Exception e) {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                    waitDialog = null;
                }
                Log.e(Settings.tag, "cgeowaypoint.loadWaypointHandler: " + e.toString());
            }
        }

        private void registerNavigationMenu(View view) {
            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    registerForContextMenu(v);
                    if (navigationPossible()) {
                        openContextMenu(v);
                    }
                }
            });
        }
    };

    public cgeowaypoint() {
        super("c:geo-waypoint-details");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.waypoint);
        setTitle(R.string.waypoint_title);

        // get parameters
        Bundle extras = getIntent().getExtras();

        // try to get data from extras
        if (extras != null) {
            id = extras.getInt("waypoint");
            geocode = extras.getString("geocode");
        }

        if (id <= 0) {
            showToast(res.getString(R.string.err_waypoint_unknown));
            finish();
            return;
        }

        if (geo == null) {
            geo = app.startGeo(geoUpdate);
        }

        waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
        waitDialog.setCancelable(true);

        (new loadWaypoint()).start();
    }

    @Override
    public void onResume() {
        super.onResume();


        if (geo == null) {
            geo = app.startGeo(geoUpdate);
        }

        if (waitDialog == null) {
            waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
            waitDialog.setCancelable(true);

            (new loadWaypoint()).start();
        }
    }

    @Override
    public void onDestroy() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onDestroy();
    }

    @Override
    public void onStop() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onStop();
    }

    @Override
    public void onPause() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ID_DEFAULT_NAVIGATION, 0, NavigationAppFactory.getDefaultNavigationApplication(this).getName()).setIcon(android.R.drawable.ic_menu_compass); // default navigation tool

        SubMenu subMenu = menu.addSubMenu(1, MENU_ID_NAVIGATION, 0, res.getString(R.string.cache_menu_navigate)).setIcon(android.R.drawable.ic_menu_mapmode);
        addNavigationMenuItems(subMenu);

        menu.add(0, MENU_ID_CACHES_AROUND, 0, res.getString(R.string.cache_menu_around)).setIcon(android.R.drawable.ic_menu_rotate); // caches around
        menu.add(0, MENU_ID_OPEN_GEOCACHE, 0, res.getString(R.string.waypoint_menu_open_cache)).setIcon(android.R.drawable.ic_menu_mylocation); // open geocache

        return true;
    }

    private void addNavigationMenuItems(Menu menu) {
        NavigationAppFactory.addMenuItems(menu, this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        try {
            boolean visible = waypoint != null && waypoint.getCoords() != null;
            menu.findItem(MENU_ID_NAVIGATION).setVisible(visible);
            menu.findItem(MENU_ID_DEFAULT_NAVIGATION).setVisible(visible);
            menu.findItem(MENU_ID_CACHES_AROUND).setVisible(visible);

            boolean openGeocache = StringUtils.isEmpty(geocode) && StringUtils.isNotEmpty(waypoint.getGeocode());
            menu.findItem(MENU_ID_OPEN_GEOCACHE).setVisible(openGeocache);
        } catch (Exception e) {
            // nothing
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int menuItem = item.getItemId();
        if (menuItem == MENU_ID_DEFAULT_NAVIGATION) {
            goDefaultNavigation(null);
            return true;
        } else if (menuItem == MENU_ID_CACHES_AROUND) {
            cachesAround();
            return true;
        } else if (menuItem == MENU_ID_OPEN_GEOCACHE) {
            goToGeocache();
            return true;
        }

        return NavigationAppFactory.onMenuItemSelected(item, geo, this, null, null, waypoint, null);
    }

    private void cachesAround() {
        if (waypoint == null || waypoint.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
        }

        cgeocaches.startActivityCachesAround(this, waypoint.getCoords());

        finish();
    }

    private void goToGeocache() {
        if (waypoint == null || waypoint.getGeocode() == null) {
            showToast(res.getString(R.string.err_waypoint_open_cache_failed));
        }

        CacheDetailActivity.startActivity(this, waypoint.getGeocode());

        finish();
    }

    private class loadWaypoint extends Thread {

        @Override
        public void run() {
            try {
                waypoint = app.loadWaypoint(id);

                loadWaypointHandler.sendMessage(new Message());
            } catch (Exception e) {
                Log.e(Settings.tag, "cgeowaypoint.loadWaypoint.run: " + e.toString());
            }
        }
    }

    private class update implements UpdateLocationCallback {

        @Override
        public void updateLocation(cgGeo geo) {
            // nothing
        }
    }

    private class editWaypointListener implements View.OnClickListener {

        public void onClick(View arg0) {
            Intent editIntent = new Intent(cgeowaypoint.this, cgeowaypointadd.class);
            editIntent.putExtra("waypoint", id);
            startActivity(editIntent);
        }
    }

    private class deleteWaypointListener implements View.OnClickListener {

        public void onClick(View arg0) {
            if (app.deleteWaypoint(id)) {
                StaticMapsProvider.removeWpStaticMaps(id, geocode);
                cgeoapplication.removeCacheFromCache(geocode);

                finish();
                return;
            } else {
                showToast(res.getString(R.string.err_waypoint_delete_failed));
            }
        }
    }

    /**
     * @param view
     *            this method is also referenced from XML layout
     */
    public void goDefaultNavigation(View view) {
        if (!navigationPossible()) {
            return;
        }

        NavigationAppFactory.startDefaultNavigationApplication(geo, this, null, null, waypoint, null);
    }

    private boolean navigationPossible() {
        if (waypoint == null || waypoint.getCoords() == null) {
            showToast(res.getString(R.string.err_location_unknown));
            return false;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        if (navigationPossible()) {
            menu.setHeaderTitle(res.getString(R.string.cache_menu_navigate));
            addNavigationMenuItems(menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }
}
