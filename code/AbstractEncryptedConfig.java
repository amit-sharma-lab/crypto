package iso.addons;

import iso.iso;
import iso.crypto.Crypto;
import iso.http.APIServlet;
import iso.http.APITag;
import iso.http.JSONResponses;
import iso.http.ParameterException;
import iso.http.ParameterParser;
import iso.util.Convert;
import iso.util.Logger;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractEncryptedConfig implements AddOn {
    private final Map<String, APIServlet.APIRequestHandler> apiRequests = new HashMap<>();

    @Override
    public void init() {
        List<String> saveParameters = new ArrayList<>();
        saveParameters.add("path");
        saveParameters.add("dataAlreadyEncrypted");
        saveParameters.add("encryptionPassword");
        saveParameters.add(getDataParameter());
        saveParameters.addAll(getExtraParameters());

        List<APITag> apiTagsList = new ArrayList<>();
        apiTagsList.add(APITag.ADDONS);
        if (getAPITag() != null) {
            apiTagsList.add(getAPITag());
        }
        APITag[] apiTags = apiTagsList.toArray(new APITag[0]);

        APIServlet.APIRequestHandler saveAPI = new APIServlet.APIRequestHandler(apiTags, saveParameters.toArray(new String[0])) {
            @Override
            protected JSONStreamAware processRequest(HttpServletRequest request) throws ParameterException {
                boolean dataAlreadyEncrypted = "true".equalsIgnoreCase(request.getParameter("dataAlreadyEncrypted"));
                try {
                    byte[] encrypted;
                    if (dataAlreadyEncrypted) {
                        encrypted = ParameterParser.getBytes(request, getDataParameter(), true);
                    } else {
                        String password = ParameterParser.getParameter(request, "encryptionPassword");
                        byte[] key = Crypto.sha256().digest(Convert.toBytes(password));
                        byte[] data = Convert.toBytes(getSaveData(request));
                        encrypted = Crypto.aesEncrypt(data, key);
                    }

                    Path path = resolvePath(request.getParameter("path"));
                    Logger.logInfoMessage(getAPIRequestName() + " saving to file " + path);
                    if (getDefaultPath().toAbsolutePath().equals(path)) {
                        Files.createDirectories(path.getParent());
                        Files.write(path, encrypted);
                    } else {
                        Files.write(path, encrypted, StandardOpenOption.CREATE_NEW);
                    }
                    JSONObject response = new JSONObject();
                    response.put("filesize", encrypted.length);
                    return response;
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }

            @Override
            protected boolean requirePost() {
                return true;
            }

            @Override
            protected boolean allowRequiredBlockParameters() {
                return false;
            }

            @Override
            protected boolean requireFullClient() {
                return true;
            }

            @Override
            protected boolean requireBlockchain() {
                return false;
            }

            @Override
            protected boolean isPassword(String parameter) {
                return "encryptionPassword".equals(parameter);
            }

            @Override
            protected boolean isTextArea(String parameter) {
                return getDataParameter().equals(parameter);
            }

            @Override
            protected boolean requirePassword() {
                return true;
            }

        };

        APIServlet.APIRequestHandler startAPI = new APIServlet.APIRequestHandler(apiTags, "path", "encryptionPassword") {
            @Override
            protected JSONStreamAware processRequest(HttpServletRequest request) throws ParameterException {
                String password = ParameterParser.getParameter(request, "encryptionPassword");
                byte[] key = Crypto.sha256().digest(Convert.toBytes(password));
                try {
                    Path path = resolvePath(request.getParameter("path"));
                    if (!Files.isReadable(path)) {
                        return JSONResponses.INCORRECT_PROCESS_FILE;
                    }
                    byte[] data = Files.readAllBytes(path);
                    byte[] decrypted = Crypto.aesDecrypt(data, key);
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(decrypted)))) {
                        return processDecrypted(reader);
                    }
                } catch (ParseException | IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                } catch (RuntimeException e) {
                    if (e.getCause() != null && e.getCause() instanceof InvalidCipherTextException) {
                        return JSONResponses.DECRYPTION_FAILED;
                    }
                    throw e;
                }
            }

            @Override
            protected boolean requirePost() {
                return true;
            }

            @Override
            protected boolean allowRequiredBlockParameters() {
                return false;
            }

            @Override
            protected boolean requireFullClient() {
                return true;
            }

            @Override
            protected boolean isPassword(String parameter) {
                return "encryptionPassword".equals(parameter);
            }
        };

        apiRequests.put("save" + getAPIRequestName() + "Encrypted", saveAPI);
        apiRequests.put("start" + getAPIRequestName() + "Encrypted", startAPI);
    }

    @Override
    public Map<String, APIServlet.APIRequestHandler> getAPIRequests() {
        return apiRequests;
    }

    protected abstract String getAPIRequestName();

    protected abstract APITag getAPITag();

    protected abstract String getDataParameter();

    protected abstract JSONStreamAware processDecrypted(BufferedReader reader) throws ParseException, IOException;

    protected List<String> getExtraParameters() {
        return Collections.emptyList();
    }

    protected String getSaveData(HttpServletRequest request) throws ParameterException {
        return ParameterParser.getParameter(request, getDataParameter());
    }

    protected String getDefaultFilename() {
        return getDataParameter();
    }

    protected Path getDefaultPath() {
        Path path = Paths.get(iso.getStringProperty("iso.addons.EncryptedConfig.path", "conf/processes/"), getDefaultFilename());
        if (!path.isAbsolute()) {
            path = Paths.get(iso.getUserHomeDir()).resolve(path).toAbsolutePath();
        }
        return path;
    }

    protected Path resolvePath(String pathString) {
        Path path = Optional.ofNullable(Convert.emptyToNull(pathString)).map(Paths::get).orElseGet(this::getDefaultPath);
        if (!path.isAbsolute()) {
            path = Paths.get(iso.getUserHomeDir()).resolve(path).toAbsolutePath();
        }
        return path;
    }
}
