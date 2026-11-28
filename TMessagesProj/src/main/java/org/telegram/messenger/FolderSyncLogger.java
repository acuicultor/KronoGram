package org.telegram.messenger;

import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import org.telegram.tgnet.ConnectionsManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FolderSyncLogger {

    public static final String LAYER_NETWORK = "NETWORK";
    public static final String LAYER_DATABASE = "DATABASE";
    public static final String LAYER_UI_STATE = "UI_STATE";
    public static final String LAYER_SYNC = "SYNC";

    public static final String LEVEL_INFO = "INFO";
    public static final String LEVEL_WARN = "WARN";
    public static final String LEVEL_ERROR = "ERROR";

    private static final String TAG = "FolderSync";
    private static final int MAX_MEMORY_LINES = 400;
    private static final long MAX_LOG_FILE_BYTES = 512L * 1024L;
    private static final long AUTO_SEND_DEBOUNCE_MS = 3000L;
    private static final long AUTO_SEND_COOLDOWN_MS = 5L * 60L * 1000L;

    private static final Object LOCK = new Object();
    private static final ArrayDeque<String> memoryBuffer = new ArrayDeque<>(MAX_MEMORY_LINES + 16);
    private static final SparseArray<Runnable> pendingAutoSendRunnables = new SparseArray<>();
    private static final SparseArray<Long> lastAutoSendElapsedByAccount = new SparseArray<>();
    private static final SparseArray<String> lastFailureLayerByAccount = new SparseArray<>();
    private static final SparseArray<String> lastFailureReasonByAccount = new SparseArray<>();
    private static final SparseArray<Long> lastFailureTimestampByAccount = new SparseArray<>();
    private static final SparseArray<AtomicBoolean> sendInProgressByAccount = new SparseArray<>();

    private static volatile DispatchQueue logQueue;

    private FolderSyncLogger() {
    }

    private static DispatchQueue getLogQueue() {
        DispatchQueue q = logQueue;
        if (q == null) {
            synchronized (LOCK) {
                q = logQueue;
                if (q == null) {
                    q = new DispatchQueue("folderSyncLogQueue");
                    logQueue = q;
                }
            }
        }
        return q;
    }

    private static AtomicBoolean getSendInProgressFlag(int account) {
        synchronized (LOCK) {
            AtomicBoolean flag = sendInProgressByAccount.get(account);
            if (flag == null) {
                flag = new AtomicBoolean(false);
                sendInProgressByAccount.put(account, flag);
            }
            return flag;
        }
    }

    public static void logInfo(int account, String layer, String event, String details) {
        log(account, layer, LEVEL_INFO, event, details, false);
    }

    public static void logWarn(int account, String layer, String event, String details, boolean isSyncFailure) {
        log(account, layer, LEVEL_WARN, event, details, isSyncFailure);
    }

    public static void logError(int account, String layer, String event, String details, boolean isSyncFailure) {
        log(account, layer, LEVEL_ERROR, event, details, isSyncFailure);
    }

    public static void log(int account, String layer, String level, String event, String details, boolean isSyncFailure) {
        final long nowWall = System.currentTimeMillis();
        final String timestamp = formatTimestamp(nowWall);
        final String connState = getConnectionStateName(account);
        final String safeLayer = TextUtils.isEmpty(layer) ? LAYER_SYNC : layer;
        final String safeLevel = TextUtils.isEmpty(level) ? LEVEL_INFO : level;
        final String safeEvent = TextUtils.isEmpty(event) ? "unknown_event" : event;
        final String safeDetails = TextUtils.isEmpty(details) ? "" : (" | " + details.replace('\n', ' ').replace('\r', ' '));

        final String line = timestamp
                + " [acc=" + account + "]"
                + " [conn=" + connState + "]"
                + " [" + safeLayer + "]"
                + " [" + safeLevel + "]"
                + " " + safeEvent
                + safeDetails;

        synchronized (LOCK) {
            if (memoryBuffer.size() >= MAX_MEMORY_LINES) {
                memoryBuffer.removeFirst();
            }
            memoryBuffer.addLast(line);
            if (isSyncFailure) {
                lastFailureLayerByAccount.put(account, safeLayer);
                lastFailureReasonByAccount.put(account, safeEvent + safeDetails);
                lastFailureTimestampByAccount.put(account, nowWall);
            }
        }

        if (BuildVars.LOGS_ENABLED) {
            if (LEVEL_ERROR.equals(safeLevel)) {
                FileLog.e("[" + TAG + "] " + line);
            } else {
                FileLog.d("[" + TAG + "] " + line);
            }
        } else if (LEVEL_ERROR.equals(safeLevel)) {
            Log.e(TAG, line);
        }

        getLogQueue().postRunnable(() -> appendLineToDisk(account, line));

        if (isSyncFailure) {
            scheduleAutoSendIfEnabled(account, safeLayer, safeEvent);
        }
    }

    public static int getBufferedEventCount() {
        synchronized (LOCK) {
            return memoryBuffer.size();
        }
    }

    public static String getLastFailureSummary(int account) {
        synchronized (LOCK) {
            String layer = lastFailureLayerByAccount.get(account);
            Long ts = lastFailureTimestampByAccount.get(account);
            if (TextUtils.isEmpty(layer) || ts == null) {
                return null;
            }
            return layer + " (" + formatShortTime(ts) + ")";
        }
    }

    private static void scheduleAutoSendIfEnabled(int account, String layer, String event) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                KronoConfig.loadConfig();
                if (!KronoConfig.autoSendFolderSyncLogs) {
                    return;
                }
                UserConfig userConfig = UserConfig.getInstance(account);
                if (userConfig == null || !userConfig.isClientActivated() || userConfig.getClientUserId() == 0) {
                    return;
                }
                final long nowElapsed = SystemClock.elapsedRealtime();
                synchronized (LOCK) {
                    Long lastElapsed = lastAutoSendElapsedByAccount.get(account);
                    if (lastElapsed != null && (nowElapsed - lastElapsed) < AUTO_SEND_COOLDOWN_MS) {
                        return;
                    }
                    Runnable existing = pendingAutoSendRunnables.get(account);
                    if (existing != null) {
                        AndroidUtilities.cancelRunOnUIThread(existing);
                    }
                    Runnable runnable = () -> {
                        synchronized (LOCK) {
                            pendingAutoSendRunnables.remove(account);
                            Long checkElapsed = lastAutoSendElapsedByAccount.get(account);
                            long currentElapsed = SystemClock.elapsedRealtime();
                            if (checkElapsed != null && (currentElapsed - checkElapsed) < AUTO_SEND_COOLDOWN_MS) {
                                return;
                            }
                            lastAutoSendElapsedByAccount.put(account, currentElapsed);
                        }
                        String failureLayer;
                        String failureReason;
                        synchronized (LOCK) {
                            failureLayer = lastFailureLayerByAccount.get(account);
                            failureReason = lastFailureReasonByAccount.get(account);
                        }
                        sendLogsToSavedMessages(
                                account,
                                false,
                                !TextUtils.isEmpty(failureLayer) ? failureLayer : layer,
                                !TextUtils.isEmpty(failureReason) ? failureReason : event,
                                null,
                                null
                        );
                    };
                    pendingAutoSendRunnables.put(account, runnable);
                    AndroidUtilities.runOnUIThread(runnable, AUTO_SEND_DEBOUNCE_MS);
                }
            } catch (Throwable e) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.e(e);
                }
            }
        });
    }

    public static void sendLogsToSavedMessages(
            int account,
            boolean manual,
            String triggerLayer,
            String triggerReason,
            Utilities.Callback<String> onSuccess,
            Utilities.Callback<String> onError
    ) {
        AndroidUtilities.runOnUIThread(() -> {
            UserConfig userConfig = UserConfig.getInstance(account);
            if (userConfig == null || !userConfig.isClientActivated() || userConfig.getClientUserId() == 0) {
                if (onError != null) {
                    onError.run("Debes iniciar sesión para enviar logs a Mensajes Guardados");
                }
                return;
            }
            AtomicBoolean inProgress = getSendInProgressFlag(account);
            if (!inProgress.compareAndSet(false, true)) {
                if (onError != null) {
                    onError.run("Ya hay un envío de logs en curso");
                }
                return;
            }

            final long selfId = userConfig.getClientUserId();
            final String ramSnapshot = captureRamStateSnapshot(account);

            getLogQueue().postRunnable(() -> {
                try {
                    File cacheDir = FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE);
                    if (cacheDir == null || !cacheDir.exists()) {
                        if (ApplicationLoader.applicationContext != null) {
                            cacheDir = ApplicationLoader.applicationContext.getCacheDir();
                        }
                    }
                    if (cacheDir == null) {
                        throw new IllegalStateException("No se pudo acceder al directorio de caché");
                    }

                    String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                    String fileName = "folder_sync_logs_acc" + account + "_" + stamp + ".txt";
                    File reportFile = new File(cacheDir, fileName);

                    StringBuilder report = new StringBuilder(8192);
                    report.append("=== KronoGram Folder Sync Diagnostic Report ===\n");
                    report.append("Generated: ").append(formatTimestamp(System.currentTimeMillis())).append('\n');
                    report.append("Trigger: ").append(manual ? "MANUAL (User Request)" : "AUTO (Sync Failure)").append('\n');
                    if (!TextUtils.isEmpty(triggerLayer)) {
                        report.append("Trigger Layer: ").append(triggerLayer).append('\n');
                    }
                    if (!TextUtils.isEmpty(triggerReason)) {
                        report.append("Trigger Reason: ").append(triggerReason).append('\n');
                    }
                    report.append("App Version: ").append(BuildVars.BUILD_VERSION_STRING)
                            .append(" / Krono ").append(BuildVars.KRONO_VERSION_STRING).append('\n');
                    report.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                            .append(" (Android ").append(Build.VERSION.RELEASE).append(", SDK ").append(Build.VERSION.SDK_INT).append(")\n");
                    report.append("Account: ").append(account)
                            .append(" | Connection: ").append(getConnectionStateName(account)).append('\n');
                    report.append('\n');
                    report.append(ramSnapshot);
                    report.append('\n');
                    report.append("=== Chronological Folder Sync Logs ===\n");

                    ArrayList<String> diskLines = readDiskLogs(account);
                    if (!diskLines.isEmpty()) {
                        for (int i = 0; i < diskLines.size(); i++) {
                            report.append(diskLines.get(i)).append('\n');
                        }
                    } else {
                        ArrayList<String> memCopy;
                        synchronized (LOCK) {
                            memCopy = new ArrayList<>(memoryBuffer);
                        }
                        if (memCopy.isEmpty()) {
                            report.append("(No folder sync events recorded yet in this session)\n");
                        } else {
                            for (int i = 0; i < memCopy.size(); i++) {
                                report.append(memCopy.get(i)).append('\n');
                            }
                        }
                    }

                    try (FileOutputStream fos = new FileOutputStream(reportFile)) {
                        fos.write(report.toString().getBytes(StandardCharsets.UTF_8));
                        fos.flush();
                    }

                    final String caption = buildCaption(account, manual, triggerLayer, triggerReason);
                    final String path = reportFile.getAbsolutePath();

                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            SendMessagesHelper.prepareSendingDocument(
                                    AccountInstance.getInstance(account),
                                    path,
                                    path,
                                    null,
                                    caption,
                                    "text/plain",
                                    selfId,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    manual,
                                    0,
                                    null,
                                    null,
                                    false
                            );
                            if (onSuccess != null) {
                                onSuccess.run(fileName);
                            }
                        } finally {
                            inProgress.set(false);
                        }
                    });
                } catch (Throwable e) {
                    inProgress.set(false);
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.e(e);
                    }
                    final String errorMsg = e.getMessage() != null ? e.getMessage() : "Error generando reporte de logs";
                    AndroidUtilities.runOnUIThread(() -> {
                        if (onError != null) {
                            onError.run(errorMsg);
                        }
                    });
                }
            });
        });
    }

    private static String buildCaption(int account, boolean manual, String triggerLayer, String triggerReason) {
        StringBuilder sb = new StringBuilder();
        sb.append(manual ? "📋 Logs de sincronización de carpetas" : "⚠️ Fallo de sincronización de carpetas detectado");
        sb.append("\nCuenta: ").append(account);
        sb.append(" | Red: ").append(getConnectionStateName(account));
        if (!TextUtils.isEmpty(triggerLayer)) {
            sb.append("\nCapa: ").append(triggerLayer);
        }
        if (!TextUtils.isEmpty(triggerReason)) {
            String cleanReason = triggerReason.length() > 220 ? triggerReason.substring(0, 220) + "..." : triggerReason;
            sb.append("\nDetalle: ").append(cleanReason);
        }
        sb.append("\nFecha: ").append(formatTimestamp(System.currentTimeMillis()));
        return sb.toString();
    }

    private static String captureRamStateSnapshot(int account) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Current RAM & Config State ===\n");
        try {
            UserConfig uc = UserConfig.getInstance(account);
            MessagesController mc = MessagesController.getInstance(account);
            sb.append("UserConfig.filtersLoaded=").append(uc != null && uc.filtersLoaded).append('\n');
            if (mc != null) {
                sb.append(mc.getFolderSyncDebugSnapshot());
                ArrayList<MessagesController.DialogFilter> filters = mc.getDialogFilters();
                sb.append("RAM dialogFilters.size()=").append(filters != null ? filters.size() : -1).append('\n');
                if (filters != null) {
                    for (int i = 0; i < filters.size(); i++) {
                        MessagesController.DialogFilter f = filters.get(i);
                        if (f == null) {
                            sb.append("  [").append(i).append("] null\n");
                            continue;
                        }
                        sb.append("  [").append(i).append("] id=").append(f.id)
                                .append(" localId=").append(f.localId)
                                .append(" ord=").append(f.order)
                                .append(" name=\"").append(f.name).append("\"")
                                .append(" flags=").append(f.flags)
                                .append(" always=").append(f.alwaysShow.size())
                                .append(" never=").append(f.neverShow.size())
                                .append(" pinned=").append(f.pinnedDialogs.size())
                                .append(" dialogs=").append(f.dialogs.size())
                                .append(" unread=").append(f.unreadCount)
                                .append(" hidden=").append(KronoConfig.isFilterHidden(account, f.id))
                                .append(" locked=").append(f.locked)
                                .append('\n');
                    }
                }
                for (int s = 0; s < mc.selectedDialogFilter.length; s++) {
                    MessagesController.DialogFilter sel = mc.selectedDialogFilter[s];
                    sb.append("selectedDialogFilter[").append(s).append("]=")
                            .append(sel != null ? ("id=" + sel.id + " name=\"" + sel.name + "\"") : "null")
                            .append('\n');
                }
            }
        } catch (Throwable e) {
            sb.append("Snapshot error: ").append(e.getMessage()).append('\n');
        }
        return sb.toString();
    }

    private static File getLogFile(int account, boolean rotatedBackup) {
        File dir = AndroidUtilities.getLogsDir();
        if (dir == null && ApplicationLoader.applicationContext != null) {
            dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "logs");
        }
        if (dir == null) {
            return null;
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String name = "folder_sync_acc_" + account + (rotatedBackup ? ".old.log" : ".log");
        return new File(dir, name);
    }

    private static void appendLineToDisk(int account, String line) {
        try {
            File file = getLogFile(account, false);
            if (file == null) {
                return;
            }
            if (file.exists() && file.length() > MAX_LOG_FILE_BYTES) {
                File oldFile = getLogFile(account, true);
                if (oldFile != null) {
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                    file.renameTo(oldFile);
                }
            }
            try (FileOutputStream fos = new FileOutputStream(file, true);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(line);
                writer.write('\n');
                writer.flush();
            }
        } catch (Throwable ignored) {
        }
    }

    private static ArrayList<String> readDiskLogs(int account) {
        ArrayList<String> result = new ArrayList<>();
        readFileLinesInto(getLogFile(account, true), result);
        readFileLinesInto(getLogFile(account, false), result);
        return result;
    }

    private static void readFileLinesInto(File file, ArrayList<String> out) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.add(line);
            }
        } catch (Throwable ignored) {
        }
    }

    public static String getConnectionStateName(int account) {
        try {
            int state = ConnectionsManager.getInstance(account).getConnectionState();
            switch (state) {
                case ConnectionsManager.ConnectionStateConnecting:
                    return "Connecting";
                case ConnectionsManager.ConnectionStateWaitingForNetwork:
                    return "WaitingForNetwork";
                case ConnectionsManager.ConnectionStateConnected:
                    return "Connected";
                case ConnectionsManager.ConnectionStateConnectingToProxy:
                    return "ConnectingToProxy";
                case ConnectionsManager.ConnectionStateUpdating:
                    return "Updating";
                default:
                    return "State(" + state + ")";
            }
        } catch (Throwable e) {
            return "Unknown";
        }
    }

    private static String formatTimestamp(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date(millis));
    }

    private static String formatShortTime(long millis) {
        return new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date(millis));
    }
}
