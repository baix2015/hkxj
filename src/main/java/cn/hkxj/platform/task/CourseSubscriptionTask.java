package cn.hkxj.platform.task;

import cn.hkxj.platform.builder.TemplateBuilder;
import cn.hkxj.platform.config.wechat.WechatMpConfiguration;
import cn.hkxj.platform.config.wechat.WechatMpPlusProperties;
import cn.hkxj.platform.pojo.ScheduleTask;
import cn.hkxj.platform.pojo.wechat.CourseGroupMsg;
import cn.hkxj.platform.pojo.wechat.OneOffSubscription;
import cn.hkxj.platform.service.CourseSubscribeService;
import cn.hkxj.platform.service.ScheduleTaskService;
import cn.hkxj.platform.utils.OneOffSubcriptionUtil;
import cn.hkxj.platform.utils.SchoolTimeUtil;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ThreadFactory;

/**
 * @author Yuki
 * @date 2018/11/6 20:36
 */
@Slf4j
@Service
public class CourseSubscriptionTask {

    @Resource
    private CourseSubscribeService courseSubscribeService;
    @Resource
    private ScheduleTaskService scheduleTaskService;
    @Resource
    private WechatMpPlusProperties wechatMpPlusProperties;
    @Resource
    private TemplateBuilder templateBuilder;

    private static final String MSG_TITLE = "今日课表";

    @Scheduled(cron = "0 0 8 ? * MON-FRI")      //这个cron表达式的意思是星期一到星期五的早上8点执行一次
//    @Scheduled(cron = "0/30 * * * * ?")
    public void sendCourseRemindMsg() {
        log.info("Course Push Task is start....");
        Map<String, Set<CourseGroupMsg>> courseGroupMsgMap = courseSubscribeService.getCoursesSubscribeForCurrentDay();
        courseGroupMsgMap.forEach((appid, courseGroupMsgSet) -> {
            //如果courseGroupMsgSet为空时，说明没有可用的订阅，直接跳过当前循环
            if(courseGroupMsgSet == null) {
                return;
            }
            WxMpService wxMpService = getWxMpService(appid);
            log.info("appid:{} data size:{}", appid, courseGroupMsgMap.size());
            for (CourseGroupMsg msg : courseGroupMsgSet) {
                //获取一个并行流，添加监视messagePeek,设置过滤条件，然后每一个都进行消息发送
                msg.getScheduleTasks().stream().filter(cgm -> !Objects.isNull(cgm)).parallel().peek(this::messagePeek).forEach(task -> {
                    //根据appid来选择不同的处理过程
                    if(Objects.equals(appid, wechatMpPlusProperties.getAppId())){
                        plusMpProcess(task, msg, wxMpService);
                    } else {
                        proMpProcess(task, msg, wxMpService);
                    }
                });
            }
        });
        log.info("course push task is end");
    }

    /**
     * plus的处理过程
     * @param task 定时任务
     * @param msg 课程推送信息
     * @param wxMpService wxMpService
     */
    private void plusMpProcess(ScheduleTask task, CourseGroupMsg msg, WxMpService wxMpService){
        List<WxMpTemplateData> templateData = assemblyTemplateContent(msg);
        String url = "http://platform.hackerda.com/platform/course/timetable";
        //构建一个课程推送的模板消息
        WxMpTemplateMessage templateMessage = templateBuilder.buildCourseMessage(task.getOpenid(), templateData, url);
        try {
            //发送成功的同时更新发送状态
            wxMpService.getTemplateMsgService().sendTemplateMsg(templateMessage);
            scheduleTaskService.updateSendStatus(task, ScheduleTaskService.SEND_SUCCESS);
            log.info("send Message to appid:{} openid:{} success", wxMpService.getWxMpConfigStorage().getAppId(), task.getOpenid());
        } catch (WxErrorException e) {
            log.info("send kefu Message to appid:{} openid:{} failed message:{}",
                    wxMpService.getWxMpConfigStorage().getAppId(), task.getOpenid(), e.getMessage());
        }
    }

    /**
     * pro的处理过程
     * @param task 定时任务
     * @param msg 课程推送信息
     * @param wxMpService wxMpService
     */
    private void proMpProcess(ScheduleTask task, CourseGroupMsg msg, WxMpService wxMpService){
        String scene = String.valueOf(task.getScene());
        //先创建一个OneOffSubscription的实体
        OneOffSubscription oneOffSubscription = new OneOffSubscription.Builder()
                        .touser(task.getOpenid())
                        .scene(scene)
                        .title(MSG_TITLE)
                        .templateId(wxMpService.getWxMpConfigStorage().getTemplateId())
                        .data(msg.getCourseContent())
                        .url(OneOffSubcriptionUtil.getHyperlinks(null, scene, wxMpService))
                        .build();
        try {
            //发送一次性订阅的模板消息，同时更新发送状态和订阅状态
            OneOffSubcriptionUtil.sendTemplateMessageToUser(oneOffSubscription, wxMpService);
            scheduleTaskService.updateSendStatus(task, ScheduleTaskService.SEND_SUCCESS);
            //因为一次性订阅一次只能发送一次消息，所以发送成功后将订阅状态置为不可用
            scheduleTaskService.updateSubscribeStatus(task, ScheduleTaskService.FUNCTION_DISABLE);
            log.info("send course push to user:{} appid:{} success", task.getOpenid(), wxMpService.getWxMpConfigStorage().getAppId());
        } catch (WxErrorException e) {
            log.info("send course push to user:{} appid:{} fail message:{}",
                    task.getOpenid(), wxMpService.getWxMpConfigStorage().getAppId(), e.getMessage());
        }
    }

    /**
     * 根据appid获取相应的wxMpService
     * @param appid appid
     * @return wxMpService
     */
    private WxMpService getWxMpService(String appid) {
        return WechatMpConfiguration.getMpServices().get(appid);
    }

    /**
     * 组装模板消息需要的WxMpTemplateData的列表
     * @param msg 课程推送信息
     * @return WxMpTemplateData的列表
     */
    private List<WxMpTemplateData> assemblyTemplateContent(CourseGroupMsg msg) {
        List<WxMpTemplateData> templateDatas = new ArrayList<>();
        //first关键字
        WxMpTemplateData first = new WxMpTemplateData();
        first.setName("first");
        first.setValue("当日课表\n");
        //keyword1关键字
        WxMpTemplateData course = new WxMpTemplateData();
        course.setName("keyword1");
        course.setValue("\n" + msg.getCourseContent() + "\n");
        //keyword2关键字
        WxMpTemplateData date = new WxMpTemplateData();
        date.setName("keyword2");
        date.setValue("第" + SchoolTimeUtil.getSchoolWeek() + "周   " + SchoolTimeUtil.getDayOfWeekChinese());
        //remark关键字
        WxMpTemplateData remark = new WxMpTemplateData();
        remark.setName("remark");
        remark.setValue("查询仅供参考，以学校下发的课表为准，如有疑问微信添加吴彦祖【hkdhd666】");

        templateDatas.add(first);
        templateDatas.add(course);
        templateDatas.add(date);
        templateDatas.add(remark);

        return templateDatas;
    }


    /**
     * 给并行流添加一个监视
     */
    private void messagePeek(ScheduleTask scheduleTask) {
        log.info("send course push to user：{} appid:{}", scheduleTask.getOpenid(), scheduleTask.getAppid());
    }
}
