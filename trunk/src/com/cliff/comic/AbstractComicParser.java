package com.cliff.comic;

public abstract class AbstractComicParser implements ComicParser {

	protected int lastError = ERROR_OK;

	abstract protected boolean parseBook(String comicUrl, Comic comic, boolean parseVolumes);
	abstract protected boolean parseVolume(Volume volume);
	
	protected ParsingVolumeListener listener = null; 

	@Override
	public Comic getComic(String comicUrl) {
		Comic comic = new Comic();
		if (!parseBook(comicUrl, comic, true))
			return null;
		return comic;
	}

	@Override
	public int getLastError() {
		return lastError;
	}

	@Override
	public Comic getComicWithoutVolumes(String comicUrl) {
		Comic comic = new Comic();
		if (!parseBook(comicUrl, comic, false))
			return null;
		return comic;
	}

	@Override
	public boolean completeComicWithoutVolumes(Comic comic) {
		if (!parseVolumes(comic)) {
//			lastError = ERROR_PARSING_VOLUMES;
			return false;
		}
		return true;
	}

	@Override
	public boolean completeComicWithoutVolumes(Comic comic, int[] volumes) {
		if (null == volumes)
			return completeComicWithoutVolumes(comic);
		
		for (int i = 0; i < volumes.length; i++) {
			Volume volume = comic.volumes[volumes[i]];
			if (null != listener)
				listener.beforeParsingVolume(volume, i, volumes.length);
			if (!parseVolume(volume)) {
//				lastError = ERROR_PARSING_VOLUMES;
				if (null != listener)
					listener.afterParsingVolume(volume, i, volumes.length, false);
				return false;
			}				
			if (null != listener)
				listener.afterParsingVolume(volume, i, volumes.length, true);
		}
		
		return true;
	}

	protected boolean parseVolumes(Comic comic) {
		for (int i = 0; i < comic.volumes.length; i++) {
			if (null != listener)
				listener.beforeParsingVolume(comic.volumes[i], i, comic.volumes.length);
			if (!parseVolume(comic.volumes[i])) {
//				lastError = ERROR_PARSING_VOLUMES;
				if (null != listener)
					listener.afterParsingVolume(comic.volumes[i], i, comic.volumes.length, false);
				return false;
			}
			if (null != listener)
				listener.afterParsingVolume(comic.volumes[i], i, comic.volumes.length, true);
		}		
		return true;		
	}

	@Override
	public void setParsingVolumeListener(ParsingVolumeListener listener) {
		this.listener = listener;
	}
	
	
}
