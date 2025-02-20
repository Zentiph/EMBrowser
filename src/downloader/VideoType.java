package downloader;

/**
 * Supported types for YouTube videos that can be downloaded.
 */
public enum VideoType {
    BOTH("video-and-audio"),
    VIDEO("video"),
    AUDIO("audio");

    private String argName;

    private VideoType(String argName) {
        this.argName = argName;
    }

    /**
     * Get this enum value as a URL arg.
     *
     * @return URL arg
     */
    public String asUrlArg() {
        return argName;
    }

    public static VideoType fromString(String s) {
        switch (s) {
            case "video":
                return VIDEO;
            case "audio":
                return AUDIO;
            case "both":
                return BOTH;

            default:
                throw new IllegalArgumentException(
                    "invalid video type. got: " + s
                    + "; expected 'video' or 'audio'"
                );
        }
    }
}
