package com.agenthub.parser.tika;

import com.agenthub.parser.ParserClient;
import com.agenthub.parser.model.ParsedDocument;
import org.apache.tika.Tika;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Apache Tika 本地解析：无需外部服务，支持 PDF/Word/PPT/HTML 等主流格式。
 * 适合开发环境与 MinerU 不可用时的降级方案，解析质量（尤其版面）低于 MinerU。
 */
@Component
@ConditionalOnProperty(name = "agenthub.parser.provider", havingValue = "tika", matchIfMissing = true)
public class TikaParserClient implements ParserClient {

    private final Tika tika = new Tika();

    @Override
    public String provider() {
        return "tika";
    }

    @Override
    public ParsedDocument parse(String fileName, InputStream in) {
        try {
            String text = tika.parseToString(in);
            return new ParsedDocument(fileName, text, provider());
        } catch (Exception e) {
            throw new IllegalStateException("Tika 解析失败: " + fileName, e);
        }
    }
}
