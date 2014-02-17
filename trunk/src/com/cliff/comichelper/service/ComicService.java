package com.cliff.comichelper.service;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ResponseHandler;

import com.cliff.comic.Comic;
import com.cliff.comic.ComicParser;
import com.cliff.comic.Manhua8ComicParser;
import com.cliff.comic.ParsingVolumeListener;
import com.cliff.comic.Volume;
import com.cliff.comichelper.MainActivity;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import de.greenrobot.event.EventBus;
import android.R.integer;
import android.app.DownloadManager;
import android.app.Service;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class ComicService extends Service {
	
	protected EventBus eventBus = EventBus.getDefault();
	protected final String TAG = "ComicService";
	protected final static int DOWNLOAD_THREAD_COUNT = 2;
	protected ThreadPoolExecutor threadPool = null;
	protected AsyncHttpClient client = null;
//	protected String urlParsing = null;
	
	private IComicService.Stub mBinder = new IComicService.Stub() {

		@Override
		public int getProgress() throws RemoteException {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getProgressTotal() throws RemoteException {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}; 
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		eventBus.register(this);
		client = new AsyncHttpClient();
		client.setThreadPool(new ThreadPoolExecutor(DOWNLOAD_THREAD_COUNT, DOWNLOAD_THREAD_COUNT,  
                0L, TimeUnit.MILLISECONDS,  
                new LinkedBlockingQueue<Runnable>()));
//		client.setThreadPool((ThreadPoolExecutor) Executors.newFixedThreadPool(DOWNLOAD_THREAD_COUNT));
	}

	@Override
	public void onDestroy() {
		eventBus.unregister(this);
		super.onDestroy();
	}
	
	public void onEvent(Command event) {
		Log.d(TAG, "I got a command");
		
		if (Constants.COMMAND_PARSE.equals(event.commandName)) {
			String url = (String)event.getParam(Constants.PARAM_URL);
			Log.d(TAG, "command is parse, url is "+url);
			new ParseTask().execute(url);		
		}
		else if (Constants.COMMAND_DOWNLOAD.equals(event.commandName)) {
			Log.d(TAG, "command is download");
			new DownloadTask().execute(event.getParam(Constants.PARAM_COMIC), 
					event.getParam(Constants.PARAM_VOLUMES),
					event.getParam(Constants.PARAM_ROOTDIR));					
		}
	}
	
	protected ComicParser getComicParser(String url) {
		if (null == url) {
			return null;
		}
		
		if (url.contains(Manhua8ComicParser.DOMAIN_NAME)||url.contains(Manhua8ComicParser.DOMAIN_NAME2))
			return new Manhua8ComicParser();
		
		return null;
	}

	private class DownloadTask extends AsyncTask<Object, Void, Void> {

		@Override
		protected Void doInBackground(Object... params) {
			// TODO Auto-generated method stub
			Comic comic = (Comic) params[0];
			int[] volumes = (int[]) params[1];
			String rootdir = (String) params[2];
			
			DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
			
			final ComicParser parser = getComicParser(comic.comicUrl.toString());
			if (null == parser) {
				Notification notification = new Notification(Constants.NOTIFICATION_VOLUME);
				notification.addParam(Constants.PARAM_ERROR, ComicParser.ERROR_OK);
				eventBus.post(notification);
				return null;
			}

			parser.setParsingVolumeListener(new ParsingVolumeListener() {
				@Override
				public void beforeParsingVolume(Volume volume, int count, int total) {
					Notification notification = new Notification(Constants.NOTIFICATION_VOLUME);
					notification.addParam(Constants.PARAM_VOLUME, volume);
					notification.addParam(Constants.PARAM_PROGRESS, Integer.valueOf(count));
					notification.addParam(Constants.PARAM_MAX, Integer.valueOf(total));
					eventBus.post(notification);
				}
				
				@Override
				public void afterParsingVolume(Volume volume, int count, int total,
						boolean result) {
					if (result)
						return;
					
					Notification notification = new Notification(Constants.NOTIFICATION_VOLUME);
					notification.addParam(Constants.PARAM_VOLUME, volume);
					notification.addParam(Constants.PARAM_ERROR, Integer.valueOf(parser.getLastError()));
					eventBus.post(notification);									
				}
			});
			
			if (!parser.completeComicWithoutVolumes(comic, volumes))
				return null;
			
			int max = 0;
			int prog = 0;
			for (int i = 0; i < comic.volumes.length; i++) {
				Volume volume = comic.volumes[i];
				if (null == volume.picUrls)
					continue;
				
				max += volume.picUrls.length;
			}
			
			for (int i = 0; i < comic.volumes.length; i++) {
				Volume volume = comic.volumes[i];
				if (null == volume.picUrls)
					continue;
				
				for (int j = 0; j<volume.picUrls.length; j++) {
					String fileName = volume.picUrls[j].getFile();
					fileName = fileName.substring(fileName.lastIndexOf('/') +1);
//					final String fullFileName = rootdir+"/"+comic.comicName+"/"+volume.volumeName+"/"+fileName;
					String directory = rootdir+"/"+comic.comicName+"/"+volume.volumeName+"/";		
					Notification notification = new Notification(Constants.NOTIFICATION_DOWNLOAD);
					notification.addParam(Constants.PARAM_PROGRESS, Integer.valueOf(prog));
					notification.addParam(Constants.PARAM_MAX, Integer.valueOf(max));
					eventBus.post(notification);
					downloadFile(downloadManager, volume.picUrls[j].toString(), directory, fileName);
					prog++;
//					DownloadManager.Request request = new DownloadManager.Request(Uri.parse(volume.picUrls[j].toString()));
//					request.setAllowedNetworkTypes(Request.NETWORK_WIFI);
//					request.setDestinationUri(Uri.fromFile(new File(fullFileName)));  
//					downloadManager.enqueue(request);  
//					client.get(volume.picUrls[j].toString(), 
//							new FileAsyncHttpResponseHandler(ComicService.this) {
//								@Override
//								public void onSuccess(File file) {
//									File newFile = new File(fullFileName);
//									newFile.mkdirs();
//									file.renameTo(newFile);
//									Log.d(TAG, "In queue="+threadPool.getQueue().size());
//									super.onSuccess(file);
//								}						
//					});
				}				
			}
			
			Notification notification = new Notification(Constants.NOTIFICATION_DOWNLOAD);
			notification.addParam(Constants.PARAM_PROGRESS, Integer.valueOf(max));
			notification.addParam(Constants.PARAM_MAX, Integer.valueOf(max));
			eventBus.post(notification);
			return null;			
		}
		
		protected void downloadFile(DownloadManager downloadManager,
				String url, String directory, String fileName) {
			File file = new File(Environment.getExternalStorageDirectory().getPath()+directory + fileName);
			if (file.exists()) {
				Log.d(TAG, directory + fileName + " exists, pass");
				return;
			}
			Log.d(TAG, directory + fileName + " not exist, download!!!");
			// 开始下载
			Uri resource = Uri.parse(url);
			DownloadManager.Request request = new DownloadManager.Request(
					resource);
			request.setAllowedNetworkTypes(Request.NETWORK_WIFI);
			request.setAllowedOverRoaming(false);
//			// 设置文件类型
			MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
			String mimeString = mimeTypeMap
					.getMimeTypeFromExtension(MimeTypeMap
							.getFileExtensionFromUrl(url));
			request.setMimeType(mimeString);
			// 在通知栏中显示
			// request.setShowRunningNotification(true);
			request.setVisibleInDownloadsUi(false);
			// sdcard的目录下的download文件夹
			request.setDestinationInExternalPublicDir(directory, fileName);
			long id = downloadManager.enqueue(request);
		}
		
	}
	
	private class ParseTask extends AsyncTask<String, Void, Comic> {
//		private String lastError = null;

		protected Comic doInBackground(String... urls) {
			String url = urls[0];
			
//			if (null != ComicService.this.urlParsing) {
//				if (ComicService.this.urlParsing.equals(url))
//					return null;
//			}
//			
//			ComicService.this.urlParsing = url;
			
			ComicParser parser = getComicParser(url);
			if (null == parser) {
				Notification notification = new Notification(Constants.NOTIFICATION_PARSE);
				eventBus.post(notification);
				return null;
			}
			
			Comic comic = parser.getComicWithoutVolumes(url);
			Notification notification = new Notification(Constants.NOTIFICATION_PARSE);
			notification.addParam(Constants.PARAM_COMIC, comic);
			notification.addParam(Constants.PARAM_ERROR, parser.getLastError());
			eventBus.post(notification);
//			ComicService.this.urlParsing = null;
			
			return comic;
		}

//		protected void downloadComic(Comic comic) {
//			DownloadManager downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
//			String rootDir = "/ibuka/my/";
//			String directory = null;
//			String fileName = null;
//			URL url = null;
//
//			Volume[] volumes = comic.volumes;
//			for (int i = 0; i < volumes.length; i++) {
//				Volume volume = volumes[i];
//				URL picUrls[] = volume.picUrls;
//				for (int j = 0; j < picUrls.length; j++) {
//					url = picUrls[j];
//					directory = rootDir + comic.comicName + "/" + volume.volumeName + "/";
//					fileName = url.getFile();
//					fileName = fileName.substring(fileName.lastIndexOf('/') +1);
//					downloadFile(downloadManager, url.toString(), directory, fileName);
//				}
//			}
//		}
		

	}

}
