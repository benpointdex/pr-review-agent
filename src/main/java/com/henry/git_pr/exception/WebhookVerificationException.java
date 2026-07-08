package com.henry.git_pr.exception;

public class WebhookVerificationException extends RuntimeException{

    public WebhookVerificationException(String message){
        super(message);

    }
}
