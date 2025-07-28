package handler;

public enum VerbHttp {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS,
    TRACE;

    public static boolean isValid(String verb) {
        try {
            VerbHttp.valueOf(verb.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
