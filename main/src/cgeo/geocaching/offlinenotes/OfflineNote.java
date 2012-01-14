package cgeo.geocaching.offlinenotes;

import cgeo.geocaching.geopoint.Geopoint;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

// OfflineNote is the base class for all offline notes
//
// It stores all common attributes
public class OfflineNote implements Comparable

{

    public static final int NOTE_TYPE_IMAGE = 1;
    public static final int NOTE_TYPE_VIDEO = 2;
    public static final int NOTE_TYPE_TEXT = 3;
    public static final int NOTE_TYPE_VOICE = 4;


    protected Date myDate;
    protected String description;
    protected Geopoint coordinate;
    protected Uri uri;
    protected int type;

    public OfflineNote(Uri uri, int type) {
        this.uri = uri;
        this.type = type;
        myDate = new Date();
        description = new String("");

        //LocationManager lm = (LocationManager) Context.getSystemService(Context.LOCATION_SERVICE);
        //Location currentLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        //// TODO get the last location
        coordinate = new Geopoint(0, 0);
    }

    public int getType() {
        return type;
    }

    public Uri getUri() {
        return uri;
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

    // @todo add method for deleting orphaned notes

    public static ArrayList<OfflineNote> loadOfflineNotesFromStorage(String geoCode)
    {
        String cacheDir = getCacheDir(geoCode);
        ArrayList<OfflineNote> offlineNotes = new ArrayList<OfflineNote>();

        File dir = new File(cacheDir);
        if (!dir.exists() || !dir.isDirectory())
            return offlineNotes;


        // load entries
        for (String filename : dir.list()) {
            filename = cacheDir + File.separator + filename;
            String ext = getExtension(filename).toLowerCase();
            if (ext.equals("jpg")) {
                File file = new File(filename);
                // todo extract coord, date and desc
                OfflineNote note = new OfflineNote(Uri.fromFile(file), NOTE_TYPE_IMAGE);
                offlineNotes.add(note);
            }
            if (ext.equals("mp4")) {
                File file = new File(filename);
                // todo extract coord, date and desc
                OfflineNote note = new OfflineNote(Uri.fromFile(file), NOTE_TYPE_VIDEO);
                offlineNotes.add(note);
            }
            // todo add file formats for text and voice
        }

        Collections.sort(offlineNotes);

        return offlineNotes;
    }

    private static String getExtension(String filename)
    {
        String filenameArray[] = filename.split("\\.");
        if (filenameArray.length == 0)
            return new String("");
        String extension = filenameArray[filenameArray.length - 1];
        return extension;
    }

    public static Uri getNewNoteURI(String geoCode, int noteType) {
        String cacheDir = getCacheDir(geoCode);

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = null;
        if (noteType == NOTE_TYPE_IMAGE)
            mediaFile = new File(cacheDir + File.separator + "IMG_" + timeStamp + ".jpg");
        else if (noteType == NOTE_TYPE_VIDEO)
            mediaFile = new File(cacheDir + File.separator + "VID_" + timeStamp + ".mp4");

        return Uri.fromFile(mediaFile);
    }

    protected static String getCacheDir(String geoCode)
    {
        File cacheDir = new File(Environment.getExternalStorageDirectory()
                + File.separator + "cgeo"
                + File.separator + "offline_notes"
                + File.separator + geoCode.toLowerCase());

        // Create the storage directory if it does not exist
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                Log.d("c:geo", "failed to create directory for offline note saving");
                return null;
            }
        }

        return cacheDir.getPath();
    }

    public int compareTo(Object otherNote) {
        if (!(otherNote instanceof OfflineNote)) {
            throw new ClassCastException("Invalid object");
        }

        OfflineNote typedOtherNote = (OfflineNote) otherNote;
        if (myDate.after(typedOtherNote.myDate))
            return 1;
        else if (myDate.before(typedOtherNote.myDate))
            return -1;

        return 0;

    }
}


