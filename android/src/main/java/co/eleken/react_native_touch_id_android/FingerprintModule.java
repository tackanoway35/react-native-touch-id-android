package co.eleken.react_native_touch_id_android;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.github.ajalt.reprint.core.AuthenticationFailureReason;
import com.github.ajalt.reprint.core.AuthenticationListener;
import com.github.ajalt.reprint.core.Reprint;

/**
 * Created by Eleken. on 16.03.17.
 */

public class FingerprintModule extends ReactContextBaseJavaModule {
    
    
    private WritableMap response;
    
    private final ReactApplicationContext mReactContext;
    
    FingerprintModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
    }
    
    @Override
    public String getName() {
        return "Fingerprint";
    }
    
    @ReactMethod
    public void requestTouch(final Promise promise) {
        try {
            response = Arguments.createMap();
            if (!isSensorAvailable()) {
                sendResponse("failed", "Finger sensor is not available", promise);
                return;
            }

            Activity currentActivity = getCurrentActivity();

            if (currentActivity == null) {
                sendResponse("failed", "Can't find current Activity", promise);
                return;
            }

            Reprint.authenticate(new AuthenticationListener() {
                @Override
                public void onSuccess(int moduleTag) {
                    sendResponse("ok", null, promise);
                }
                @Override
                public void onFailure(final AuthenticationFailureReason failureReason, final boolean fatal,
                                      final CharSequence errorMessage, int moduleTag, int errorCode) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        if(failureReason == AuthenticationFailureReason.LOCKED_OUT) {
                            final Thread t = new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        sendResponse("failed", "LOCKED_OUT", promise);
                                    } catch (Exception e) {
                                        Log.d("exceptionLog", errorMessage.toString());
                                    }
                                }
                            });
                            t.start();
                        } else {
                            String errorCodeString = Integer.toString(errorCode);
                            String errorString = errorCodeString + '_' + errorMessage.toString();
                            sendResponse("failed", errorString, promise);
                            Log.d("errorMessage", errorString);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.d("requestTouchException", e.toString());
        }
    }
    
    @ReactMethod
    public void dismiss() {
        Reprint.cancelAuthentication();
    }
    
    
    @ReactMethod
    public void isSensorAvailable(final Promise promise) {
        try {
            response = Arguments.createMap();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(mReactContext, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                    sendResponse("failed", "NOT_PERMISSIONS", promise);
                    return;
                }

                FingerprintManager fpm = (FingerprintManager) mReactContext.getSystemService(Context.FINGERPRINT_SERVICE);
                if (fpm == null) {
                    sendResponse("failed", "NOT_SUPPORT", promise);
                    return;
                }
                boolean isHardwareDetected = fpm.isHardwareDetected();
                boolean hasEnrolledFingerprints = fpm.hasEnrolledFingerprints();

                if (mReactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) || isHardwareDetected) {
                    if (hasEnrolledFingerprints) {
                        sendResponse("ok", null, promise);
                    } else {
                        sendResponse("failed", "NOT_ENROLLED", promise);
                    }
                } else {
                    sendResponse("failed", "NOT_SUPPORT", promise);
                }
            }
        } catch (Exception e) {
            sendResponse("failed", "NOT_SUPPORT", promise);
            Log.d("sensorException", e.toString());
        }

    }
    
    private boolean isSensorAvailable() {
        try {
            if (ActivityCompat.checkSelfPermission(mReactContext, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (mReactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) || ((FingerprintManager) mReactContext.getSystemService(Context.FINGERPRINT_SERVICE)).isHardwareDetected());
        } catch (Exception e) {
            return false;
        }
    }
    
    private void sendResponse(String status, String message, Promise promise) {
        Reprint.cancelAuthentication();
        response = Arguments.createMap();
        response.putString("status", status);
        response.putString("error", message);
        promise.resolve(response);
    }
}
