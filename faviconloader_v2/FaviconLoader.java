package com.stickypassword.android.misc.faviconloader_v2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import com.stickypassword.android.misc.MiscMethods;
import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;

public class FaviconLoader {

	public interface SaveCallback {
		public void saveData(String parentUrl, byte[] blob);
	}

	Context context;
	SaveCallback callback;
	static String TAG = "FaviconDownloader";

	public FaviconLoader(Context context) {
		this.context = context;
	}

	public void setCallback(SaveCallback callback) {
		this.callback = callback;
	}

	/**************************** THIC CODE MUST BE CROSSPLATFORM - C/C++ ****************************/

	// FAVICON DOCS:
	// http://www.jonathantneal.com/blog/understand-the-favicon/
	// http://olegorestov.ru/html5/favicon/
	// http://habrahabr.ru/company/ifree/blog/216045/

	// ICON SIZE DOCS:
	// https://developer.apple.com/library/ios/documentation/UserExperience/Conceptual/MobileHIG/IconMatrix.html
	// http://msdn.microsoft.com/en-us/library/ie/dn255024(v=vs.85).aspx
	// http://www.aha-soft.com/faq/android-icons-images.htm

	// data in HREF may have values:
	// absolute path - 'http://site.com/img/icon.ico'
	// relative '../../../icon.ico'
	// './icon.ico'
	// '/icon.ico'
	// 'icon.ico'
	// base64 encoded icon 'data:image/png;base64,123456=='

	// https://thebc.co/website-design/design-implement-favicon/
	// Favicon Size List

	// 16 x 16 Standard size for browsers
	// 24 x 24 IE9 pinned site size for user interface
	// 32 x 32 IE new page tab, Windows 7+ taskbar button, Safari Reading List
	// sidebar
	// 48 x 48 Windows site
	// 57 x 57 iPod touch, iPhone up to 3G
	// 60 x 60 iPhone touch up to iOS7
	// 64 x 64 Windows site, Safari Reader List sidebar in HiDPI/Retina
	// 70 x 70 Win 8.1 Metro tile
	// 72 x 72 iPad touch up to iOS6
	// 76 x 76 iOS7
	// 96 x 96 GoogleTV
	// 114 x 114 iPhone retina touch up to iOS6
	// 120 x 120 iPhone retina touch iOS7
	// 128 x 128 Chrome Web Store app, Android
	// 144 x 144 IE10 Metro tile for pinned site, iPad retina up to iOS6
	// 150 x 150 Win 8.1 Metro tile
	// 152 x 152 iPad retina touch iOS7
	// 196 x 196 Android Chrome
	// 310 x 150 Win 8.1 wide Metro tile
	// 310 x 310 Win 8.1 Metro tile

	// MS WIN8
	static String[] msTags = new String[] { "msapplication-square70x70logo",
			"msapplication-TileImage", "msapplication-square150x150logo",
			"msapplication-square310x310logo" };

	// need for conversation
	// name="msapplication-square70x70logo" -> sizes="70x70"
	static String[] msTagsHack = new String[] { "70x70", "144x144", "150x150",
			"310x310" };

	/*
	 * // tags basic list static String[] iconTagsBasic = new String[] { //
	 * Firefox, Opera "icon", // Chrome, Safari, IE "shortcut icon"};
	 */
	// tags full list
	static String[] iconTagsFull = new String[] {
			// Firefox, Opera
			"icon",
			// Chrome, Safari, IE
			"shortcut icon",
			// iOS
			"apple-touch-icon", "apple-touch-icon-precomposed",
			"apple-touch-startup-image" };

	/*
	 * 
	 * // mime
	 * 
	 * static String[] iconMime = new String[] { "image/png", "image/gif",
	 * "image/jpeg", "image/bmp", "image/x-bmp", "image/x-ms-bmp",
	 * "image/vnd.microsoft.icon", "image/x-icon", //old value, must be
	 * converted "image/svg+xml", "image/icns", ""// any other };
	 */

	String parentUrl;
	int iconSize;	
	// add new Task for execution
	public void startNewTask(String url, int size) throws Exception {		
		load(parentUrl = url, iconSize = size);		
	}

	private void load(String url, int size) throws Exception {

		String schm = getScheme(url);

		String path = url.substring(schm.length(), url.length());
		if (path.startsWith("www.")) {
			schm = schm + "www.";
			path = url.substring(schm.length(), url.length());
		}
		ArrayList<String> schemes = new ArrayList<String>();

		schemes.add(schm);

		if (!schemes.contains("https://www."))
			schemes.add("https://www.");
		if (!schemes.contains("http://www."))
			schemes.add("http://www.");
		if (!schemes.contains("https://"))
			schemes.add("https://");
		if (!schemes.contains("http://"))
			schemes.add("http://");

		for (int i = 0; i < schemes.size(); i++) {
			url = getUrl(schemes.get(i), path);

			if (!TextUtils.isEmpty(url))
				break;

		}
		schemes = null;

		if (TextUtils.isEmpty(url)) {
			Log.d(TAG, "Location not found: " + parentUrl);
			return;
		}

		Log.d(TAG, "Location: " + url);

		String faviconIcoUrl = RequestFaviconIco(url);
		if (faviconIcoUrl != null)
			if (trySaveThisIcon(faviconIcoUrl)) {
				Log.d(TAG, "Icon Link: " + faviconIcoUrl);
			}

		GetIconLinkForUrl(url, size);
	}

	private String RequestFaviconIco(String url) throws Exception {

		String baseUrl = url;

		if (baseUrl.indexOf("/", baseUrl.indexOf("://") + 3) == -1)
			baseUrl = baseUrl + "/";
		else
			baseUrl = baseUrl.substring(0,
					baseUrl.indexOf("/", baseUrl.indexOf("://") + 3) + 1);

		String faviconIcoUrl = baseUrl + "favicon.ico";

		if (ExistsOnWeb(url))
			return faviconIcoUrl;

		return null;
	}

	private void GetIconLinkForUrl(String url, int prefferedIconSize)
			throws Exception {

		HtmlCleaner cleaner = new HtmlCleaner();

		TagNode rootNode = cleaner.clean(new URL(url));

		TagNode[] headElements = rootNode.getElementsByName("head", true);

		if (headElements == null || headElements.length == 0)
			return;
		headElements = null;

		List<TagNode> tagsList = new ArrayList<TagNode>();

		TagNode[] linkElements = rootNode.getElementsByName("link", true);

		if (linkElements != null)
			for (TagNode linkElement : linkElements) {
				String rel = linkElement.getAttributeByName("rel");

				if (TextUtils.isEmpty(rel))
					continue;

				for (String tag : iconTagsFull) {

					if (!rel.contains(tag))
						continue;

					tagsList.add(linkElement);

				}
			}

		linkElements = null;

		TagNode[] metaElements = rootNode.getElementsByName("meta", true);
		if (metaElements != null)
			for (TagNode metaElement : metaElements) {

				String rel = metaElement.getAttributeByName("name");

				if (TextUtils.isEmpty(rel))
					continue;

				for (String tag : msTags) {

					if (!rel.contains(tag))
						continue;

					tagsList.add(metaElement);

				}
			}

		metaElements = null;

		cleaner = null;
		rootNode = null;

		class CustomComparator implements Comparator<TagNode> {

			private int width;

			CustomComparator(int width) {
				this.width = width;
			}

			private int getSize(String tag) {

				if (TextUtils.isEmpty(tag))
					return 0;

				int start1 = 0;
				int end1 = tag.indexOf("x", start1);

				int start2 = end1 + "x".length() + 1;

				int end2 = tag.length();

				return Math.max(Integer.parseInt(tag.substring(start1, end1)),
						Integer.parseInt(tag.substring(start2, end2)));

			}

			private String convert4Win(TagNode rootTag) {

				for (int i = 0; i < msTags.length; i++) {

					String tag = msTags[i];

					if (!TextUtils.isEmpty(rootTag.getAttributeByName("name")))
						if (!rootTag.getAttributeByName("name").contains(tag))
							continue;

					return msTagsHack[i];

				}

				return null;

			}

			@Override
			public int compare(TagNode tag1, TagNode tag2) {

				String str1 = tag1.getAttributeByName("sizes");
				String str2 = tag2.getAttributeByName("sizes");

				if (TextUtils.isEmpty(str1))
					str1 = convert4Win(tag1);

				if (TextUtils.isEmpty(str2))
					str2 = convert4Win(tag2);

				if (TextUtils.isEmpty(str1) && TextUtils.isEmpty(str2))
					return 0;
				if (TextUtils.isEmpty(str1) && !TextUtils.isEmpty(str2))
					return -1;
				if (TextUtils.isEmpty(str2) && !TextUtils.isEmpty(str1))
					return 1;
				if (Math.abs(width - getSize(str1)) == Math.abs(width
						- getSize(str2)))
					return 0;

				return (Math.abs(width - getSize(str1)) > Math.abs(width
						- getSize(str2))) ? 1 : -1;

			}
		}

		Collections.sort(tagsList, new CustomComparator(prefferedIconSize));

		for (int i = 0; i < tagsList.size(); i++) {

			String iconPath = tagsList.get(i).getAttributeByName("href");

			if (TextUtils.isEmpty(iconPath))
				iconPath = tagsList.get(i).getAttributeByName("content");

			iconPath = processURI(url, iconPath);

			if (iconPath != null) {
				if (trySaveThisIcon(iconPath)) {
					Log.d(TAG, "Icon Link: " + iconPath);

					return;
				}
			}

		}
		tagsList.clear();
		tagsList = null;

		String faviconIcoUrl = RequestFaviconIco(url);

		if (faviconIcoUrl != null)
			if (trySaveThisIcon(faviconIcoUrl)) {
				Log.d(TAG, "Icon Link: " + faviconIcoUrl);
				return;
			}

		Log.d(TAG, "Icon not found: " + url);

	}

	private boolean trySaveThisIcon(String iconUrl) throws Exception {

		if (!TextUtils.isEmpty(iconUrl)) {

			byte[] data = null;

			String name = iconUrl.hashCode() + "-" + iconSize + ".png";

			File dst = new File(context.getCacheDir(), name);
			if (dst.exists()) {
				FileInputStream inStream = new FileInputStream(dst);
				FileChannel inChannel = inStream.getChannel();
				ByteBuffer buffer = ByteBuffer.allocate((int) inChannel.size());
				inChannel.read(buffer);
				buffer.rewind();
				inChannel.close();
				inStream.close();
				data = buffer.array();

				if (callback != null) {
					callback.saveData(parentUrl, data);
					linkedTable.put(parentUrl, iconUrl);
					File file = new File(context.getCacheDir(),
							"linkedtable.map");
					if (file.exists())
						file.delete();
					FileOutputStream f = new FileOutputStream(file);
					ObjectOutputStream s = new ObjectOutputStream(f);
					s.writeObject(linkedTable);
					s.close();
				}
				data = null;
				return true;
			}

			// base64 encoded into HTML

			if (iconUrl.startsWith("data:")) {
				String base64 = iconUrl.substring(iconUrl.indexOf("base64")
						+ "base64".length() + 1, iconUrl.length());
				data = Base64.decode(base64, Base64.DEFAULT);

			} else {
				/*
				 * ArrayList<String> acceptedMime = new ArrayList<String>();
				 * addToList(acceptedMime, task.mime); addToList(acceptedMime,
				 * getMimeForUrl(iconUrl)); //instagram
				 * addToList(acceptedMime,"gzip"); //blackberry.com
				 * addToList(acceptedMime,"plain/text");
				 * addToList(acceptedMime,"application/octet-stream");
				 * 
				 * if(task.mime.equals("image/x-icon") ||
				 * task.mime.equals("image/vnd.microsoft.icon")){
				 * addToList(acceptedMime,"image/x-icon");
				 * addToList(acceptedMime,"image/vnd.microsoft.icon"); } boolean
				 * isMimeValid = false; for(int i =0; i < acceptedMime.size();
				 * i++){ String mime = acceptedMime.get(i);
				 * if(contains(task.headersResponse, mime)){ isMimeValid = true;
				 * break; } } if(!isMimeValid) return false;
				 */

				data = LoadFromWeb(iconUrl);
			}
			if (data != null) {
					if (callback != null) {
						callback.saveData(parentUrl, data);
						setIcon(context, parentUrl, iconUrl, iconSize, data);					
					data = null;
					return true;
				}
			}

		}

		return false;
	}

	private String processURI(String parentUrl, String inputUrl)
			throws Exception {

		if (isAbsolute(inputUrl))
			return inputUrl;

		if (!isAbsolute(inputUrl)) {

			// base64
			if (inputUrl.startsWith("data:")) {
				return inputUrl;
			}

			URI absolute = new URI(parentUrl);
			URI relative = new URI(inputUrl);
			return absolute.resolve(relative).toString();

		}

		return null;
	}

	private String getScheme(String url) throws Exception {
		if (TextUtils.isEmpty(url))
			return null;

		if (!Patterns.WEB_URL.matcher(url).matches()) {
			return null;
		}

		return new URI(url).getScheme() + "://";

	}

	public String getUrl(String scheme, String url) throws Exception {
		if (TextUtils.isEmpty(scheme) || TextUtils.isEmpty(url))
			return null;

		if (!Patterns.WEB_URL.matcher(scheme + url).matches()) {
			return null;
		}

		if (url.lastIndexOf('/') == -1)
			return null;

		if (!ExistsOnWeb(scheme + url))
			return getUrl(scheme, url.substring(0, url.lastIndexOf('/')));
		else
			return scheme + url;

	}

	private boolean isAbsolute(String url) throws Exception {
		if (TextUtils.isEmpty(url))
			return false;

		if (!Patterns.WEB_URL.matcher(url).matches()) {
			return false;
		}
		return new URI(url).isAbsolute();

	}
	//network func.

	private boolean ExistsOnWeb(String url) {
		try {
			if (TextUtils.isEmpty(url))
				return false;

			if (!Patterns.WEB_URL.matcher(url).matches()) {
				return false;
			}

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			HttpConnectionParams.setConnectionTimeout(params, 1500);
			HttpConnectionParams.setSoTimeout(params, 1500);

			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);
			MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			DefaultHttpClient client = new DefaultHttpClient(ccm, params);

			HttpGet request = new HttpGet(url);

			HttpResponse response = client.execute(request);

			return (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
		} catch (Exception ex) {

		}
		return false;

	}

	private byte[] LoadFromWeb(String url) throws Exception {

		if (TextUtils.isEmpty(url))
			return null;

		if (!Patterns.WEB_URL.matcher(url).matches()) {
			return null;
		}

		byte[] data = null;
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

		HttpConnectionParams.setConnectionTimeout(params, 1500);
		HttpConnectionParams.setSoTimeout(params, 1500);

		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null, null);
		MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		registry.register(new Scheme("https", sf, 443));

		ClientConnectionManager ccm = new ThreadSafeClientConnManager(params,
				registry);

		DefaultHttpClient client = new DefaultHttpClient(ccm, params);

		HttpGet get = new HttpGet(url);
		HttpResponse response = client.execute(get);

		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

			InputStream in = response.getEntity().getContent();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ReadableByteChannel source = Channels.newChannel(in);
			WritableByteChannel target = Channels.newChannel(out);

			int size = ((MiscMethods.isConnectedFast(context) ? 500 : 8) * 1024);

			if (response.getEntity().getContentLength() > 0)
				size = ((int) (MiscMethods.isConnectedFast(context) ? response
						.getEntity().getContentLength() : 8 * 1024));

			ByteBuffer buffer = ByteBuffer.allocate(size);
			while (source.read(buffer) != -1) {
				buffer.flip(); // Prepare the buffer to be drained
				while (buffer.hasRemaining()) {
					target.write(buffer);
				}
				buffer.clear(); // Empty buffer to get ready for filling
			}

			source.close();
			target.close();
			in.close();
			data = out.toByteArray();
			source = null;
			target = null;
			in = null;
			out = null;
		} else
			data = null;

		response = null;
		get = null;

		return data;

	}

	public class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public MySSLSocketFactory(KeyStore truststore)
				throws NoSuchAlgorithmException, KeyManagementException,
				KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port,
				boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host,
					port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

	
	
	//FS cache
	
	HashMap<String, String> linkedTable;
	final long keepaliveResources = 604800000L; // 7 days

	private void init(Context context) throws Exception {
		if (linkedTable == null) {
			linkedTable = new HashMap<String, String>();

			File file = new File(context.getCacheDir(), "linkedtable.map");
			if (file.exists()) {
				FileInputStream f = new FileInputStream(file);
				ObjectInputStream s = new ObjectInputStream(f);
				linkedTable = (HashMap<String, String>) s.readObject();
				s.close();
			}

			File fileCacheDir = context.getCacheDir();
			clear(fileCacheDir, keepaliveResources);

		}
	}

	private int clear(File fileCacheDir, long expiredPeriod) throws Exception {
		int counter = 0;
		long now = System.currentTimeMillis();

		File[] list = fileCacheDir.listFiles();
		if (list == null || list.length == 0)
			return 0;

		Arrays.sort(list, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return Long.valueOf(f1.lastModified()).compareTo(
						f2.lastModified());
			}
		});

		File target;

		if (list != null && list.length > 0) {

			for (int index = 0; index < list.length; index++) {
				target = list[index];
				if (target.getName().endsWith("linkedtable.map"))
					continue;

				if (target.lastModified() + expiredPeriod <= now) {
					target.delete();
					counter++;
				}
			}
		}

		return counter;
	}

	private void setIcon(Context context, String parenturl, String iconUrl,
			int sz, final byte[] data) throws Exception {
		init(context);
		linkedTable.put(parenturl, iconUrl);
		final String name = iconUrl.hashCode() + "-" + sz + ".png";

		File dst = new File(context.getCacheDir(), name);
		if (!dst.exists()) {
			FileOutputStream outStream = new FileOutputStream(dst);
			FileChannel outChannel = outStream.getChannel();
			ByteBuffer byteBuffer = ByteBuffer.wrap(data);
			outChannel.write(byteBuffer);
			outChannel.close();
			outStream.close();
		}

		File file = new File(context.getCacheDir(), "linkedtable.map");
		if (file.exists())
			file.delete();
		FileOutputStream f = new FileOutputStream(file);
		ObjectOutputStream s = new ObjectOutputStream(f);
		s.writeObject(linkedTable);
		s.close();

	}

	public byte[] getIcon(Context context, final String url, int sz)
			throws Exception {
		init(context);
		String name = linkedTable.get(url);
		if (TextUtils.isEmpty(name))
			return null;

		name = name.hashCode() + "-" + sz + ".png";

		final File dst = new File(context.getCacheDir(), name);
		if (!dst.exists()) {
			if (linkedTable.containsKey(url))
				linkedTable.remove(url);
			return null;
		}
		FileInputStream inStream = new FileInputStream(dst);
		FileChannel inChannel = inStream.getChannel();
		ByteBuffer buffer = ByteBuffer.allocate((int) inChannel.size());
		inChannel.read(buffer);
		buffer.rewind();
		inChannel.close();
		inStream.close();
		return buffer.array();

	}

}
