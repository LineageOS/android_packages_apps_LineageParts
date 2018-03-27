package inc.lingeage.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.lineageos.lineageparts.R;

import java.security.SecureRandom;

public class GenuineActivity extends Activity {

    private AlertDialog mCurrentDialog;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // From now on the user shall go through the 5 steps of grief
        mCurrentDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.genuine_dialog_title)
                .setMessage(R.string.genuine_dialog_grief_message)
                .setPositiveButton(R.string.genuine_dialog_acceptance, (d, i) -> acceptance())
                .setNegativeButton(R.string.genuine_dialog_denial, (d, i) -> denial())
                .setNeutralButton(R.string.genuine_dialog_anger, (d, i) -> anger())
                .setCancelable(false)
                .show();
    }

    private void denial() {
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
        }

        mCurrentDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.genuine_dialog_title)
                .setMessage(getString(R.string.genuine_dialog_anger_message, getErrorVal()))
                .setPositiveButton(R.string.genuine_dialog_acceptance, (d, i) -> acceptance())
                .setNegativeButton(R.string.genuine_dialog_bargaining, (d, i) -> bargaining())
                .setCancelable(false)
                .show();
    }

    private void anger() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(GenuineService.FOOL_URL);
        startActivity(intent);
        acceptance();
    }

    private void bargaining() {
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
        }
        mCurrentDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.genuine_dialog_title)
                .setMessage(R.string.genuine_dialog_bargaining_message)
                .setPositiveButton(R.string.genuine_dialog_acceptance, (d, i) -> acceptance())
                .setNegativeButton(R.string.genuine_dialog_depression, (d, i) -> depression())
                .setCancelable(false)
                .show();
    }

    private void depression() {
        Toast.makeText(this, R.string.genuine_toast_depression_message, Toast.LENGTH_LONG).show();
        acceptance();
    }

    private void acceptance() {
        mPrefs.edit().putBoolean("grief", true).apply();
        startService(new Intent(this, GenuineService.class));
        finish();
    }

    private int getErrorVal() {
        return new SecureRandom().nextInt(21);
    }
}
