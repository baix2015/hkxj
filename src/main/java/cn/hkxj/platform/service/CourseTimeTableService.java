package cn.hkxj.platform.service;

import cn.hkxj.platform.MDCThreadPool;
import cn.hkxj.platform.dao.*;
import cn.hkxj.platform.exceptions.UrpRequestException;
import cn.hkxj.platform.pojo.*;
import cn.hkxj.platform.pojo.vo.CourseTimeTableVo;
import cn.hkxj.platform.spider.newmodel.coursetimetable.UrpCourseTimeTable;
import cn.hkxj.platform.spider.newmodel.coursetimetable.UrpCourseTimeTableForSpider;
import cn.hkxj.platform.utils.DateUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Yuki
 * @date 2019/8/29 21:40
 */
@Slf4j
@Service
public class CourseTimeTableService {

    private static final String NO_COURSE_TEXT = "今天没有课呐，可以出去浪了~\n";
    @Resource
    private RoomService roomService;
    @Resource
    private StudentDao studentDao;
    @Resource
    private UrpCourseService urpCourseService;
    @Resource
    private NewUrpSpiderService newUrpSpiderService;
    @Resource
    private ClassService classService;
    @Resource
    private CourseTimeTableDao courseTimeTableDao;
    @Resource
    private StudentCourseTimeTableDao studentCourseTimeTableDao;
    @Resource
    private TeacherCourseTimeTableDao teacherCourseTimeTableDao;
    @Resource
    private TeacherDao teacherDao;
    @Resource
    private UrpClassDao urpClassDao;

    private Executor courseSpiderExecutor = new MDCThreadPool(7, 7, 0L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), r -> new Thread(r, "courseSpider"));

    /**
     * 这个方法只能将一天的数据转换成当日课表所需要的文本
     *
     * @param details 当天的课程时间表详情
     * @return 课表
     */
    public String convertToText(List<CourseTimeTableDetail> details) {
        if (CollectionUtils.isEmpty(details)) {
            return NO_COURSE_TEXT;
        }
        StringBuilder builder = new StringBuilder();
        details.sort(Comparator.comparing(CourseTimeTableDetail::getOrder));
        int count = 0;
        int length = details.size();
        for (CourseTimeTableDetail detail : details) {
            if (detail == null) {
                continue;
            }
            count++;
            builder.append(computationOfKnots(detail)).append("节").append("\n")
                    .append(urpCourseService.getCurrentTermCourse(detail.getCourseId(),
                            detail.getCourseSequenceNumber()).getName()).append(
                    "\n")
                    .append(detail.getRoomName());
            if (count != length) {
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }


    public List<CourseTimeTableVo> updateCourseTimeTableByStudent(int account) {
        studentCourseTimeTableDao.deleteByAccount(account);
        return getCurrentTermCourseTimeTableByStudent(account);
    }


    public List<CourseTimeTableVo> getCurrentTermCourseTimeTableByStudent(int account) {
        Student student = studentDao.selectStudentByAccount(account);
        return getCurrentTermCourseTimeTableByStudent(student);
    }


    public List<CourseTimeTableVo> getCurrentTermCourseTimeTableByStudent(Student student) {
        // TODO 优化这部分逻辑
        List<CourseTimetable> courseTimetableList = getCurrentTermCourseTimeTableByAccount(student.getAccount());
        if (courseTimetableList.isEmpty()) {
            return getCourseTimeTableByStudentFromSpider(student);
        } else {
            return transCourseTimeTableToVo(courseTimetableList);
        }
    }

    List<CourseTimeTableVo> getCourseTimeTableByStudentFromSpider(Student student) {
        try {
            CompletableFuture<UrpCourseTimeTableForSpider> future =
                    CompletableFuture.supplyAsync(() -> getCourseTimeTableDetails(student), courseSpiderExecutor);

            UrpCourseTimeTableForSpider tableForSpider = future.get(3000L, TimeUnit.MILLISECONDS);
            if (!hasSchoolCourse(tableForSpider)) {
                return getCurrentTermCourseTimetableVOByClazz(student);
            } else {

                List<CourseTimetable> list = tableForSpider.adaptToList();
                list.forEach(x -> {
                    UrpClassroom room = roomService.getClassRoomByName(x.getRoomName());
                    x.setRoomNumber(room == null ? "" : room.getNumber());
                });

                List<CourseTimetable> current = list.stream()
                        .filter(CourseTimetable::isCurrentTerm)
                        .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(current)){
                    saveCourseTimeTableDetailsFromSearch(current, student);
                }
                return transCourseTimeTableToVo(list);
            }

        } catch (UrpRequestException | InterruptedException | ExecutionException | TimeoutException e) {
            return getCurrentTermCourseTimetableVOByClazz(student);
        }
    }

    public List<CourseTimeTableVo> getCourseTimeTableByTeacherAccount(String account) {
        Teacher teacher = teacherDao.selectByAccount(account);
        return getCourseTimeTableByTeacher(teacher.getId());
    }

    public List<CourseTimeTableVo> getCourseTimetableVoByClazz(String classNum) {
        UrpClass urpClass = urpClassDao.selectByClassNumber(classNum);
        return transCourseTimeTableToVo(getCurrentTermCourseTimetableByClass(urpClass));
    }

    public List<CourseTimeTableVo> getCourseTimeTableByTeacher(Integer teacherId) {
        List<Integer> idList = teacherCourseTimeTableDao.selectByPojo(new TeacherCourseTimetable().setTeacherId(teacherId))
                .stream()
                .map(TeacherCourseTimetable::getCourseTimetableId)
                .collect(Collectors.toList());

        if (idList.isEmpty()) {
            return Collections.emptyList();
        }
        return transCourseTimeTableToVo(courseTimeTableDao.selectByIdList(idList));
    }

    public List<CourseTimeTableVo> getCourseTimeTableByClassroom(String roomId) {
        List<CourseTimetable> courseTimetableList = courseTimeTableDao.selectByCourseTimetable(new CourseTimetable().setRoomNumber(roomId));
        return transCourseTimeTableToVo(courseTimetableList);
    }

    public List<CourseTimeTableVo> getCourseTimeTableByCourse(String courseId, String courseOrder) {
        List<CourseTimetable> courseTimetableList = courseTimeTableDao.selectByCourseTimetable(new CourseTimetable().setCourseId(courseId).setCourseSequenceNumber(courseOrder));
        return transCourseTimeTableToVo(courseTimetableList);
    }

    public List<CourseTimeTableVo> transCourseTimeTableToVo(List<CourseTimetable> courseTimetableList) {
        return courseTimetableList.stream().map(x ->
                new CourseTimeTableVo()
                        .setAttendClassTeacher(x.getAttendClassTeacher())
                        .setCampusName(x.getCampusName())
                        .setClassDay(x.getClassDay())
                        .setClassOrder(x.getClassOrder())
                        .setClassInSchoolWeek(x.getClassInSchoolWeek())
                        .setContinuingSession(x.getContinuingSession())
                        .setStartWeek(x.getStartWeek())
                        .setEndWeek(x.getEndWeek())
                        .setRoomName(x.getRoomName())
                        .setRoomNumber(x.getRoomNumber())
                        .setWeekDescription(x.getWeekDescription())
                        .setTermOrder(x.getTermOrder())
                        .setTermYear(x.getTermYear())
                        .setStudentCount(x.getStudentCount())
                        .setCourse(urpCourseService.getCourseFromCache(x.getCourseId(), x.getCourseSequenceNumber(), x.getTermYear(), x.getTermOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 判断爬虫的返回结果是否只有网课
     */
    private boolean hasSchoolCourse(UrpCourseTimeTableForSpider tableForSpider) {
        for (HashMap<String, UrpCourseTimeTable> table : tableForSpider.getDetails()) {
            for (Map.Entry<String, UrpCourseTimeTable> entry : table.entrySet()) {
                if (entry.getValue().getTimeAndPlaceList().size() != 0) {
                    return true;
                }
            }
        }
        return false;

    }


    UrpCourseTimeTableForSpider getCourseTimeTableDetails(Student student) {
        return newUrpSpiderService.getUrpCourseTimeTable(student);
    }

    private void saveRelative(List<Integer> needInsertIds, Student student, String termYear, Integer termOrder) {
        for (Integer id : needInsertIds) {
            studentCourseTimeTableDao.insertSelective(new StudentCourseTimeTable()
                    .setCourseTimetableId(id)
                    .setStudentId(student.getAccount())
                    .setTermYear(termYear)
                    .setTermOrder(termOrder)
            );
        }
    }

    /**
     * 将课程搜索结果保存到数据库中，数据来源是不包含学生信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveCourseTimeTableDetailsFromSearch(List<CourseTimetable> courseTimetableList, Student student) {
        if (courseTimetableList.isEmpty()) {
            return;
        }
        String termYear = courseTimetableList.get(0).getTermYear();
        Integer termOrder = courseTimetableList.get(0).getTermOrder();

        List<Integer> idList = getCourseTimetableIdList(courseTimetableList);
        //关联班级和课程详情
        if (!CollectionUtils.isEmpty(idList)) {
            saveRelative(idList, student, termYear, termOrder);
        }
    }

    List<Integer> getCourseTimetableIdList(List<CourseTimetable> courseTimetableList) {
        List<Integer> idList = Lists.newArrayList();
        for (CourseTimetable courseTimetable : courseTimetableList) {
            CourseTimetable course = courseTimeTableDao.selectUniqueCourse(courseTimetable);

            // 如果没有这个课程的上课信息，用搜索功能去抓取
            // 理论上应该都有  没有的话可能是程序有问题或者是教务网的数据缺失  都应该仔细检查一下
            if (course == null) {
                courseTimeTableDao.insertSelective(courseTimetable);
                log.info("insert course info {}", courseTimetable);
                idList.add(courseTimetable.getId());
            } else {
                if (!course.equals(courseTimetable)) {
                    courseTimeTableDao.updateByUniqueKey(courseTimetable);
                    log.info("courseTimetable origin {}\nupdate {}", course, courseTimetable);
                }
                idList.add(course.getId());
            }
        }

        return idList;
    }

    /**
     * 计算节数
     *
     * @param detail 课程时间表详情
     * @return 节数字符串
     */
    private String computationOfKnots(CourseTimeTableDetail detail) {
        return detail.getOrder() + "-" + (detail.getOrder() + detail.getContinuingSession() - 1);
    }

    private List<CourseTimetable> getCurrentTermCourseTimeTableByAccount(Integer account) {
        SchoolTime schoolTime = DateUtils.getCurrentSchoolTime();

        StudentCourseTimeTable table = new StudentCourseTimeTable()
                .setStudentId(account)
                .setTermOrder(schoolTime.getTerm().getOrder())
                .setTermYear(schoolTime.getTerm().getTermYear());
        return courseTimeTableDao.selectByStudentRelative(table);
    }

    List<CourseTimetable> getCurrentTermCourseTimetableByClass(String classNum) {

        if (StringUtils.isEmpty(classNum)) {
            return Collections.emptyList();
        }

        SchoolTime schoolTime = DateUtils.getCurrentSchoolTime();
        ClassCourseTimetable relative = new ClassCourseTimetable()
                .setClassId(classNum)
                .setTermYear(schoolTime.getTerm().getTermYear())
                .setTermOrder(schoolTime.getTerm().getOrder());

        return courseTimeTableDao.selectByClassRelative(relative);
    }

    List<CourseTimetable> getCurrentTermCourseTimetableByClass(UrpClass urpClass) {
        if (urpClass == null){
            return Collections.emptyList();
        }
        return getCurrentTermCourseTimetableByClass(urpClass.getClassNum());
    }

    List<CourseTimetable> getCurrentTermCourseTimetableByClass(Student student){
        UrpClass urpClass = classService.getUrpClassByStudent(student);
        return getCurrentTermCourseTimetableByClass(urpClass);
    }

    List<CourseTimeTableVo> getCurrentTermCourseTimetableVOByClazz(Student student){
        List<CourseTimetable> timetableList = getCurrentTermCourseTimetableByClass(student);
        return transCourseTimeTableToVo(timetableList);
    }

}
