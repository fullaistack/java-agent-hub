package com.agenthub.rag.splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 递归字符切分器（LangChain RecursiveCharacterTextSplitter 的简化实现）。
 *
 * <p>按分隔符优先级逐级切分：段落 → 换行 → 中文句号 → 空格 → 逐字符，
 * 每块不超过 chunkSize，块间保留 chunkOverlap 重叠，保证语义上下文连续。</p>
 */
public class RecursiveCharacterTextSplitter implements TextSplitter {

    /** 分隔符优先级（从粗粒度到细粒度） */
    private static final List<String> SEPARATORS = List.of("\n\n", "\n", "。", "！", "？", "；", ". ", " ", "");

    /** 句子结尾标点：优先在这些位置断开，避免把一句话拦腰截断 */
    private static final Set<Character> SENTENCE_END = Set.of('。', '！', '？', '.', '!', '?', '；', ';');

    private final int chunkSize;
    private final int chunkOverlap;

    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap) {
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap 必须小于 chunkSize");
        }
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @Override
    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        for (String piece : splitBy(text, 0)) {
            piece = piece.trim();
            if (piece.isEmpty()) {
                continue;
            }
            // 超长块：递归切分；普通块：带重叠滑动窗口输出
            if (piece.length() > chunkSize) {
                chunks.addAll(splitBy(piece, 0).stream().map(String::trim)
                        .filter(s -> !s.isEmpty()).toList());
            } else {
                chunks.add(piece);
            }
        }
        return chunks;
    }

    /** 递归按当前层级分隔符切分 */
    private List<String> splitBy(String text, int separatorIndex) {
        if (text.length() <= chunkSize || separatorIndex >= SEPARATORS.size()) {
            return mergeWithOverlap(text);
        }
        String separator = SEPARATORS.get(separatorIndex);
        List<String> parts;
        if (separator.isEmpty()) {
            // 最后一级：逐字符
            parts = new ArrayList<>();
            for (char c : text.toCharArray()) {
                parts.add(String.valueOf(c));
            }
        } else {
            parts = new ArrayList<>();
            int start = 0;
            while (start <= text.length()) {
                int idx = text.indexOf(separator, start);
                if (idx < 0) {
                    parts.add(text.substring(start));
                    break;
                }
                parts.add(text.substring(start, idx + separator.length()));
                start = idx + separator.length();
            }
        }
        // 合并小片段至接近 chunkSize，再对超长片段降级递归
        List<String> merged = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String part : parts) {
            if (buf.length() + part.length() > chunkSize) {
                if (!buf.isEmpty()) {
                    merged.add(buf.toString());
                    buf.setLength(0);
                }
                if (part.length() > chunkSize) {
                    merged.addAll(splitBy(part, separatorIndex + 1));
                } else {
                    buf.append(part);
                }
            } else {
                buf.append(part);
            }
        }
        if (!buf.isEmpty()) {
            merged.add(buf.toString());
        }
        return merged;
    }

    /** 对符合长度要求的文本生成带重叠的窗口块 */
    private List<String> mergeWithOverlap(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            // 优先在句子边界截断，减少语义断裂
            if (end < text.length()) {
                for (int i = end - 1; i > start + chunkSize / 2; i--) {
                    if (SENTENCE_END.contains(text.charAt(i))) {
                        end = i + 1;
                        break;
                    }
                }
            }
            chunks.add(text.substring(start, end));
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - chunkOverlap, start + 1);
        }
        return chunks;
    }
}
