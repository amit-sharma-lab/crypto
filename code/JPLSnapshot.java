package isoRaw.addons;

import isoRaw.Account;
import isoRaw.Block;
import isoRaw.BlockchainProcessor;
import isoRaw.Db;
import isoRaw.FxtDistribution;
import isoRaw.isoRaw;
import isoRaw.isoRawException;
import isoRaw.http.APIServlet;
import isoRaw.http.APITag;
import isoRaw.http.JSONResponses;
import isoRaw.http.ParameterParser;
import isoRaw.util.Convert;
import isoRaw.util.JSON;
import isoRaw.util.Listener;
import isoRaw.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public final class JPLSnapshot implements AddOn {

    public APIServlet.APIRequestHandler getAPIRequestHandler() {
        return new JPLSnapshotAPI("newGenesisAccounts", new APITag[] {APITag.ADDONS}, "height");
    }

    public String getAPIRequestType() {
        return "downloadJPLSnapshot";
    }


    
    public static class JPLSnapshotAPI extends APIServlet.APIRequestHandler {

        private JPLSnapshotAPI(String fileParameter, APITag[] apiTags, String... origParameters) {
            super(fileParameter, apiTags, origParameters);
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws isoRawException {
            int height = ParameterParser.getHeight(request);
            if (height <= 0 || height > isoRaw.getBlockchain().getHeight()) {
                return JSONResponses.INCORRECT_HEIGHT;
            }
            JSONObject inputJSON = new JSONObject();
            try {
                Part part = request.getPart("newGenesisAccounts");
                if (part != null) {
                    ParameterParser.FileData fileData = new ParameterParser.FileData(part).invoke();
                    String input = Convert.toString(fileData.getData());
                    if (!input.trim().isEmpty()) {
                        inputJSON = (JSONObject) JSONValue.parseWithException(input);
                    }
                }
            } catch (IOException | ServletException | ParseException e) {
                return JSONResponses.INCORRECT_FILE;
            }
            JPLSnapshotListener listener = new JPLSnapshotListener(height, inputJSON);
            isoRaw.getBlockchainProcessor().addListener(listener, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
            isoRaw.getBlockchainProcessor().scan(height - 1, false);
            isoRaw.getBlockchainProcessor().removeListener(listener, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);
            StringBuilder sb = new StringBuilder(1024);
            JSON.encodeObject(listener.getSnapshot(), sb);
            response.setHeader("Content-Disposition", "attachment; filename=genesisAccounts.json");
            response.setContentLength(sb.length());
            response.setCharacterEncoding("UTF-8");
            try (PrintWriter writer = response.getWriter()) {
                writer.write(sb.toString());
            } catch (IOException e) {
                return JSONResponses.RESPONSE_WRITE_ERROR;
            }
            return null;
        }

        @Override
        protected JSONStreamAware processRequest(HttpServletRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean requirePost() {
            return true;
        }

        @Override
        protected boolean requirePassword() {
            return true;
        }

        @Override
        protected boolean requireFullClient() {
            return true;
        }

        @Override
        protected boolean allowRequiredBlockParameters() {
            return false;
        }

    }

    private static class JPLSnapshotListener implements Listener<Block> {

        private final int height;
        private final JSONObject inputJSON;
        private final SortedMap<String, Object> snapshot = new TreeMap<>();

        private JPLSnapshotListener(int height, JSONObject inputJSON) {
            this.height = height;
            this.inputJSON = inputJSON;
        }

        @Override
        public void notify(Block block) {
            if (block.getHeight() == height) {
                SortedMap<String, String> snapshotPublicKeys = snapshotPublicKeys();
                JSONArray inputPublicKeys = (JSONArray)inputJSON.get("publicKeys");
                if (inputPublicKeys != null) {
                    Logger.logInfoMessage("Loading " + inputPublicKeys.size() + " input public keys");
                    inputPublicKeys.forEach(publicKey -> {
                        String account = Long.toUnsignedString(Account.getId(Convert.parseHexString((String)publicKey)));
                        String snapshotPublicKey = snapshotPublicKeys.putIfAbsent(account, (String)publicKey);
                        if (snapshotPublicKey != null && !snapshotPublicKey.equals(publicKey)) {
                            throw new RuntimeException("Public key collision, input " + publicKey + ", snapshot contains " + snapshotPublicKey);
                        }
                    });
                }
                JSONArray publicKeys = new JSONArray();
                publicKeys.addAll(snapshotPublicKeys.values());
                snapshot.put("publicKeys", publicKeys);
                SortedMap<String, Long> snapshotisoRawBalances = snapshotisoRawBalances();
                BigInteger snapshotTotal = BigInteger.valueOf(snapshotisoRawBalances.values().stream().mapToLong(Long::longValue).sum());
                JSONObject inputBalances = (JSONObject)inputJSON.get("balances");
                if (inputBalances != null) {
                    Logger.logInfoMessage("Loading " + inputBalances.size() + " input account balances");
                    BigInteger inputTotal = BigInteger.valueOf(inputBalances.values().stream().mapToLong(value -> (Long) value).sum());
                    if (!inputTotal.equals(BigInteger.ZERO)) {
                        snapshotisoRawBalances.entrySet().forEach(entry -> {
                            long snapshotBalance = entry.getValue();
                            long adjustedBalance = Convert.longValueExact(
                                    BigInteger.valueOf(snapshotBalance).multiply(inputTotal)
                                    .divide(snapshotTotal).divide(BigInteger.valueOf(9)));
                            entry.setValue(adjustedBalance);
                        });
                    }
                    inputBalances.entrySet().forEach(entry -> {
                        long accountId = Convert.parseAccountId((String)((Map.Entry)entry).getKey());
                        String account = Long.toUnsignedString(accountId);
                        long inputBalance = (Long)((Map.Entry)entry).getValue();
                        snapshotisoRawBalances.merge(account, inputBalance, (a, b) -> a + b);
                    });
                }
                snapshot.put("balances", snapshotisoRawBalances);
            }
        }

        private SortedMap<String, Object> getSnapshot() {
            return snapshot;
        }

        private SortedMap<String, String> snapshotPublicKeys() {
            SortedMap<String, String> map = new TreeMap<>();
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT public_key FROM public_key WHERE public_key IS NOT NULL "
                         + "AND height <= ? ORDER by account_id")) {
                pstmt.setInt(1, height);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        byte[] publicKey = rs.getBytes("public_key");
                        long accountId = Account.getId(publicKey);
                        map.put(Long.toUnsignedString(accountId), Convert.toHexString(publicKey));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return map;
        }

        private SortedMap<String, Long> snapshotisoRawBalances() {
            SortedMap<String, Long> map = new TreeMap<>();
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT id, balance FROM account WHERE LATEST=true")) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        long accountId = rs.getLong("id");
                        if (accountId == FxtDistribution.FXT_ISSUER_ID) {
                            Logger.logInfoMessage("Skip FXT issuer balance of " + rs.getLong("balance"));
                            continue;
                        }
                        long balance = rs.getLong("balance");
                        if (balance <= 0) {
                            if (balance < 0) {
                                Logger.logInfoMessage("Skip negative balance of " + balance);
                            }
                            continue;
                        }
                        String account = Long.toUnsignedString(accountId);
                        map.put(account, balance);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return map;
        }
    }
}
