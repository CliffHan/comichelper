package com.cliff.comic;

public interface ComicParser {
	public final int ERROR_OK = 0;
	public final int ERROR_UNKNOWN_DOMAIN = -1;
	public final int ERROR_INVALID_BOOK_URL = -2;
	public final int ERROR_PARSING = -3;
	public final int ERROR_PARSING_BOOK_NAME = -4;
	public final int ERROR_PARSING_VOLUME_URL = -5;
	public final int ERROR_PARSING_VOLUMES = -6;

	public Comic getComic(String comicUrl);
	public Comic getComicWithoutVolumes(String comicUrl);
	public boolean completeComicWithoutVolumes(Comic comic);
	public boolean completeComicWithoutVolumes(Comic comic, int volumes[]);
	public int getLastError();
	public void setParsingVolumeListener(ParsingVolumeListener listener);
}
