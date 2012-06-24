package ca.bitjutsu.kloutacular.v2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * KloutProfileManager fetches, stores, caches, and returns Klout scores for users.
 * 
 * The standard usage pattern is as follows:<br><br>
 * <code>
 * KloutProfileManager mKloutMan = KloutProfileManager.getInstance(API_KEY);<br>
 * mKloutMan.addOnScoreUpdatedListener(new KloutProfileManager.OnScoreUpdatedListener() {<br>
 * &nbsp;&nbsp;public void onReceive(KloutProfile k) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;//Do stuff with the received KloutProfile<br>
 * &nbsp;&nbsp;}<br>
 * });<br>
 * mKloutMan.requestKlout("bitjutsu");<br>
 * </code>
 * 
 * @author Adam Carruthers (adam.carruthers@bitjutsu.ca)
 * 
 */
public class KloutScoreManager {
	//TODO: pick a number of threads that isn't just an arbitrary choice
	private static final int THREAD_COUNT = 10;
	
	private static KloutScoreManager sInstance;
	private boolean mIsDebugMode;
	private HashMap<String, String> mIdMapping;
	private HashMap<String, KloutProfile> mScores;
	private ArrayList<OnScoreUpdatedListener> mUpdateListeners;
	private ExecutorService mExecutor;
	private String mApiKey;
	
	private KloutScoreManager(String apiKey, boolean debug) {
		mApiKey = apiKey;
		// TODO: print times for requests and such if debug = true
		mIsDebugMode = debug;
		mUpdateListeners = new ArrayList<OnScoreUpdatedListener>();
		//TODO: retrieve mappings from disk
		mIdMapping = new HashMap<String, String>();
		mScores = new HashMap<String, KloutProfile>();
		mExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
	}
	
	/**
	 * Convenience method for {@link #getInstance(String, boolean)}.
	 * @param apiKey the Klout API key to use
	 * @return the global KloutProfileManager instance
	 */
	public static KloutScoreManager getInstance(String apiKey) {
		return getInstance(apiKey, false);
	}
	
	/**
	 * Obtain the global KloutProfileManager instance.
	 * @param apiKey the Klout API key to use
	 * @param debug specify whether we should print load times to the log
	 * @return the global KloutProfileManager instance
	 */
	public static KloutScoreManager getInstance(String apiKey, boolean debug) {
		if (sInstance == null)
			sInstance = new KloutScoreManager(apiKey, debug);
		
		sInstance.setApiKey(apiKey);
		sInstance.setDebugMode(debug);
		return sInstance;
	}
	
	/**
	 * Convenience method for {@link #requestKlout(String, boolean)}.
	 * @param screenName the Twitter handle of the user
	 */
	public void requestKlout(String screenName) {
		requestKlout(screenName, false);
	}
	
	/**
	 * Request a user's Klout score.
	 * @param screenName the Twitter handle of the user
	 * @param forceRequery force a refresh of the Klout score
	 */
	public void requestKlout(String screenName, boolean forceRequery) {
		// TODO: check age of score and requery if necessary
		// If we already have a cached score, and we aren't requerying, return the cached score
		if (haveScore(screenName) && !forceRequery) {
			scoreUpdate(mScores.get(screenName));
		} else {
			mExecutor.execute(new KloutProfileFetcher(screenName));
		}
	}
	
	/**
	 * Convenience method for {@link #requestInfluence(String, boolean)}.
	 * @param screenName the Twitter handle of the user
	 */
	public void requestInfluence(String screenName) {
		requestInfluence(screenName, false);
	}
	
	/**
	 * Request a user's influencers and influencees.
	 * @param screenName the Twitter handle of the user
	 * @param forceRequery force a refresh of the Klout score
	 */
	public void requestInfluence(String screenName, boolean forceRequery) {
		// TODO: check age of influencers and requery if necessary
		if (haveInfluence(screenName) && !forceRequery) {
			scoreUpdate(mScores.get(screenName));
		} else {
			mExecutor.execute(new InfluenceFetcher(screenName));
		}
	}
	
	/**
	 * Convenience method for {@link #requestTopics(String, boolean)}.
	 * @param screenName the Twitter handle of the user
	 */
	public void requestTopics(String screenName) {
		requestTopics(screenName, false);
	}
	
	/**
	 * Request a user's topics.
	 * @param screenName the Twitter handle of the user
	 * @param forceRequery force a refresh of the Klout score
	 */
	public void requestTopics(String screenName, boolean forceRequery) {
		// TODO: check age of topics and requery if necessary
		if (haveTopics(screenName) && !forceRequery) {
			scoreUpdate(mScores.get(screenName));
		} else {
			mExecutor.execute(new TopicsFetcher(screenName));
		}
	}
	
	private void setApiKey(String apiKey) {
		mApiKey = apiKey;
	}
	
	private void scoreUpdate(KloutProfile score) {
		for (OnScoreUpdatedListener l : mUpdateListeners) {
			l.onReceive(score);
		}
	}
	
	/**
	 * Add a listener to be notified when scores are updated.
	 * @param l the listener to be registered
	 */
	public void addOnScoreUpdatedListener(OnScoreUpdatedListener l) {
		mUpdateListeners.add(l);
	}
	
	private long getKloutId(String screenName) {
		//XXX: this only returns a mapping that we already have
		return (mIdMapping.get(screenName) != null) ? Long.valueOf(mIdMapping.get(screenName)) : -1L;
	}

	/**
	 * Checks if we have a Klout score for the Twitter screen name in question.
	 * @param screenName the Twitter screen name to check
	 * @return whether or not we have a Klout score stored for this screen name
	 */
	//TODO: decide whether this is public or private
	private boolean haveScore(String screenName) {
		return (mScores != null && mScores.size() != 0 && mScores.get(screenName).getScore() != -1);
	}
	
	private boolean haveInfluence(String screenName) {
		// We only have to see if either influencers or influencees exist, as they can't be fetched separately
		return (mScores != null && mScores.size() != 0 &&  mScores.get(screenName).getInfluencers() != null);
	}
	
	private boolean haveTopics(String screenName) {
		return (mScores != null && mScores.size() != 0 &&  mScores.get(screenName).getTopics() != null);
	}

	private void setDebugMode(boolean debug) {
		mIsDebugMode = debug;
	}

	/**
	 * 
	 * Listener interface for Klout score updates.
	 * 
	 * @author Adam Carruthers (adam.carruthers@bitjutsu.ca)
	 * 
	 */
	public interface OnScoreUpdatedListener {
		/**
		 * Passes the result of the most recently <b>returned</b> request to the Klout API.  Be sure to check that this is the Klout score
		 * you are trying to receive, as KloutProfileManager uses multiple worker threads, and therefore can return out of request order.
		 * @param ks the most recently retrieved KloutProfile
		 */
		public void onReceive(KloutProfile ks);
	}
	
	// Should not ever be called on the main thread.
	private void fetchKloutId(String screenName) {
		//map Klout ID to Twitter name
		try {
			HttpGet get = new HttpGet("http://api.klout.com/v2/identity.json/twitter?screenName=" + screenName + "&key=" + mApiKey);
			HttpResponse response = new DefaultHttpClient().execute(get);

			if (response.getStatusLine().getStatusCode() != 200)
				throw new HttpResponseException(response.getStatusLine().getStatusCode(),
		        		"Remote server responded with code " + response.getStatusLine().getStatusCode());
			
			BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}
			
			in.close();
			
			JSONObject id = new JSONObject(sb.toString());
			
			String kloutId = id.getString("id");
			
			mIdMapping.put(screenName, kloutId);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private class KloutProfileFetcher implements Runnable {
		private String mScreenName;
		
		public KloutProfileFetcher(String screenName) {
			mScreenName = screenName;
		}
		
		@Override
		public void run() {
			try {
				if (getKloutId(mScreenName) == -1L) {
					// If we don't have a Klout ID for the user, fetch it.
					fetchKloutId(mScreenName);
				}
				
				String id = mIdMapping.get(mScreenName);
				// IllegalArgumentException should never get called.
				if (id == null)
					throw new IllegalArgumentException("No mapping exists for \"" + mScreenName + "\"");
				
				HttpGet get = new HttpGet("http://api.klout.com/v2/user.json/" + id + "/score?key=" + mApiKey);
				HttpResponse response = new DefaultHttpClient().execute(get);

				if (response.getStatusLine().getStatusCode() != 200)
					throw new HttpResponseException(response.getStatusLine().getStatusCode(),
			        		"Remote server responded with code " + response.getStatusLine().getStatusCode());
				
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					sb.append(line);
				}
				
				in.close();
				
				JSONObject obj = new JSONObject(sb.toString());
				
				double score = obj.getDouble("score");
				
				JSONObject scoreDelta = obj.getJSONObject("scoreDelta");
				double daychange = scoreDelta.getDouble("dayChange");
				double weekchange = scoreDelta.getDouble("weekChange");
				double monthchange = scoreDelta.getDouble("monthChange");
				
				KloutProfile k = new KloutProfile(mScreenName, score, daychange, weekchange, monthchange);
				//Put the new score in the map, unless we already have a mapping and we're just updating the score
				if (mScores.get(mScreenName) != null)
					mScores.get(mScreenName).updateScore(score, daychange, weekchange, monthchange);
				else
					mScores.put(mScreenName, k);
				//Call all of the listeners.
				scoreUpdate(k);
				
			} catch (HttpResponseException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class InfluenceFetcher implements Runnable {
		private String mScreenName;
		
		public InfluenceFetcher(String screenName) {
			mScreenName = screenName;
		}
		
		@Override
		public void run() {
			try {
				if (getKloutId(mScreenName) == -1L) {
					// If we don't have a Klout ID for the user, fetch it.
					fetchKloutId(mScreenName);
				}
				
				String id = mIdMapping.get(mScreenName);
				// IllegalArgumentException should never get called.
				if (id == null)
					throw new IllegalArgumentException("No mapping exists for \"" + mScreenName + "\"");
				
				HttpGet get = new HttpGet("http://api.klout.com/v2/user.json/" + id + "/influence?key=" + mApiKey);
				HttpResponse response = new DefaultHttpClient().execute(get);

				if (response.getStatusLine().getStatusCode() != 200)
					throw new HttpResponseException(response.getStatusLine().getStatusCode(),
			        		"Remote server responded with code " + response.getStatusLine().getStatusCode());
				
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					sb.append(line);
				}
				
				in.close();
				
				JSONObject obj = new JSONObject(sb.toString());
				
				//populate influencers
				ArrayList<KloutProfile> influencers = new ArrayList<KloutProfile>();
				JSONArray influencersArray = obj.getJSONArray("myInfluencers");
				
				for (int i = 0; i < influencersArray.length(); i++) {
					// skip over id in entity, the one in payload is identical
					JSONObject payload = influencersArray.getJSONObject(i).getJSONObject("entity").getJSONObject("payload");
					String kloutId = payload.getString("kloutId");
					String screenName = payload.getString("nick");
					double score = payload.getJSONObject("score").getDouble("score");
					
					//TODO: SCORE DELTAS
					
					// add the KloutProfiles to mScores, mIdMapping, and influencers
					mIdMapping.put(screenName, kloutId);
					KloutProfile temp = new KloutProfile(screenName, score);
					mScores.put(screenName, temp);
					influencers.add(temp);
				}
				
				//populate influencees
				ArrayList<KloutProfile> influencees = new ArrayList<KloutProfile>();
				JSONArray influenceesArray = obj.getJSONArray("myInfluencees");
				
				for (int i = 0; i < influenceesArray.length(); i++) {
					// skip over id in entity, the one in payload is identical
					JSONObject payload = influenceesArray.getJSONObject(i).getJSONObject("entity").getJSONObject("payload");
					String kloutId = payload.getString("kloutId");
					String screenName = payload.getString("nick");
					double score = payload.getJSONObject("score").getDouble("score");
					
					//TODO: SCORE DELTAS
					
					// add the KloutProfiles to mScores, mIdMapping, and influencers
					mIdMapping.put(screenName, kloutId);
					KloutProfile temp = new KloutProfile(screenName, score);
					mScores.put(screenName, temp);
					influencees.add(temp);
				}
				
				KloutProfile k = new KloutProfile(mScreenName, influencers, influencees);
				//Put the new score in the map, unless we already have a mapping and we're just updating the score
				if (mScores.get(mScreenName) != null)
					mScores.get(mScreenName).updateScore(influencers, influencees);
				else
					mScores.put(mScreenName, k);
				//Call all of the listeners.
				scoreUpdate(k);
				
			} catch (HttpResponseException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class TopicsFetcher implements Runnable {
		private String mScreenName;
		
		public TopicsFetcher(String screenName) {
			mScreenName = screenName;
		}
		
		@Override
		public void run() {
			try {
				if (getKloutId(mScreenName) == -1L) {
					// If we don't have a Klout ID for the user, fetch it.
					fetchKloutId(mScreenName);
				}
				
				String id = mIdMapping.get(mScreenName);
				// IllegalArgumentException should never get called.
				if (id == null)
					throw new IllegalArgumentException("No mapping exists for \"" + mScreenName + "\"");
				
				HttpGet get = new HttpGet("http://api.klout.com/v2/user.json/" + id + "/topics?key=" + mApiKey);
				HttpResponse response = new DefaultHttpClient().execute(get);

				if (response.getStatusLine().getStatusCode() != 200)
					throw new HttpResponseException(response.getStatusLine().getStatusCode(),
			        		"Remote server responded with code " + response.getStatusLine().getStatusCode());
				
				BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					sb.append(line);
				}
				
				in.close();
				
				JSONArray arr = new JSONArray(sb.toString());
				
				ArrayList<Topic> topics = new ArrayList<Topic>();
				
				for (int i = 0; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					
					long topicId = obj.getLong("id");
					String name = obj.getString("name");
					String displayName = obj.getString("displayName");
					String slug = obj.getString("slug");
					String imageUrl = obj.getString("imageUrl");
					
					topics.add(new Topic(topicId, name, displayName, slug, imageUrl));
				}
				
				KloutProfile k = new KloutProfile(mScreenName, topics);
				//Put the new score in the map, unless we already have a mapping and we're just updating the score
				if (mScores.get(mScreenName) != null)
					mScores.get(mScreenName).updateScore(topics);
				else
					mScores.put(mScreenName, k);
				//Call all of the listeners.
				scoreUpdate(k);
			} catch (HttpResponseException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
}
