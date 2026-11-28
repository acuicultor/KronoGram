package org.telegram.messenger;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DataReader;
import androidx.media3.common.Format;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.FileDataSource;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.decoder.SimpleDecoderOutputBuffer;
import androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder;
import androidx.media3.decoder.ffmpeg.FfmpegLibrary;
import androidx.media3.extractor.DefaultExtractorInput;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.DiscardingTrackOutput;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.TrackOutput;

import org.json.JSONObject;
import org.telegram.messenger.video.AudioDecoder;
import org.telegram.tgnet.TLRPC;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Universal free offline voice-to-text transcriber powered by Vosk + Media3 FFmpeg/AudioDecoder.
 * Works across all Android versions without API keys or Telegram Premium.
 */
@UnstableApi
public final class KronoVoiceTranscriber {

    private static final float TARGET_SAMPLE_RATE = 16000.0f;
    private static final int TARGET_SAMPLE_RATE_INT = 16000;
    private static final String MODELS_ROOT_DIR = "vosk-models";
    private static final String READY_MARKER = ".ready";

    public interface TranscriptionCallback {
        void onResult(boolean success, @Nullable String text, @Nullable String errorMessage);
    }

    public interface ProgressListener {
        void onStatus(@NonNull String message);
    }

    public static final class LanguageModelSpec {
        public final String code;
        public final String displayName;
        public final String dirName;
        public final String downloadUrl;
        public final String approxSize;

        LanguageModelSpec(String code, String displayName, String dirName, String downloadUrl, String approxSize) {
            this.code = code;
            this.displayName = displayName;
            this.dirName = dirName;
            this.downloadUrl = downloadUrl;
            this.approxSize = approxSize;
        }
    }

    private static final Map<String, LanguageModelSpec> MODELS = new LinkedHashMap<>();

    static {
        register("es", "Español", "vosk-model-small-es-0.42", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip", "~39 MB");
        register("en", "Inglés (US)", "vosk-model-small-en-us-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip", "~40 MB");
        register("pt", "Portugués", "vosk-model-small-pt-0.3", "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip", "~31 MB");
        register("fr", "Francés", "vosk-model-small-fr-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip", "~41 MB");
        register("de", "Alemán", "vosk-model-small-de-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip", "~45 MB");
        register("it", "Italiano", "vosk-model-small-it-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip", "~48 MB");
        register("ru", "Ruso", "vosk-model-small-ru-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip", "~45 MB");
        register("ca", "Catalán", "vosk-model-small-ca-0.4", "https://alphacephei.com/vosk/models/vosk-model-small-ca-0.4.zip", "~42 MB");
    }

    private static void register(String code, String displayName, String dirName, String url, String approxSize) {
        MODELS.put(code, new LanguageModelSpec(code, displayName, dirName, url, approxSize));
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "KronoVoiceTranscriber");
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    });

    private static final ExecutorService DOWNLOAD_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "KronoVoiceDownload");
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    });

    private static final Object MODEL_LOCK = new Object();
    @Nullable
    private static Model cachedModel;
    @Nullable
    private static String cachedModelLang;

    private KronoVoiceTranscriber() {
    }

    public static String[] getSelectableLanguageCodes() {
        String[] codes = new String[MODELS.size() + 1];
        codes[0] = "auto";
        int i = 1;
        for (String code : MODELS.keySet()) {
            codes[i++] = code;
        }
        return codes;
    }

    public static String getLanguageDisplayName(@Nullable String code) {
        if (TextUtils.isEmpty(code) || "auto".equalsIgnoreCase(code)) {
            String resolved = resolveEffectiveLanguageCode("auto");
            return LocaleController.formatString(R.string.KronoVoiceLangAuto, getLocalizedSingleLanguageName(resolved));
        }
        return getLocalizedSingleLanguageName(code.toLowerCase(Locale.US));
    }

    private static String getLocalizedSingleLanguageName(@NonNull String code) {
        try {
            Locale appLocale = LocaleController.getInstance().getCurrentLocale();
            if (appLocale == null) {
                appLocale = Locale.getDefault();
            }
            Locale target = new Locale(code);
            String name = target.getDisplayLanguage(appLocale);
            if (!TextUtils.isEmpty(name) && !name.equalsIgnoreCase(code)) {
                return name.substring(0, 1).toUpperCase(appLocale) + name.substring(1);
            }
        } catch (Throwable ignore) {
        }
        LanguageModelSpec spec = MODELS.get(code);
        return spec != null ? spec.displayName : code;
    }

    @Nullable
    public static LanguageModelSpec getModelSpec(@Nullable String code) {
        String resolved = resolveEffectiveLanguageCode(code);
        return MODELS.get(resolved);
    }

    @NonNull
    public static String resolveEffectiveLanguageCode(@Nullable String configured) {
        if (!TextUtils.isEmpty(configured) && !"auto".equalsIgnoreCase(configured)) {
            String norm = normalizeLangCode(configured);
            if (MODELS.containsKey(norm)) {
                return norm;
            }
        }
        try {
            LocaleController.LocaleInfo info = LocaleController.getInstance().getCurrentLocaleInfo();
            if (info != null) {
                String fromInfo = normalizeLangCode(info.pluralLangCode);
                if (MODELS.containsKey(fromInfo)) {
                    return fromInfo;
                }
                fromInfo = normalizeLangCode(info.shortName);
                if (MODELS.containsKey(fromInfo)) {
                    return fromInfo;
                }
            }
            Locale appLocale = LocaleController.getInstance().getCurrentLocale();
            if (appLocale != null) {
                String fromLocale = normalizeLangCode(appLocale.getLanguage());
                if (MODELS.containsKey(fromLocale)) {
                    return fromLocale;
                }
            }
        } catch (Throwable ignore) {
        }
        String sys = normalizeLangCode(Locale.getDefault().getLanguage());
        if (MODELS.containsKey(sys)) {
            return sys;
        }
        return "en";
    }

    private static String normalizeLangCode(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return "";
        }
        String lower = raw.trim().toLowerCase(Locale.US);
        int sep = lower.indexOf('-');
        if (sep > 0) {
            lower = lower.substring(0, sep);
        }
        int under = lower.indexOf('_');
        if (under > 0) {
            lower = lower.substring(0, under);
        }
        return lower;
    }

    @Nullable
    private static File getModelsRootDir() {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) {
            return null;
        }
        File dir = new File(ctx.getFilesDir(), MODELS_ROOT_DIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    @Nullable
    private static File getModelDir(@NonNull LanguageModelSpec spec) {
        File root = getModelsRootDir();
        return root == null ? null : new File(root, spec.dirName);
    }

    public static boolean isModelDownloaded(@Nullable String langCode) {
        LanguageModelSpec spec = getModelSpec(langCode);
        if (spec == null) {
            return false;
        }
        File dir = getModelDir(spec);
        return isModelDirValid(dir);
    }

    private static boolean isModelDirValid(@Nullable File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        File ready = new File(dir, READY_MARKER);
        File conf = new File(dir, "conf");
        return ready.exists() && conf.isDirectory();
    }

    public static long getDownloadedModelsSizeBytes() {
        File root = getModelsRootDir();
        if (root == null || !root.exists()) {
            return 0L;
        }
        return dirSize(root);
    }

    private static long dirSize(File f) {
        if (f == null || !f.exists()) {
            return 0L;
        }
        if (f.isFile()) {
            return f.length();
        }
        long sum = 0L;
        File[] children = f.listFiles();
        if (children != null) {
            for (File child : children) {
                sum += dirSize(child);
            }
        }
        return sum;
    }

    public static void deleteAllModels(@Nullable Runnable onDone) {
        EXECUTOR.execute(() -> {
            synchronized (MODEL_LOCK) {
                closeCachedModelLocked();
                File root = getModelsRootDir();
                if (root != null && root.exists()) {
                    deleteRecursive(root);
                }
            }
            if (onDone != null) {
                AndroidUtilities.runOnUIThread(onDone);
            }
        });
    }

    public static void downloadModelAsync(@Nullable String langCode, @Nullable Utilities.Callback2<Boolean, String> done) {
        EXECUTOR.execute(() -> {
            LanguageModelSpec spec = getModelSpec(langCode);
            if (spec == null) {
                if (done != null) {
                    AndroidUtilities.runOnUIThread(() -> done.run(false, "Idioma de modelo no soportado."));
                }
                return;
            }
            try {
                ensureModelDownloaded(spec, null);
                if (done != null) {
                    AndroidUtilities.runOnUIThread(() -> done.run(true, "Modelo listo: " + spec.displayName));
                }
            } catch (Exception e) {
                FileLog.e(e);
                String msg = !TextUtils.isEmpty(e.getMessage()) ? e.getMessage() : "Error descargando el modelo de voz.";
                if (done != null) {
                    AndroidUtilities.runOnUIThread(() -> done.run(false, msg));
                }
            }
        });
    }

    public static void transcribe(
            @NonNull MessageObject messageObject,
            @Nullable ProgressListener progressListener,
            @NonNull TranscriptionCallback callback
    ) {
        DOWNLOAD_EXECUTOR.execute(() -> {
            notifyStatus(progressListener, null);

            final File mediaFile;
            try {
                mediaFile = ensureAudioFileDownloaded(messageObject);
            } catch (Exception e) {
                FileLog.e(e);
                postCallback(callback, false, null, "No se pudo descargar el audio del mensaje.");
                return;
            }
            if (mediaFile == null || !mediaFile.exists() || mediaFile.length() == 0) {
                postCallback(callback, false, null, "No se pudo descargar el audio del mensaje.");
                return;
            }

            EXECUTOR.execute(() -> {
                try {
                    KronoConfig.loadConfig();
                    LanguageModelSpec spec = getModelSpec(KronoConfig.voiceTranscriptionLanguage);
                    if (spec == null) {
                        postCallback(callback, false, null, "No hay modelo de voz disponible para este idioma.");
                        return;
                    }

                    boolean needDownload = !isModelDirValid(getModelDir(spec));
                    if (needDownload) {
                        notifyStatus(progressListener, "Descargando modelo de voz (" + spec.displayName + ", " + spec.approxSize + ") por única vez...");
                    }

                    Model model = obtainModel(spec, progressListener);
                    String text = recognizeFile(model, mediaFile);
                    postCallback(callback, true, text, null);
                } catch (Exception e) {
                    FileLog.e(e);
                    String err = e.getMessage();
                    if (TextUtils.isEmpty(err)) {
                        err = "No se pudo transcribir el audio.";
                    }
                    postCallback(callback, false, null, err);
                }
            });
        });
    }

    private static void notifyStatus(@Nullable ProgressListener listener, @Nullable String msg) {
        if (listener != null && !TextUtils.isEmpty(msg)) {
            AndroidUtilities.runOnUIThread(() -> listener.onStatus(msg));
        }
    }

    private static void postCallback(@NonNull TranscriptionCallback cb, boolean ok, @Nullable String text, @Nullable String err) {
        AndroidUtilities.runOnUIThread(() -> cb.onResult(ok, text, err));
    }

    @Nullable
    private static File ensureAudioFileDownloaded(@NonNull MessageObject messageObject) {
        int account = messageObject.currentAccount;
        FileLoader loader = FileLoader.getInstance(account);
        File file = loader.getPathToMessage(messageObject.messageOwner);
        if (file != null && file.exists() && file.length() > 0) {
            return file;
        }
        TLRPC.Document doc = messageObject.getDocument();
        if (doc == null) {
            return null;
        }
        File attachFile = loader.getPathToAttach(doc, true);
        if (attachFile != null && attachFile.exists() && attachFile.length() > 0) {
            return attachFile;
        }

        final String attachFileName = FileLoader.getAttachFileName(doc);
        if (TextUtils.isEmpty(attachFileName)) {
            return null;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final File[] loadedFile = new File[1];
        final NotificationCenter.NotificationCenterDelegate delegate = (id, acc, args) -> {
            if (id == NotificationCenter.fileLoaded) {
                if (args != null && args.length >= 2 && attachFileName.equals(args[0])) {
                    if (args[1] instanceof File) {
                        loadedFile[0] = (File) args[1];
                    }
                    latch.countDown();
                }
            } else if (id == NotificationCenter.fileLoadFailed) {
                if (args != null && args.length >= 1 && attachFileName.equals(args[0])) {
                    latch.countDown();
                }
            }
        };

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(account).addObserver(delegate, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(account).addObserver(delegate, NotificationCenter.fileLoadFailed);

            File quickCheck = loader.getPathToMessage(messageObject.messageOwner);
            if (quickCheck != null && quickCheck.exists() && quickCheck.length() > 0) {
                loadedFile[0] = quickCheck;
                latch.countDown();
                return;
            }
            loader.loadFile(doc, messageObject, FileLoader.PRIORITY_HIGH, 0);
        });

        boolean completed = false;
        try {
            completed = latch.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            final boolean timedOut = !completed;
            AndroidUtilities.runOnUIThread(() -> {
                NotificationCenter.getInstance(account).removeObserver(delegate, NotificationCenter.fileLoaded);
                NotificationCenter.getInstance(account).removeObserver(delegate, NotificationCenter.fileLoadFailed);
                if (timedOut) {
                    loader.cancelLoadFile(doc);
                }
            });
        }

        if (loadedFile[0] != null && loadedFile[0].exists() && loadedFile[0].length() > 0) {
            return loadedFile[0];
        }
        File finalCheck = loader.getPathToMessage(messageObject.messageOwner);
        if (finalCheck != null && finalCheck.exists() && finalCheck.length() > 0) {
            return finalCheck;
        }
        File attachFinal = loader.getPathToAttach(doc, true);
        if (attachFinal != null && attachFinal.exists() && attachFinal.length() > 0) {
            return attachFinal;
        }
        return null;
    }

    @NonNull
    private static Model obtainModel(@NonNull LanguageModelSpec spec, @Nullable ProgressListener progressListener) throws IOException {
        synchronized (MODEL_LOCK) {
            if (cachedModel != null && spec.code.equals(cachedModelLang)) {
                return cachedModel;
            }
            File dir = ensureModelDownloaded(spec, progressListener);
            closeCachedModelLocked();
            Model model = new Model(dir.getAbsolutePath());
            cachedModel = model;
            cachedModelLang = spec.code;
            return model;
        }
    }

    public static void releaseCachedModelAsync() {
        EXECUTOR.execute(() -> {
            synchronized (MODEL_LOCK) {
                closeCachedModelLocked();
            }
        });
    }

    private static void closeCachedModelLocked() {
        if (cachedModel != null) {
            try {
                cachedModel.close();
            } catch (Throwable ignore) {
            }
            cachedModel = null;
            cachedModelLang = null;
        }
    }

    @NonNull
    private static File ensureModelDownloaded(@NonNull LanguageModelSpec spec, @Nullable ProgressListener progressListener) throws IOException {
        File modelDir = getModelDir(spec);
        if (modelDir == null) {
            throw new IOException("Almacenamiento interno no disponible.");
        }
        if (isModelDirValid(modelDir)) {
            return modelDir;
        }

        File root = getModelsRootDir();
        File tmpZip = new File(root, spec.dirName + ".zip.part");
        File tmpDir = new File(root, spec.dirName + ".tmp");
        deleteRecursive(tmpDir);
        if (tmpZip.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tmpZip.delete();
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(spec.downloadUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(45000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "KronoGram-Android");
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("No se pudo descargar el modelo de voz (HTTP " + code + ").");
            }
            try (InputStream in = new BufferedInputStream(conn.getInputStream(), 32768);
                 FileOutputStream out = new FileOutputStream(tmpZip)) {
                byte[] buf = new byte[32768];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            tmpZip.delete();
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        notifyStatus(progressListener, "Preparando modelo de voz (" + spec.displayName + ")...");
        try {
            if (!tmpDir.mkdirs() && !tmpDir.isDirectory()) {
                throw new IOException("No se pudo crear directorio temporal del modelo.");
            }
            unzipModelSafely(tmpZip, tmpDir, spec.dirName);

            File marker = new File(tmpDir, READY_MARKER);
            try (FileOutputStream fos = new FileOutputStream(marker)) {
                fos.write('1');
            }
            if (!isModelDirValid(tmpDir)) {
                throw new IOException("El archivo del modelo descargado está incompleto o dañado.");
            }

            if (modelDir.exists()) {
                deleteRecursive(modelDir);
            }
            if (!tmpDir.renameTo(modelDir)) {
                throw new IOException("No se pudo guardar el modelo de voz en memoria.");
            }
            return modelDir;
        } catch (IOException e) {
            deleteRecursive(tmpDir);
            throw e;
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmpZip.delete();
        }
    }

    private static void unzipModelSafely(File zipFile, File destDir, String expectedTopPrefix) throws IOException {
        String canonicalDest = destDir.getCanonicalPath();
        String stripPrefix = expectedTopPrefix + "/";
        byte[] buffer = new byte[16384];
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new java.io.FileInputStream(zipFile), 32768))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (TextUtils.isEmpty(name)) {
                    continue;
                }
                if (name.startsWith(stripPrefix)) {
                    name = name.substring(stripPrefix.length());
                } else if (name.equals(expectedTopPrefix)) {
                    continue;
                }
                if (TextUtils.isEmpty(name)) {
                    continue;
                }
                File outFile = new File(destDir, name);
                String canonicalOut = outFile.getCanonicalPath();
                if (!canonicalOut.equals(canonicalDest) && !canonicalOut.startsWith(canonicalDest + File.separator)) {
                    throw new IOException("Entrada ZIP inválida: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    if (!outFile.isDirectory() && !outFile.mkdirs()) {
                        throw new IOException("No se pudo crear carpeta: " + outFile.getName());
                    }
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("No se pudo crear carpeta padre: " + parent.getName());
                    }
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile), 16384)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursive(c);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @NonNull
    private static String recognizeFile(@NonNull Model model, @NonNull File audioFile) throws IOException {
        StringBuilder transcript = new StringBuilder();
        boolean decodedWithMedia3 = false;
        Exception media3Err = null;
        try (Recognizer recognizer = new Recognizer(model, TARGET_SAMPLE_RATE)) {
            byte[] feedChunk = new byte[8192];
            PcmStreamWriter pcmSink = new PcmStreamWriter(recognizer, transcript, feedChunk);
            try {
                decodedWithMedia3 = decodeWithMedia3Ffmpeg(audioFile, pcmSink);
                if (decodedWithMedia3) {
                    pcmSink.flush();
                    appendJsonText(transcript, recognizer.getFinalResult());
                }
            } catch (Exception e) {
                media3Err = e;
                FileLog.e(e);
                decodedWithMedia3 = false;
            }
        }

        if (!decodedWithMedia3) {
            transcript.setLength(0);
            try (Recognizer recognizer = new Recognizer(model, TARGET_SAMPLE_RATE)) {
                byte[] feedChunk = new byte[8192];
                PcmStreamWriter pcmSink = new PcmStreamWriter(recognizer, transcript, feedChunk);
                decodeWithPlatformAudioDecoder(audioFile, pcmSink);
                pcmSink.flush();
                appendJsonText(transcript, recognizer.getFinalResult());
            } catch (Exception platformErr) {
                if (media3Err != null) {
                    FileLog.e(media3Err);
                }
                FileLog.e(platformErr);
                throw new IOException("No se pudo decodificar el formato de audio.");
            }
        }

        String raw = transcript.toString().trim();
        if (TextUtils.isEmpty(raw)) {
            return "";
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private static boolean decodeWithMedia3Ffmpeg(@NonNull File audioFile, @NonNull PcmStreamWriter pcmSink) throws Exception {
        DefaultExtractorsFactory factory = new DefaultExtractorsFactory();
        Extractor[] extractors = factory.createExtractors(Uri.fromFile(audioFile), Collections.emptyMap());
        if (extractors == null || extractors.length == 0) {
            return false;
        }

        FileDataSource dataSource = new FileDataSource();
        Extractor matched = null;
        try {
            long len = dataSource.open(new DataSpec.Builder().setUri(Uri.fromFile(audioFile)).setPosition(0).build());
            ExtractorInput sniffInput = new DefaultExtractorInput(dataSource, 0, len);
            for (Extractor extractor : extractors) {
                try {
                    if (extractor.sniff(sniffInput)) {
                        matched = extractor;
                        break;
                    }
                } catch (EOFException ignored) {
                } finally {
                    sniffInput.resetPeekPosition();
                }
            }
        } finally {
            closeQuietly(dataSource);
        }

        if (matched == null) {
            releaseExtractors(extractors, null);
            return false;
        }
        releaseExtractors(extractors, matched);

        final FfmpegAudioDecoder[] decoderRef = new FfmpegAudioDecoder[1];
        final int[] audioTrackId = new int[]{-1};
        final boolean[] unsupportedCodec = new boolean[]{false};
        final Format[] lastFormat = new Format[1];

        ExtractorOutput output = new ExtractorOutput() {
            @Override
            public TrackOutput track(int id, int type) {
                if (type == C.TRACK_TYPE_AUDIO && audioTrackId[0] == -1) {
                    audioTrackId[0] = id;
                    return new TrackOutput() {
                        private final ByteArrayOutputStream staging = new ByteArrayOutputStream(8192);

                        @Override
                        public void format(Format format) {
                            lastFormat[0] = format;
                            if (decoderRef[0] == null && !unsupportedCodec[0]) {
                                if (format.sampleMimeType != null && FfmpegLibrary.supportsFormat(format.sampleMimeType)) {
                                    try {
                                        decoderRef[0] = new FfmpegAudioDecoder(format, 4, 4, 32768, false);
                                    } catch (Exception e) {
                                        FileLog.e(e);
                                        unsupportedCodec[0] = true;
                                    }
                                } else {
                                    unsupportedCodec[0] = true;
                                }
                            }
                        }

                        @Override
                        public int sampleData(DataReader input, int length, boolean allowEndOfInput, int sampleDataPart) throws IOException {
                            byte[] tmp = new byte[Math.min(length, 8192)];
                            int read = input.read(tmp, 0, tmp.length);
                            if (read == C.RESULT_END_OF_INPUT) {
                                if (allowEndOfInput) {
                                    return C.RESULT_END_OF_INPUT;
                                }
                                throw new EOFException();
                            }
                            if (sampleDataPart == SAMPLE_DATA_PART_MAIN && read > 0) {
                                staging.write(tmp, 0, read);
                            }
                            return read;
                        }

                        @Override
                        public void sampleData(ParsableByteArray data, int length, int sampleDataPart) {
                            if (sampleDataPart == SAMPLE_DATA_PART_MAIN && length > 0) {
                                byte[] tmp = new byte[length];
                                data.readBytes(tmp, 0, length);
                                staging.write(tmp, 0, length);
                            } else {
                                data.skipBytes(length);
                            }
                        }

                        @Override
                        public void sampleMetadata(long timeUs, int flags, int size, int offset, @Nullable CryptoData cryptoData) {
                            byte[] buffered = staging.toByteArray();
                            int sampleEnd = Math.max(0, buffered.length - offset);
                            int sampleStart = Math.max(0, sampleEnd - size);
                            staging.reset();
                            if (offset > 0 && sampleEnd < buffered.length) {
                                staging.write(buffered, sampleEnd, buffered.length - sampleEnd);
                            }
                            int sampleLen = sampleEnd - sampleStart;
                            if (sampleLen <= 0 || decoderRef[0] == null) {
                                return;
                            }
                            try {
                                feedPacketToFfmpegDecoder(decoderRef[0], buffered, sampleStart, sampleLen, timeUs, flags, lastFormat[0], pcmSink);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                }
                return new DiscardingTrackOutput();
            }

            @Override
            public void endTracks() {
            }

            @Override
            public void seekMap(SeekMap seekMap) {
            }
        };

        FileDataSource readSource = new FileDataSource();
        try {
            long len = readSource.open(new DataSpec.Builder().setUri(Uri.fromFile(audioFile)).setPosition(0).build());
            long totalLen = len == C.LENGTH_UNSET ? audioFile.length() : len;
            ExtractorInput input = new DefaultExtractorInput(readSource, 0, totalLen);
            matched.init(output);

            PositionHolder positionHolder = new PositionHolder();
            while (true) {
                int res = matched.read(input, positionHolder);
                if (unsupportedCodec[0]) {
                    return false;
                }
                if (res == Extractor.RESULT_END_OF_INPUT) {
                    break;
                } else if (res == Extractor.RESULT_SEEK) {
                    long seekPos = positionHolder.position;
                    closeQuietly(readSource);
                    readSource = new FileDataSource();
                    long rem = readSource.open(new DataSpec.Builder().setUri(Uri.fromFile(audioFile)).setPosition(seekPos).build());
                    long knownLen = rem == C.LENGTH_UNSET ? audioFile.length() : (seekPos + rem);
                    input = new DefaultExtractorInput(readSource, seekPos, knownLen);
                }
            }

            if (decoderRef[0] != null) {
                feedEosAndDrain(decoderRef[0], pcmSink);
                return pcmSink.hasWrittenSamples();
            }
            return false;
        } finally {
            closeQuietly(readSource);
            matched.release();
            if (decoderRef[0] != null) {
                decoderRef[0].release();
            }
        }
    }

    private static void feedPacketToFfmpegDecoder(
            FfmpegAudioDecoder decoder,
            byte[] data,
            int offset,
            int length,
            long timeUs,
            int flags,
            @Nullable Format format,
            PcmStreamWriter pcmSink
    ) throws Exception {
        DecoderInputBuffer inBuf = waitForInputBuffer(decoder, pcmSink);
        inBuf.clear();
        inBuf.ensureSpaceForWrite(length);
        inBuf.data.put(data, offset, length);
        inBuf.data.flip();
        inBuf.timeUs = timeUs;
        inBuf.setFlags(flags);
        inBuf.format = format;
        decoder.queueInputBuffer(inBuf);
        drainFfmpegOutput(decoder, pcmSink, false);
    }

    private static void feedEosAndDrain(FfmpegAudioDecoder decoder, PcmStreamWriter pcmSink) throws Exception {
        DecoderInputBuffer inBuf = waitForInputBuffer(decoder, pcmSink);
        inBuf.clear();
        inBuf.addFlag(C.BUFFER_FLAG_END_OF_STREAM);
        decoder.queueInputBuffer(inBuf);
        drainFfmpegOutput(decoder, pcmSink, true);
    }

    private static DecoderInputBuffer waitForInputBuffer(FfmpegAudioDecoder decoder, PcmStreamWriter pcmSink) throws Exception {
        int attempts = 0;
        while (attempts++ < 2500) {
            drainFfmpegOutput(decoder, pcmSink, false);
            DecoderInputBuffer inBuf = decoder.dequeueInputBuffer();
            if (inBuf != null) {
                return inBuf;
            }
            Thread.sleep(2);
        }
        throw new IOException("Tiempo de espera agotado al decodificar el audio.");
    }

    private static void drainFfmpegOutput(FfmpegAudioDecoder decoder, PcmStreamWriter pcmSink, boolean waitForEos) throws Exception {
        int idleAttempts = 0;
        while (true) {
            SimpleDecoderOutputBuffer outBuf = decoder.dequeueOutputBuffer();
            if (outBuf == null) {
                if (!waitForEos || ++idleAttempts > 2500) {
                    return;
                }
                Thread.sleep(2);
                continue;
            }
            idleAttempts = 0;
            try {
                boolean eos = outBuf.isEndOfStream();
                if (!outBuf.shouldBeSkipped && outBuf.data != null && outBuf.data.hasRemaining()) {
                    int sr = decoder.getSampleRate();
                    int ch = decoder.getChannelCount();
                    if (sr <= 0) {
                        sr = 48000;
                    }
                    if (ch <= 0) {
                        ch = 1;
                    }
                    pcmSink.writePcm16(outBuf.data, sr, ch);
                }
                if (eos) {
                    return;
                }
            } finally {
                outBuf.release();
            }
        }
    }

    private static void decodeWithPlatformAudioDecoder(@NonNull File audioFile, @NonNull PcmStreamWriter pcmSink) throws Exception {
        AudioDecoder decoder = new AudioDecoder(audioFile.getAbsolutePath());
        try {
            decoder.start();
            while (!decoder.isDecodingDone()) {
                AudioDecoder.DecodedBufferData data = decoder.decode();
                if (data.index >= 0) {
                    try {
                        if (data.byteBuffer != null && data.byteBuffer.hasRemaining()) {
                            int sr = decoder.getSampleRate();
                            int ch = decoder.getChannelCount();
                            if (sr <= 0) {
                                sr = 48000;
                            }
                            if (ch <= 0) {
                                ch = 1;
                            }
                            pcmSink.writePcm16(data.byteBuffer, sr, ch);
                        }
                    } finally {
                        decoder.releaseOutputBuffer(data.index);
                    }
                }
            }
        } finally {
            decoder.release();
        }
    }

    private static void releaseExtractors(Extractor[] extractors, @Nullable Extractor keep) {
        for (Extractor e : extractors) {
            if (e != null && e != keep) {
                try {
                    e.release();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private static void closeQuietly(@Nullable FileDataSource ds) {
        if (ds != null) {
            try {
                ds.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private static void appendJsonText(StringBuilder sb, @Nullable String jsonStr) {
        if (TextUtils.isEmpty(jsonStr)) {
            return;
        }
        try {
            JSONObject obj = new JSONObject(jsonStr);
            String text = obj.optString("text", "").trim();
            if (!TextUtils.isEmpty(text)) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(text);
            }
        } catch (Throwable ignore) {
        }
    }

    /**
     * Stateful linear resampler + channel downmixer converting arbitrary PCM16 LE buffers to 16 kHz mono
     * and streaming them to Vosk Recognizer.
     */
    private static final class PcmStreamWriter {
        private final Recognizer recognizer;
        private final StringBuilder transcript;
        private final byte[] outChunk;
        private int outPos = 0;
        private boolean wroteAny = false;

        private boolean hasPrevSample = false;
        private short prevSample = 0;
        private double phase = 0.0;

        PcmStreamWriter(Recognizer recognizer, StringBuilder transcript, byte[] outChunk) {
            this.recognizer = recognizer;
            this.transcript = transcript;
            this.outChunk = outChunk;
        }

        void resetState() {
            outPos = 0;
            hasPrevSample = false;
            prevSample = 0;
            phase = 0.0;
            wroteAny = false;
        }

        boolean hasWrittenSamples() {
            return wroteAny;
        }

        void writePcm16(@NonNull ByteBuffer buffer, int srcSampleRate, int srcChannels) {
            ByteBuffer le = buffer.order() == ByteOrder.LITTLE_ENDIAN ? buffer : buffer.slice().order(ByteOrder.LITTLE_ENDIAN);
            int remainingBytes = le.remaining();
            int frameBytes = srcChannels * 2;
            int frames = remainingBytes / frameBytes;
            if (frames <= 0) {
                return;
            }
            wroteAny = true;

            if (srcSampleRate == TARGET_SAMPLE_RATE_INT && srcChannels == 1) {
                for (int i = 0; i < frames; i++) {
                    emitSample(le.getShort());
                }
                return;
            }

            double step = (double) srcSampleRate / (double) TARGET_SAMPLE_RATE_INT;
            for (int f = 0; f < frames; f++) {
                long sum = 0;
                for (int c = 0; c < srcChannels; c++) {
                    sum += le.getShort();
                }
                short mono = (short) (sum / srcChannels);

                if (!hasPrevSample) {
                    prevSample = mono;
                    hasPrevSample = true;
                    phase = 0.0;
                    emitSample(mono);
                    phase += step;
                    continue;
                }

                // phase tracks position between prevSample (0.0) and current mono (1.0)
                while (phase <= 1.0) {
                    double val = prevSample + (mono - prevSample) * phase;
                    int clamped = (int) Math.round(val);
                    if (clamped > Short.MAX_VALUE) clamped = Short.MAX_VALUE;
                    if (clamped < Short.MIN_VALUE) clamped = Short.MIN_VALUE;
                    emitSample((short) clamped);
                    phase += step;
                }
                phase -= 1.0;
                prevSample = mono;
            }
        }

        private void emitSample(short sample) {
            if (outPos + 2 > outChunk.length) {
                flushOutChunk();
            }
            outChunk[outPos++] = (byte) (sample & 0xFF);
            outChunk[outPos++] = (byte) ((sample >> 8) & 0xFF);
        }

        void flush() {
            if (hasPrevSample) {
                emitSample(prevSample);
            }
            flushOutChunk();
        }

        private void flushOutChunk() {
            if (outPos > 0) {
                if (recognizer.acceptWaveForm(outChunk, outPos)) {
                    appendJsonText(transcript, recognizer.getResult());
                }
                outPos = 0;
            }
        }
    }
}
