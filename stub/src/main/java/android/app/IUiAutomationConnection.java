package android.app;

import android.os.IInterface;
import android.os.Binder;
import android.os.IBinder;

public interface IUiAutomationConnection extends IInterface {
    void connect(IUiAutomationConnection client);

    abstract class Stub extends Binder implements IUiAutomationConnection {
        public static IUiAutomationConnection asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }

        @Override
        public IBinder asBinder() {
            return this;
        }
    }
}
