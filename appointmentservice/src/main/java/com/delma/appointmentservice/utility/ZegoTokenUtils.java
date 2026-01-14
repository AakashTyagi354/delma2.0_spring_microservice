package com.delma.appointmentservice.utility;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class ZegoTokenUtils {

    /**
     * Step 1: Generate the internal Token04
     */
    public static String generateToken04(Long appId, String userId, String secret, int effectiveTimeInSeconds, String roomId) throws Exception {
        long expiredTime = (System.currentTimeMillis() / 1000L) + effectiveTimeInSeconds;

        // 1. Create the Payload JSON
        JSONObject payloadJson = new JSONObject();
        payloadJson.put("room_id", roomId);

        // FIX: Use Integers for keys, not Strings "1" and "2"
        JSONObject privilege = new JSONObject();
        privilege.put("1", 1); // Login: 1 (Enable)
        privilege.put("2", 1); // Publish: 1 (Enable)
        payloadJson.put("privilege", privilege);

        String payloadString = payloadJson.toString();

        // 2. Prepare the encryption JSON
        JSONObject json = new JSONObject();
        json.put("app_id", appId);
        json.put("user_id", userId);
        json.put("nonce", new Random().nextInt());
        json.put("ctime", System.currentTimeMillis() / 1000L);
        json.put("expire", expiredTime);
        json.put("payload", payloadString);

        // 3. Prepare IV (16 bytes)
        byte[] iv = new byte[16];
        new Random().nextBytes(iv);

        // 4. Encrypt using AES-256
        byte[] content = encrypt(json.toString().getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8), iv);

        // 5. Build Binary Buffer (28 bytes of headers + content)
        ByteBuffer buffer = ByteBuffer.allocate(content.length + 28);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(expiredTime);        // 8 bytes
        buffer.putShort((short) iv.length);  // 2 bytes
        buffer.put(iv);                    // 16 bytes
        buffer.putShort((short) content.length); // 2 bytes
        buffer.put(content);                // actual encrypted data

        // 6. Encode using standard Java Util Base64 (No line breaks)
        byte[] bytes = buffer.array();
        String base64Content = java.util.Base64.getEncoder().encodeToString(bytes);

        return "04" + base64Content;
    }
    /**
     * Step 2: Wrap Token04 into the KitToken for the Frontend
     */
    public static String makeKitToken(Long appId, String roomId, String token04) {
        JSONObject kitJson = new JSONObject();
        kitJson.put("appID", appId);
        kitJson.put("roomID", roomId);
        kitJson.put("token", token04);

        byte[] bytes = kitJson.toString().getBytes(StandardCharsets.UTF_8);
        return Base64.encodeBase64String(bytes).replaceAll("\\s", "");
    }

    private static byte[] encrypt(byte[] content, byte[] secretKey, byte[] iv) throws Exception {
        if (secretKey.length != 32) {
            throw new IllegalArgumentException("ZEGO Server Secret must be exactly 32 characters.");
        }
        SecretKeySpec key = new SecretKeySpec(secretKey, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        return cipher.doFinal(content);
    }
}