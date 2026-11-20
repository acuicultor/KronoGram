package org.telegram.messenger;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OsmMapsProvider implements IMapsProvider {

    private static final float MIN_ZOOM = 2f;
    private static final float MAX_ZOOM = 19f;
    private static final double MAX_LATITUDE = 85.05112878;

    private static final LruCache<String, Bitmap> tileCache = new LruCache<String, Bitmap>(96) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return 1;
        }
    };
    private static final Set<String> inFlightTiles = Collections.synchronizedSet(new HashSet<>());
    private static final ExecutorService tileExecutor = Executors.newFixedThreadPool(4);

    @Override
    public void initializeMaps(Context context) {
        // No API key or external SDK initialization required.
    }

    @Override
    public IMapView onCreateMapView(Context context) {
        return new OsmMapView(context);
    }

    @Override
    public IMarkerOptions onCreateMarkerOptions() {
        return new OsmMarkerOptions();
    }

    @Override
    public ICircleOptions onCreateCircleOptions() {
        return new OsmCircleOptions();
    }

    @Override
    public ILatLngBoundsBuilder onCreateLatLngBoundsBuilder() {
        return new OsmLatLngBoundsBuilder();
    }

    @Override
    public ICameraUpdate newCameraUpdateLatLng(LatLng latLng) {
        return new OsmCameraUpdate(latLng, Float.NaN, null, 0);
    }

    @Override
    public ICameraUpdate newCameraUpdateLatLngZoom(LatLng latLng, float zoom) {
        return new OsmCameraUpdate(latLng, zoom, null, 0);
    }

    @Override
    public ICameraUpdate newCameraUpdateLatLngBounds(ILatLngBounds bounds, int padding) {
        return new OsmCameraUpdate(null, Float.NaN, (OsmLatLngBounds) bounds, padding);
    }

    @Override
    public IMapStyleOptions loadRawResourceStyle(Context context, int resId) {
        return new OsmMapStyleOptions(resId != 0);
    }

    @Override
    public String getMapsAppPackageName() {
        return "com.google.android.apps.maps";
    }

    @Override
    public int getInstallMapsString() {
        return R.string.InstallGoogleMaps;
    }

    private static double clampLat(double lat) {
        return Math.max(-MAX_LATITUDE, Math.min(MAX_LATITUDE, lat));
    }

    private static double normalizeLon(double lon) {
        while (lon > 180.0) {
            lon -= 360.0;
        }
        while (lon < -180.0) {
            lon += 360.0;
        }
        return lon;
    }

    private static double lonToNormX(double lon) {
        return (normalizeLon(lon) + 180.0) / 360.0;
    }

    private static double latToNormY(double lat) {
        double sinLat = Math.sin(Math.toRadians(clampLat(lat)));
        return 0.5 - Math.log((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * Math.PI);
    }

    private static double normXToLon(double normX) {
        return normalizeLon(normX * 360.0 - 180.0);
    }

    private static double normYToLat(double normY) {
        double clamped = Math.max(0.0, Math.min(1.0, normY));
        double n = Math.PI - 2.0 * Math.PI * clamped;
        return clampLat(Math.toDegrees(Math.atan(Math.sinh(n))));
    }

    public static final class OsmCameraUpdate implements ICameraUpdate {
        final LatLng target;
        final float zoom;
        final OsmLatLngBounds bounds;
        final int padding;

        OsmCameraUpdate(LatLng target, float zoom, OsmLatLngBounds bounds, int padding) {
            this.target = target;
            this.zoom = zoom;
            this.bounds = bounds;
            this.padding = padding;
        }
    }

    public static final class OsmMapStyleOptions implements IMapStyleOptions {
        final boolean dark;

        OsmMapStyleOptions(boolean dark) {
            this.dark = dark;
        }
    }

    public static final class OsmLatLngBounds implements ILatLngBounds {
        final double south;
        final double west;
        final double north;
        final double east;

        OsmLatLngBounds(double south, double west, double north, double east) {
            this.south = south;
            this.west = west;
            this.north = north;
            this.east = east;
        }

        @Override
        public LatLng getCenter() {
            return new LatLng((south + north) / 2.0, (west + east) / 2.0);
        }
    }

    public static final class OsmLatLngBoundsBuilder implements ILatLngBoundsBuilder {
        private double south = Double.POSITIVE_INFINITY;
        private double north = Double.NEGATIVE_INFINITY;
        private double west = Double.POSITIVE_INFINITY;
        private double east = Double.NEGATIVE_INFINITY;
        private boolean hasPoints;

        @Override
        public ILatLngBoundsBuilder include(LatLng latLng) {
            if (latLng == null) {
                return this;
            }
            hasPoints = true;
            south = Math.min(south, latLng.latitude);
            north = Math.max(north, latLng.latitude);
            west = Math.min(west, latLng.longitude);
            east = Math.max(east, latLng.longitude);
            return this;
        }

        @Override
        public ILatLngBounds build() {
            if (!hasPoints) {
                return new OsmLatLngBounds(0, 0, 0, 0);
            }
            return new OsmLatLngBounds(south, west, north, east);
        }
    }

    public static final class OsmMarkerOptions implements IMarkerOptions {
        LatLng position = new LatLng(0, 0);
        Bitmap bitmap;
        int resId;
        float anchorU = 0.5f;
        float anchorV = 1.0f;
        String title;
        String snippet;
        boolean flat;

        @Override
        public IMarkerOptions position(LatLng latLng) {
            if (latLng != null) {
                this.position = latLng;
            }
            return this;
        }

        @Override
        public IMarkerOptions icon(Bitmap bitmap) {
            this.bitmap = bitmap;
            this.resId = 0;
            return this;
        }

        @Override
        public IMarkerOptions icon(int resId) {
            this.resId = resId;
            this.bitmap = null;
            return this;
        }

        @Override
        public IMarkerOptions anchor(float lat, float lng) {
            this.anchorU = lat;
            this.anchorV = lng;
            return this;
        }

        @Override
        public IMarkerOptions title(String title) {
            this.title = title;
            return this;
        }

        @Override
        public IMarkerOptions snippet(String snippet) {
            this.snippet = snippet;
            return this;
        }

        @Override
        public IMarkerOptions flat(boolean flat) {
            this.flat = flat;
            return this;
        }
    }

    public static final class OsmCircleOptions implements ICircleOptions {
        LatLng center = new LatLng(0, 0);
        double radius;
        int strokeColor = 0xff2196f3;
        int fillColor = 0x222196f3;
        List<PatternItem> patternItems;
        int strokeWidth = 2;

        @Override
        public ICircleOptions center(LatLng latLng) {
            if (latLng != null) {
                this.center = latLng;
            }
            return this;
        }

        @Override
        public ICircleOptions radius(double radius) {
            this.radius = radius;
            return this;
        }

        @Override
        public ICircleOptions strokeColor(int color) {
            this.strokeColor = color;
            return this;
        }

        @Override
        public ICircleOptions fillColor(int color) {
            this.fillColor = color;
            return this;
        }

        @Override
        public ICircleOptions strokePattern(List<PatternItem> patternItems) {
            this.patternItems = patternItems;
            return this;
        }

        @Override
        public ICircleOptions strokeWidth(int width) {
            this.strokeWidth = width;
            return this;
        }
    }

    public static final class OsmUISettings implements IUISettings {
        @Override
        public void setZoomControlsEnabled(boolean enabled) {
        }

        @Override
        public void setMyLocationButtonEnabled(boolean enabled) {
        }

        @Override
        public void setCompassEnabled(boolean enabled) {
        }
    }

    private static final class OsmMapImpl implements IMap {
        private final OsmMapView view;
        private final OsmUISettings uiSettings = new OsmUISettings();

        OsmMapImpl(OsmMapView view) {
            this.view = view;
        }

        @Override
        public void setMapType(int mapType) {
            view.invalidate();
        }

        @Override
        public void animateCamera(ICameraUpdate update) {
            animateCamera(update, 250, null);
        }

        @Override
        public void animateCamera(ICameraUpdate update, ICancelableCallback callback) {
            animateCamera(update, 250, callback);
        }

        @Override
        public void animateCamera(ICameraUpdate update, int duration, ICancelableCallback callback) {
            if (update instanceof OsmCameraUpdate) {
                view.animateTo((OsmCameraUpdate) update, duration, callback);
            } else if (callback != null) {
                callback.onFinish();
            }
        }

        @Override
        public void moveCamera(ICameraUpdate update) {
            if (update instanceof OsmCameraUpdate) {
                view.jumpTo((OsmCameraUpdate) update);
            }
        }

        @Override
        public float getMaxZoomLevel() {
            return MAX_ZOOM;
        }

        @Override
        public float getMinZoomLevel() {
            return MIN_ZOOM;
        }

        @Override
        public void setMyLocationEnabled(boolean enabled) {
            view.setMyLocationEnabled(enabled);
        }

        @Override
        public IUISettings getUiSettings() {
            return uiSettings;
        }

        @Override
        public void setOnCameraIdleListener(Runnable callback) {
            view.onCameraIdleListener = callback;
        }

        @Override
        public void setOnCameraMoveStartedListener(OnCameraMoveStartedListener listener) {
            view.onCameraMoveStartedListener = listener;
        }

        @Override
        public CameraPosition getCameraPosition() {
            return new CameraPosition(new LatLng(view.cameraLat, view.cameraLon), view.cameraZoom);
        }

        @Override
        public void setOnMapLoadedCallback(Runnable callback) {
            view.onMapLoadedCallback = callback;
            if (callback != null) {
                if (view.hasDrawnFirstFrame) {
                    view.post(callback);
                } else {
                    view.invalidate();
                }
            }
        }

        @Override
        public IProjection getProjection() {
            return latLng -> {
                if (latLng == null) {
                    return new Point(0, 0);
                }
                float[] xy = view.latLngToScreen(latLng.latitude, latLng.longitude);
                return new Point(Math.round(xy[0]), Math.round(xy[1]));
            };
        }

        @Override
        public void setPadding(int left, int top, int right, int bottom) {
            view.setMapPadding(left, top, right, bottom);
        }

        @Override
        public void setMapStyle(IMapStyleOptions style) {
            view.setDarkStyle(style instanceof OsmMapStyleOptions && ((OsmMapStyleOptions) style).dark);
        }

        @Override
        public IMarker addMarker(IMarkerOptions markerOptions) {
            OsmMarkerOptions opts = (OsmMarkerOptions) markerOptions;
            Bitmap bmp = opts.bitmap;
            if (bmp == null && opts.resId != 0) {
                bmp = OsmMarker.decodeBitmapFromRes(view.getContext(), opts.resId);
            }
            OsmMarker marker = new OsmMarker(view, opts.position, bmp, opts.anchorU, opts.anchorV);
            view.markers.add(marker);
            view.invalidate();
            return marker;
        }

        @Override
        public void setOnMyLocationChangeListener(Consumer<Location> callback) {
            view.onMyLocationChangeListener = callback;
            if (callback != null && view.lastKnownLocation != null) {
                Location loc = new Location(view.lastKnownLocation);
                view.post(() -> callback.accept(loc));
            }
        }

        @Override
        public void setOnMarkerClickListener(OnMarkerClickListener markerClickListener) {
            view.onMarkerClickListener = markerClickListener;
        }

        @Override
        public void setOnCameraMoveListener(Runnable callback) {
            view.onCameraMoveListener = callback;
        }

        @Override
        public ICircle addCircle(ICircleOptions circleOptions) {
            OsmCircleOptions opts = (OsmCircleOptions) circleOptions;
            OsmCircle circle = new OsmCircle(view, opts);
            view.circles.add(circle);
            view.invalidate();
            return circle;
        }
    }

    private static final class OsmMarker implements IMarker {
        private final OsmMapView view;
        private LatLng position;
        private Bitmap bitmap;
        private final float anchorU;
        private final float anchorV;
        private int rotation;
        private Object tag;

        OsmMarker(OsmMapView view, LatLng position, Bitmap bitmap, float anchorU, float anchorV) {
            this.view = view;
            this.position = position;
            this.bitmap = bitmap;
            this.anchorU = anchorU;
            this.anchorV = anchorV;
        }

        @Override
        public Object getTag() {
            return tag;
        }

        @Override
        public void setTag(Object tag) {
            this.tag = tag;
        }

        @Override
        public LatLng getPosition() {
            return position;
        }

        @Override
        public void setPosition(LatLng latLng) {
            if (latLng != null) {
                this.position = latLng;
                view.invalidate();
            }
        }

        @Override
        public void setRotation(int rotation) {
            this.rotation = rotation;
            view.invalidate();
        }

        @Override
        public void setIcon(Bitmap bitmap) {
            this.bitmap = bitmap;
            view.invalidate();
        }

        @Override
        public void setIcon(int resId) {
            this.bitmap = decodeBitmapFromRes(view.getContext(), resId);
            view.invalidate();
        }

        private static Bitmap decodeBitmapFromRes(Context context, int resId) {
            if (context == null || resId == 0) {
                return null;
            }
            Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resId);
            if (bmp != null) {
                return bmp;
            }
            try {
                android.graphics.drawable.Drawable drawable = androidx.core.content.ContextCompat.getDrawable(context, resId);
                if (drawable != null) {
                    int w = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : AndroidUtilities.dp(32);
                    int h = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : AndroidUtilities.dp(32);
                    Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(out);
                    drawable.setBounds(0, 0, w, h);
                    drawable.draw(canvas);
                    return out;
                }
            } catch (Throwable ignore) {
            }
            return null;
        }

        @Override
        public void remove() {
            view.markers.remove(this);
            view.invalidate();
        }
    }

    private static final class OsmCircle implements ICircle {
        private final OsmMapView view;
        private LatLng center;
        private double radius;
        private int strokeColor;
        private int fillColor;
        private final int strokeWidth;
        private final List<PatternItem> patternItems;

        OsmCircle(OsmMapView view, OsmCircleOptions opts) {
            this.view = view;
            this.center = opts.center;
            this.radius = opts.radius;
            this.strokeColor = opts.strokeColor;
            this.fillColor = opts.fillColor;
            this.strokeWidth = opts.strokeWidth;
            this.patternItems = opts.patternItems;
        }

        @Override
        public void setStrokeColor(int color) {
            this.strokeColor = color;
            view.invalidate();
        }

        @Override
        public void setFillColor(int color) {
            this.fillColor = color;
            view.invalidate();
        }

        @Override
        public void setRadius(double radius) {
            this.radius = radius;
            view.invalidate();
        }

        @Override
        public double getRadius() {
            return radius;
        }

        @Override
        public void setCenter(LatLng latLng) {
            if (latLng != null) {
                this.center = latLng;
                view.invalidate();
            }
        }

        @Override
        public void remove() {
            view.circles.remove(this);
            view.invalidate();
        }
    }

    private static final class OsmMapView extends FrameLayout implements IMapView {
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final float tilePx;
        private final OsmMapImpl mapImpl;

        private ITouchInterceptor dispatchInterceptor;
        private ITouchInterceptor interceptInterceptor;
        private Runnable onLayoutListener;

        Runnable onCameraIdleListener;
        OnCameraMoveStartedListener onCameraMoveStartedListener;
        Runnable onCameraMoveListener;
        OnMarkerClickListener onMarkerClickListener;
        Consumer<Location> onMyLocationChangeListener;
        Runnable onMapLoadedCallback;

        final List<OsmMarker> markers = new ArrayList<>();
        final List<OsmCircle> circles = new ArrayList<>();

        double cameraLat = 0.0;
        double cameraLon = 0.0;
        float cameraZoom = 2f;

        private int padLeft;
        private int padTop;
        private int padRight;
        private int padBottom;
        private boolean darkStyle;
        private volatile boolean destroyed;
        private volatile int activeTileZ = -1;
        private boolean myLocationEnabled;
        private Location lastKnownLocation;
        private LocationListener locationListener;
        private ILocationServiceProvider.ILocationListener fusedLocationListener;
        private ValueAnimator cameraAnimator;
        private OsmCameraUpdate pendingBoundsUpdate;
        boolean hasDrawnFirstFrame;

        private final Paint tilePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint darkTilePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circleStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint myLocationAccuracyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint myLocationBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint myLocationDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint defaultMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint attributionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect srcRect = new Rect();
        private final RectF dstRect = new RectF();

        private final ScaleGestureDetector scaleDetector;
        private final GestureDetector gestureDetector;
        private boolean isDragging;
        private boolean isScaling;
        private int activePointerId = MotionEvent.INVALID_POINTER_ID;
        private float lastTouchX;
        private float lastTouchY;

        OsmMapView(Context context) {
            super(context);
            setWillNotDraw(false);
            float density = context.getResources().getDisplayMetrics().density;
            tilePx = Math.round(256f * Math.max(1f, Math.min(2f, density * 0.65f)));
            mapImpl = new OsmMapImpl(this);

            ColorMatrix darkMatrix = new ColorMatrix(new float[]{
                -0.82f, 0f, 0f, 0f, 232f,
                0f, -0.82f, 0f, 0f, 236f,
                0f, 0f, -0.78f, 0f, 245f,
                0f, 0f, 0f, 1f, 0f
            });
            darkTilePaint.setColorFilter(new ColorMatrixColorFilter(darkMatrix));

            gridPaint.setStrokeWidth(Math.max(1f, density));
            circleFillPaint.setStyle(Paint.Style.FILL);
            circleStrokePaint.setStyle(Paint.Style.STROKE);
            myLocationAccuracyPaint.setStyle(Paint.Style.FILL);
            myLocationAccuracyPaint.setColor(0x281e88e5);
            myLocationBorderPaint.setStyle(Paint.Style.FILL);
            myLocationBorderPaint.setColor(0xffffffff);
            myLocationDotPaint.setStyle(Paint.Style.FILL);
            myLocationDotPaint.setColor(0xff1e88e5);
            defaultMarkerPaint.setStyle(Paint.Style.FILL);
            defaultMarkerPaint.setColor(0xffe53935);
            attributionPaint.setTextSize(10f * density);

            scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                    isScaling = true;
                    cancelAnimation();
                    if (onCameraMoveStartedListener != null) {
                        onCameraMoveStartedListener.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_GESTURE);
                    }
                    return true;
                }

                @Override
                public boolean onScale(@NonNull ScaleGestureDetector detector) {
                    float scaleFactor = detector.getScaleFactor();
                    if (scaleFactor > 0f && !Float.isNaN(scaleFactor)) {
                        float zoomDelta = (float) (Math.log(scaleFactor) / Math.log(2.0));
                        cameraZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, cameraZoom + zoomDelta));
                        invalidate();
                        if (onCameraMoveListener != null) {
                            onCameraMoveListener.run();
                        }
                    }
                    return true;
                }

                @Override
                public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
                    isScaling = false;
                }
            });

            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(@NonNull MotionEvent e) {
                    if (onMarkerClickListener != null && !markers.isEmpty()) {
                        float hitRadius = 28f * getResources().getDisplayMetrics().density;
                        float tapX = e.getX();
                        float tapY = e.getY();
                        for (int i = markers.size() - 1; i >= 0; i--) {
                            OsmMarker m = markers.get(i);
                            float[] xy = latLngToScreen(m.position.latitude, m.position.longitude);
                            float dx = tapX - xy[0];
                            float dy = tapY - xy[1];
                            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                                if (onMarkerClickListener.onClick(m)) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }

                @Override
                public boolean onDoubleTap(@NonNull MotionEvent e) {
                    float targetZoom = Math.min(MAX_ZOOM, cameraZoom + 1.5f);
                    animateTo(new OsmCameraUpdate(new LatLng(cameraLat, cameraLon), targetZoom, null, 0), 220, null);
                    return true;
                }
            });
        }

        private float getCenterX() {
            int w = getWidth();
            int usable = Math.max(0, w - padLeft - padRight);
            return padLeft + usable / 2f;
        }

        private float getCenterY() {
            int h = getHeight();
            int usable = Math.max(0, h - padTop - padBottom);
            return padTop + usable / 2f;
        }

        private double getWorldSize(float zoom) {
            return tilePx * Math.pow(2.0, zoom);
        }

        float[] latLngToScreen(double lat, double lon) {
            double worldSize = getWorldSize(cameraZoom);
            double camWorldX = lonToNormX(cameraLon) * worldSize;
            double camWorldY = latToNormY(cameraLat) * worldSize;
            double ptWorldX = lonToNormX(lon) * worldSize;
            double ptWorldY = latToNormY(lat) * worldSize;
            double dx = ptWorldX - camWorldX;
            if (dx > worldSize * 0.5) {
                dx -= worldSize;
            } else if (dx < -worldSize * 0.5) {
                dx += worldSize;
            }
            return new float[]{(float) (getCenterX() + dx), (float) (getCenterY() + (ptWorldY - camWorldY))};
        }

        void setMapPadding(int left, int top, int right, int bottom) {
            this.padLeft = left;
            this.padTop = top;
            this.padRight = right;
            this.padBottom = bottom;
            invalidate();
        }

        void setDarkStyle(boolean dark) {
            this.darkStyle = dark;
            invalidate();
        }

        @SuppressLint("MissingPermission")
        void setMyLocationEnabled(boolean enabled) {
            this.myLocationEnabled = enabled;
            if (destroyed) {
                return;
            }
            if (enabled) {
                startLocationUpdates();
            } else {
                stopLocationUpdates();
            }
            invalidate();
        }

        @SuppressLint("MissingPermission")
        private void startLocationUpdates() {
            Context ctx = getContext();
            boolean hasFine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!hasFine && !hasCoarse) {
                return;
            }
            try {
                LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
                if (lm != null) {
                    Location best = null;
                    ArrayList<String> providersToCheck = new ArrayList<>();
                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                        providersToCheck.add(LocationManager.FUSED_PROVIDER);
                    }
                    providersToCheck.add(LocationManager.GPS_PROVIDER);
                    providersToCheck.add(LocationManager.NETWORK_PROVIDER);
                    providersToCheck.add(LocationManager.PASSIVE_PROVIDER);
                    for (int i = 0; i < providersToCheck.size(); i++) {
                        try {
                            Location l = lm.getLastKnownLocation(providersToCheck.get(i));
                            if (l != null && (best == null || l.getTime() > best.getTime())) {
                                best = l;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    if (best != null) {
                        updateMyLocation(best);
                    }
                    if (locationListener == null) {
                        locationListener = location -> {
                            if (location != null && !destroyed) {
                                updateMyLocation(location);
                            }
                        };
                        if (android.os.Build.VERSION.SDK_INT >= 31) {
                            try {
                                if (lm.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                                    lm.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 4000L, 5f, locationListener, Looper.getMainLooper());
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                        try {
                            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000L, 5f, locationListener, Looper.getMainLooper());
                            }
                        } catch (Throwable ignored) {
                        }
                        try {
                            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000L, 10f, locationListener, Looper.getMainLooper());
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
            try {
                ILocationServiceProvider locationServiceProvider = ApplicationLoader.getLocationServiceProvider();
                if (locationServiceProvider != null && locationServiceProvider.checkServices()) {
                    locationServiceProvider.getLastLocation(location -> {
                        if (location != null && !destroyed) {
                            mainHandler.post(() -> {
                                if (!destroyed) {
                                    updateMyLocation(location);
                                }
                            });
                        }
                    });
                    if (fusedLocationListener == null) {
                        fusedLocationListener = location -> {
                            if (location != null && !destroyed) {
                                mainHandler.post(() -> {
                                    if (!destroyed) {
                                        updateMyLocation(location);
                                    }
                                });
                            }
                        };
                        ILocationServiceProvider.ILocationRequest request = locationServiceProvider.onCreateLocationRequest();
                        request.setPriority(hasFine ? ILocationServiceProvider.PRIORITY_HIGH_ACCURACY : ILocationServiceProvider.PRIORITY_BALANCED_POWER_ACCURACY);
                        request.setInterval(4000L);
                        request.setFastestInterval(2000L);
                        locationServiceProvider.requestLocationUpdates(request, fusedLocationListener);
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        private void stopLocationUpdates() {
            if (locationListener != null) {
                try {
                    LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                    if (lm != null) {
                        lm.removeUpdates(locationListener);
                    }
                } catch (Throwable ignored) {
                }
                locationListener = null;
            }
            if (fusedLocationListener != null) {
                try {
                    ILocationServiceProvider locationServiceProvider = ApplicationLoader.getLocationServiceProvider();
                    if (locationServiceProvider != null) {
                        locationServiceProvider.removeLocationUpdates(fusedLocationListener);
                    }
                } catch (Throwable ignored) {
                }
                fusedLocationListener = null;
            }
        }

        private void updateMyLocation(Location location) {
            lastKnownLocation = location;
            invalidate();
            if (onMyLocationChangeListener != null) {
                onMyLocationChangeListener.accept(new Location(location));
            }
        }

        private void cancelAnimation() {
            if (cameraAnimator != null) {
                cameraAnimator.cancel();
                cameraAnimator = null;
            }
        }

        private CameraPosition resolveTarget(OsmCameraUpdate update) {
            if (update.bounds != null) {
                int w = getWidth() - padLeft - padRight - update.padding * 2;
                int h = getHeight() - padTop - padBottom - update.padding * 2;
                LatLng center = update.bounds.getCenter();
                if (w <= 0 || h <= 0) {
                    pendingBoundsUpdate = update;
                    return new CameraPosition(center, cameraZoom);
                }
                double spanX = Math.abs(lonToNormX(update.bounds.east) - lonToNormX(update.bounds.west));
                double spanY = Math.abs(latToNormY(update.bounds.south) - latToNormY(update.bounds.north));
                float zoomX = spanX > 1e-7 ? (float) (Math.log(w / (tilePx * spanX)) / Math.log(2.0)) : MAX_ZOOM - 3f;
                float zoomY = spanY > 1e-7 ? (float) (Math.log(h / (tilePx * spanY)) / Math.log(2.0)) : MAX_ZOOM - 3f;
                float targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM - 1f, Math.min(zoomX, zoomY)));
                return new CameraPosition(center, targetZoom);
            }
            double lat = update.target != null ? clampLat(update.target.latitude) : cameraLat;
            double lon = update.target != null ? normalizeLon(update.target.longitude) : cameraLon;
            float zoom = Float.isNaN(update.zoom) ? cameraZoom : Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, update.zoom));
            return new CameraPosition(new LatLng(lat, lon), zoom);
        }

        void jumpTo(OsmCameraUpdate update) {
            cancelAnimation();
            CameraPosition pos = resolveTarget(update);
            cameraLat = pos.target.latitude;
            cameraLon = pos.target.longitude;
            cameraZoom = pos.zoom;
            invalidate();
            if (onCameraMoveListener != null) {
                onCameraMoveListener.run();
            }
            if (onCameraIdleListener != null) {
                onCameraIdleListener.run();
            }
        }

        void animateTo(OsmCameraUpdate update, int duration, ICancelableCallback callback) {
            cancelAnimation();
            final CameraPosition target = resolveTarget(update);
            final double startLat = cameraLat;
            final double startLon = cameraLon;
            final float startZoom = cameraZoom;
            double deltaLon = target.target.longitude - startLon;
            if (deltaLon > 180.0) {
                deltaLon -= 360.0;
            } else if (deltaLon < -180.0) {
                deltaLon += 360.0;
            }
            final double finalDeltaLon = deltaLon;
            final double finalDeltaLat = target.target.latitude - startLat;
            final float finalDeltaZoom = target.zoom - startZoom;

            if (duration <= 0) {
                jumpTo(update);
                if (callback != null) {
                    callback.onFinish();
                }
                return;
            }
            if (onCameraMoveStartedListener != null) {
                onCameraMoveStartedListener.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_API_ANIMATION);
            }
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(duration);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(anim -> {
                float t = (float) anim.getAnimatedValue();
                cameraLat = clampLat(startLat + finalDeltaLat * t);
                cameraLon = normalizeLon(startLon + finalDeltaLon * t);
                cameraZoom = startZoom + finalDeltaZoom * t;
                invalidate();
                if (onCameraMoveListener != null) {
                    onCameraMoveListener.run();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                private boolean cancelled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                    if (callback != null) {
                        callback.onCancel();
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (cameraAnimator == animation) {
                        cameraAnimator = null;
                    }
                    if (!cancelled) {
                        if (onCameraIdleListener != null) {
                            onCameraIdleListener.run();
                        }
                        if (callback != null) {
                            callback.onFinish();
                        }
                    }
                }
            });
            cameraAnimator = animator;
            animator.start();
        }

        private void requestTile(int z, int x, int y, String key) {
            if (destroyed || !inFlightTiles.add(key)) {
                return;
            }
            tileExecutor.execute(() -> {
                if (destroyed || (activeTileZ >= 0 && Math.abs(z - activeTileZ) > 1)) {
                    inFlightTiles.remove(key);
                    return;
                }
                Bitmap decoded = null;
                HttpURLConnection conn = null;
                try {
                    String urlStr = String.format(Locale.US, "https://tile.openstreetmap.org/%d/%d/%d.png", z, x, y);
                    conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(8000);
                    conn.setRequestProperty("User-Agent", "KronoGram-Android/13.1.1 (org.telegram.messenger)");
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        try (InputStream is = conn.getInputStream()) {
                            decoded = BitmapFactory.decodeStream(is);
                        }
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                    inFlightTiles.remove(key);
                }
                if (decoded != null && !destroyed) {
                    tileCache.put(key, decoded);
                    mainHandler.post(this::invalidate);
                }
            });
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }

            canvas.drawColor(darkStyle ? 0xff161b22 : 0xffeef1f4);

            int tileZ = Math.max(0, Math.min(19, Math.round(cameraZoom)));
            activeTileZ = tileZ;
            int maxTileIndex = 1 << tileZ;
            double zoomScale = Math.pow(2.0, cameraZoom - tileZ);
            float drawnTileSize = (float) (tilePx * zoomScale);
            double worldSize = drawnTileSize * maxTileIndex;

            double camWorldX = lonToNormX(cameraLon) * worldSize;
            double camWorldY = latToNormY(cameraLat) * worldSize;
            float originX = (float) (getCenterX() - camWorldX);
            float originY = (float) (getCenterY() - camWorldY);

            int startTileX = (int) Math.floor(-originX / drawnTileSize);
            int endTileX = (int) Math.floor((w - originX) / drawnTileSize);
            int startTileY = Math.max(0, (int) Math.floor(-originY / drawnTileSize));
            int endTileY = Math.min(maxTileIndex - 1, (int) Math.floor((h - originY) / drawnTileSize));

            gridPaint.setColor(darkStyle ? 0xff222b36 : 0xffdce3ea);
            Paint activeTilePaint = darkStyle ? darkTilePaint : tilePaint;

            for (int ty = startTileY; ty <= endTileY; ty++) {
                float top = originY + ty * drawnTileSize;
                float bottom = top + drawnTileSize;
                for (int tx = startTileX; tx <= endTileX; tx++) {
                    int wrappedX = ((tx % maxTileIndex) + maxTileIndex) % maxTileIndex;
                    float left = originX + tx * drawnTileSize;
                    float right = left + drawnTileSize;
                    dstRect.set(left, top, right, bottom);

                    String key = tileZ + "/" + wrappedX + "/" + ty;
                    Bitmap tile = tileCache.get(key);
                    if (tile != null && !tile.isRecycled()) {
                        srcRect.set(0, 0, tile.getWidth(), tile.getHeight());
                        canvas.drawBitmap(tile, srcRect, dstRect, activeTilePaint);
                    } else {
                        canvas.drawRect(dstRect, gridPaint);
                        requestTile(tileZ, wrappedX, ty, key);
                    }
                }
            }

            // Accuracy / proximity circles
            double metersPerPixel = 156543.03392 * Math.cos(Math.toRadians(cameraLat)) / Math.pow(2.0, cameraZoom) * (256.0 / tilePx);
            for (int i = 0; i < circles.size(); i++) {
                OsmCircle circle = circles.get(i);
                float[] centerPx = latLngToScreen(circle.center.latitude, circle.center.longitude);
                float radiusPx = metersPerPixel > 0 ? (float) (circle.radius / metersPerPixel) : 0f;
                if (radiusPx > 1f) {
                    circleFillPaint.setColor(circle.fillColor);
                    canvas.drawCircle(centerPx[0], centerPx[1], radiusPx, circleFillPaint);
                    circleStrokePaint.setColor(circle.strokeColor);
                    circleStrokePaint.setStrokeWidth(Math.max(1f, circle.strokeWidth));
                    if (circle.patternItems != null && !circle.patternItems.isEmpty()) {
                        float[] intervals = new float[circle.patternItems.size()];
                        boolean nonZero = false;
                        for (int j = 0; j < circle.patternItems.size(); j++) {
                            PatternItem item = circle.patternItems.get(j);
                            if (item instanceof PatternItem.Dash) {
                                intervals[j] = Math.max(1f, ((PatternItem.Dash) item).length);
                                nonZero = true;
                            } else if (item instanceof PatternItem.Gap) {
                                intervals[j] = Math.max(1f, ((PatternItem.Gap) item).length);
                                nonZero = true;
                            } else {
                                intervals[j] = 8f;
                            }
                        }
                        if (nonZero && intervals.length >= 2) {
                            circleStrokePaint.setPathEffect(new DashPathEffect(intervals, 0f));
                        } else {
                            circleStrokePaint.setPathEffect(null);
                        }
                    } else {
                        circleStrokePaint.setPathEffect(null);
                    }
                    canvas.drawCircle(centerPx[0], centerPx[1], radiusPx, circleStrokePaint);
                }
            }

            // My location blue dot
            if (myLocationEnabled && lastKnownLocation != null) {
                float[] myPx = latLngToScreen(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                float density = getResources().getDisplayMetrics().density;
                float accPx = metersPerPixel > 0 ? (float) Math.min(Math.max(14f * density, lastKnownLocation.getAccuracy() / metersPerPixel), 96f * density) : 18f * density;
                canvas.drawCircle(myPx[0], myPx[1], accPx, myLocationAccuracyPaint);
                canvas.drawCircle(myPx[0], myPx[1], 8.5f * density, myLocationBorderPaint);
                canvas.drawCircle(myPx[0], myPx[1], 6f * density, myLocationDotPaint);
            }

            // Markers
            float density = getResources().getDisplayMetrics().density;
            for (int i = 0; i < markers.size(); i++) {
                OsmMarker m = markers.get(i);
                float[] xy = latLngToScreen(m.position.latitude, m.position.longitude);
                canvas.save();
                canvas.translate(xy[0], xy[1]);
                if (m.rotation != 0) {
                    canvas.rotate(m.rotation);
                }
                if (m.bitmap != null && !m.bitmap.isRecycled()) {
                    float drawX = -m.bitmap.getWidth() * m.anchorU;
                    float drawY = -m.bitmap.getHeight() * m.anchorV;
                    canvas.drawBitmap(m.bitmap, drawX, drawY, tilePaint);
                } else {
                    canvas.drawCircle(0f, -8f * density, 8f * density, defaultMarkerPaint);
                }
                canvas.restore();
            }

            attributionPaint.setColor(darkStyle ? 0x99ffffff : 0x99222222);
            float attrY = Math.max(attributionPaint.getTextSize(), h - padBottom - 8f * density);
            canvas.drawText("© OpenStreetMap", padLeft + 8f * density, attrY, attributionPaint);

            if (!hasDrawnFirstFrame) {
                hasDrawnFirstFrame = true;
                if (onMapLoadedCallback != null) {
                    mainHandler.post(onMapLoadedCallback);
                }
            }
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (pendingBoundsUpdate != null && getWidth() > 0 && getHeight() > 0) {
                OsmCameraUpdate update = pendingBoundsUpdate;
                pendingBoundsUpdate = null;
                jumpTo(update);
            }
            if (onLayoutListener != null) {
                onLayoutListener.run();
            }
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (dispatchInterceptor != null) {
                return dispatchInterceptor.onInterceptTouchEvent(ev, super::dispatchTouchEvent);
            }
            return super.dispatchTouchEvent(ev);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (interceptInterceptor != null) {
                return interceptInterceptor.onInterceptTouchEvent(ev, super::onInterceptTouchEvent);
            }
            return super.onInterceptTouchEvent(ev);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);

            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    cancelAnimation();
                    activePointerId = event.getPointerId(0);
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    isDragging = false;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!isScaling && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                        int idx = event.findPointerIndex(activePointerId);
                        if (idx >= 0) {
                            float x = event.getX(idx);
                            float y = event.getY(idx);
                            float dx = x - lastTouchX;
                            float dy = y - lastTouchY;
                            if (!isDragging && (dx * dx + dy * dy) > 9f) {
                                isDragging = true;
                                if (onCameraMoveStartedListener != null) {
                                    onCameraMoveStartedListener.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_GESTURE);
                                }
                            }
                            if (isDragging) {
                                double worldSize = getWorldSize(cameraZoom);
                                double camWorldX = lonToNormX(cameraLon) * worldSize - dx;
                                double camWorldY = latToNormY(cameraLat) * worldSize - dy;
                                cameraLon = normXToLon(camWorldX / worldSize);
                                cameraLat = normYToLat(camWorldY / worldSize);
                                lastTouchX = x;
                                lastTouchY = y;
                                invalidate();
                                if (onCameraMoveListener != null) {
                                    onCameraMoveListener.run();
                                }
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    int pointerIndex = event.getActionIndex();
                    int pointerId = event.getPointerId(pointerIndex);
                    if (pointerId == activePointerId) {
                        int newIndex = pointerIndex == 0 ? 1 : 0;
                        if (newIndex < event.getPointerCount()) {
                            activePointerId = event.getPointerId(newIndex);
                            lastTouchX = event.getX(newIndex);
                            lastTouchY = event.getY(newIndex);
                        } else {
                            activePointerId = MotionEvent.INVALID_POINTER_ID;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isDragging && onCameraIdleListener != null) {
                        onCameraIdleListener.run();
                    }
                    isDragging = false;
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    break;
            }
            return true;
        }

        @Override
        public View getView() {
            return this;
        }

        @Override
        public void getMapAsync(Consumer<IMap> callback) {
            if (callback == null || destroyed) {
                return;
            }
            mainHandler.post(() -> {
                if (!destroyed) {
                    callback.accept(mapImpl);
                }
            });
        }

        @Override
        public void onResume() {
            if (myLocationEnabled && !destroyed) {
                startLocationUpdates();
            }
        }

        @Override
        public void onPause() {
            stopLocationUpdates();
        }

        @Override
        public void onCreate(Bundle savedInstance) {
            // May be called from background preload thread; keep thread-safe no-op.
        }

        @Override
        public void onDestroy() {
            destroyed = true;
            mainHandler.removeCallbacksAndMessages(null);
            Runnable cleanup = () -> {
                cancelAnimation();
                stopLocationUpdates();
                markers.clear();
                circles.clear();
            };
            if (Looper.myLooper() == Looper.getMainLooper()) {
                cleanup.run();
            } else {
                mainHandler.post(cleanup);
            }
        }

        @Override
        public void onLowMemory() {
            tileCache.evictAll();
        }

        @Override
        public void setOnDispatchTouchEventInterceptor(ITouchInterceptor touchInterceptor) {
            this.dispatchInterceptor = touchInterceptor;
        }

        @Override
        public void setOnInterceptTouchEventInterceptor(ITouchInterceptor touchInterceptor) {
            this.interceptInterceptor = touchInterceptor;
        }

        @Override
        public void setOnLayoutListener(Runnable callback) {
            this.onLayoutListener = callback;
        }
    }
}
