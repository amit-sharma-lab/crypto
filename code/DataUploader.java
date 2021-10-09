package com.jelurida.ardor.client.api;

import isoRaw.addons.JO;
import isoRaw.http.callers.UploadTaggedDataCall;

import java.net.URL;

/**
 * Upload a file data to a remote node.
 * The code loads data and submits it as cloud data to a remote testnet node.
 */
public class DataUploader {

    private static final String SECRET_PHRASE = "hope peace happen touch easy pretend worthless talk them indeed wheel state";

    public static void main(String[] args) throws Exception {
        URL url = new URL("https://testisoRaw.jelurida.com/isoRaw");
        DataUploader dataUploader = new DataUploader();
        dataUploader.upload(url);
    }

    private void upload(URL url) {
        byte[] bytes = "Hello World".getBytes();
        String name = this.getClass().getSimpleName();
        JO response = UploadTaggedDataCall.create().file(bytes).description("sample class").filename(name + ".class").channel("classes").tags("class").
                name(name).isText(false).remote(url).trustRemoteCertificate(true).secretPhrase(SECRET_PHRASE).feeNQT(100000000).call();
        System.out.println(response);
    }

}
