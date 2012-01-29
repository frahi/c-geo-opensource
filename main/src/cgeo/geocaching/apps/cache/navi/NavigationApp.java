package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.App;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;

public interface NavigationApp extends App {
    public boolean invoke(final cgGeo geo, final Activity activity,
            final cgCache cache,
            final SearchResult search, final cgWaypoint waypoint,
            final Geopoint coords);
}
