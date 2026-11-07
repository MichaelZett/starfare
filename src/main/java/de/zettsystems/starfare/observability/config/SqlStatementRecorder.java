package de.zettsystems.starfare.observability.config;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Counts the SQL statements of one thread between {@link #begin()} and {@link #end()}.
 * Hibernate creates it from {@code hibernate.session_factory.statement_inspector}. Unlike the
 * global statistics, statements of concurrent requests and background jobs (round check,
 * cleanup) do not count; outside a recording the inspector does nothing.
 */
public final class SqlStatementRecorder implements StatementInspector {

    private static final long serialVersionUID = 1L;
    private static final ThreadLocal<Recording> CURRENT = new ThreadLocal<>();

    @Override
    public String inspect(String sql) {
        Recording recording = CURRENT.get();
        if (recording != null) {
            recording.add(sql);
        }
        return sql;
    }

    static void begin() {
        CURRENT.set(new Recording());
    }

    /** Ends the recording of this thread; an empty one if none was started. */
    static Recording end() {
        Recording recording = CURRENT.get();
        CURRENT.remove();
        return recording == null ? new Recording() : recording;
    }

    /** The statements of one request; confined to its thread. */
    static final class Recording {
        private static final int SHORT_LENGTH = 70;

        private final Map<String, Integer> countBySql = new HashMap<>();
        private int count;

        void add(String sql) {
            count++;
            countBySql.merge(sql, 1, Integer::sum);
        }

        int count() {
            return count;
        }

        /** Statements run at least {@code minimum} times, most frequent first, as „9× from table where …“. */
        List<String> repeated(int minimum, int limit) {
            return countBySql.entrySet().stream()
                    .filter(entry -> entry.getValue() >= minimum)
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(limit)
                    .map(entry -> entry.getValue() + "× " + shorten(entry.getKey()))
                    .toList();
        }

        /** From the {@code from} clause on: the column list says nothing, the table and condition do. */
        static String shorten(String sql) {
            String text = sql.replaceAll("\\s+", " ").trim();
            int from = text.toLowerCase(Locale.ROOT).indexOf(" from ");
            String tail = from < 0 ? text : text.substring(from + 1);
            return tail.length() <= SHORT_LENGTH ? tail : tail.substring(0, SHORT_LENGTH) + "…";
        }
    }
}
