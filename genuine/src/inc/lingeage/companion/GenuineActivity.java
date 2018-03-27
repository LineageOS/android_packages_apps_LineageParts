package inc.lingeage.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.security.SecureRandom;

public class GenuineActivity extends Activity {

    private AlertDialog mCurrentDialog;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.mCurrentDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.genuine_dialog_title)
                .setMessage(R.string.genuine_dialog_grief_message)
                .setPositiveButton(R.string.genuine_dialog_acceptance, (d, i) -> acceptance())
                .setNegativeButton(R.string.genuine_dialog_anger, (d, i) -> anger())
                .setNeutralButton(R.string.genuine_dialog_denial, (d, i) -> denial())
                .setCancelable(false)
                .show();
    }

    private void denial() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(Uri.parse("https://lineageos.org/april_fool_url"));
        startActivity(intent);
    }

    private void anger() {
        AlertDialog alertDialog = this.mCurrentDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }

        this.mCurrentDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.genuine_dialog_title)
                .setMessage(getString(R.string.genuine_dialog_anger_message, getErrorVal()))
                .setPositiveButton(R.string.genuine_dialog_acceptance, (d, i) -> acceptance())
                .setNegativeButton(R.string.genuine_dialog_bargaining, (d, i) -> bargaining())
                .show();
    }

    private void bargaining() {
        AlertDialog alertDialog = this.mCurrentDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        this.mCurrentDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.genuine_dialog_title)
                .setMessage(R.string.genuine_dialog_bargaining_message)
                .setPositiveButton(R.string.genuine_dialog_acceptance, (d, i) -> acceptance())
                .setNegativeButton(R.string.genuine_dialog_depression, (d, i) -> depression())
                .show();
    }

    private void depression() {
        Toast.makeText(this, R.string.genuine_toast_depression_message, Toast.LENGTH_LONG).show();
        acceptance();
    }

    private void acceptance() {
        mPrefs.edit().putBoolean("grief", true).apply();
        startService(new Intent(this, GenuineService.class));
    }

    private int getErrorVal() {
        return new SecureRandom().nextInt(21);
    }
}
