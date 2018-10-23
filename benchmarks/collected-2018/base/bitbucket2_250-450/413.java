// https://searchcode.com/api/result/56393645/

package se.svt.textanalysis.saplo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.LoggerFactory;

import com.voltvoodoo.saplo4j.Saplo;
import com.voltvoodoo.saplo4j.exception.SaploException;
import com.voltvoodoo.saplo4j.http.JsonResponseHandler;
import com.voltvoodoo.saplo4j.model.SaploCorpus;
import com.voltvoodoo.saplo4j.model.SaploDocument;
import java.util.List;

import se.svt.textanalysis.Article;
import se.svt.textanalysis.ArticleFactory;
import se.svt.textanalysis.DAO;
import se.svt.textanalysis.Params;
import se.svt.textanalysis.opencalais.A;

//	client = SaploJSONClient("5158f7344a55a74a980af2a88c6ade40","03cf9ddfc87caeb5e3dbe255ffde1dfb")

public class SaploDAO extends DAO {
	String sessionId;
	public SaploDAO() throws UnsupportedEncodingException, IOException {
		super();
		logger = LoggerFactory.getLogger(this.getClass());
		
		createSession();
	}
	
	private void createSession() throws IOException,
			UnsupportedEncodingException {
		SaploParams p = new SaploParams();
		p.setMethod("auth.createSession");
		p.addParameter(p.getPublicAPIKey());
		p.addParameter(p.getSecretAPIKey());
		sessionId = (String)parseResult(getContent(p,getConnection(p))).get(0).get("result");
		logger.info(sessionId);
	}

	/**
	 * Parses the resulting JSON from a Saplo API call.
	 * I made this static to reduce the amount of Saplo calls made during tests.
	 * 
	 * @param content
	 * @return a list of maps containing values. 
	 * If there's only one JSONObject, it's added to the map. 
	 * If there's only a simple result (ie a boolean or long), it's added to the map with the key "result" 
	 */
	protected static ArrayList<HashMap<String, Object>> parseResult(String content) {
		ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> map=null;// = new HashMap<String, Object>();
		
		LoggerFactory.getLogger(SaploDAO.class).info("Parsing result: " + content);

		JSONObject json = null;
		
		try {
			json = (JSONObject)JSONValue.parseWithException(content);
		} catch (ParseException ex) {
			LoggerFactory.getLogger(SaploDAO.class).warn("Failed to parse JSON content: "+content);
			return list;
		}
		
		Object e = json.get("error");
		
		if (e!=null) {
			LoggerFactory.getLogger(SaploDAO.class).warn("SaploDAO returned an error: "+content);
			return list;
		}
		
		Object r = json.get("result");
		
		if (r instanceof JSONArray) {
			JSONArray resArray = (JSONArray)r;
			for (Object o: resArray) {
				map = new HashMap<String, Object>();
				map.putAll((Map<? extends String, ? extends Object>)o);
				list.add(map);
			}
		} else if (r instanceof JSONObject) {
			JSONObject resObj  = (JSONObject)r;
			map = new HashMap<String, Object>();
			map.putAll((Map<? extends String, ? extends Object>)resObj);
			list.add(map);			
		} else {
			map = new HashMap<String, Object>();
			map.put("result",r);
			list.add(map);
		}
		return list;
	}

//	public SaploDocument shootAtSaplo(SaploParams params) throws SaploException {
//		Saplo saplo = new Saplo(params.getPublicAPIKey(), params.getSecretAPIKey());
//		return saplo.getDocument(new SaploCorpus.Id(183L), new SaploDocument.Id(7L));
//	}
	/*
corpus.addArticle

    Add a new article to a corpus.
    
Name 	Type 	Description 	
corpusId 	int 	The unique id for the corpus you want to add the article to. 	required
headline 	string 	Article title
lead 	string 	Article lead text. Max 500 chars
body 	string 	Article body text 	required
publishDate 	timestamp 	The date for when the article was published. (YYYY-MM-DD HH:MM:SS, i.e. 2010-01-24 12:23:44)
publishUrl 	string 	The url for where the article can be found on internet
authors 	string 	The authors of the article
lang 	string 	The language for the article. English and Swedish are supported and specified according to ISO 639-1 (http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes). Swedish = "sv", English = "en". 	required

Response Parameters:
Name 	Type 	Description
articleId 	int 	The id for the added article in the corpus.

 
 */
	
	public Long addArticle(int corpusId, Article a) throws UnsupportedEncodingException, IOException {
		SaploParams p = new SaploParams(sessionId);
		p.addParameter(corpusId);
		p.addParameter(a.getTitle());
		p.addParameter(a.getLead());
		p.addParameter(a.getBody());
//		p.addParameter(a.getPublishDate());
		p.addParameter(ArticleFactory.getDateFormat().format(a.getPublishDate()));
		p.addParameter('"'+a.getUrl().toString()+'"');
		p.addParameter(a.getAuthor());
		p.addParameter("sv");
		p.setMethod("corpus.addArticle");
		
		String content = getContent(p, getConnection(p));

		return (Long)parseResult(content).get(0).get("articleId");
	}
	/*
corpus.getArticle

    Gives information about a saved article.
    Request Parameters:
Name 	Type 	Description 	
corpusId 	int 	The unique id for the corpus where the article exists. 	required
articleId 	int 	The id for the article you want to get. 	required

Response Parameters:
Name 	Type 	Description
headline 	string 	Article title
publishUrl 	string 	The url for where the article can be found on internet 
    */
	
	public HashMap<String, Object> getArticle(long corpusId, long saploArticleId) throws UnsupportedEncodingException, IOException {
		SaploParams p = new SaploParams(sessionId);

		p.addParameter(corpusId);
		p.addParameter(saploArticleId);
		p.setMethod("corpus.getArticle");
		
		String content = getContent(p, getConnection(p));

		return parseResult(content).get(0);
	}
	
/*
corpus.updateArticle 
Request Parameters:
Name 	Type 	Description 	
corpusId 	int 	The unique id for the corpus where the article exists. 	required
articleId 	int 	The id for the article you want to update in the provided corpus. 	required
headline 	string 	Article title
lead 	string 	Article lead text. Max 500 chars
body 	string 	Article body text 	required
publishDate 	timestamp 	The date for when the article was published. (YYYY-MM-DD HH:MM:SS, i.e. 2010-01-24 12:23:44)
publishUrl 	string 	The url for where the article can be found on internet
authors 	string 	The authors of the article
lang 	string 	The language for the article. English and Swedish are supported and specified according to ISO 639-1 (http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes). Swedish = "sv", English = "en". 	required

Response Parameters:
Name 	Type 	Description
true/false 	boolean 	
*/
	
	public boolean updateArticle(long corpusId, Article article) throws UnsupportedEncodingException, IOException {
		SaploParams p = new SaploParams(sessionId);

		p.addParameter(corpusId);
		p.addParameter(article.getSaploarticleid());
		p.addParameter(article.getTitle());
		p.addParameter(article.getLead());
		p.addParameter(article.getBody());
		p.addParameter(article.getPublishDate());
		p.addParameter(article.getUrl());
		p.addParameter(article.getAuthor());
		p.addParameter("sv");
		p.setMethod("corpus.updateArticle");
		
		String content = getContent(p, getConnection(p));

		return (Boolean)parseResult(content).get(0).get("result");
	}

        public List<HashMap<String, Object>> createCorpus(String name, String description, String lang) throws IOException{
                SaploParams p = new SaploParams(sessionId);
                p.addParameter(name);
                p.addParameter(description);
                p.addParameter(lang);
                p.setMethod("corpus.createCorpus");

                String content = getContent(p, getConnection(p));

                return parseResult(content);
        }

        /**
         * Get a list of all corpuses and read/write permissions for them.
         * @return A list of maps, each containing the keys: "corpusId", "permissionId"
         */
        public List<HashMap<String, Object>> getPermissions() throws IOException{
                SaploParams p = new SaploParams(sessionId);
                p.setMethod("corpus.getPermissions");

                String content = getContent(p, getConnection(p));

                return parseResult(content);
        }

        /**
         * Get info about a corpus.
         * @param corpusid The corpus id, must be a number (Long).
         * @return A list containing a map with the keys: "corpusName", "corpusDescription", "lang","nextArticleId"
         */
        public List<HashMap<String, Object>> getInfo(String corpusid) throws IOException{
                SaploParams p = new SaploParams(sessionId);
                p.addParameter(corpusid);
                p.setMethod("corpus.getInfo");

                String content = getContent(p, getConnection(p));

                return parseResult(content);
        }


        public List<HashMap<String, Object>> similarArticles(long corpusId, Article article) throws IOException{
                SaploParams p = new SaploParams(sessionId);

		p.addParameter(corpusId);
		p.addParameter(article.getSaploarticleid());
                p.addParameter(120); //Wait
                p.addParameter(50); //maxNumberOfResults
                p.addParameter(0.2);//minThreshold
                p.addParameter(0.99);//maxThreshold
                p.setMethod("match.getSimilarArticles");

                String content = getContent(p, getConnection(p));

                return parseResult(content);
        }

        /*
tags.getEntityTags

    Gives you the persons, organisations and geographical names (places) that are mentioned within an article/text. 
Request Parameters:
Name 	Type 	Description 	
corpusId 	int 	The id for the corpus where the source article exists. 	required
articleId 	int 	The id for the source article. 	required
wait 	int 	Maximum time you want to wait for a result to be calculated. Maximum 120 sec. If you set 0 you will only start the process and can check out the result later. 	required

Response Parameters:
Name 	Type 	Description 	Example
tagId 	int 	A unique id combined with corpusId and articleId for the tag result	1
tagWord 	string 	The tag word 	Saplo
tagTypeId 	int 	An id that represents which category the tag exists in. 	5 
*/
	
	public ArrayList<SaploTag> getEntityTags(long corpusId, long saploArticleId) throws UnsupportedEncodingException, IOException {
		SaploParams p = new SaploParams(sessionId);

		p.addParameter(corpusId);
		p.addParameter(saploArticleId);
		p.addParameter(120); // wait time.
		p.setMethod("tags.getEntityTags");
		
		String content = getContent(p, getConnection(p));

		ArrayList<HashMap<String,Object>> response = parseResult(content);
		ArrayList<SaploTag> result  = new ArrayList<SaploTag>();
		for (HashMap<String,Object> map: response) {
			result.add(new SaploTag((Long)map.get("tagId"), (String)map.get("tagWord"), (Long)map.get("tagTypeId")));
		}
		
		return result;
	}
/*

tags.addTag

    Adds a tag to the result for an article. 
Name 	 Type 	 Description  	
corpusId 	int 	The id for the corpus where the source article exists. 	required
articleId 	int 	The id for the source article. 	required
tagWord 	string 	The tag word 	Saplo
tagTypeId 	int 	An id that represents which category the tag exists in. 	5

See Tag type ids for more information about used tag type ids

Response Parameters:
Name 	Type 	Description 	Example
tagId 	int 	A unique id combined with corpusId and articleId for the added tag 	4 
*/
	public long addTag(long corpusId, long articleId, String tagWord, SaploTag.SaploTagTypes tagType) throws UnsupportedEncodingException, IOException {
		SaploParams p = new SaploParams(sessionId);

		p.addParameter(corpusId);
		p.addParameter(articleId);
		p.addParameter(tagWord);
		p.addParameter(tagType.ordinal());
		p.setMethod("tags.addTag");
		
		String content = getContent(p, getConnection(p));
		return (Long) parseResult(content).get(0).get("tagId");
	}
/*
tags.deleteTag

    Deletes an existing tag from the result.
    tags.deleteTag

Delete an existing entity tag from the result set.

Request Parameters:
Name 	Type 	Description 	
corpusId 	int 	The id for the corpus where the source article exists. 	required
articleId 	int 	The id for the source article. 	required
tagId 	int 	A unique id combined with corpusId and articleId for the added tag 	4

Response Parameters:
Name 	Type 	Description 	Example
tagId 	int 	A unique id combined with corpusId and articleId for the added tag 	4  
*/
	public void deleteTag(long corpusId, long articleId, long tagId) throws UnsupportedEncodingException, IOException {
		SaploParams p = new SaploParams(sessionId);
		
		p.addParameter(corpusId);
		p.addParameter(articleId);
		p.addParameter(tagId);
		p.setMethod("tags.deleteTag");
		
		String content = getContent(p, getConnection(p));
	}
/*
	TODO later

tags.updateTag

    Updates an existing tag. 
tags.getTag

    Get a tag from the result for an article. 

    TODO later
match.getSimilarArticles

    Searches your corpus for articles that has a similar semantic meaning as your source article. 

match.deleteMatch

    Deletes an article from the result set. 

match.getMatch

    Get the corpusId and articleId for a matchId for a source article. 
	 */
}

