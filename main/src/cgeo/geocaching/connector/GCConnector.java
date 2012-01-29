package cgeo.geocaching.connector;

import cgeo.geocaching.GCConstants;
import cgeo.geocaching.ParseResult;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.util.Log;

import java.util.regex.Pattern;

public class GCConnector extends AbstractConnector {

    private static GCConnector instance;
    private static final Pattern gpxZipFilePattern = Pattern.compile("\\d{7,}(_.+)?\\.zip", Pattern.CASE_INSENSITIVE);

    private GCConnector() {
        // singleton
    }

    public static GCConnector getInstance() {
        if (instance == null) {
            instance = new GCConnector();
        }
        return instance;
    }

    @Override
    public boolean canHandle(String geocode) {
        return StringUtils.startsWithIgnoreCase(geocode, "GC");
    }

    @Override
    public boolean supportsRefreshCache(cgCache cache) {
        return true;
    }

    @Override
    public String getCacheUrl(cgCache cache) {
        // it would also be possible to use "http://www.geocaching.com/seek/cache_details.aspx?wp=" + cache.getGeocode();
        return "http://coord.info/" + cache.getGeocode();
    }

    @Override
    public boolean supportsWatchList() {
        return true;
    }

    @Override
    public boolean supportsLogging() {
        return true;
    }

    @Override
    public String getName() {
        return "GeoCaching.com";
    }

    @Override
    public String getHost() {
        return "www.geocaching.com";
    }

    @Override
    public boolean supportsUserActions() {
        return true;
    }

    @Override
    public boolean supportsCachesAround() {
        return true;
    }

    @Override
    public ParseResult searchByGeocode(final String geocode, final String guid, final cgeoapplication app, final int listId, final CancellableHandler handler) {

        if (app == null) {
            Log.e(Settings.tag, "cgeoBase.searchByGeocode: No application found");
            return null;
        }

        final Parameters params = new Parameters("decrypt", "y");
        if (StringUtils.isNotBlank(geocode)) {
            params.put("wp", geocode);
        } else if (StringUtils.isNotBlank(guid)) {
            params.put("guid", guid);
        }
        params.put("log", "y");
        params.put("numlogs", String.valueOf(GCConstants.NUMBER_OF_LOGS));

        cgBase.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final String page = cgBase.requestLogged("http://www.geocaching.com/seek/cache_details.aspx", params, false, false, false);

        if (StringUtils.isEmpty(page)) {
            ParseResult search = new ParseResult();
            if (app.isThere(geocode, guid, true, false)) {
                if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(guid)) {
                    Log.i(Settings.tag, "Loading old cache from cache.");

                    search.addGeocode(app.getGeocode(guid));
                } else {
                    search.addGeocode(geocode);
                }
                search.error = StatusCode.NO_ERROR;
                return search;
            }

            Log.e(Settings.tag, "cgeoBase.searchByGeocode: No data from server");
            search.error = StatusCode.COMMUNICATION_ERROR;
            return search;
        }

        final ParseResult parseResult = cgBase.parseCache(page, listId, handler);

        if (parseResult == null || CollectionUtils.isEmpty(parseResult.cacheList)) {

            Log.e(Settings.tag, "cgeoBase.searchByGeocode: No cache parsed");
            return parseResult;
        }

        ParseResult search = ParseResult.filterParseResults(parseResult, false, false, Settings.getCacheType());
        app.addSearch(search.cacheList, listId);

        return search;
    }

    @Override
    public boolean isZippedGPXFile(final String fileName) {
        return gpxZipFilePattern.matcher(fileName).matches();
    }
}
