package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import org.apache.commons.collections.CollectionUtils;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public class StaticMapsActivity extends AbstractActivity {

    private final List<Bitmap> maps = new ArrayList<Bitmap>();
    private String geocode = null;
    private LayoutInflater inflater = null;
    private ProgressDialog waitDialog = null;
    private LinearLayout smapsView = null;
    private BitmapFactory factory = null;
    private final Handler loadMapsHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (CollectionUtils.isEmpty(maps)) {
                    if (waitDialog != null) {
                        waitDialog.dismiss();
                    }

                    showToast(res.getString(R.string.err_detail_not_load_map_static));

                    finish();
                    return;
                } else {
                    if (waitDialog != null) {
                        waitDialog.dismiss();
                    }

                    if (inflater == null) {
                        inflater = getLayoutInflater();
                    }

                    if (smapsView == null) {
                        smapsView = (LinearLayout) findViewById(R.id.maps_list);
                    }
                    smapsView.removeAllViews();

                    for (Bitmap image : maps) {
                        if (image != null) {
                            final ImageView map = (ImageView) inflater.inflate(R.layout.map_static_item, null);
                            map.setImageBitmap(image);
                            smapsView.addView(map);
                        }
                    }
                }
            } catch (Exception e) {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                }
                Log.e(Settings.tag, "StaticMapsActivity.loadMapsHandler: " + e.toString());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.map_static);
        setTitle(res.getString(R.string.map_static_title));

        // get parameters
        final Bundle extras = getIntent().getExtras();

        // try to get data from extras
        if (extras != null) {
            geocode = extras.getString("geocode");
        }

        if (geocode == null) {
            showToast("Sorry, c:geo forgot for what cache you want to load static maps.");
            finish();
            return;
        }

        waitDialog = ProgressDialog.show(this, null, res.getString(R.string.map_static_loading), true);
        waitDialog.setCancelable(true);

        (new LoadMapsThread()).start();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    private class LoadMapsThread extends Thread {

        @Override
        public void run() {
            try {
                if (factory == null) {
                    factory = new BitmapFactory();
                }

                for (int level = 1; level <= 5; level++) {
                    try {
                        final Bitmap image = BitmapFactory.decodeFile(StaticMapsProvider.getMapFile(geocode, level, false).getPath());
                        if (image != null) {
                            maps.add(image);
                        }
                    } catch (Exception e) {
                        Log.e(Settings.tag, "StaticMapsActivity.LoadMapsThread.run.1: " + e.toString());
                    }
                }

                if (maps.isEmpty()) {
                    for (int level = 1; level <= 5; level++) {
                        try {
                            final Bitmap image = BitmapFactory.decodeFile(StaticMapsProvider.getMapFile(geocode, level, false).getPath());
                            if (image != null) {
                                maps.add(image);
                            }
                        } catch (Exception e) {
                            Log.e(Settings.tag, "StaticMapsActivity.LoadMapsThread.run.2: " + e.toString());
                        }
                    }
                }

                loadMapsHandler.sendMessage(new Message());
            } catch (Exception e) {
                Log.e(Settings.tag, "StaticMapsActivity.LoadMapsThread.run: " + e.toString());
            }
        }
    }
}