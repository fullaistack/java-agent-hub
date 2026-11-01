package com.agenthub.parser;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.parser.config.ParserProperties;
import com.agenthub.parser.model.ParsedDocument;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 解析引擎路由：按配置选择解析器，并支持运行时降级（主引擎失败时回退 Tika）。
 */
@Service
public class ParserService {

    private final com.agenthub.parser.config.ParserProperties properties;
    private final Map<String, ParserClient> clients;

    public ParserService(ParserProperties properties, List<ParserClient> parserClients) {
        this.properties = properties;
        this.clients = parserClients.stream()
                .collect(Collectors.toMap(ParserClient::provider, Function.identity()));
    }

    /** 按配置解析文档 */
    public ParsedDocument parse(String fileName, InputStream in) {
        ParserClient client = clients.get(properties.getProvider());
        if (client == null) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "未找到解析引擎: " + properties.getProvider());
        }
        return client.parse(fileName, in);
    }
}
