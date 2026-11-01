package com.agenthub.knowledge.service;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.knowledge.dto.KnowledgeBaseRequest;
import com.agenthub.knowledge.entity.KnowledgeBase;
import com.agenthub.knowledge.mapper.KnowledgeBaseMapper;
import com.agenthub.tenant.context.TenantContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库服务：知识库是租户内向量数据的边界，检索时按 kb_id 过滤。
 * 租户隔离由 MyBatis-Plus 租户插件自动完成。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper kbMapper;

    /** 创建知识库 */
    public KnowledgeBase create(KnowledgeBaseRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setTenantId(TenantContext.requireTenantId());
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setEmbeddingModel(request.getEmbeddingModel());
        kb.setChunkSize(request.getChunkSize() == null ? 800 : request.getChunkSize());
        kb.setChunkOverlap(request.getChunkOverlap() == null ? 100 : request.getChunkOverlap());
        kb.setStatus(1);
        kb.setCreatedBy(TenantContext.getUserId());
        kbMapper.insert(kb);
        return kb;
    }

    /** 当前租户的知识库列表 */
    public List<KnowledgeBase> list() {
        return kbMapper.selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .orderByDesc(KnowledgeBase::getId));
    }

    /** 获取知识库（校验归属，跨租户访问返回 null） */
    public KnowledgeBase require(Long kbId) {
        KnowledgeBase kb = kbMapper.selectById(kbId);
        if (kb == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识库不存在: " + kbId);
        }
        return kb;
    }

    /** 删除知识库（向量数据由 PG 侧按 kb_id 清理） */
    public void delete(Long kbId) {
        require(kbId);
        kbMapper.deleteById(kbId);
    }
}
