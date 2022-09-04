package io.github.zhangliangbo.savetime.inner;

import com.google.common.base.Stopwatch;
import com.opencsv.CSVReader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author zhangliangbo
 * @since 2022/8/29
 */
public class IO {
    /**
     * 逐行读取数据
     *
     * @param url      数据源
     * @param consumer 消费者
     * @param skip     跳过行数
     * @return 【处理的行数，时间ms】
     * @throws IOException 异常
     */
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

    /**
     * 分批读取数据
     *
     * @param url      数据源
     * @param consumer 消费者
     * @param skip     跳过行数
     * @param batch    每批大小
     * @return 【处理的批次数量，时间ms】
     * @throws IOException 异常
     */
    public Pair<Long, Long> csvByBatch(URL url, BiConsumer<Long, List<String[]>> consumer, int skip, int batch) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        CSVReader reader = new CSVReader(new InputStreamReader(url.openStream()));
        reader.skip(skip);

        Iterator<String[]> iterator = reader.iterator();
        long qty = 0;
        long line = 0;
        List<String[]> buffer = new LinkedList<>();
        while (iterator.hasNext()) {
            String[] next = iterator.next();
            buffer.add(next);
            if (buffer.size() >= batch) {
                consumer.accept(qty, buffer);
                buffer = new LinkedList<>();
                qty++;
            }
            line++;
        }
        if (CollectionUtils.isNotEmpty(buffer)) {
            consumer.accept(qty, buffer);
        }
        reader.close();

        stopwatch.stop();
        return Pair.of(line, stopwatch.elapsed(java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    public String fileString(URL url) throws IOException {
        return fileString(new File(url.getFile()));
    }

    public String fileString(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

}
