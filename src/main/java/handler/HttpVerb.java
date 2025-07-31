package handler;

import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

public enum HttpVerb {

    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS,
    TRACE;

    private static final Logger logger = Logger.getLogger(HttpVerb.class.getName());
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HttpVerb.class);

    public static boolean isValid(String verb) {
        try {
            HttpVerb.valueOf(verb.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid HTTP verb: " + verb + ". Error: " + e.getMessage());
            return false;
        }
    }
}
