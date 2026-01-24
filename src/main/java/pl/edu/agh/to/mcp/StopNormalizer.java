package pl.edu.agh.to.mcp;

class StopNormalizer {

    private StopNormalizer() {}  // utility class

    public static String normalize(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
