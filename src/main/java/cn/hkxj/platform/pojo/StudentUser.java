package cn.hkxj.platform.pojo;

import java.util.Date;

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAccount() {
        return account;
    }

    public void setAccount(Integer account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex == null ? null : sex.trim();
    }

    public String getEthnic() {
        return ethnic;
    }

    public void setEthnic(String ethnic) {
        this.ethnic = ethnic == null ? null : ethnic.trim();
    }

    public Integer getUrpclassNum() {
        return urpclassNum;
    }

    public void setUrpclassNum(Integer urpclassNum) {
        this.urpclassNum = urpclassNum;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getAcademyName() {
        return academyName;
    }

    public void setAcademyName(String academyName) {
        this.academyName = academyName == null ? null : academyName.trim();
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName == null ? null : subjectName.trim();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className == null ? null : className.trim();
    }

    @Override
    public String toString() {
        return "StudentUser{" +
                "id=" + id +
                ", account=" + account +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", sex='" + sex + '\'' +
                ", ethnic='" + ethnic + '\'' +
                ", urpclassNum=" + urpclassNum +
                ", isCorrect=" + isCorrect +
                ", gmtCreate=" + gmtCreate +
                ", gmtModified=" + gmtModified +
                ", academyName='" + academyName + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", className='" + className + '\'' +
                '}';
    }
}