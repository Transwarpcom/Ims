package android.app;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

public interface IActivityManager extends IInterface {
    void startDelegateShellPermissionIdentity(int uid, String[] permissions) throws android.os.RemoteException;
    void stopDelegateShellPermissionIdentity() throws android.os.RemoteException;
    void startInstrumentation(ComponentName className, String profileFile, int flags, Bundle arguments,
            Object watcher, Object connection, int userId, Bundle abiOverride) throws android.os.RemoteException;

    abstract class Stub implements IActivityManager {
        public static IActivityManager asInterface(IBinder obj) { throw new RuntimeException("Stub!"); }
    }
}
