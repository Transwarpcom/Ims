package io.github.vvb2060.ims;

import static io.github.vvb2060.ims.PrivilegedProcess.TAG;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.system.Os;
import android.util.Log;

import org.lsposed.hiddenapibypass.LSPass;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;

public class ShizukuProvider extends rikka.shizuku.ShizukuProvider {
    static {
        LSPass.setHiddenApiExemptions("");
    }

    private boolean skip = false;

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        int sdkUid = -1;
        try {
            sdkUid = (int) Process.class.getMethod("toSdkSandboxUid", int.class).invoke(null, Os.getuid());
        } catch (Exception e) {
            // ignore
        }
        var callingUid = Binder.getCallingUid();
        if (callingUid != sdkUid && callingUid != Process.SHELL_UID) {
            return new Bundle();
        }

        if (METHOD_SEND_BINDER.equals(method)) {
            Shizuku.addBinderReceivedListener(() -> {
                if (!skip && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    startInstrument(getContext());
                }
            });
        } else if (METHOD_GET_BINDER.equals(method) && callingUid == sdkUid && extras != null) {
            skip = true;
            final int finalSdkUid = sdkUid;
            Shizuku.addBinderReceivedListener(() -> {
                var binder = extras.getBinder("binder");
                if (binder != null && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    startShellPermissionDelegate(binder, finalSdkUid);
                }
            });
        }
        return super.call(method, arg, extras);
    }

    private static void startShellPermissionDelegate(IBinder binder, int sdkUid) {
        try {
            var sm = Class.forName("android.os.ServiceManager");
            var activity = (IBinder) sm.getMethod("getService", String.class).invoke(null, Context.ACTIVITY_SERVICE);
            var amClass = Class.forName("android.app.IActivityManager");
            var amStub = Class.forName("android.app.IActivityManager$Stub");
            var am = amStub.getMethod("asInterface", IBinder.class).invoke(null, new ShizukuBinderWrapper(activity));

            amClass.getMethod("startDelegateShellPermissionIdentity", int.class, String[].class).invoke(am, sdkUid, null);
            var data = Parcel.obtain();
            binder.transact(1, data, null, 0);
            data.recycle();
            amClass.getMethod("stopDelegateShellPermissionIdentity").invoke(am);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private static void startInstrument(Context context) {
        try {
            var sm = Class.forName("android.os.ServiceManager");
            var binder = (IBinder) sm.getMethod("getService", String.class).invoke(null, Context.ACTIVITY_SERVICE);
            var amClass = Class.forName("android.app.IActivityManager");
            var amStub = Class.forName("android.app.IActivityManager$Stub");
            var am = amStub.getMethod("asInterface", IBinder.class).invoke(null, new ShizukuBinderWrapper(binder));

            var name = new ComponentName(context, PrivilegedProcess.class);
            var flags = 1; // ActivityManager.INSTR_FLAG_DISABLE_HIDDEN_API_CHECKS
            flags |= 4; // ActivityManager.INSTR_FLAG_INSTRUMENT_SDK_SANDBOX

            var connectionClass = Class.forName("android.app.UiAutomationConnection");
            var connection = connectionClass.getConstructor().newInstance();

            var watcherClass = Class.forName("android.app.IInstrumentationWatcher");
            var uiConnectionClass = Class.forName("android.app.IUiAutomationConnection");

            Method method = amClass.getMethod("startInstrumentation",
                ComponentName.class, String.class, int.class, Bundle.class, watcherClass, uiConnectionClass, int.class, Bundle.class);
            method.invoke(am, name, null, flags, new Bundle(), null, connection, 0, null);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
