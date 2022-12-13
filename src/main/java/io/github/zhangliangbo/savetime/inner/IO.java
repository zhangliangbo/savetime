package io.github.zhangliangbo.savetime.inner;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Stopwatch;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.rabbitmq.client.LongString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
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
     * json转map
     *
     * @param json json
     * @return map
     * @throws JsonProcessingException 异常
     */
    public Map<String, Object> toMap(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
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
        return toMap(json);
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
     * @param uri 图片地址
     * @return 图片数据
     * @throws IOException 异常
     */
    public BufferedImage readImage(URI uri) throws IOException {
        return ImageIO.read(uri.toURL());
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

    /**
     * 读取图片
     *
     * @param file 图片文件
     * @return 图片数据
     * @throws IOException 异常
     */
    public BufferedImage readImage(File file) throws IOException {
        return ImageIO.read(file);
    }

    /**
     * 文件追加内容
     *
     * @param file   文件
     * @param string 内容
     * @throws IOException 异常
     */
    public void appendFile(File file, String string) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
        IOUtils.write(string, fileOutputStream, StandardCharsets.UTF_8);
        fileOutputStream.close();
    }

    /**
     * excel转csv
     *
     * @param csv          csv文件
     * @param batch        批量复制大小
     * @param excel        excel文件
     * @param sheetAndHead 表格编号，表头数目，表格编号，表头数目，...
     * @return 【总数,时间ms】
     * @throws IOException 异常
     */
    public Pair<Long, Long> excelToCsv(File csv, int batch, File excel, int... sheetAndHead) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        AtomicLong line = new AtomicLong(0L);

        CSVWriter writer = new CSVWriter(new FileWriter(csv));

        List<String[]> batchList = new LinkedList<>();

        AnalysisEventListener<Map<Integer, String>> listener = new AnalysisEventListener<Map<Integer, String>>() {

            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                System.out.println(headMap);
                String[] headers = headMap.values().toArray(new String[0]);
                writer.writeNext(headers);
                long count = line.incrementAndGet();
                System.out.printf("s %s head %s%n", context.readSheetHolder().getSheetNo(), count);
            }

            @Override
            public void invoke(Map<Integer, String> o, AnalysisContext context) {
                String[] record = o.values().toArray(new String[0]);
                batchList.add(record);
                long count = line.incrementAndGet();
                if (batchList.size() >= batch) {
                    writer.writeAll(batchList);
                    writer.flushQuietly();
                    batchList.clear();
                    System.out.printf("s %s record %s%n", context.readSheetHolder().getSheetNo(), count);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                if (batchList.size() > 0) {
                    writer.writeAll(batchList);
                    writer.flushQuietly();
                    batchList.clear();
                    System.out.printf("s %s record %s%n", context.readSheetHolder().getSheetNo(), line.get());
                }
                System.out.printf("s %s end%n", context.readSheetHolder().getSheetNo());
            }
        };

        if (sheetAndHead.length % 2 != 0) {
            throw new IllegalArgumentException("sheetAndHead数组个数必须是2的倍数");
        }
        if (sheetAndHead.length == 0) {
            ExcelReader excelReader = EasyExcel.read(excel, listener).build();
            excelReader.readAll();
            excelReader.close();
        } else {
            ExcelReader excelReader = EasyExcel.read(excel).build();
            List<ReadSheet> readSheetList = new LinkedList<>();
            for (int i = 0; i < sheetAndHead.length; i += 2) {
                ReadSheet readSheet = EasyExcel.readSheet(sheetAndHead[i]).headRowNumber(sheetAndHead[i + 1]).registerReadListener(listener).build();
                readSheetList.add(readSheet);
            }
            excelReader.read(readSheetList);
            excelReader.close();
        }

        writer.close();

        stopwatch.stop();
        return Pair.of(line.get(), stopwatch.elapsed(java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    public JsonNode toJsonNode(Map<String, Object> map) {
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        if (MapUtils.isEmpty(map)) {
            return objectNode;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            fillNode(objectNode, key, value);
        }
        return objectNode;
    }

    private void fillNode(JsonNode jsonNode, String key, Object value) {
        if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            if (value instanceof String) {
                arrayNode.add((String) value);
            } else if (value instanceof Long) {
                arrayNode.add((Long) value);
            } else if (value instanceof Integer) {
                arrayNode.add((Integer) value);
            } else if (value instanceof Short) {
                arrayNode.add((Short) value);
            } else if (value instanceof Double) {
                arrayNode.add((Double) value);
            } else if (value instanceof Float) {
                arrayNode.add((Float) value);
            } else if (value instanceof BigDecimal) {
                arrayNode.add((BigDecimal) value);
            } else if (value instanceof BigInteger) {
                arrayNode.add((BigInteger) value);
            } else if (value instanceof Boolean) {
                arrayNode.add((Boolean) value);
            } else if (value instanceof byte[]) {
                arrayNode.add((byte[]) value);
            } else if (value instanceof LongString) {
                arrayNode.add(value.toString());
            } else if (value instanceof Map) {
                arrayNode.add(toJsonNode((Map) value));
            }
        } else {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            if (value instanceof String) {
                objectNode.put(key, (String) value);
            } else if (value instanceof Long) {
                objectNode.put(key, (Long) value);
            } else if (value instanceof Integer) {
                objectNode.put(key, (Integer) value);
            } else if (value instanceof Short) {
                objectNode.put(key, (Short) value);
            } else if (value instanceof Double) {
                objectNode.put(key, (Double) value);
            } else if (value instanceof Float) {
                objectNode.put(key, (Float) value);
            } else if (value instanceof BigDecimal) {
                objectNode.put(key, (BigDecimal) value);
            } else if (value instanceof BigInteger) {
                objectNode.put(key, (BigInteger) value);
            } else if (value instanceof Boolean) {
                objectNode.put(key, (Boolean) value);
            } else if (value instanceof byte[]) {
                objectNode.put(key, (byte[]) value);
            } else if (value instanceof LongString) {
                objectNode.put(key, value.toString());
            } else if (value instanceof Map) {
                objectNode.set(key, toJsonNode((Map) value));
            } else if (value instanceof Iterable) {
                ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
                Iterator iterator = ((Iterable) value).iterator();
                while (iterator.hasNext()) {
                    Object next = iterator.next();
                    fillNode(arrayNode, null, next);
                }
                objectNode.set(key, arrayNode);
            } else {
                objectNode.putPOJO(key, value);
            }
        }
    }

}
