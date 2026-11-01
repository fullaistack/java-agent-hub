package com.agenthub.parser.mineru;

import com.agenthub.common.util.JsonUtils;
import com.agenthub.parser.ParserClient;
import com.agenthub.parser.config.ParserProperties;
import com.agenthub.parser.model.ParsedDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * MinerU 解析客户端：对接 MinerU WebAPI（HTTP 异步调用，避免阻塞业务线程）。
 *
 * <p>MinerU 1.x 自带 FastAPI 服务，接口为 {@code POST {url}/file_parse}，
 * 返回 JSON 中 data.md_content 为 Markdown 全文。
 * 依赖 Docker 服务（opendatalab/mineru），需 GPU 以获得最佳解析效果。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "agenthub.parser.provider", havingValue = "mineru")
public class MineruParserClient implements ParserClient {

    private final WebClient webClient;

    public MineruParserClient(ParserProperties properties) {
        this.webClient = WebClient.builder()
                .baseUrl(properties.getMineru().getUrl())
                .build();
    }

    @Override
    public String provider() {
        return "mineru";
    }

    @Override
    public ParsedDocument parse(String fileName, InputStream in) {
        byte[] bytes = readAll(in);
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("files", new ByteArrayResource(bytes)).filename(fileName);
        // 是否启用 OCR 与解析模式，可按需调整
        builder.part("is_ocr", "true");
        builder.part("paser_method", "auto");

        String response;
        try {
            response = webClient.post()
                    .uri("/file_parse")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(10))
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("MinerU 解析调用失败: " + fileName, e);
        }
        if (response == null) {
            throw new IllegalStateException("MinerU 返回为空: " + fileName);
        }
        return new ParsedDocument(fileName, extractMarkdown(response), provider());
    }

    /**
     * 从 MinerU 响应中提取 Markdown 全文。
     * 响应结构（1.x）：{"code":200, "data": {"md_content": "...", "content_list": [...]}}
     */
    @SuppressWarnings("unchecked")
    private String extractMarkdown(String response) {
        Map<String, Object> root = JsonUtils.parseMap(response);
        Object data = root.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object md = ((Map<String, Object>) dataMap).get("md_content");
            if (md instanceof String mdText && !mdText.isBlank()) {
                return mdText;
            }
        }
        // 兼容 content_list 结构：拼接每个块的文本
        Object contentList = data instanceof Map<?, ?> m ? m.get("content_list") : null;
        if (contentList instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                if (item instanceof Map<?, ?> block && block.get("text") != null) {
                    sb.append(block.get("text")).append('\n');
                }
            }
            if (!sb.isEmpty()) {
                return sb.toString();
            }
        }
        throw new IllegalStateException("MinerU 响应缺少 md_content/content_list 字段");
    }

    private byte[] readAll(InputStream in) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("读取上传文件失败", e);
        }
    }
}
