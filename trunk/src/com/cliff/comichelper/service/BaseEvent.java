package com.cliff.comichelper.service;

import java.util.HashMap;

public class BaseEvent {

	public HashMap<String, Object> paramMap = new HashMap<String, Object>();

	public void addParam(String paramName, Object param) {
		paramMap.put(paramName, param);		
	}
	
	public void removeParam(String paramName) {
		paramMap.remove(paramName);
	}
	
	public Object getParam(String paramName) {
		return paramMap.get(paramName);
	}
}
