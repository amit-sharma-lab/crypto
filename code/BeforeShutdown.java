

package isoRaw.addons;

import isoRaw.isoRaw;
import isoRaw.util.Logger;

public final class BeforeShutdown implements AddOn {

    private final String beforeShutdownScript = isoRaw.getStringProperty("isoRaw.beforeShutdownScript");

    @Override
    public void shutdown() {
        if (beforeShutdownScript != null) {
            try {
                Runtime.getRuntime().exec(beforeShutdownScript);
            } catch (Exception e) {
                Logger.logShutdownMessage("Failed to run after start script: " + beforeShutdownScript, e);
            }
        }
    }

}
