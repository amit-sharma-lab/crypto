
package isoRaw.addons;

import isoRaw.util.Convert;
import isoRaw.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.Reader;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JO extends AbstractMap {

    private final JSONObject jo;

    public JO() {
        this.jo = new JSONObject();
    }

    public JO(JSONObject jo) {
        if (jo == null) {
            throw new IllegalArgumentException("Attempt to initialize JO with null JSONObject");
        }
        this.jo = jo;
    }

    public JO(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Attempt to initialize JO with null Object");
        }
        if (obj instanceof JSONObject) {
            this.jo = (JSONObject)obj;
        } else {
            this.jo = ((JO)obj).toJSONObject();
        }
    }

    public JSONObject toJSONObject() {
        return jo;
    }

    public void put(String key, Object o) {
        jo.put(key, o);
    }

    public JA getArray(String key) {
        Object o = jo.get(key);
        if (o == null) {
            return new JA(new JSONArray()); // no need to deal with null checks
        }
        if (o instanceof JA) {
            return (JA)o;
        }
        return new JA((JSONArray) o);
    }

    public List<JO> getJoList(String key) {
        Object o = jo.get(key);
        if (o == null) {
            return Collections.EMPTY_LIST; // no need to deal with null checks
        } else if (o instanceof JSONArray) {
            return (List<JO>)(new JA((JSONArray) o));
        } else if(o instanceof JA) {
            return (List<JO>)(o);
        } else {
            throw new IllegalArgumentException(key);
        }
    }

    public static JO valueOf(Object o) {
        return new JO((JSONObject)o);
    }

    public static JO parse(String s) {
        try {
            return new JO(JSONValue.parseWithException(s));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static JO parse(Reader r) {
        try {
            return new JO(JSONValue.parseWithException(r));
        } catch (ParseException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Object get(String key) {
        return jo.get(key);
    }

    public JO getJo(String key) {
        Object o = jo.get(key);
        if (o instanceof JSONObject) {
            return new JO(o);
        }
        return (JO)o;
    }

    public long getEntityId(String key) {
        Object value = jo.get(key);
        if (value == null) {
            return 0;
        }
        return Long.parseUnsignedLong((String) value);
    }

    // Used by JSON encodeObject
    @Override
    public Set<Entry> entrySet() {
        return jo.entrySet();
    }

    public String toJSONString() {
        return JSON.toJSONString(jo);
    }

    public long getLong(String key, long defaultValue) {
        if (isExist(key)) {
            return getLong(key);
        }
        return defaultValue;
    }

    public long getLong(String key) {
        Object value = jo.get(key);
        if (value instanceof String) {
            return Long.parseLong((String)value);
        }
        return (long)value;
    }

    public int getInt(String key, int defaultValue) {
        if (isExist(key)) {
            return getInt(key);
        }
        return defaultValue;
    }

    public int getInt(String key) {
        Object value = jo.get(key);
        if (value instanceof Integer) {
            return (int)value;
        }
        return (int)getLong(key);
    }

    public double getDouble(String key, double defaultValue) {
        if (isExist(key)) {
            return getDouble(key);
        }
        return defaultValue;
    }

    public double getDouble(String key) {
        Object value = jo.get(key);
        if (value instanceof String) {
            return Double.parseDouble((String)value);
        }
        return (double)value;
    }

    public float getFloat(String key, float defaultValue) {
        if (isExist(key)) {
            return getFloat(key);
        }
        return defaultValue;
    }

    public float getFloat(String key) {
        Object value = jo.get(key);
        if (value instanceof Float) {
            return (float)value;
        }
        return (float)getDouble(key);
    }

    public short getShort(String key, short defaultValue) {
        if (isExist(key)) {
            return getShort(key);
        }
        return defaultValue;
    }

    public short getShort(String key) {
        Object value = jo.get(key);
        if (value instanceof Short) {
            return (short)value;
        }
        return (short)getLong(key);
    }

    public byte getByte(String key, byte defaultValue) {
        if (isExist(key)) {
            return getByte(key);
        }
        return defaultValue;
    }

    public byte getByte(String key) {
        Object value = jo.get(key);
        if (value instanceof Byte) {
            return (byte)value;
        }
        return (byte)getLong(key);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (isExist(key)) {
            return getBoolean(key);
        }
        return defaultValue;
    }

    public boolean getBoolean(String key) {
        Object o = jo.get(key);
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean) {
            return (boolean)o;
        }
        return Boolean.valueOf((String)o);
    }

    public String getString(String key, String defaultValue) {
        if (isExist(key)) {
            return getString(key);
        }
        return defaultValue;
    }

    public String getString(String key) {
        Object o = jo.get(key);
        if (o == null) {
            return null;
        }
        if (!(o instanceof String)) {
            return o.toString();
        }
        return (String)o;
    }

    public byte[] parseHexString(String key) {
        Object o = jo.get(key);
        if (o == null) {
            return null;
        }
        return Convert.parseHexString((String)o);
    }

    public boolean isExist(String key) {
        return jo.get(key) != null;
    }

}
