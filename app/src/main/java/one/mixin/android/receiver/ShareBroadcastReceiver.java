package one.mixin.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import one.mixin.android.R;

public class ShareBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String url = intent.getDataString();
        if (url != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            Intent chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.Share));
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooserIntent);
        }
    }
}
