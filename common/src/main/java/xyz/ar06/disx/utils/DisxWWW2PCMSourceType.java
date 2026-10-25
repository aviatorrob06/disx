package xyz.ar06.disx.utils;

public enum DisxWWW2PCMSourceType{
    YOUTUBE("youtube"),
    SPOTIFY("spotify"),
    SOUNDCLOUD("soundcloud"),
    BANDCAMP("bandcamp"),
    DIRECT("direct"),
    INVALID("invalid");

    private final String value;

    DisxWWW2PCMSourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
