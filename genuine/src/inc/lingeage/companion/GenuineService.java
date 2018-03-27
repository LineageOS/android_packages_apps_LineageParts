package inc.lingeage.companion;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemProperties;
import android.preference.PreferenceManager;

import org.lineageos.lineageparts.R;

public class GenuineService extends Service {
    public static final Uri FOOL_URL = Uri.parse("https://lineageos.org/Changelog-17");
    private static final String CHANNEL = "grief_info";

    public int onStartCommand( Intent intent, int flags, int startId) {
        work(this);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static void work(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        if (isDisabledFromProp()) {
            return;
        }

        boolean hasGoneThrough5GriefSteps = checkStatus(context);

        Intent intent;
        if (hasGoneThrough5GriefSteps) {
            intent = new Intent("android.intent.action.VIEW");
            intent.setData(GenuineService.FOOL_URL);
        } else {
            intent = new Intent(context, GenuineActivity.class);
        }
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Notification.Builder notification = Build.VERSION.SDK_INT >= 26 ?
                new Notification.Builder(context, CHANNEL) : new Notification.Builder(context);

        notification.setSmallIcon(R.drawable.ic_error)
                .setColor(context.getColor(R.color.genuine_error))
                .setContentIntent(pIntent)
                .setOngoing(true);
        if (hasGoneThrough5GriefSteps) {
            notification.setContentTitle(context.getString(R.string.genuine_notification_grief));
        } else {
            notification.setContentTitle(context.getString(R.string.genuine_notification_title))
                    .setContentText(context.getString(R.string.genuine_notification_message))
                    .setStyle(new Notification.BigTextStyle()
                    .bigText(context.getString(R.string.genuine_notification_message)))
                    .setAutoCancel(true);
        }

        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                    "LineaGenuine info", NotificationManager.IMPORTANCE_DEFAULT));
        }

        manager.notify(151, notification.build());
    }

    private static boolean checkStatus(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("grief", false);
    }

    private static boolean isDisabledFromProp() {
        return SystemProperties.getBoolean("persist.lineage.nofool", false);
    }
}
