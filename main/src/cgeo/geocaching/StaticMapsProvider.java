package cgeo.geocaching;

import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;

public class StaticMapsProvider {
    private static final String MARKERS_URL = "http://cgeo.carnero.cc/_markers/";
    /**
     * in my tests the "no image available" image had 5470 bytes, while "street only" maps had at least 20000 bytes
     */
    private static final int MIN_MAP_IMAGE_BYTES = 6000;

    public static File getMapFile(final String geocode, final int level, final boolean createDirs) {
        return LocalStorage.getStorageFile(geocode, "map_" + level, false, createDirs);
    }

    private static void downloadMapsInThread(final cgCache cache, String latlonMap, int edge, String waypoints) {
        downloadMap(cache, 20, "satellite", 1, latlonMap, edge, waypoints);
        downloadMap(cache, 18, "satellite", 2, latlonMap, edge, waypoints);
        downloadMap(cache, 16, "roadmap", 3, latlonMap, edge, waypoints);
        downloadMap(cache, 14, "roadmap", 4, latlonMap, edge, waypoints);
        downloadMap(cache, 11, "roadmap", 5, latlonMap, edge, waypoints);
    }

    private static void downloadMap(cgCache cache, int zoom, String mapType, int level, String latlonMap, int edge, String waypoints) {
        final String mapUrl = "http://maps.google.com/maps/api/staticmap?center=" + latlonMap;
        final String markerUrl = getMarkerUrl(cache);

        final String url = mapUrl + "&zoom=" + zoom + "&size=" + edge + "x" + edge + "&maptype=" + mapType + "&markers=icon%3A" + markerUrl + "%7C" + latlonMap + waypoints + "&sensor=false";

        final File file = getMapFile(cache.getGeocode(), level, true);
        final HttpResponse httpResponse = cgBase.request(url, null, false);

        if (httpResponse != null) {
            if (LocalStorage.saveEntityToFile(httpResponse.getEntity(), file)) {
                // Delete image if it has no contents
                final long fileSize = file.length();
                if (fileSize < MIN_MAP_IMAGE_BYTES) {
                    file.delete();
                }
            }
        }
    }

    public static void downloadMaps(cgCache cache, Activity activity) {
        if (!Settings.isStoreOfflineMaps() || cache.getCoords() == null || StringUtils.isBlank(cache.getGeocode())) {
            return;
        }

        final String latlonMap = cache.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA);
        final Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int maxWidth = display.getWidth() - 25;
        final int maxHeight = display.getHeight() - 25;
        int edge = 0;
        if (maxWidth > maxHeight) {
            edge = maxWidth;
        } else {
            edge = maxHeight;
        }

        final StringBuilder waypoints = new StringBuilder();
        if (cache.hasWaypoints()) {
            for (cgWaypoint waypoint : cache.getWaypoints()) {
                if (waypoint.getCoords() == null) {
                    continue;
                }

                waypoints.append("&markers=icon%3A");
                waypoints.append(MARKERS_URL);
                waypoints.append("marker_waypoint_");
                waypoints.append(waypoint.getWaypointType() != null ? waypoint.getWaypointType().id : null);
                waypoints.append(".png%7C");
                waypoints.append(waypoint.getCoords().format(Format.LAT_LON_DECDEGREE_COMMA));
            }
        }

        // download map images in separate background thread for higher performance
        downloadMaps(cache, latlonMap, edge, waypoints.toString());
    }

    private static void downloadMaps(final cgCache cache, final String latlonMap, final int edge,
            final String waypoints) {
        final Thread staticMapsThread = new Thread("getting static map") {
            @Override
            public void run() {
                downloadMapsInThread(cache, latlonMap, edge, waypoints);
            }
        };
        staticMapsThread.setPriority(Thread.MIN_PRIORITY);
        staticMapsThread.start();
    }

    private static String getMarkerUrl(final cgCache cache) {
        String type = cache.getType().id;
        if (cache.isFound()) {
            type += "_found";
        } else if (cache.isDisabled()) {
            type += "_disabled";
        }

        return cgBase.urlencode_rfc3986(MARKERS_URL + "marker_cache_" + type + ".png");
    }
}
