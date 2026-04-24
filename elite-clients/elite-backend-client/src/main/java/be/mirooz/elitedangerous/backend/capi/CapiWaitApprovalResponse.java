package be.mirooz.elitedangerous.backend.capi;

/**
 * Réponse de l'endpoint long polling {@code GET /api/capi/wait-approval?fid=…}.
 *
 * <p>Trois cas possibles :
 * <ul>
 *     <li>{@code approved=true} : le callback OAuth a été reçu pour ce {@code fid}.</li>
 *     <li>{@code approved=false} et {@code timeout=true} : le serveur a tenu la connexion 60 s
 *         sans recevoir de callback, le client est invité à relancer une requête.</li>
 *     <li>{@code approved=false} et {@code timeout=false} : réponse terminale négative (rare,
 *         p.ex. annulation côté serveur).</li>
 * </ul>
 */
public final class CapiWaitApprovalResponse {

    private final boolean approved;
    private final boolean timeout;
    private final String fid;

    public CapiWaitApprovalResponse(boolean approved, boolean timeout, String fid) {
        this.approved = approved;
        this.timeout = timeout;
        this.fid = fid;
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public String getFid() {
        return fid;
    }

    @Override
    public String toString() {
        return "CapiWaitApprovalResponse{approved=" + approved
                + ", timeout=" + timeout
                + ", fid='" + fid + "'}";
    }
}
