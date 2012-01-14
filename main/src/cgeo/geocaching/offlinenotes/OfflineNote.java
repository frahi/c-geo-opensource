package cgeo.geocaching.offlinenotes;

import cgeo.geocaching.geopoint.Geopoint;

import java.util.Date;

// OfflineNote is the base class for all offline notes
//
// It stores all common attributes
public class OfflineNote {
    protected Date myDate;
    protected String description;
    protected Geopoint coordinate;

    OfflineNote() {
        myDate = new Date();
        description = new String("");

        //LocationManager lm = (LocationManager) Context.getSystemService(Context.LOCATION_SERVICE);
        //Location currentLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        //// TODO get the last location
        coordinate = new Geopoint(0, 0);
    }

    public Date getDate() {
        return myDate;
    }

    public String getDescription() {
        return description;
    }

    public Geopoint getCoordinate() {
        return coordinate;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}


