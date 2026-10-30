package io.github.sumyabazar1.weather;

/** No wildcard or certificate-only trust: a replacement package/signer requires another approval. */
final class SharingPolicy {
    static boolean allows(String approvedPackage, String approvedSigner, String callerPackage, String callerSigner) {
        return approvedPackage != null && !approvedPackage.isEmpty() && approvedSigner != null
                && approvedSigner.matches("[0-9a-f]{64}") && approvedPackage.equals(callerPackage)
                && approvedSigner.equals(callerSigner);
    }
    static boolean refreshDue(long now, long updated, long retryAt, boolean busy) {
        return !busy && now >= retryAt && (updated <= 0 || updated > now || now - updated >= Forecast.REFRESH_MS);
    }
    private SharingPolicy() {}
}
