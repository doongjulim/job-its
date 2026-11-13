package com.dongju.jobits.notify;

import java.io.IOException;

/** 알림 채널 하나. 새 채널(카카오톡, 이메일 …)은 이 인터페이스만 구현하면 된다. */
public interface Notifier {

    String name();

    /** 이 채널이 받는 메시지 종류인가. 예: GitHub 이슈는 POSTING 만 받는다. */
    boolean supports(NotificationMessage.Kind kind);

    void send(NotificationMessage message) throws IOException, InterruptedException;
}
