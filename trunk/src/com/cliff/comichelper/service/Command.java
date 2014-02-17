package com.cliff.comichelper.service;

public class Command extends BaseEvent{
	public String commandName;
	
	public Command(String commandName) {
		this.commandName = commandName;
	}

}
