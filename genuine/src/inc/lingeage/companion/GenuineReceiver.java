package inc.lingeage.companion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GenuineReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("OHAI", "Genuine triggered");
        if (context != null) {
            context.startService(new Intent(context, GenuineService.class));
        }
    }
}
