package com.licenta.v1.configuration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentUtil {

    /**
     * Converts URLs in the input text to HTML anchor tags.
     * @param text the input text containing URLs to be converted
     * @return the text with URLs converted to HTML anchor tags
     */
    public static String convertUrlsToLinks(String text) {
        String urlRegex = "(https?://\\S+)";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            matcher.appendReplacement(result, "<a href=\"" + url + "\" target=\"_blank\">" + url + "</a>");
        }
        matcher.appendTail(result);
        return result.toString();
    }
}