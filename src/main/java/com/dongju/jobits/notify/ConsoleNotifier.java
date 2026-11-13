package com.dongju.jobits.notify;

/** 항상 켜져 있는 채널. Actions 로그나 로컬 실행에서 무엇이 나갔는지 확인용. */
public class ConsoleNotifier implements Notifier {

    @Override
    public String name() { return "console"; }

    @Override
    public boolean supports(NotificationMessage.Kind kind) { return true; }

    @Override
    public void send(NotificationMessage message) {
        System.out.println("──── [" + message.kind() + "] ────");
        System.out.println(message.asText());
    }
}
