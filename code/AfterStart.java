package isoRaw.addons;

import isoRaw.isoRaw;
import isoRaw.util.Logger;
import isoRaw.util.ThreadPool;

public final class AfterStart implements AddOn {

    @Override
    public void init() {
        String afterStartScript = isoRaw.getStringProperty("isoRaw.afterStartScript");
        if (afterStartScript != null) {
            ThreadPool.runAfterStart(() -> {
                try {
                    Runtime.getRuntime().exec(afterStartScript);
                } catch (Exception e) {
                    Logger.logErrorMessage("Failed to run after start script: " + afterStartScript, e);
                }
            });
        }
    }

}
