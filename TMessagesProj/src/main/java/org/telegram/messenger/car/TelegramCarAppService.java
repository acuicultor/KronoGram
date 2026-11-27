package org.telegram.messenger.car;

import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;

public class TelegramCarAppService extends CarAppService {

    @Override
    public void onCreate() {
        super.onCreate();
        FileLog.d("TelegramCarAppService: onCreate");
        ApplicationLoader.postInitApplication();
    }

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (BuildVars.DEBUG_VERSION || isDebuggable) {
            FileLog.d("TelegramCarAppService: createHostValidator -> ALLOW_ALL_HOSTS_VALIDATOR");
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        }
        FileLog.d("TelegramCarAppService: createHostValidator -> hosts_allowlist_sample");
        // Production: only Google-published Auto/Automotive hosts (signed by Google).
        return new HostValidator.Builder(getApplicationContext())
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build();
    }

    @NonNull
    @Override
    public Session onCreateSession() {
        FileLog.d("TelegramCarAppService: onCreateSession");
        ApplicationLoader.postInitApplication();
        return new TelegramCarSession();
    }
}
