package com.cliff.comic;

public interface ParsingVolumeListener {
	void beforeParsingVolume(Volume volume, int count, int total);
	void afterParsingVolume(Volume volume, int count, int total, boolean result);
}
