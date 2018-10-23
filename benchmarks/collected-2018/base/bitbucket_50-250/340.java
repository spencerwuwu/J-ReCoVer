// https://searchcode.com/api/result/124199715/

/*
 * Copyright 2011 Kevin Formsma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arothian.repomanager.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import oauth.signpost.exception.OAuthException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;

import android.net.Uri;

import com.arothian.repomanager.exception.RepoManException;

/**
 * Abstract implementation and interface for a repository 
 * provider. A provider is a data source of repository information.
 * This provides the API used to access the information within the app.
 * 
 * @author Kevin Formsma
 *
 */
public abstract class ProviderManager implements Serializable {
	private static final long serialVersionUID = -3199066602671691021L;

	//Buffer for processing http responses
	private static byte[] buff = new byte[1024];

	//The user with access to the provider
    protected ProviderUser user;
    
	public abstract ProviderType getProviderType();
	public abstract Uri getProviderAuthenticationURL() throws RepoManException;
	public abstract ProviderUser retrieveAccessToken(Uri result) throws RepoManException;
	
    //Data retrieval
    public abstract List<Repository> getUserRepositories() throws RepoManException;
    public abstract List<Repository> getFollowingRepositories() throws RepoManException;
    public abstract List<Repository> searchForRepositories(String query) throws RepoManException;
    public abstract DetailedRepository getDetailedRepository(Repository repo) throws RepoManException;
    public abstract ArrayList<? extends Changeset> getChangesetsForRepository(Repository repo) throws RepoManException;
    public abstract ArrayList<? extends Changeset> getChangesetsForRepository(Repository repo, String branch) throws RepoManException;
    public abstract Changeset getMostRecentChangesetForRepository(String repoURL) throws RepoManException;
    public abstract DetailedChangeset getDetailedChangeset(String repoURL, String changesetID) throws RepoManException;
    public abstract String getUnifiedDiff(String repoURL, File file, String changesetID, String parentChangesetID) throws RepoManException;
    public abstract ArrayList<Issue> getIssuesForRepository(Repository repo) throws RepoManException;
    public abstract ArrayList<String> getBranchesForRepository(Repository repo) throws RepoManException;
	public abstract DetailedIssue getDetailedIssue(String repoURL, Issue issue) throws RepoManException;
	
	//This stuff is only used by oauth 1.0a
    public String getRequestToken() {
        return "";
    }
    public String getRequestTokenSecret() {
        return "";
    }
    public void setRequestToken(String requestToken, String requestTokenSecret) {
    }
	
    public void setUser(ProviderUser user) {
        this.user = user;
    }
    public ProviderUser getUser() {
        return user;
    }
	
	//Shared functions
	protected HttpGet generateAPIGetRequest(String url) throws OAuthException {
        HttpGet request = new HttpGet(url);
        request.addHeader("Accept-Encoding", "gzip"); //Compressed responses reduce network usage
        return request;
    }
    
	protected HttpPost generateAPIPostRequest(String url) throws OAuthException {
        HttpPost request = new HttpPost(url);
        request.addHeader("Accept-Encoding", "gzip"); //Compressed responses reduce network usage
        return request;
    }
    
    protected HttpPut generateAPIPutRequest(String url) throws OAuthException {
        HttpPut request = new HttpPut(url);
        request.addHeader("Accept-Encoding", "gzip"); //Compressed responses reduce network usage
        return request;
    }
    
    protected HttpDelete generateAPIDeleteRequest(String url) throws OAuthException {
        HttpDelete request = new HttpDelete(url);
        request.addHeader("Accept-Encoding", "gzip"); //Compressed responses reduce network usage
        return request;
    }
    
    //Helper method to handle the http stream response and decompress it if in gzip format
    protected String retrieveJSONResponse(HttpResponse response) throws IOException {
        InputStream inputStream = null;
        try {
            Header encoding = response.getFirstHeader("Content-Encoding");
            if(encoding != null && "gzip".equalsIgnoreCase(encoding.getValue())) {
                inputStream = new GZIPInputStream(response.getEntity().getContent());
            } else {
                inputStream = response.getEntity().getContent();
            }
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            
            int readCount = 0;
            while ((readCount = inputStream.read(buff)) != -1) {
                content.write(buff, 0, readCount);
            }
            
            return new String(content.toByteArray());
        } finally {
            if(inputStream != null) {
                try { inputStream.close(); } catch (IOException e) {}
            }
        }
    }
}

