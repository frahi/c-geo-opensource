package cgeo.geocaching.apps;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;

import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.LocusUtils;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataWaypoint;
import menion.android.locus.addon.publiclib.geoData.PointsData;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * for the Locus API:
 *
 * @see http://forum.asamm.cz/viewtopic.php?f=29&t=767
 */
public abstract class AbstractLocusApp extends AbstractApp {
    private static final String INTENT = Intent.ACTION_VIEW;
    private static final SimpleDateFormat ISO8601DATE = new SimpleDateFormat("yyyy-MM-dd'T'");

    protected AbstractLocusApp() {
        super(getString(R.string.caches_map_locus), INTENT);
    }

    @Override
    public boolean isInstalled(Context context) {
        return LocusUtils.isLocusAvailable(context);
    }

    /**
     * Display a list of caches / waypoints in Locus
     *
     * @param objectsToShow
     *            which caches/waypoints to show
     * @param withCacheWaypoints
     *            wether to give waypoints of caches to Locus or not
     * @param activity
     * @author koem
     */
    protected static boolean showInLocus(final List<? extends Object> objectsToShow, final boolean withCacheWaypoints,
            final Activity activity) {
        if (objectsToShow == null || objectsToShow.isEmpty()) {
            return false;
        }

        final boolean withCacheDetails = objectsToShow.size() < 200;
        final PointsData pd = new PointsData("c:geo");
        for (Object o : objectsToShow) {
            Point p = null;
            // get icon and Point
            if (o instanceof cgCache) {
                p = getCachePoint((cgCache) o, withCacheWaypoints, withCacheDetails);
            } else if (o instanceof cgWaypoint) {
                p = getWaypointPoint((cgWaypoint) o);
            }
            if (p != null) {
                pd.addPoint(p);
            }
        }

        if (pd.getPoints().isEmpty()) {
            return false;
        }

        if (pd.getPoints().size() <= 1000) {
            DisplayData.sendData(activity, pd, false);
        } else {
            final ArrayList<PointsData> data = new ArrayList<PointsData>();
            data.add(pd);
            DisplayData.sendDataCursor(activity, data,
                    "content://" + LocusDataStorageProvider.class.getCanonicalName().toLowerCase(),
                    false);
        }

        return true;
    }

    /**
     * This method constructs a <code>Point</code> for displaying in Locus
     *
     * @param cache
     * @param withWaypoints
     *            whether to give waypoints to Locus or not
     * @param withCacheDetails
     *            whether to give cache details (description, hint) to Locus or not
     *            should be false for all if more then 200 Caches are transferred
     * @return null, when the <code>Point</code> could not be constructed
     * @author koem
     */
    private static Point getCachePoint(cgCache cache, boolean withWaypoints, boolean withCacheDetails) {
        if (cache == null || cache.getCoords() == null) {
            return null;
        }

        // create one simple point with location
        final Location loc = new Location(Settings.tag);
        loc.setLatitude(cache.getCoords().getLatitude());
        loc.setLongitude(cache.getCoords().getLongitude());

        final Point p = new Point(cache.getName(), loc);
        final PointGeocachingData pg = new PointGeocachingData();
        p.setGeocachingData(pg);

        // set data in Locus' cache
        pg.cacheID = cache.getGeocode();
        pg.available = !cache.isDisabled();
        pg.archived = cache.isArchived();
        pg.premiumOnly = cache.isPremiumMembersOnly();
        pg.name = cache.getName();
        pg.placedBy = cache.getOwner();
        if (cache.getHiddenDate() != null) {
            pg.hidden = ISO8601DATE.format(Long.valueOf(cache.getHiddenDate().getTime()));
        }
        int locusId = toLocusType(cache.getType());
        if (locusId != NO_LOCUS_ID) {
            pg.type = locusId;
        }
        locusId = toLocusSize(cache.getSize());
        if (locusId != NO_LOCUS_ID) {
            pg.container = locusId;
        }
        if (cache.getDifficulty() > 0) {
            pg.difficulty = cache.getDifficulty();
        }
        if (cache.getTerrain() > 0) {
            pg.terrain = cache.getTerrain();
        }
        pg.found = cache.isFound();

        if (withWaypoints && cache.hasWaypoints()) {
            pg.waypoints = new ArrayList<PointGeocachingDataWaypoint>();
            for (cgWaypoint waypoint : cache.getWaypoints()) {
                if (waypoint == null || waypoint.getCoords() == null) {
                    continue;
                }
                PointGeocachingDataWaypoint wp = new PointGeocachingDataWaypoint();
                wp.code = waypoint.getGeocode();
                wp.name = waypoint.getName();
                String locusWpId = toLocusWaypoint(waypoint.getWaypointType());
                if (locusWpId != null) {
                    wp.type = locusWpId;
                }
                wp.lat = waypoint.getCoords().getLatitude();
                wp.lon = waypoint.getCoords().getLongitude();
                pg.waypoints.add(wp);
            }
        }

        // Other properties of caches. When there are many caches to be displayed
        // in Locus, using these properties can lead to Exceptions in Locus.
        // Should not be used if caches count > 200

        if (withCacheDetails) {
            pg.shortDescription = cache.getShortDescription();
            pg.longDescription = cache.getDescription();
            pg.encodedHints = cache.getHint();
        }

        return p;
    }

    /**
     * This method constructs a <code>Point</code> for displaying in Locus
     *
     * @param waypoint
     * @return null, when the <code>Point</code> could not be constructed
     * @author koem
     */
    private static Point getWaypointPoint(cgWaypoint waypoint) {
        if (waypoint == null || waypoint.getCoords() == null) {
            return null;
        }

        // create one simple point with location
        final Location loc = new Location(Settings.tag);
        loc.setLatitude(waypoint.getCoords().getLatitude());
        loc.setLongitude(waypoint.getCoords().getLongitude());

        final Point p = new Point(waypoint.getName(), loc);
        p.setDescription("<a href=\"" + waypoint.getUrl() + "\">"
                + waypoint.getGeocode() + "</a>");

        return p;
    }

    private static final int NO_LOCUS_ID = -1;

    private static int toLocusType(final CacheType ct) {
        switch (ct) {
            case TRADITIONAL:
                return PointGeocachingData.CACHE_TYPE_TRADITIONAL;
            case MULTI:
                return PointGeocachingData.CACHE_TYPE_MULTI;
            case MYSTERY:
                return PointGeocachingData.CACHE_TYPE_MYSTERY;
            case LETTERBOX:
                return PointGeocachingData.CACHE_TYPE_LETTERBOX;
            case EVENT:
                return PointGeocachingData.CACHE_TYPE_EVENT;
            case MEGA_EVENT:
                return PointGeocachingData.CACHE_TYPE_MEGA_EVENT;
            case EARTH:
                return PointGeocachingData.CACHE_TYPE_EARTH;
            case CITO:
                return PointGeocachingData.CACHE_TYPE_CACHE_IN_TRASH_OUT;
            case WEBCAM:
                return PointGeocachingData.CACHE_TYPE_WEBCAM;
            case VIRTUAL:
                return PointGeocachingData.CACHE_TYPE_VIRTUAL;
            case WHERIGO:
                return PointGeocachingData.CACHE_TYPE_WHERIGO;
            case PROJECT_APE:
                return PointGeocachingData.CACHE_TYPE_PROJECT_APE;
            case GPS_EXHIBIT:
                return PointGeocachingData.CACHE_TYPE_GPS_ADVENTURE;
            default:
                return NO_LOCUS_ID;
        }
    }

    private static int toLocusSize(final CacheSize cs) {
        switch (cs) {
            case MICRO:
                return PointGeocachingData.CACHE_SIZE_MICRO;
            case SMALL:
                return PointGeocachingData.CACHE_SIZE_SMALL;
            case REGULAR:
                return PointGeocachingData.CACHE_SIZE_REGULAR;
            case LARGE:
                return PointGeocachingData.CACHE_SIZE_LARGE;
            case NOT_CHOSEN:
                return PointGeocachingData.CACHE_SIZE_NOT_CHOSEN;
            case OTHER:
                return PointGeocachingData.CACHE_SIZE_OTHER;
            default:
                return NO_LOCUS_ID;
        }
    }

    private static String toLocusWaypoint(final WaypointType wt) {
        switch (wt) {
            case FINAL:
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_FINAL;
            case OWN:
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES;
            case PARKING:
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_PARKING;
            case PUZZLE:
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_QUESTION;
            case STAGE:
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES;
            case TRAILHEAD:
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_TRAILHEAD;
            case WAYPOINT:
                return PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES;
            default:
                return null;
        }
    }

}
