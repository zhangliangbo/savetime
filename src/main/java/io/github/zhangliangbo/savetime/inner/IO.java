package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Stopwatch;
import com.opencsv.CSVReader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * @author zhangliangbo
 * @since 2022/8/29
 */
public class IO {
    /**
     * json
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        CSVReader reader = new CSVReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
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
     * 逐行读取数据
     *
     * @param file     文件
     * @param consumer 消费者
     * @param skip     跳过行数
     * @return 【处理的行数，时间ms】
     * @throws IOException 异常
     */
    public Pair<Long, Long> csvByLine(File file, BiConsumer<Long, String[]> consumer, int skip) throws IOException {
        return csvByLine(file.toURI().toURL(), consumer, skip);
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

        CSVReader reader = new CSVReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
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

    /**
     * 分批读取数据
     *
     * @param file     文件
     * @param consumer 消费者
     * @param skip     跳过行数
     * @param batch    每批大小
     * @return 【处理的批次数量，时间ms】
     * @throws IOException 异常
     */
    public Pair<Long, Long> csvByBatch(File file, BiConsumer<Long, List<String[]>> consumer, int skip, int batch) throws IOException {
        return csvByBatch(file.toURI().toURL(), consumer, skip, batch);
    }

    /**
     * 文件转字符串
     *
     * @param url 数据源
     * @return 字符串
     * @throws IOException 异常
     */
    public String fileString(URL url) throws IOException {
        return fileString(new File(url.getFile()));
    }

    /**
     * 文件转字符串
     *
     * @param file 文件
     * @return 字符串
     * @throws IOException 异常
     */
    public String fileString(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    /**
     * 对象转json字符串
     *
     * @param obj 对象
     * @return json字符串
     * @throws JsonProcessingException 异常
     */
    public String toJson(Object obj) throws JsonProcessingException {
        if (obj instanceof String) {
            return (String) obj;
        }
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 对象转JsonNode
     *
     * @param obj 对象
     * @return json字符串
     * @throws JsonProcessingException 异常
     */
    public JsonNode toJsonNode(Object obj) throws JsonProcessingException {
        if (obj instanceof String) {
            return new TextNode((String) obj);
        }
        return objectMapper.readTree(toJson(obj));
    }

    /**
     * JsonNode转map
     *
     * @param jsonNode jsonNode
     * @return map
     * @throws JsonProcessingException 异常
     */
    public Map<String, Object> toMap(JsonNode jsonNode) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(jsonNode);
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * 文件转map
     *
     * @param file 文件
     * @return map
     * @throws Exception 异常
     */
    public Map<String, Object> toMap(File file) throws Exception {
        return toMap(file, true);
    }

    /**
     * 文件转map
     *
     * @param file       文件
     * @param filterNull 是否过滤null
     * @return map
     * @throws Exception 异常
     */
    public Map<String, Object> toMap(File file, boolean filterNull) throws Exception {
        Map<String, Object> map = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {
        });
        if (!filterNull) {
            return map;
        }
        map.entrySet().removeIf(next -> Objects.isNull(next.getValue()));
        return map;
    }

    public JsonNode readTree(URL url) throws IOException {
        return objectMapper.readTree(url);
    }

    public JsonNode readTree(byte[] bytes) throws IOException {
        return objectMapper.readTree(bytes);
    }

    public JsonNode readTree(File file) throws IOException {
        return objectMapper.readTree(file);
    }

    public JsonNode readTree(String str) throws IOException {
        return objectMapper.readTree(str);
    }

    /**
     * 对象转json文件
     *
     * @param file 文件
     * @param obj  对象
     * @throws IOException 异常
     */
    public void writeJson(File file, Object obj) throws IOException {
        if (obj instanceof String) {
            writeString(file, (String) obj);
        }
        objectMapper.writeValue(file, obj);
    }

    /**
     * 对象转json文件
     *
     * @param file   文件
     * @param string 字符串
     * @throws IOException 异常
     */
    public void writeString(File file, String string) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        IOUtils.write(string, fileOutputStream, StandardCharsets.UTF_8);
        fileOutputStream.close();
    }

    /**
     * 读取图片
     *
     * @param url 图片地址
     * @return 图片数据
     * @throws IOException 异常
     */
    public BufferedImage readImage(URL url) throws IOException {
        return ImageIO.read(url);
    }

}
