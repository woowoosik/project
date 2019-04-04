package com.woojinsik.mytalk.model;

public class NotificationModel {

    public String to;
    public Notification notification = new Notification(); // 초기화도 시킴


    // inner 클래스
    public static class Notification {
        public String title;
        public String text;
    }


}
