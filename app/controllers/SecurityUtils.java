package controllers;

import org.apache.commons.codec.binary.Base32;
import play.Logger;

/**
 * Created by lenovo on 7/27/2015.
 */
public class SecurityUtils {
    static Base32 enc = new Base32();
    static Base32 dec = new Base32();

    public static String encodeToString(String str) {
        int length = str.length();
        String toEncode = str + "zzzzz";
        toEncode = toEncode.substring(0, length-(length%5)+5);
        String encoded = enc.encodeToString(toEncode.getBytes());
        length = encoded.length();
        encoded = encoded + "zzzzz";
        encoded = encoded.substring(0, length-(length%5)+5);
        encoded = enc.encodeToString(encoded.getBytes());
        return encoded;
    }

    public static String createSecret(String farmerId, String fileMeta) {
        return (farmerId+fileMeta).toLowerCase().replaceAll("[aiueo]","").substring(0,10);
    }

    public static boolean checkIdentifier(String identifier, String farmerIdEnc, String fileMetaEnc) {
        String secret = encodeToString(createSecret(farmerIdEnc, fileMetaEnc));
        return secret.equals(identifier);
    }
}
