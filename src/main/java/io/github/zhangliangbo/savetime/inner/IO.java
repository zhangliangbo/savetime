package io.github.zhangliangbo.savetime.inner;

import com.google.common.base.Stopwatch;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.function.BiConsumer;

/**
 * @author zhangliangbo
 * @since 2022/8/29
 */
public class IO {
    //逐行读csv
    public Pair<Long, Long> csvByLine(URL url, BiConsumer<Long, String[]> consumer, int skip) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        CSVReader reader = new CSVReader(new InputStreamReader(url.openStream()));
        reader.skip(skip);

        Iterator<String[]> iterator = reader.iterator();
        long line = 0;
        while (iterator.hasNext()) {
            String[] next = iterator.next();
            consumer.accept(line, next);
            line++;
        }
        reader.close();

        stopwatch.stop();
        return Pair.of(line, stopwatch.elapsed(java.util.concurrent.TimeUnit.MILLISECONDS));
    }
}
