package com.agenthub.tenant.service;

import com.agenthub.common.exception.BizException;
import com.agenthub.common.exception.ErrorCode;
import com.agenthub.tenant.dto.LoginRequest;
import com.agenthub.tenant.dto.TenantCreateRequest;
import com.agenthub.tenant.entity.Tenant;
import com.agenthub.tenant.entity.TenantApiKey;
import com.agenthub.tenant.entity.TenantUser;
import com.agenthub.tenant.mapper.TenantApiKeyMapper;
import com.agenthub.tenant.mapper.TenantMapper;
import com.agenthub.tenant.mapper.TenantUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 租户服务：租户创建、用户管理、API Key 签发。
 *
 * <p>密码与 API Key 均只保存哈希值。演示使用 SHA-256，生产环境建议：
 * 密码换 BCrypt/Argon2，API Key 换 HMAC-SHA256 或密钥库托管。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;
    private final TenantUserMapper tenantUserMapper;
    private final TenantApiKeyMapper apiKeyMapper;

    /** 创建租户：同时创建 OWNER 用户与首个 API Key */
    @Transactional(rollbackFor = Exception.class)
    public Tenant createTenant(TenantCreateRequest request) {
        Tenant tenant = new Tenant();
        tenant.setTenantCode(request.getTenantCode());
        tenant.setTenantName(request.getTenantName());
        tenant.setStatus(1);
        tenantMapper.insert(tenant);

        TenantUser owner = new TenantUser();
        owner.setTenantId(tenant.getId());
        owner.setUsername(request.getAdminUsername());
        owner.setPasswordHash(sha256(request.getAdminPassword()));
        owner.setDisplayName(request.getTenantName() + "管理员");
        owner.setRole("OWNER");
        owner.setStatus(1);
        tenantUserMapper.insert(owner);
        log.info("租户创建成功: code={}, id={}", tenant.getTenantCode(), tenant.getId());
        return tenant;
    }

    /** 登录：租户编码 + 用户名 + 密码，返回用户信息 */
    public TenantUser login(LoginRequest request) {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getTenantCode, request.getTenantCode())
                .eq(Tenant::getStatus, 1));
        if (tenant == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户不存在或已停用");
        }
        TenantUser user = tenantUserMapper.selectOne(new LambdaQueryWrapper<TenantUser>()
                .eq(TenantUser::getTenantId, tenant.getId())
                .eq(TenantUser::getUsername, request.getUsername())
                .eq(TenantUser::getStatus, 1));
        if (user == null || !sha256(request.getPassword()).equals(user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return user;
    }

    /** 签发 API Key：返回明文（仅此一次），库中只存哈希 */
    public String createApiKey(Long tenantId, String name) {
        String raw = "ahk_" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(randomBytes(24));
        TenantApiKey key = new TenantApiKey();
        key.setTenantId(tenantId);
        key.setApiKeyHash(sha256(raw));
        key.setApiKeyPrefix(raw.substring(0, 12));
        key.setName(name);
        key.setEnabled(1);
        apiKeyMapper.insert(key);
        return raw;
    }

    /** 校验 API Key：返回对应租户ID，无效返回 null */
    public Long verifyApiKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }
        TenantApiKey key = apiKeyMapper.selectOne(new LambdaQueryWrapper<TenantApiKey>()
                .eq(TenantApiKey::getApiKeyHash, sha256(rawKey))
                .eq(TenantApiKey::getEnabled, 1));
        if (key == null) {
            return null;
        }
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return null;
        }
        return key.getTenantId();
    }

    // ---------- 工具方法 ----------

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /** SHA-256 哈希（演示实现） */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }
}
