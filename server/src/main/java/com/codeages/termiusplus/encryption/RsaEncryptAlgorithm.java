package com.codeages.termiusplus.encryption;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import com.cxytiandi.encrypt.algorithm.EncryptAlgorithm;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RsaEncryptAlgorithm implements EncryptAlgorithm {

    private final SM2 requestSm2;

    private final SM2 responseSm2;

    private final Gson gson;

    private final Boolean enable;


    public RsaEncryptAlgorithm(@Value("${spring.encrypt.request.public.key}") String reqPublicKey,
                               @Value("${spring.encrypt.request.private.key}") String reqPrivateKey,
                               @Value("${spring.encrypt.response.public.key}") String resPublicKey,
                               @Value("${spring.encrypt.response.private.key}") String resPrivateKey,
                               @Value("${spring.encrypt.enabled}") Boolean enable) {
        requestSm2 = SmUtil.sm2(reqPrivateKey, reqPublicKey);
        responseSm2 = SmUtil.sm2(resPrivateKey, resPublicKey);
        gson = new Gson();
        this.enable = enable;
    }

    @Override
    public String encrypt(String content, String encryptKey) {
        if (!enable) {
            return content;
        }
        if (StrUtil.isBlank(content)) {
            return content;
        }
        return responseSm2.encryptBcd(content, KeyType.PublicKey);
    }

    @Override
    public String decrypt(String encryptStr, String decryptKey) {
        if (!enable) {
            return encryptStr;
        }
        if (StrUtil.isBlank(encryptStr)) {
            return encryptStr;
        }
        return requestSm2.decryptStr(encryptStr, KeyType.PrivateKey);
    }

    public String decrypt(String encryptStr) {
        return decrypt(encryptStr, null);
    }

    public <O> Object encrypt(O o) {
        if (!enable) {
            return o;
        }
        return encrypt(gson.toJson(o), null);
    }
}
