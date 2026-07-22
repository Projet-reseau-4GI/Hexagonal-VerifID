package com.projects.domain.model;

/**
 * Plans tarifaires VerifID.
 *
 * <ul>
 *   <li>FREE    : 0 vérifications/jour, aucune taille de fichier autorisée (plan bloqué).</li>
 *   <li>PREMIUM : 100 vérifications/jour, fichiers jusqu'à 10 MB.</li>
 *   <li>MAX     : 1 000 vérifications/jour, fichiers jusqu'à 50 MB.</li>
 * </ul>
 */
public enum Plan {

    FREE(0, 0L),
    PREMIUM(100, 10 * 1024 * 1024L),
    MAX(1000, 50 * 1024 * 1024L);

    private final int dailyLimit;
    private final long maxFileSizeBytes;

    Plan(int dailyLimit, long maxFileSizeBytes) {
        this.dailyLimit = dailyLimit;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /** Nombre maximum de vérifications autorisées par jour. */
    public int getDailyLimit() {
        return dailyLimit;
    }

    /** Taille maximale de fichier acceptée, en octets. */
    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    /**
     * Convertit une chaîne (insensible à la casse) en {@link Plan}.
     * Retourne {@link Plan#FREE} pour toute valeur inconnue ou nulle.
     *
     * <pre>
     *   Plan.fromString("premium")  // → PREMIUM
     *   Plan.fromString("MAX")      // → MAX
     *   Plan.fromString("FREEMIUM") // → FREE  (fallback)
     *   Plan.fromString(null)       // → FREE  (fallback)
     * </pre>
     *
     * @param value la chaîne à convertir
     * @return le {@link Plan} correspondant, ou {@link Plan#FREE} si inconnu
     */
    public static Plan fromString(String value) {
        if (value == null) {
            return FREE;
        }
        for (Plan plan : values()) {
            if (plan.name().equalsIgnoreCase(value)) {
                return plan;
            }
        }
        return FREE;
    }
}
