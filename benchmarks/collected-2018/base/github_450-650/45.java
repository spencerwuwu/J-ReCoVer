// https://searchcode.com/api/result/71330750/

package Mapred;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

public class TranslateReducer extends
		Reducer<Text, CompositeValueFormatTranslate, Text, Text> {

	List<String> parties, people, hashTags,translations;
	String matches[];

	@Override
	protected void setup(Context context) throws IOException,
			InterruptedException {

		parties = new ArrayList<String>(Arrays.asList(context.getConfiguration().get("parties").toString().split("\n")));
		people = new ArrayList<String>(Arrays.asList(context.getConfiguration().get("people").toString().split("\n")));
		hashTags = new ArrayList<String>(Arrays.asList(context.getConfiguration().get("hashTags").toString().split("\n")));
		matches = context.getConfiguration().get("xmlToSearch").toString().split("\n");
		translations = new ArrayList<String>(Arrays.asList(context.getConfiguration().get("translations").toString().split("\n")));
		
		context.write(new Text(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?> <All>"), null);

	}

	@Override
	protected void cleanup(Context context) throws IOException,
			InterruptedException {
		context.write(new Text("</All>"), null);
	}

	public void reduce(Text key,
			Iterable<CompositeValueFormatTranslate> values, Context context)
			throws IOException, InterruptedException {

		try {
			// List<String> Tweets = new ArrayList<String>();
			for (CompositeValueFormatTranslate value : values) {

				// outputKey.set(constructPropertyXml(key, value));
				// Tweets.addAll(value.getTweet());
				// System.out.println(value.getScreenName());
				String tweetsRaw = value.getTweet();
				String screenName = value.getScreenName();
				String tweetsSplit[] = tweetsRaw.split("\n");
				String tweets = new String();
				for (String s : tweetsSplit) {
					tweets += s.replaceAll("[^a-zA-Z ]", "") + " |\n";
				}

				List<String> TweetsRaw = new ArrayList<String>(
						Arrays.asList(tweetsRaw.split("\n")));
				List<String> Tweets = new ArrayList<String>(
						Arrays.asList(tweets.split("\n")));
				List<String> ScreenName = new ArrayList<String>(
						Arrays.asList(screenName.split("\n")));
				String locations = value.getLocation();
				Pattern pat = Pattern.compile("\\[(.*?)\\]");
				Matcher matcher = pat.matcher(locations);
				List<String> finalLocations = new ArrayList<String>();
				while(matcher.find()){
					finalLocations.add(matcher.group(1));
				}
//				System.out.println(locations);
				// List<String> TranslatedText = value.getTranslatedText();
				CompositeValueFormatTranslate cvf = new CompositeValueFormatTranslate();

				HttpPost httppost = new HttpPost(
						"http://localhost/google_translate1.php");

				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						1);
				nameValuePairs.add(new BasicNameValuePair("text", Tweets
						.toString().replaceAll("[^a-zA-Z |]", "")));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// System.out.println(nameValuePairs);
				List<String> TranslatedText = new ArrayList<String>();
				DefaultHttpClient httpclient = new DefaultHttpClient();
				httpclient
						.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
							public long getKeepAliveDuration(
									HttpResponse response, HttpContext context) {
								// Honor 'keep-alive' header
								HeaderElementIterator it = new BasicHeaderElementIterator(
										response.headerIterator(HTTP.CONN_KEEP_ALIVE));
								while (it.hasNext()) {
									HeaderElement he = it.nextElement();
									String param = he.getName();
									String value = he.getValue();
									if (value != null
											&& param.equalsIgnoreCase("timeout")) {
										try {
											return Long.parseLong(value) * 1000;
										} catch (NumberFormatException ignore) {
										}
									}
								}
								HttpHost target = (HttpHost) context
										.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
								if ("www.naughty-server.com"
										.equalsIgnoreCase(target.getHostName())) {
									// Keep alive for 5 seconds only
									return 5 * 1000;
								} else {
									// otherwise keep alive for 30 seconds
									return 30 * 1000 * 1000;
								}
							}

						});
				HttpResponse response = httpclient.execute(httppost);
				// HttpEntity entity = response.getEntity();
				// if (entity != null) {
				// long len = entity.getContentLength();
				// if (len != -1) {
				// String rep = URLDecoder.decode(EntityUtils.toString(
				// entity).replaceAll(" ", ""));
				// Pattern p = Pattern.compile("\\[(.*?)\\]");
				// Matcher m = p.matcher(rep);
				//
				// while (m.find()) {
				// rep = m.group(1);
				// }
				// String ind[] = rep
				// .split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				// for (String s : ind)
				// TranslatedText.add(s.trim());
				// // as = new ArrayList<String>(Arrays.asList());
				// } else {
				// // Stream content out
				// }

				// }
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						response.getEntity().getContent()));
				String line = "";
				String lin = "";
				while ((line = rd.readLine()) != null) {
					// System.out.println(line);
					lin += line.replaceAll("\\[+", "[");

				}
				String rep = null;
				Pattern pattern = Pattern.compile("\\[(.*?)\\]");
				Matcher m = pattern.matcher(lin);
				String translatedCombined = new String();
				while (m.find()) {
					rep = m.group(1);
					String ind[] = rep.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
					translatedCombined += ind[0].replaceAll("^\"|\"$|\\r|\\n",
							"");
				}
				// System.out.println(translatedCombined);
				// // String ind[] =
				// rep.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				// // for (String s : ind)
				// as.add(s.trim());

				String seperatedText[] = translatedCombined.split("\\|");
				for (String s : seperatedText) {
					TranslatedText.add(s.trim());
					// System.out.println("Translated : " + s.trim());
				}
				// System.out.println("Original : " + Tweets.size() +
				// " ScreenName: " + ScreenName.size() + "Translated + " +
				// TranslatedText.size());
				if (TranslatedText.size() != finalLocations.size()) {
					System.out.println("Locations ");
					for (String s : finalLocations) {
						System.out.println(s);
					}
					System.out.println("Translated ");
					for (String s : TranslatedText) {
						System.out.println(s);
					}
				}
				for (int l = 0; l < TranslatedText.size(); l++) {
					boolean found = false;
					boolean tweetFound=false;
					List<String> Parties = new ArrayList<String>();
					List<String> People = new ArrayList<String>();
					List<String> HashTags = new ArrayList<String>();
//					for (int i = 0; i < matches.length; i++) {
//						boolean Partyadded = false;
//						boolean Peopleadded = false;
//						boolean HashTagadded = false;
//						String spl[] = matches[i].split(" ");
//						String match = matches[i].replaceAll(" ", "");
//						for (int j = 0; j < spl.length; j++) {
//							if (!spl[j].toLowerCase().equals("the")
//									|| spl[j].toLowerCase().equals("of"))
//								if (TweetsRaw.get(l).toLowerCase()
//										.contains(spl[j].toLowerCase())) {
//									found = true;
//								}
//						}
//						if (TweetsRaw.get(l).toLowerCase()
//								.contains(match.toLowerCase()))
//							found = true;
//						if (found) {
//							for (int j = 0; j < spl.length; j++) {
//								if (!(spl[j].toLowerCase().equals("the")
//										|| spl[j].toLowerCase().equals("of"))) {
//									if (contains(parties, spl[j].toLowerCase())
//											&& TweetsRaw
//													.get(l)
//													.toLowerCase()
//													.contains(
//															spl[j].toLowerCase())) {
//										if(containsList(parties,matches[i].toLowerCase()))
//										Parties.add(matches[i]);
//									}
//									if (contains(people, spl[j].toLowerCase())
//											&& TweetsRaw
//													.get(l)
//													.toLowerCase()
//													.contains(
//															spl[j].toLowerCase())) {
//										
//										if(containsList(people,matches[i].toLowerCase()))
//										People.add(matches[i]);
//										Peopleadded = true;
//									}
//									if (contains(hashTags, spl[j].toLowerCase())
//											&& TweetsRaw
//													.get(l)
//													.toLowerCase()
//													.contains(
//															spl[j].toLowerCase())) {
//										if(containsList(hashTags,matches[i].toLowerCase()))
//										HashTags.add(matches[i]);
//										HashTagadded = true;
//									}
//								}
//							}
//						}
//					}
					
					for(int i=0;i<parties.size();i++){
						found=false;

						String match = parties.get(i).replaceAll(" ", "");
						if(TweetsRaw.get(l).toLowerCase().contains(match.toLowerCase())){

							found=true;
						}
						if(TweetsRaw.get(l).toLowerCase().contains(parties.get(i).toLowerCase())){

							found=true;
						}
						if(found){
							tweetFound=true;
							if(!(containsList(Parties,parties.get(i)))){

								Parties.add(parties.get(i));
							}
						}
					}
					for(int i=0;i<people.size();i++){
						found=false;

						String match = people.get(i).replaceAll(" ", "");
						String sp[] = people.get(i).split(" ");
						if(TweetsRaw.get(l).toLowerCase().contains(match.toLowerCase())){
							found=true;
						}
						if(TweetsRaw.get(l).toLowerCase().contains(people.get(i).toLowerCase())){
							found=true;
						}
						for(int j=0;j<sp.length;j++){
							if(TweetsRaw.get(l).toLowerCase().contains(sp[j].toLowerCase()))
								found=true;
						}
						if(found){
							tweetFound=true;
							if(!(containsList(People,people.get(i)))){
								People.add(people.get(i));
							}
						}
					}
					for(int i=0;i<hashTags.size();i++){
						found=false;
						String match = hashTags.get(i).replaceAll(" ", "");

						if(TweetsRaw.get(l).toLowerCase().contains(match.toLowerCase())){

							found=true;
						}
						if(TweetsRaw.get(l).toLowerCase().contains(hashTags.get(i).toLowerCase())){

							found=true;
						}
						if(found){
							tweetFound=true;
							if(!(containsList(HashTags,hashTags.get(i)))){

								HashTags.add(hashTags.get(i));
							}
						}
					}
					

					// Sentiment

					if (tweetFound) {
						boolean translated = false;
						BufferedReader in = null;
						String inputLine;
						// String translatedText = new String();
						int countG = 0;
						// System.out.println("Translated " +
						// TranslatedText.get(l) + "Original " + Tweets.get(l));
						URL translate = new URL(
								"http://localhost:8604/v1/sentence/"
										+ URLEncoder
												.encode(TranslatedText.get(l))
												.replaceAll("%23|%3F|%2F", "")
												.replaceAll(
														"http%3A%2F%2F[^ ]+",
														"") + ".json");
						String jsonText = new String();
						URLConnection yc = translate.openConnection();
						in = new BufferedReader(new InputStreamReader(
								yc.getInputStream()));
						while ((inputLine = in.readLine()) != null)
							jsonText += inputLine;

						JSONObject json = new JSONObject(jsonText);

						List<String> list = new ArrayList<String>();
						list.add("python");
						list.add("/twitter/translate.py");
						list.add("\""
								+ TranslatedText.get(l).replaceAll(
										"[^\\x00-\\x7F]", "") + "\"");
						String s;
						String secondsentiment = new String();
						ProcessBuilder b = new ProcessBuilder(list);
						int count = 0;
						while (count <= 10) {
							Process p = b.start();
							BufferedReader stdInput = new BufferedReader(
									new InputStreamReader(p.getInputStream()));
							BufferedReader stdError = new BufferedReader(
									new InputStreamReader(p.getErrorStream()));
							s = stdInput.readLine();
							if (s == null) {
								System.out.println("Failed : "
										+ TranslatedText.get(l).replaceAll(
												"[^\\x00-\\x7F]", "")
										+ "Original:" + Tweets.get(l));
								count++;
								String s2;
								while ((s2 = stdError.readLine()) != null)
									System.out.println(s2);
								continue;
							}
							String arr[] = s.split(":");
							String arr1[] = arr[arr.length - 1].split("}");
							secondsentiment = arr1[0];
							break;
						}
						String finalSentiment = new String();
						float jsonSentiment = Float.parseFloat(json.get(
								"sentiment").toString());
						// float jsonSentiment = 4 ;
						if (secondsentiment.trim().equals("\'neutral\'"))
							if (jsonSentiment > 0)
								finalSentiment = new String("positive");
							else if (jsonSentiment < 0)
								finalSentiment = new String("negative");
							else
								finalSentiment = new String("neutral");
						if (secondsentiment.trim().equals("\'positive\'"))
							finalSentiment = new String("positive");
						if (secondsentiment.trim().equals("\'negative\'"))
							finalSentiment = new String("negative");
						Text outputKey = new Text();
						removeDuplicates(Parties);
						removeDuplicates(People);
						removeDuplicates(HashTags);
						
						
						outputKey.set(constructPropertyXml(key, TweetsRaw
								.get(l), ScreenName.get(l), finalSentiment,
								Parties, People, HashTags,finalLocations.get(l), Float
										.parseFloat(json.get("certainty")
												.toString())));
						context.write(outputKey, null);
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String constructPropertyXml(Text name, String Tweet,
			String ScreenName, String finalSentiment, List<String> Parties,
			List<String> People, List<String> HashTags,String finalLocation, Float certainty) {
		StringBuilder sb = new StringBuilder();
		sb.append("<Tweet><name>").append(ScreenName)
				.append("</name>");
		
		sb.append("<gpe ");
		if(!finalLocation.trim().equals("Not Available")){
				String latLang[] = finalLocation.trim().split(",");
				if(latLang.length>0)
					sb.append("lat=\"").append(latLang[0]).append("\" long=\"").append(latLang[1]).append("\" ");
		}
		sb.append("></gpe><Concerning>");
//		System.out.println(People);
		if (Parties.size() != 0) {

			sb.append("<Parties>");

			for (String s : Parties) {
				sb.append("<Party>");
				String translatedParty ;
				if((translatedParty = contains(translations,s))!=null)
					sb.append(translatedParty);
				else
					sb.append(s);
				sb.append("</Party>");
			}

			sb.append("</Parties>");
		}
		if (People.size() != 0) {

			sb.append("<Candidates>");

			for (String s : People) {
				sb.append("<Candidate>").append(s).append("</Candidate>");
			}

			sb.append("</Candidates>");
		}
		if (HashTags.size() != 0) {

			sb.append("<HashTags>");

			for (String s : HashTags) {
				sb.append("<HashTag>").append(s).append("</HashTag>");
			}

			sb.append("</HashTags>");
		}
		sb.append("</Concerning><Text>").append(Tweet.replaceAll("|$", ""))
				.append("</Text><Sentiment>").append(finalSentiment)
				.append("</Sentiment><Certainty>").append(certainty)
				.append("</Certainty></Tweet>");
		return sb.toString();
	}

	String contains(List<String> list, String word) {
		for (String s : list) {
			if(s.toLowerCase().trim().contains(word.toLowerCase()))
				return s.split("-")[0];
		}
		return null;
	}
	
	static boolean containsList(List<String> list, String word) {
		for (String s : list) {
				if(s.toLowerCase().trim().equals(word))
					return true;
			}
		return false;
	}

	public static void removeDuplicates(List<String> list) {
		HashSet<String> set = new HashSet<String>(list);
		list.clear();
		list.addAll(set);
	}

}

