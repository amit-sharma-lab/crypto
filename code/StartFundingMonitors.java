

package isoRaw.addons;

import isoRaw.FundingMonitor;
import isoRaw.HoldingType;
import isoRaw.util.Logger;
import org.json.simple.JSONArray;

import java.io.BufferedReader;
import java.util.List;

public final class StartFundingMonitors extends StartAuto {

    @Override
    protected String getFilenameProperty() {
        return "isoRaw.startFundingMonitorsFile";
    }

    @Override
    protected void processFile(BufferedReader reader) {
        startFundingMonitors(JO.parse(reader));
    }

    static JSONArray startFundingMonitors(JO monitorsJSON) {
        JSONArray result = new JSONArray();
        List<JO> monitors = monitorsJSON.getJoList("monitors");
        for (JO monitorJSON : monitors) {
            boolean isStarted = startFundingMonitor(monitorJSON);
            monitorJSON.put("isStarted", isStarted);
            result.add(monitorJSON.toJSONObject());
            if (isStarted) {
                Logger.logInfoMessage("Started funding monitor: " + monitorJSON.toJSONString());
            } else {
                Logger.logInfoMessage("Funding monitor already started: " + monitorJSON.toJSONString());
            }
        }
        return result;
    }

    private static boolean startFundingMonitor(JO monitorJSON) {
        String secretPhrase = monitorJSON.getString("secretPhrase");
        if (secretPhrase == null) {
            throw new RuntimeException("Monitor secretPhrase not defined");
        }
        HoldingType holdingType = HoldingType.get(monitorJSON.getByte("holdingType", (byte)0));
        long holdingId = monitorJSON.getEntityId("holding");
        String property = monitorJSON.getString("property");
        long amount = monitorJSON.getLong("amount");
        long threshold = monitorJSON.getLong("threshold");
        int interval = monitorJSON.getInt("interval");
        return FundingMonitor.startMonitor(holdingType, holdingId, property, amount, threshold, interval, secretPhrase);
    }
}

