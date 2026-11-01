package com.agenthub.rag.splitter;

import java.util.List;

/**
 * 文本切分抽象。
 */
public interface TextSplitter {

    /** 把长文本切分为块列表 */
    List<String> split(String text);
}
