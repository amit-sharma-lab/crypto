package com.jelurida.ardor.client.api;

import isoRaw.addons.JO;
import isoRaw.http.callers.GetPeersCall;

import java.net.URL;

/**
 * Read the list of connected API peers from a remote node
 */
public class GetPeersForMobileApp {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Specify remote node url, for example https://testisoRaw.com/isoRaw");
            System.exit(0);
        }
        URL url = new URL(args[0]);
        getPeers(url);
    }

    private static void getPeers(URL url) {
        JO peers = GetPeersCall.create().active("true").state("CONNECTED").service("API", "CORS").includePeerInfo(true).
            remote(url).trustRemoteCertificate(true).call();
        System.out.println(peers.toJSONString());
    }
}
