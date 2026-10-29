package one.mixin.android;

import dalvik.system.DexClassLoader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public final class ReleaseApiJsonProbe {
    private static final Map<String, String> classes = new HashMap<>();
    private static final Map<String, String> fields = new HashMap<>();
    private static ClassLoader loader;

    public static void main(String[] args) throws Exception {
        String owner = null;
        try (BufferedReader mapping = new BufferedReader(new FileReader(args[2]))) {
            String line;
            while ((line = mapping.readLine()) != null) {
                if (line.startsWith("#")) continue;
                if (!line.startsWith(" ") && line.endsWith(":")) {
                    String[] names = line.substring(0, line.length() - 1).split(" -> ");
                    owner = names[0];
                    classes.put(owner, names[1]);
                } else if (owner != null && !line.contains("(") && line.contains(" -> ")) {
                    String[] names = line.trim().split(" -> ");
                    fields.put(owner + "." + names[0].substring(names[0].lastIndexOf(' ') + 1), names[1]);
                }
            }
        }
        loader = new DexClassLoader(args[0], args[1], null, ReleaseApiJsonProbe.class.getClassLoader());
        Class<?> gsonClass = Class.forName("com.google.gson.Gson", true, loader);
        Object defaultGson = gsonClass.getDeclaredConstructor().newInstance();
        Object customGson = field("one.mixin.android.util.GsonHelper", "customGson").get(null);
        Method fromJson = gsonClass.getMethod("fromJson", String.class, Type.class);
        Method toJson = gsonClass.getMethod("toJson", Object.class, Type.class);
        JSONObject report = new JSONObject(new String(Files.readAllBytes(Paths.get(args[3])), "UTF-8"));
        int failures = 0;
        JSONArray metadata = report.getJSONArray("fields");
        for (int i = 0; i < metadata.length(); i++) {
            JSONObject expected = metadata.getJSONObject(i);
            String label = expected.getString("owner") + "." + expected.getString("name");
            try {
                Field actual = field(expected.getString("owner"), expected.getString("name"));
                String expectedType = mappedTypeName(expected.getString("type"));
                if (!actual.getGenericType().getTypeName().equals(expectedType)) {
                    throw new AssertionError("generic type: " + actual.getGenericType() + ", expected " + expectedType);
                }
            } catch (Throwable error) {
                System.out.println("FAIL field " + label + ": " + error);
                failures++;
            }
        }
        JSONArray contracts = report.getJSONArray("contracts");
        for (int i = 0; i < contracts.length(); i++) {
            JSONObject contract = contracts.getJSONObject(i);
            try {
                Type type = type(contract.getJSONObject("type"));
                Object gson = contract.getBoolean("customGson") ? customGson : defaultGson;
                Object decoded = fromJson.invoke(gson, contract.getString("input"), type);
                Object actual = new JSONTokener((String) toJson.invoke(gson, decoded, type)).nextValue();
                if (!sameJson(contract.get("expected"), actual)) {
                    throw new AssertionError("JSON changed: " + actual + ", expected " + contract.get("expected"));
                }
            } catch (Throwable error) {
                System.out.println("FAIL payload " + contract.getString("endpoint") + ": " + error);
                failures++;
            }
        }
        System.out.println("Release API JSON: " + report.getInt("endpoints") + " endpoints, " + report.getInt("models")
            + " models, " + metadata.length() + " fields, " + contracts.length() + " payloads; failures=" + failures);
        System.exit(failures == 0 ? 0 : 1);
    }

    private static Field field(String owner, String name) throws Exception {
        Field field = Class.forName(classes.getOrDefault(owner, owner), true, loader)
            .getDeclaredField(fields.getOrDefault(owner + "." + name, name));
        field.setAccessible(true);
        return field;
    }

    private static String mappedTypeName(String original) {
        Matcher matcher = Pattern.compile("[\\w$]+(?:\\.[\\w$]+)*").matcher(original);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(result, Matcher.quoteReplacement(classes.getOrDefault(matcher.group(), matcher.group())));
        matcher.appendTail(result);
        return result.toString();
    }

    private static Type type(JSONObject description) throws Exception {
        if (description.has("component")) return Array.newInstance((Class<?>) type(description.getJSONObject("component")), 0).getClass();
        String name = description.getString("name");
        Class<?> raw;
        switch (name) {
            case "boolean": raw = boolean.class; break;
            case "byte": raw = byte.class; break;
            case "short": raw = short.class; break;
            case "int": raw = int.class; break;
            case "long": raw = long.class; break;
            case "float": raw = float.class; break;
            case "double": raw = double.class; break;
            case "char": raw = char.class; break;
            case "void": raw = void.class; break;
            default: raw = Class.forName(classes.getOrDefault(name, name), true, loader);
        }
        if (!description.has("arguments")) return raw;
        JSONArray arguments = description.getJSONArray("arguments");
        Type[] parameters = new Type[arguments.length()];
        for (int i = 0; i < parameters.length; i++) parameters[i] = type(arguments.getJSONObject(i));
        final Class<?> rawType = raw;
        return new ParameterizedType() {
            public Type[] getActualTypeArguments() { return parameters.clone(); }
            public Type getRawType() { return rawType; }
            public Type getOwnerType() { return rawType.getDeclaringClass(); }
        };
    }

    private static boolean sameJson(Object expected, Object actual) throws Exception {
        if (expected instanceof JSONObject && actual instanceof JSONObject) {
            JSONObject left = (JSONObject) expected, right = (JSONObject) actual;
            if (left.length() != right.length()) return false;
            Iterator<String> keys = left.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!right.has(key) || !sameJson(left.get(key), right.get(key))) return false;
            }
            return true;
        }
        if (expected instanceof JSONArray && actual instanceof JSONArray) {
            JSONArray left = (JSONArray) expected, right = (JSONArray) actual;
            if (left.length() != right.length()) return false;
            for (int i = 0; i < left.length(); i++) if (!sameJson(left.get(i), right.get(i))) return false;
            return true;
        }
        if (expected instanceof Number && actual instanceof Number) {
            return new java.math.BigDecimal(expected.toString()).compareTo(new java.math.BigDecimal(actual.toString())) == 0;
        }
        return expected.equals(actual);
    }
}
