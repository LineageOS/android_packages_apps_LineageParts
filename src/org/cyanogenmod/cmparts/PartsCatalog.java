package org.cyanogenmod.cmparts;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.XmlRes;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

import org.cyanogenmod.internal.cmparts.IPartChangedCallback;
import org.cyanogenmod.internal.cmparts.IPartsCatalog;
import org.cyanogenmod.internal.cmparts.PartInfo;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PartsCatalog extends Service {

    private static final String TAG = "PartsCatalog";

    private static final Map<String, PartInfo> sParts = new ArrayMap<String, PartInfo>();

    private static final Map<String, RemoteCallbackList<IPartChangedCallback>> sCallbacks =
            new ArrayMap<String, RemoteCallbackList<IPartChangedCallback>>();

    private static final AtomicBoolean mCatalogLoaded = new AtomicBoolean(false);

    private final IPartsCatalog.Stub mBinder = new IPartsCatalog.Stub() {

        @Override
        public boolean isPartAvailable(String key) throws RemoteException {
            synchronized (sParts) {
                PartInfo info = sParts.get(key);
                return info != null && info.isAvailable();
            }
        }

        @Override
        public PartInfo getPartInfo(String key) throws RemoteException {
            synchronized (sParts) {
                return sParts.get(key);
            }
        }

        @Override
        public void registerCallback(String key, IPartChangedCallback cb) throws RemoteException {
            synchronized (sParts) {
                if (sParts.containsKey(key)) {
                    RemoteCallbackList<IPartChangedCallback> cbs = sCallbacks.get(key);
                    if (cbs == null) {
                        cbs = new RemoteCallbackList<>();
                        sCallbacks.put(key, cbs);
                    }
                    cbs.register(cb);
                }
            }
        }

        @Override
        public void unregisterCallback(String key, IPartChangedCallback cb) throws RemoteException {
            synchronized (sParts) {
                if (sParts.containsKey(key)) {
                    RemoteCallbackList<IPartChangedCallback> cbs = sCallbacks.get(key);
                    if (cbs != null) {
                        cbs.unregister(cb);
                    }
                }
            }
        }

        @Override
        public String[] getPartsList() throws RemoteException {
            return sParts.keySet().toArray(new String[sParts.size()]);
        }

        public void notifyPartChanged(String key) {
            synchronized (sParts) {
                if (sParts.containsKey(key) && sCallbacks.containsKey(key)) {
                    final RemoteCallbackList<IPartChangedCallback> cb = sCallbacks.get(key);
                    int i = cb.beginBroadcast();
                    while (i > 0) {
                        i--;
                        try {
                            cb.getBroadcastItem(i).onPartChanged(sParts.get(key));
                        } catch (RemoteException e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }
                    cb.finishBroadcast();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        synchronized (sParts) {
            loadPartsFromResourceLocked(getResources(), R.xml.parts_catalog, sParts);
        }
    }

    public void notifyPartChanged(String key) {
        synchronized (sParts) {
            if (sParts.containsKey(key) && sCallbacks.containsKey(key)) {
                final RemoteCallbackList<IPartChangedCallback> cb = sCallbacks.get(key);
                int i = cb.beginBroadcast();
                while (i > 0) {
                    i--;
                    try {
                        cb.getBroadcastItem(i).onPartChanged(sParts.get(key));
                    } catch (RemoteException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
                cb.finishBroadcast();
            }
        }
    }

    static final PartInfo getPartInfo(Resources res, String key) {
        synchronized (sParts) {
            loadPartsFromResourceLocked(res, R.xml.parts_catalog, sParts);
            return sParts.get(key);
        }
    }

    static final PartInfo getPartInfoForClass(Resources res, String clazz) {
        synchronized (sParts) {
            loadPartsFromResourceLocked(res, R.xml.parts_catalog, sParts);
            for (PartInfo info : sParts.values()) {
                if (info.getFragmentClass() != null && info.getFragmentClass().equals(clazz)) {
                    return info;
                }
            }
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        synchronized (sParts) {
            for (Map.Entry<String, RemoteCallbackList<IPartChangedCallback>> entry : sCallbacks.entrySet()) {
                if (entry.getValue() != null) {
                    entry.getValue().kill();
                }
            }
            sCallbacks.clear();
        }
    }

    private static void loadPartsFromResourceLocked(Resources res, @XmlRes int resid,
                                                    Map<String, PartInfo> target) {
        if (mCatalogLoaded.get()) {
            return;
        }

        XmlResourceParser parser = null;

        try {
            parser = res.getXml(resid);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String nodeName = parser.getName();
            if (!"parts-catalog".equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <parts-catalog> tag; found"
                                + nodeName + " at " + parser.getPositionDescription());
            }

            final int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("part".equals(nodeName)) {
                    TypedArray sa = res.obtainAttributes(attrs, R.styleable.PartsCatalog);

                    String key = null;
                    TypedValue tv = sa.peekValue(R.styleable.PartsCatalog_key);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            key = res.getString(tv.resourceId);
                        } else {
                            key = String.valueOf(tv.string);
                        }
                    }
                    if (key == null) {
                        throw new RuntimeException("Attribute 'key' is required");
                    }

                    final PartInfo info = new PartInfo(key);

                    tv = sa.peekValue(R.styleable.PartsCatalog_title);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            info.setTitle(res.getString(tv.resourceId));
                        } else {
                            info.setTitle(String.valueOf(tv.string));
                        }
                    }

                    tv = sa.peekValue(R.styleable.PartsCatalog_summary);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            info.setSummary(res.getString(tv.resourceId));
                        } else {
                            info.setSummary(String.valueOf(tv.string));
                        }
                    }

                    info.setFragmentClass(sa.getString(R.styleable.PartsCatalog_fragment));
                    info.setIconRes(sa.getResourceId(R.styleable.PartsCatalog_icon, 0));
                    sa.recycle();

                    target.put(key, info);

                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing catalog", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing catalog", e);
        } finally {
            if (parser != null) parser.close();
        }
        mCatalogLoaded.set(true);
    }
}
