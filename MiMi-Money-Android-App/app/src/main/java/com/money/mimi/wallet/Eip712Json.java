package com.money.mimi.wallet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

public final class Eip712Json {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final BigInteger TWO_255 = BigInteger.ONE.shiftLeft(255);
    private static final BigInteger TWO_256 = BigInteger.ONE.shiftLeft(256);

    private Eip712Json() {
    }

    public static TypedDataHashResult hash(Object payload) throws Exception {
        JSONObject typedData = coerceObject(payload, "Typed data payload must be a JSON object");
        JSONObject typesObject = typedData.optJSONObject("types");
        if (typesObject == null) {
            throw new IllegalArgumentException("Typed data must include a types object");
        }

        Map<String, List<Field>> types = parseTypes(typesObject);
        String primaryType = safeTrim(typedData.optString("primaryType"));
        if (primaryType.isEmpty()) {
            primaryType = inferPrimaryType(types);
        }
        if (primaryType.isEmpty()) {
            throw new IllegalArgumentException("Typed data must include a primaryType");
        }

        JSONObject domain = typedData.optJSONObject("domain");
        if (domain == null) {
            domain = new JSONObject();
        }
        JSONObject message = typedData.optJSONObject("message");
        if (message == null) {
            message = new JSONObject();
        }

        TypeContext context = new TypeContext(types);
        byte[] domainSeparator = context.hashDomain(domain);
        byte[] messageHash = context.hashStruct(primaryType, message);
        byte[] digest = Hash.sha3(concat(new byte[]{0x19, 0x01}, domainSeparator, messageHash));

        return new TypedDataHashResult(
                digest,
                Numeric.toHexString(digest),
                primaryType,
                parseOptionalBigInteger(domain.opt("chainId")),
                safeTrim(domain.optString("name")),
                normalizeOptionalAddress(domain.opt("verifyingContract"))
        );
    }

    private static Map<String, List<Field>> parseTypes(JSONObject typesObject) throws Exception {
        Map<String, List<Field>> types = new LinkedHashMap<>();
        Iterator<String> keys = typesObject.keys();
        while (keys.hasNext()) {
            String typeName = keys.next();
            JSONArray fieldArray = typesObject.optJSONArray(typeName);
            if (fieldArray == null) {
                continue;
            }
            ArrayList<Field> fields = new ArrayList<>();
            for (int i = 0; i < fieldArray.length(); i++) {
                JSONObject field = fieldArray.optJSONObject(i);
                if (field == null) {
                    throw new IllegalArgumentException("Invalid field definition in type " + typeName);
                }
                String name = safeTrim(field.optString("name"));
                String type = safeTrim(field.optString("type"));
                if (name.isEmpty() || type.isEmpty()) {
                    throw new IllegalArgumentException("Invalid field definition in type " + typeName);
                }
                fields.add(new Field(name, type));
            }
            types.put(typeName, fields);
        }
        return types;
    }

    private static String inferPrimaryType(Map<String, List<Field>> types) {
        TreeSet<String> candidates = new TreeSet<>();
        TreeSet<String> referenced = new TreeSet<>();
        for (Map.Entry<String, List<Field>> entry : types.entrySet()) {
            String typeName = entry.getKey();
            if (!"EIP712Domain".equals(typeName)) {
                candidates.add(typeName);
            }
            for (Field field : entry.getValue()) {
                String baseType = getBaseType(field.type);
                if (types.containsKey(baseType) && !baseType.equals(typeName)) {
                    referenced.add(baseType);
                }
            }
        }
        for (String candidate : candidates) {
            if (!referenced.contains(candidate)) {
                return candidate;
            }
        }
        return candidates.isEmpty() ? "" : candidates.first();
    }

    private static JSONObject coerceObject(Object value, String errorMessage) throws Exception {
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }
        if (value instanceof String) {
            Object parsed = new JSONTokener((String) value).nextValue();
            if (parsed instanceof JSONObject) {
                return (JSONObject) parsed;
            }
        }
        throw new IllegalArgumentException(errorMessage);
    }

    private static JSONArray coerceArray(Object value, String errorMessage) throws Exception {
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        }
        if (value instanceof String) {
            Object parsed = new JSONTokener((String) value).nextValue();
            if (parsed instanceof JSONArray) {
                return (JSONArray) parsed;
            }
        }
        throw new IllegalArgumentException(errorMessage);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeOptionalAddress(Object raw) {
        String value = raw == null || raw == JSONObject.NULL ? "" : safeTrim(String.valueOf(raw));
        if (value.isEmpty()) {
            return "";
        }
        String normalized = value.startsWith("0x") || value.startsWith("0X") ? value : "0x" + value;
        return normalized.matches("^(0x)?[0-9a-fA-F]{40}$") ? normalized : value;
    }

    private static String getBaseType(String type) {
        int bracket = type.indexOf('[');
        return bracket >= 0 ? type.substring(0, bracket) : type;
    }

    private static boolean isArrayType(String type) {
        return type != null && type.contains("[");
    }

    private static int parseIntegerBitSize(String type) {
        String suffix = type.startsWith("uint") ? type.substring(4) : type.substring(3);
        int bits = suffix.isEmpty() ? 256 : Integer.parseInt(suffix);
        if (bits < 8 || bits > 256 || (bits % 8) != 0) {
            throw new IllegalArgumentException("Unsupported integer size: " + type);
        }
        return bits;
    }

    private static int parseFixedBytesSize(String type) {
        String suffix = type.substring(5);
        int size = Integer.parseInt(suffix);
        if (size < 1 || size > 32) {
            throw new IllegalArgumentException("Unsupported bytes size: " + type);
        }
        return size;
    }

    private static BigInteger parseBigInteger(Object raw) {
        if (raw == null || raw == JSONObject.NULL) {
            throw new IllegalArgumentException("Missing numeric value");
        }
        if (raw instanceof Number) {
            return new BigDecimal(String.valueOf(raw)).toBigIntegerExact();
        }
        String text = safeTrim(String.valueOf(raw));
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Missing numeric value");
        }
        try {
            return (text.startsWith("0x") || text.startsWith("0X")) ? Numeric.decodeQuantity(text) : new BigInteger(text);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid numeric value: " + text);
        }
    }

    private static BigInteger parseOptionalBigInteger(Object raw) {
        if (raw == null || raw == JSONObject.NULL || safeTrim(String.valueOf(raw)).isEmpty()) {
            return null;
        }
        return parseBigInteger(raw);
    }

    private static boolean parseBoolean(Object raw) {
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue() != 0;
        }
        String text = safeTrim(String.valueOf(raw));
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }

    private static byte[] encodeBoolean(Object raw) {
        return leftPad(parseBoolean(raw) ? BigInteger.ONE : BigInteger.ZERO, 32);
    }

    private static byte[] encodeAddress(Object raw) {
        String value = normalizeOptionalAddress(raw);
        if (value.isEmpty()) {
            return new byte[32];
        }
        return leftPad(new BigInteger(1, Numeric.hexStringToByteArray(value)), 32);
    }

    private static byte[] encodeInteger(Object raw, boolean unsigned, int bits) {
        BigInteger value = parseBigInteger(raw);
        if (unsigned) {
            if (value.signum() < 0 || value.bitLength() > bits) {
                throw new IllegalArgumentException("Unsigned integer out of range");
            }
            return leftPad(value, 32);
        }
        BigInteger min = TWO_255.negate().shiftRight(256 - bits);
        BigInteger max = TWO_255.subtract(BigInteger.ONE).shiftRight(256 - bits);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException("Signed integer out of range");
        }
        return leftPad(value.mod(TWO_256), 32);
    }

    private static byte[] encodeFixedBytes(Object raw, int size) {
        byte[] value = decodeBytes(raw);
        if (value.length > size) {
            throw new IllegalArgumentException("bytes value is larger than " + size);
        }
        return rightPad(value, 32);
    }

    private static byte[] decodeBytes(Object raw) {
        if (raw == null || raw == JSONObject.NULL) {
            return new byte[0];
        }
        if (raw instanceof JSONArray) {
            JSONArray array = (JSONArray) raw;
            byte[] out = new byte[array.length()];
            for (int i = 0; i < array.length(); i++) {
                out[i] = (byte) parseBigInteger(array.opt(i)).intValue();
            }
            return out;
        }
        String text = String.valueOf(raw);
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return Numeric.hexStringToByteArray(text);
        }
        return text.getBytes(UTF_8);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            if (part != null) {
                out.write(part, 0, part.length);
            }
        }
        return out.toByteArray();
    }

    private static byte[] leftPad(BigInteger value, int size) {
        return leftPad(toUnsignedBytes(value), size);
    }

    private static byte[] leftPad(byte[] value, int size) {
        if (value.length == size) {
            return value;
        }
        if (value.length > size) {
            return Arrays.copyOfRange(value, value.length - size, value.length);
        }
        byte[] out = new byte[size];
        System.arraycopy(value, 0, out, size - value.length, value.length);
        return out;
    }

    private static byte[] rightPad(byte[] value, int size) {
        if (value.length == size) {
            return value;
        }
        if (value.length > size) {
            return Arrays.copyOf(value, size);
        }
        byte[] out = new byte[size];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }

    private static byte[] toUnsignedBytes(BigInteger value) {
        byte[] raw = value.toByteArray();
        if (raw.length > 1 && raw[0] == 0) {
            return Arrays.copyOfRange(raw, 1, raw.length);
        }
        return raw;
    }

    public static final class TypedDataHashResult {
        public final byte[] digest;
        public final String digestHex;
        public final String primaryType;
        public final BigInteger domainChainId;
        public final String domainName;
        public final String verifyingContract;

        private TypedDataHashResult(byte[] digest, String digestHex, String primaryType,
                                    BigInteger domainChainId, String domainName, String verifyingContract) {
            this.digest = digest;
            this.digestHex = digestHex;
            this.primaryType = primaryType;
            this.domainChainId = domainChainId;
            this.domainName = domainName;
            this.verifyingContract = verifyingContract;
        }
    }

    private static final class Field {
        private final String name;
        private final String type;

        private Field(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    private static final class ArrayType {
        private final String baseType;
        private final List<Integer> dimensions;

        private ArrayType(String baseType, List<Integer> dimensions) {
            this.baseType = baseType;
            this.dimensions = dimensions;
        }

        private static ArrayType parse(String type) {
            String baseType = getBaseType(type);
            ArrayList<Integer> dimensions = new ArrayList<>();
            int offset = baseType.length();
            while (offset < type.length()) {
                int open = type.indexOf('[', offset);
                int close = type.indexOf(']', open + 1);
                String sizeText = type.substring(open + 1, close).trim();
                dimensions.add(sizeText.isEmpty() ? null : Integer.parseInt(sizeText));
                offset = close + 1;
            }
            return new ArrayType(baseType, dimensions);
        }
    }

    private static final class TypeContext {
        private final Map<String, List<Field>> types;
        private final Map<String, String> encodeTypeCache = new LinkedHashMap<>();
        private final Map<String, byte[]> typeHashCache = new LinkedHashMap<>();

        private TypeContext(Map<String, List<Field>> types) {
            this.types = types;
        }

        private byte[] hashDomain(JSONObject domain) throws Exception {
            if (types.containsKey("EIP712Domain")) {
                return hashStruct("EIP712Domain", domain);
            }
            if (domain.length() == 0) {
                return Hash.sha3(Hash.sha3("EIP712Domain()".getBytes(UTF_8)));
            }
            throw new IllegalArgumentException("Typed data domain is missing EIP712Domain type");
        }

        private byte[] hashStruct(String typeName, JSONObject data) throws Exception {
            if (!types.containsKey(typeName)) {
                throw new IllegalArgumentException("Unknown EIP-712 type: " + typeName);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(typeHash(typeName));
            for (Field field : types.get(typeName)) {
                if (!data.has(field.name) || data.isNull(field.name)) {
                    throw new IllegalArgumentException("Typed data field is missing: " + typeName + "." + field.name);
                }
                out.write(encodeValue(field.type, data.get(field.name)));
            }
            return Hash.sha3(out.toByteArray());
        }

        private byte[] encodeValue(String type, Object value) throws Exception {
            if (isArrayType(type)) {
                return hashArray(ArrayType.parse(type), 0, coerceArray(value, "Expected array for type " + type));
            }

            String baseType = getBaseType(type);
            if (types.containsKey(baseType)) {
                return hashStruct(baseType, coerceObject(value, "Expected object for type " + baseType));
            }
            if ("string".equals(baseType)) {
                return Hash.sha3(String.valueOf(value).getBytes(UTF_8));
            }
            if ("bytes".equals(baseType)) {
                return Hash.sha3(decodeBytes(value));
            }
            if ("bool".equals(baseType)) {
                return encodeBoolean(value);
            }
            if ("address".equals(baseType)) {
                return encodeAddress(value);
            }
            if (baseType.startsWith("uint")) {
                return encodeInteger(value, true, parseIntegerBitSize(baseType));
            }
            if (baseType.startsWith("int")) {
                return encodeInteger(value, false, parseIntegerBitSize(baseType));
            }
            if (baseType.startsWith("bytes") && baseType.length() > 5) {
                return encodeFixedBytes(value, parseFixedBytesSize(baseType));
            }

            throw new IllegalArgumentException("Unsupported EIP-712 field type: " + type);
        }

        private byte[] hashArray(ArrayType arrayType, int depth, JSONArray array) throws Exception {
            Integer expectedSize = arrayType.dimensions.get(depth);
            if (expectedSize != null && expectedSize != array.length()) {
                throw new IllegalArgumentException("Unexpected array length for type " + arrayType.baseType);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            boolean lastDepth = depth == arrayType.dimensions.size() - 1;
            for (int i = 0; i < array.length(); i++) {
                Object item = array.opt(i);
                byte[] encoded = lastDepth
                        ? encodeValue(arrayType.baseType, item)
                        : hashArray(arrayType, depth + 1, coerceArray(item, "Expected nested array for type " + arrayType.baseType));
                out.write(encoded);
            }
            return Hash.sha3(out.toByteArray());
        }

        private byte[] typeHash(String typeName) {
            byte[] cached = typeHashCache.get(typeName);
            if (cached != null) {
                return cached;
            }
            byte[] hash = Hash.sha3(encodeType(typeName).getBytes(UTF_8));
            typeHashCache.put(typeName, hash);
            return hash;
        }

        private String encodeType(String primaryType) {
            String cached = encodeTypeCache.get(primaryType);
            if (cached != null) {
                return cached;
            }
            if (!types.containsKey(primaryType)) {
                throw new IllegalArgumentException("Unknown EIP-712 type: " + primaryType);
            }
            TreeSet<String> dependencies = new TreeSet<>();
            collectDependencies(primaryType, dependencies);
            dependencies.remove(primaryType);

            StringBuilder builder = new StringBuilder();
            appendType(builder, primaryType);
            for (String dependency : dependencies) {
                appendType(builder, dependency);
            }
            String encoded = builder.toString();
            encodeTypeCache.put(primaryType, encoded);
            return encoded;
        }

        private void collectDependencies(String typeName, TreeSet<String> dependencies) {
            if (!types.containsKey(typeName)) {
                return;
            }
            dependencies.add(typeName);
            for (Field field : types.get(typeName)) {
                String baseType = getBaseType(field.type);
                if (types.containsKey(baseType) && !dependencies.contains(baseType)) {
                    collectDependencies(baseType, dependencies);
                }
            }
        }

        private void appendType(StringBuilder builder, String typeName) {
            builder.append(typeName).append('(');
            List<Field> fields = types.get(typeName);
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) builder.append(',');
                builder.append(fields.get(i).type).append(' ').append(fields.get(i).name);
            }
            builder.append(')');
        }
    }
}