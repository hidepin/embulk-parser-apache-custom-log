package org.embulk.parser.apache.log;

import com.google.common.collect.Lists;
import org.embulk.spi.time.TimestampParser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LogFormats implements Patterns {

    TimestampParser.Task task;

    public LogFormats(TimestampParser.Task task) {
        this.task = task;
    }

    public Map<String, LogElementFactory<? extends LogElement>> getLogElementMappings(){

        Map<String, LogElementFactory<? extends LogElement>> mapping = new HashMap<>();

        mapping.put("a", new StringLogElementFactory("remote-ip",     IP_ADDRESS));
        mapping.put("A", new StringLogElementFactory("local-ip",      IP_ADDRESS));
        mapping.put("b", new LongLogElementFactory("response-bytes"));
        mapping.put("B", new LongLogElementFactory("response-bytes"));
        mapping.put("C", new StringLogElementFactory("request-cookie"));
        mapping.put("D", new LongLogElementFactory("request-process-time-us"));
        mapping.put("e", new StringLogElementFactory("env"));
        mapping.put("f", new StringLogElementFactory("file-name"));
        mapping.put("h", new StringLogElementFactory("remote-host"));
        mapping.put("H", new StringLogElementFactory("request-protocol", NON_SPACE));
        mapping.put("i", new StringLogElementFactory("request-header"));
        mapping.put("l", new StringLogElementFactory("remote-log-name", NON_SPACE));
        mapping.put("m", new StringLogElementFactory("request-method", METHOD));

        mapping.put("n", new StringLogElementFactory("module-note"));
        mapping.put("o", new StringLogElementFactory("response-header"));

        mapping.put("p", new LongLogElementFactory("request-port"));

        mapping.put("P", new LongLogElementFactory("request-process"));

        mapping.put("q", new StringLogElementFactory("request-query", QUERY));

        mapping.put("r", new StringLogElementFactory("request-line"));
        mapping.put("s", new LongLogElementFactory("response-status", STATUS));

        mapping.put("t", new TimestampLogElementFactory(task, "request-time"));

        mapping.put("T", new LongLogElementFactory("request-process-time-s"));

        mapping.put("u", new StringLogElementFactory("request-user"));
        mapping.put("U", new StringLogElementFactory("request-path", PATH));
        mapping.put("v", new StringLogElementFactory("request-server-name", NON_SPACE));
        mapping.put("V", new StringLogElementFactory("canonical-server-name", NON_SPACE));
        mapping.put("X", new StringLogElementFactory("connection-status", CONN_STATUS));
        mapping.put("I", new LongLogElementFactory("request-total-bytes"));
        mapping.put("O", new LongLogElementFactory("response-total-bytes"));

        mapping.put("%", new StringLogElementFactory("%", "(¥¥%)"));

        return mapping;
    }

    /**
     * RegExp pattern of extract log format key
     *
     * this pattern has 9 groups, which are described as below.
     *
     * (%((!)?(\d{3}(,\d{3})*))?(<|>)?(\{([^\}]+)\})?([A-z]))
     * | ||   |     |           |     |  |           |- group(9) key
     * | ||   |     |           |     |  |------------- group(8) optional parameter
     * | ||   |     |           |     |---------------- group(7) optional parameter wrapper group
     * | ||   |     |           |---------------------- group(6) logging timing parameter
     * | ||   |     |---------------------------------- group(5) additional http status(es)
     * | ||   |---------------------------------------- group(4) http status(es)
     * | ||-------------------------------------------- group(3) inverse http status specifier
     * | |--------------------------------------------- group(2) http status specifier
     * |----------------------------------------------- group(0), group(1)
     *
     */
    public static final Pattern logFormatExtractor =
            Pattern.compile("(%((!)?(\\d{3}(,\\d{3})*))?(<|>)?(\\{([^\\}]+)\\})?([A-z]))",
                    Pattern.DOTALL);

    /**
     * Convert logFormat String to Regexp String
     * @param logFormat apache custom log format
     * @return The pattern that matches CustomLog Configuration.
     *
     */
    public String logFormat2RegexpString(String logFormat){
        List<Replacement> replacements = getReplacements(logFormat);
        return replace(logFormat, replacements);
    }

    private String replace(String logFormat, List<Replacement> replacements) {
        int offset = 0;

        for (Replacement replacement : replacements) {
            String left  = logFormat.substring(0, offset + replacement.getStart());
            String right = logFormat.substring(offset + replacement.getEnd(), logFormat.length());
            int originalLength = logFormat.length() - left.length() - right.length();

            String regexp = replacement.getLogElement().getRegexp();
            logFormat = left + regexp + right;
            offset += regexp.length() - originalLength;
        }
        return logFormat;
    }

    public List<Replacement> getReplacements(String logFormat) {
        Matcher matcher = logFormatExtractor.matcher(logFormat);

        List<Replacement> replacements = Lists.newArrayList();

        while(matcher.find()){
            if(matcher.groupCount() != 9){
                throw new IllegalArgumentException("invalid regexp pattern");
            }
            String all = empty(matcher.group(1));

            //TODO implement
            //String ignoreStatus = empty(matcher.group(3));
            //Object[] statuses = Arrays.stream(empty(matcher.group(4)).split(",")).toArray();
            //String position = empty(matcher.group(6));

            String parameter = matcher.group(8);
            String key = empty(matcher.group(9));

            LogElementFactory<? extends LogElement> factory = getLogElementMappings().get(key);

            if(factory != null){
                int start = matcher.start();
                int end   = matcher.end();
                LogElement logElement = factory.create(parameter);
                replacements.add(new Replacement(start, end, logElement));
            }else{
                throw new IllegalStateException("unknown log format key " + all);
            }

        }
        return replacements;
    }

    private String empty(String s){
        return s == null ? "" : s;
    }


}
