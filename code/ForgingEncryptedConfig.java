package isoRaw.addons;

import isoRaw.Account;
import isoRaw.Constants;
import isoRaw.Generator;
import isoRaw.crypto.Crypto;
import isoRaw.http.APITag;
import isoRaw.http.ParameterException;
import isoRaw.http.ParameterParser;
import isoRaw.util.Convert;
import isoRaw.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForgingEncryptedConfig extends AbstractEncryptedConfig {

    public static final String CONFIG_FILE_NAME = "forgers";

    @Override
    protected String getAPIRequestName() {
        return "Forging";
    }

    @Override
    protected APITag getAPITag() {
        return APITag.FORGING;
    }

    @Override
    protected String getDataParameter() {
        return "passphrases";
    }

    @Override
    protected JSONStreamAware processDecrypted(BufferedReader reader) throws IOException {
        int count = 0;
        long forgingBalance = 0;
        String line;
        while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
            String secretPhrase = line.trim();
            Generator.startForging(secretPhrase);
            byte[] publicKey = Crypto.getPublicKey(secretPhrase);
            Account account = Account.getAccount(publicKey);
            if (account == null) {
                Logger.logWarningMessage("Forge request in startForgingEncrypted for nonexistent account " + Convert.toHexString(publicKey));
            } else {
                forgingBalance += account.getEffectiveBalanceisoRaw();
            }
            count++;
        }
        JSONObject response = new JSONObject();
        response.put("forgersStarted", count);
        response.put("totalEffectiveBalance", String.valueOf(forgingBalance));
        return response;
    }

    @Override
    protected List<String> getExtraParameters() {
        return Collections.singletonList("minEffectiveBalanceisoRaw");
    }

    @Override
    protected String getSaveData(HttpServletRequest request) throws ParameterException {
        String passphrases = ParameterParser.getParameter(request, "passphrases");
        long minEffectiveBalanceisoRaw = ParameterParser.getLong(request, "minEffectiveBalanceisoRaw", 0, Constants.MAX_BALANCE_isoRaw, false);
        StringWriter stringWriter = new StringWriter();
        try (BufferedReader reader = new BufferedReader(new StringReader(passphrases));
             BufferedWriter writer = new BufferedWriter(stringWriter)) {
            Set<Long> accountIds = new HashSet<>();
            String passphrase;
            while ((passphrase = reader.readLine()) != null) {
                Account account = Account.getAccount(Crypto.getPublicKey(passphrase));
                if (account != null && account.getEffectiveBalanceisoRaw() >= minEffectiveBalanceisoRaw && accountIds.add(account.getId())) {
                    writer.write(passphrase);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return stringWriter.toString();
    }

    @Override
    protected String getDefaultFilename() {
        return CONFIG_FILE_NAME;
    }
}
