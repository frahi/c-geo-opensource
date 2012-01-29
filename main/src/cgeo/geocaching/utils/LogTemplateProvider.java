package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;

import org.apache.commons.lang3.StringUtils;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * provides all the available templates for logging
 *
 */
public class LogTemplateProvider {
    public static abstract class LogTemplate {
        private final String template;
        private final int resourceId;

        protected LogTemplate(String template, int resourceId) {
            this.template = template;
            this.resourceId = resourceId;
        }

        abstract public String getValue(boolean offline);

        public int getResourceId() {
            return resourceId;
        }

        public int getItemId() {
            return template.hashCode();
        }

        public String getTemplateString() {
            return template;
        }

        protected String apply(String input, boolean offline) {
            if (input.contains("[" + template + "]")) {
                return StringUtils.replace(input, "[" + template + "]", getValue(offline));
            }
            return input;
        }
    }

    private static LogTemplate[] templates;

    public static LogTemplate[] getTemplates() {
        if (templates == null) {
            templates = new LogTemplate[] {
                    new LogTemplate("DATE", R.string.init_signature_template_date) {

                        @Override
                        public String getValue(final boolean offline) {
                            return cgBase.formatFullDate(System.currentTimeMillis());
                        }
                    },
                    new LogTemplate("TIME", R.string.init_signature_template_time) {

                        @Override
                        public String getValue(final boolean offline) {
                            return cgBase.formatTime(System.currentTimeMillis());
                        }
                    },
                    new LogTemplate("DATETIME", R.string.init_signature_template_datetime) {

                        @Override
                        public String getValue(final boolean offline) {
                            final long currentTime = System.currentTimeMillis();
                            return cgBase.formatFullDate(currentTime) + " " + cgBase.formatTime(currentTime);
                        }
                    },
                    new LogTemplate("USER", R.string.init_signature_template_user) {

                        @Override
                        public String getValue(final boolean offline) {
                            return Settings.getUsername();
                        }
                    },
                    new LogTemplate("NUMBER", R.string.init_signature_template_number) {

                        @Override
                        public String getValue(final boolean offline) {
                            if (offline) {
                                return "";
                            }
                            String findCount = "";
                            final String page = cgBase.getResponseData(cgBase.request("http://www.geocaching.com/email/", null, false, false, false));
                            int current = parseFindCount(page);

                            if (current >= 0) {
                                findCount = String.valueOf(current + 1);
                            }
                            return findCount;
                        }
                    }
            };
        }
        return templates;
    }

    public static LogTemplate getTemplate(int itemId) {
        for (LogTemplate template : getTemplates()) {
            if (template.getItemId() == itemId) {
                return template;
            }
        }
        return null;
    }

    public static String applyTemplates(String signature, boolean offline) {
        if (signature == null) {
            return "";
        }
        String result = signature;
        for (LogTemplate template : getTemplates()) {
            result = template.apply(result, offline);
        }
        return result;
    }

    private static int parseFindCount(String page) {
        if (StringUtils.isBlank(page)) {
            return -1;
        }

        int findCount = -1;

        try {
            final Pattern findPattern = Pattern.compile("<strong><img.+?icon_smile.+?title=\"Caches Found\" /> ([,\\d]+)", Pattern.CASE_INSENSITIVE);
            final Matcher findMatcher = findPattern.matcher(page);
            if (findMatcher.find()) {
                if (findMatcher.groupCount() > 0) {
                    String count = findMatcher.group(1);

                    if (count != null) {
                        if (count.length() == 0) {
                            findCount = 0;
                        } else {
                            findCount = Integer.parseInt(count.replaceAll("[.,]", ""));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(Settings.tag, "cgBase.parseFindCount: " + e.toString());
        }

        return findCount;
    }

}
