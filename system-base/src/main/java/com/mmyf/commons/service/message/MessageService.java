package com.mmyf.commons.service.message;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * MessageService
 *
 * @author Teddy
 * @version 1.0
 */
@Service
@Slf4j
public class MessageService {

    @Value("${message.enable:true}")
    private Boolean messageEnable;

    /**
     * 发送邮件消息给用户
     *
     * @param senderId   发送人
     * @param receiverId 接收人
     * @param title      标题
     * @param content    内容
     * @return true表示发送成功，false表示
     */
    public boolean sendMailMessage(String senderId, String receiverId, String title, String content, String url) {
        if (!messageEnable) {
            return true;
        }
        if (StringUtils.isBlank(senderId) || StringUtils.isBlank(receiverId)) {
            log.warn("发送消息时参数有误，发送者={}，接收者={}", senderId, receiverId);
            return false;
        }
        boolean result = true;
        if (!result) {
            log.warn("发送消息时返回失败，发送者={}，接收者={}", senderId, receiverId);
        } else {
            log.debug("发送消息成功，发送者={}，接收者={}", senderId, receiverId);
        }
        return result;
    }

}
