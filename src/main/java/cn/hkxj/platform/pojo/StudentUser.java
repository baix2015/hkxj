package cn.hkxj.platform.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class StudentUser {
    private Integer id;

    private Integer account;

    private String password;

    private String name;

    private String sex;

    private String ethnic;

    private Integer urpclassNum;

    private Boolean isCorrect;

    private Date gmtCreate;

    private Date gmtModified;

    private String academyName;

    private String subjectName;

    private String className;
}