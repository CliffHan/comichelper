package com.cliff.comic;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

public class Manhua8ComicParser extends AbstractComicParser {

	public final static String DOMAIN_NAME = "manhua8.com";
	public final static String DOMAIN_NAME2 = "manhua8.net";
	protected final String HOST_URL = "http://www.manhua8.com";
	protected final String HOST_URL2 = "http://www.manhua8.net";
	protected final String CHARSET = "gb2312";
	protected LinkedHashMap<String, String> urlMap = new LinkedHashMap<String, String>();

	@Override
	protected boolean parseBook(String comicUrl, Comic comic, boolean parseVolumes) {
		
		if (null == comic)
			return false;
		
		URL url = null;
		try {
			url = new URL(comicUrl);
			if (!(url.getHost().contains(DOMAIN_NAME)||url.getHost().contains(DOMAIN_NAME2))) {
				lastError = ERROR_UNKNOWN_DOMAIN;
				return false;
			}
			if (!(url.getPath().startsWith("/manhua/")&&url.getPath().endsWith("/"))) {
				lastError = ERROR_INVALID_BOOK_URL;
				return false;
			}
				
		} catch (MalformedURLException e) {
			e.printStackTrace();
			lastError = ERROR_INVALID_BOOK_URL;
			return false;
		}

		comic.comicUrl = url;
		CleanerProperties props = new CleanerProperties();
		props.setCharset(CHARSET);

		// do parsing
		try {
			TagNode tagNode = new HtmlCleaner(props).clean(url);
//			System.out.println(tagNode.getText());
			
			comic.comicName = getComicName(tagNode);
			if (null == comic.comicName) {
				lastError = ERROR_PARSING_BOOK_NAME;
				return false;
			}
			
			if (!parseVolumeUrls(tagNode, comic)) {
				lastError = ERROR_PARSING_VOLUME_URL;
				return false;
			}
			
			if (parseVolumes) {
				if (!parseVolumes(comic)) {
					lastError = ERROR_PARSING_VOLUMES;
					return false;
				}
			}

		} catch (IOException e) {
			lastError = ERROR_PARSING;
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	protected String getComicName(TagNode node) {
		Object[] ns;
		try {
			ns = node.evaluateXPath("//div[@class='bookInfo']//h1//b");
			if (ns.length <= 0)
				return null;
			return ((TagNode)ns[0]).getText().toString();
		} catch (XPatherException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected boolean parseVolumeUrls(TagNode node, Comic comic) {
		Object[] ns;
		String volumeUrl;
		ArrayList<Volume> volumes = new ArrayList<Volume>();
		try {
			ns = node.evaluateXPath("//div[@class='bookList']//*//a[@href]");
			for (Object object : ns) {
				TagNode hrefNode = (TagNode) object;
				volumeUrl = hrefNode.getAttributeByName("href");
				if (volumeUrl.startsWith("/")) {
					Volume volume = new Volume();
					volume.volumeUrl = new URL(HOST_URL + volumeUrl);
					volume.volumeName = hrefNode.getAttributeByName("title");
					volumes.add(volume);
				}
			}
			comic.volumes = volumes.toArray(new Volume[volumes.size()]);
			return true;
		} catch (XPatherException e) {
			e.printStackTrace();
			return false;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	protected boolean parseVolume(Volume volume) {
		
		if (null != volume.picUrls)
			return true;
		
		URL url = volume.volumeUrl;
		if (!(url.getHost().contains(DOMAIN_NAME)||url.getHost().contains(DOMAIN_NAME2))) {
			lastError = ERROR_UNKNOWN_DOMAIN;
			return false;
		}
		if (!(url.getPath().startsWith("/manhua/")&&url.getPath().endsWith(".htm"))) {
			lastError = ERROR_INVALID_BOOK_URL;
			return false;
		}

		CleanerProperties props = new CleanerProperties();
		props.setCharset(CHARSET);

		// do parsing
		try {
			TagNode tagNode = new HtmlCleaner(props).clean(volume.volumeUrl);
//			System.out.println(tagNode.getText());

			Object[] ns;
			ns = tagNode.evaluateXPath("//script");
			String start = "var picArray = new Array(";
			String end = ");"; 
			for (Object object : ns) {
				TagNode scriptNode = (TagNode) object;
				String script = scriptNode.getText().toString();
				int offset = script.indexOf(start);
				if (offset >= 0) {
					script = script.substring(offset + start.length());
					offset = script.indexOf(end);
					if (offset >= 0) {
						script = script.substring(0, offset);
						String urlStrings[] = script.replaceAll("\'", "").split("'|,");
						volume.picUrls = new URL[urlStrings.length];
						for (int i = 0; i < urlStrings.length; i++)
							volume.picUrls[i] = new URL(urlStrings[i]); 
						return true;
					}
					else 
						return false;
				}
				else continue;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (XPatherException e) {
			e.printStackTrace();
			return false;
		}		
		return false;
	}


}
