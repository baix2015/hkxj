package cn.hkxj.platform.service.wechat;

import cn.hkxj.platform.pojo.Student;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutTextMessage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author JR Chan
 * @date 2019/5/3
 */
@Slf4j
public class CustomerMessageService {

    private static ExecutorService cacheThreadPool = Executors.newCachedThreadPool();

    private WxMpXmlMessage wxMpXmlMessage;
    private WxMpService wxMpService;

    public CustomerMessageService(WxMpXmlMessage wxMpXmlMessage, WxMpService wxMpService) {
        this.wxMpService = wxMpService;
        this.wxMpXmlMessage = wxMpXmlMessage;
    }

    public void sentTextMessageAsync(Future<String> resultFuture) {
        cacheThreadPool.submit(() -> sentTextMessage(wxMpXmlMessage, wxMpService, resultFuture));
    }

    public WxMpXmlOutTextMessage sendMessage(Future<String> future, Student student) {
        try {
            String result = future.get(3, TimeUnit.SECONDS);
            return buildMessage(result);
        } catch (TimeoutException e) {
            sentTextMessageAsync(future);
        } catch (InterruptedException | ExecutionException e) {
            log.error("student {} get grade error {}", student.getAccount(), e);
        }
        return buildMessage("服务器正在努力查询中");
    }

    private void sentTextMessage(WxMpXmlMessage wxMpXmlMessage, WxMpService wxMpService, Future<String> resultFuture) {
        if (resultFuture.isCancelled()) {
            return;
        }
        String result;
        try {
            result = resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("send custom service get result error {}", e);
            return;
        }

        WxMpKefuMessage wxMpKefuMessage = new WxMpKefuMessage();
        wxMpKefuMessage.setContent(result);
        wxMpKefuMessage.setMsgType("text");
        wxMpKefuMessage.setToUser(wxMpXmlMessage.getFromUser());
        try {
            wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
        } catch (WxErrorException e) {
            log.error("send grade customer message error", e);
        }
    }

    private WxMpXmlOutTextMessage buildMessage(String content) {
        return WxMpXmlOutMessage.TEXT().content(content)
                .fromUser(wxMpXmlMessage.getToUser()).toUser(wxMpXmlMessage.getFromUser())
                .build();
    }
}