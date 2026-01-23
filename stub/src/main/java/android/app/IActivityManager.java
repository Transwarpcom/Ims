package android.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IActivityManager extends IInterface {
    void startDelegateShellPermissionIdentity(int uid, String[] permissions) throws RemoteException;
    void stopDelegateShellPermissionIdentity() throws RemoteException;
    void startInstrumentation(android.content.ComponentName className, String profileFile, int flags, android.os.Bundle arguments, android.app.IInstrumentationWatcher watcher, android.app.IUiAutomationConnection connection, int userId, android.os.Bundle abiOverride) throws RemoteException;

    abstract class Stub extends Binder implements IActivityManager {
        public static IActivityManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
