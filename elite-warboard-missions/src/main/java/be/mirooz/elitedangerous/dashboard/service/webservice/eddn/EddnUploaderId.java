package be.mirooz.elitedangerous.dashboard.service.webservice.eddn;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Génère un identifiant uploader anonyme stable pour EDDN.
 *
 * <p>EDDN autorise n'importe quelle chaîne non-vide ; la convention retenue par la plupart des outils
 * (EDMC, EDDiscovery, ...) est un hash du FID du commandant. On retient SHA-256 (hex) ici : stable par
 * commandant, impossible de revenir au FID par simple regard.
 */
public final class EddnUploaderId {

    private EddnUploaderId() {}

    public static String fromFid(String fid) {
        if (fid == null || fid.isBlank()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(fid.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
