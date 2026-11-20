package org.telegram.messenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationRepeatReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        final int currentAccount = intent.getIntExtra("currentAccount", UserConfig.selectedAccount);
        if (!UserConfig.isValidAccount(currentAccount)) {
            return;
        }
        final PendingResult pendingResult = goAsync();
        AndroidUtilities.runOnUIThread(() -> {
            try {
                ApplicationLoader.postInitApplication();
                NotificationsController.getInstance(currentAccount).repeatNotificationMaybe();
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                if (pendingResult != null) {
                    try {
                        pendingResult.finish();
                    } catch (Throwable ignore) {
                    }
                }
            }
        });
    }
}
