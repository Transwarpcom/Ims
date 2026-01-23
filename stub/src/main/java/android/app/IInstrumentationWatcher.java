package android.app;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IInterface;

public interface IInstrumentationWatcher extends IInterface {
    void instrumentationStatus(ComponentName name, int resultCode, Bundle results);
    void instrumentationFinished(ComponentName name, int resultCode, Bundle results);
}
