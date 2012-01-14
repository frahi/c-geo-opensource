package cgeo.geocaching.offlinenotes;

import cgeo.geocaching.cgImage;

public class OfflineNoteImage extends OfflineNote {

    protected cgImage image;

    public cgImage getImage() {
        return image;
    }

    public void setImage(cgImage image) {
        this.image = image;
    }

    public OfflineNoteImage(cgImage image) {
        this.image = image;
    }
}
