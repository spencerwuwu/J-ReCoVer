// https://searchcode.com/api/result/71330657/

package Mapred;
//package Mapred;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.URI;
//import java.net.URL;
//import java.net.URLConnection;
//import java.net.URLDecoder;
//import java.net.URLEncoder;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import org.apache.hadoop.io.LongWritable;
//import org.apache.hadoop.io.Text;
//
//import org.apache.hadoop.mapreduce.Reducer;
//import org.apache.http.HeaderElement;
//import org.apache.http.HeaderElementIterator;
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpHost;
//import org.apache.http.HttpResponse;
//import org.apache.http.NameValuePair;
//import org.apache.http.client.entity.UrlEncodedFormEntity;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.client.utils.URIBuilder;
//import org.apache.http.conn.ConnectionKeepAliveStrategy;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.message.BasicHeaderElementIterator;
//import org.apache.http.message.BasicNameValuePair;
//import org.apache.http.protocol.ExecutionContext;
//import org.apache.http.protocol.HTTP;
//import org.apache.http.protocol.HttpContext;
//import org.apache.http.util.EntityUtils;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//public class TranslateCombine
//		extends
//		Reducer<LongWritable, CompositeValueFormatTranslate, LongWritable, CompositeValueFormatTranslate> {
//
//	List<String> parties, people, hashTags;
//	String matches[];
//	boolean found;
//
//	@Override
//	protected void setup(Context context) throws IOException,
//			InterruptedException {
//		parties = new ArrayList<String>(Arrays.asList(context
//				.getConfiguration().get("parties").toString().split("\n")));
//		people = new ArrayList<String>(Arrays.asList(context.getConfiguration()
//				.get("people").toString().split("\n")));
//		hashTags = new ArrayList<String>(Arrays.asList(context
//				.getConfiguration().get("hashTags").toString().split("\n")));
//		matches = context.getConfiguration().get("xmlToSearch").toString()
//				.toLowerCase().split("\n");
//		found = false;
//	}
//
//	public void reduce(LongWritable key,
//			Iterable<CompositeValueFormatTranslate> values, Context context)
//			throws IOException, InterruptedException {
//		try {
//			for (CompositeValueFormatTranslate value : values) {
//				System.out.println(key);
//				List<String> ScreenName = value.getScreenName();
//				List<String> Tweets = value.getTweet();
//				CompositeValueFormatTranslate cvf = new CompositeValueFormatTranslate();
//				DefaultHttpClient httpclient = new DefaultHttpClient();
//				httpclient
//						.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
//							public long getKeepAliveDuration(
//									HttpResponse response, HttpContext context) {
//								// Honor 'keep-alive' header
//								HeaderElementIterator it = new BasicHeaderElementIterator(
//										response.headerIterator(HTTP.CONN_KEEP_ALIVE));
//								while (it.hasNext()) {
//									HeaderElement he = it.nextElement();
//									String param = he.getName();
//									String value = he.getValue();
//									if (value != null
//											&& param.equalsIgnoreCase("timeout")) {
//										try {
//											return Long.parseLong(value) * 1000;
//										} catch (NumberFormatException ignore) {
//										}
//									}
//								}
//								HttpHost target = (HttpHost) context
//										.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
//								if ("www.naughty-server.com"
//										.equalsIgnoreCase(target.getHostName())) {
//									// Keep alive for 5 seconds only
//									return 5 * 1000;
//								} else {
//									// otherwise keep alive for 30 seconds
//									return 30 * 1000 * 1000;
//								}
//							}
//
//						});
//				
//				HttpPost httppost = new HttpPost("http://localhost/google_translate.php");
//				
//				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
//		        nameValuePairs.add(new BasicNameValuePair("text", Tweets.toString()));
//		        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//				
//				// System.out.println(httpget.getURI());
//				List<String> TranslatedText = new ArrayList<String>();
//
//				HttpResponse response = httpclient.execute(httppost);
//				HttpEntity entity = response.getEntity();
//				if (entity != null) {
//					long len = entity.getContentLength();
//					if (len != -1) {
//						String rep = URLDecoder.decode(EntityUtils.toString(
//								entity).replaceAll(" ", ""));
//						Pattern p = Pattern.compile("\\[(.*?)\\]");
//						Matcher m = p.matcher(rep);
//
//						while (m.find()) {
//							rep = m.group(1);
//						}
//						String ind[] = rep
//								.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
//						for (String s : ind)
//							TranslatedText.add(s.trim());
//						// as = new ArrayList<String>(Arrays.asList());
//					} else {
//						// Stream content out
//					}
//				}
//				cvf.setTweet(Tweets);
//				cvf.setScreenName(ScreenName);
//				cvf.setTranslatedText(TranslatedText);
//				context.write(key, cvf);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//}

