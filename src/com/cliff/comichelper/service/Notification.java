package com.cliff.comichelper.service;

public class Notification extends BaseEvent {
	public String notificationName;
	
	public Notification(String notificationName) {
		this.notificationName = notificationName;
	}

}
