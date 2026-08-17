package com.money.mimi.wallet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Eip712JsonTest {
    private static final String MAIL_TYPED_DATA = "{"
            + "\"types\":{"
            + "\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],"
            + "\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallet\",\"type\":\"address\"}],"
            + "\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person\"},{\"name\":\"contents\",\"type\":\"string\"}]},"
            + "\"primaryType\":\"Mail\","
            + "\"domain\":{\"name\":\"Ether Mail\",\"version\":\"1\",\"chainId\":1,\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\"},"
            + "\"message\":{\"from\":{\"name\":\"Cow\",\"wallet\":\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\"},\"to\":{\"name\":\"Bob\",\"wallet\":\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\"},\"contents\":\"Hello, Bob!\"}}";

    private static final String ARRAY_TYPED_DATA = "{"
            + "\"types\":{\"EIP712Domain\":[],\"Numbers\":[{\"name\":\"values\",\"type\":\"uint256[]\"}]},"
            + "\"primaryType\":\"Numbers\",\"domain\":{},\"message\":{\"values\":[1,2,3]}}";

    @Test
    public void hash_matchesCanonicalMailDigest() throws Exception {
        Eip712Json.TypedDataHashResult result = Eip712Json.hash(new JSONObject(MAIL_TYPED_DATA));

        assertEquals("Mail", result.primaryType);
        assertEquals("0xbe609aee343fb3c4b28e1df9e632fca64fcfaede20f02e86244efddf30957bd2", result.digestHex);
        assertEquals(BigInteger.ONE, result.domainChainId);
        assertEquals("Ether Mail", result.domainName);
        assertEquals("0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC", result.verifyingContract);
    }

    @Test
    public void hash_acceptsStringPayloadAndInfersPrimaryType() throws Exception {
        JSONObject typedData = new JSONObject(MAIL_TYPED_DATA);
        typedData.remove("primaryType");

        Eip712Json.TypedDataHashResult result = Eip712Json.hash(typedData.toString());

        assertEquals("Mail", result.primaryType);
        assertEquals("0xbe609aee343fb3c4b28e1df9e632fca64fcfaede20f02e86244efddf30957bd2", result.digestHex);
    }

    @Test
    public void hash_changesWhenArrayOrderChanges() throws Exception {
        JSONObject original = new JSONObject(ARRAY_TYPED_DATA);
        JSONObject reversed = new JSONObject(ARRAY_TYPED_DATA);
        reversed.getJSONObject("message").put("values", new JSONArray("[3,2,1]"));

        Eip712Json.TypedDataHashResult first = Eip712Json.hash(original);
        Eip712Json.TypedDataHashResult second = Eip712Json.hash(reversed);

        assertNotEquals(first.digestHex, second.digestHex);
    }

    @Test
    public void hash_rejectsMissingRequiredField() throws Exception {
        JSONObject typedData = new JSONObject(MAIL_TYPED_DATA);
        typedData.getJSONObject("message").remove("contents");

        try {
            Eip712Json.hash(typedData);
            fail("Expected missing field error");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Mail.contents"));
        }
    }
}