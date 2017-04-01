package com.bang.bangapplication.sms;

import io.realm.RealmObject;

/**
 * Created by boxfox on 2017-04-01.
 */

public class Person extends RealmObject {
    private String number, name, message;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
