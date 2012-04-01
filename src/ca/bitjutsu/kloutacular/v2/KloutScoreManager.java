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
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * KloutScoreManager fetches, stores, caches, and returns Klout scores for users.
 * 
 * The standard usage pattern is as follows:<br><br>
 * <code>
 * KloutScoreManager mKloutMan = KloutScoreManager.getInstance(API_KEY);<br>
 * mKloutMan.addOnScoreUpdatedListener(new KloutScoreManager.OnScoreUpdatedListener() {<br>
 * &nbsp;&nbsp;public void onReceive(KloutScore k) {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;//Do stuff with the received KloutScore<br>
 * &nbsp;&nbsp;}<br>
 * });<br>
 * mKloutMan.requestKlout("bitjutsu");<br>
 * </code>
 * 
 * @author Adam Carruthers (adam.carruthers@bitjutsu.ca)
 * 
 */
public class KloutScoreManager {
	// "If that was a drug deal, I would have shot Hotel Luxury Linens in the face." -- Aziz Ansari.
	//TODO: pick a number of threads that isn't just an arbitrary choice
	private static final int THREAD_COUNT = 10;
	
	private static KloutScoreManager sInstance;
	private boolean mIsDebugMode;
	private HashMap<String, String> mIdMapping;
	private HashMap<String, KloutScore> mScores;
	private ArrayList<OnScoreUpdatedListener> mUpdateListeners;
	private ExecutorService mExecutor;
	private String mApiKey;
	
	private KloutScoreManager(String apiKey, boolean debug) {
		mApiKey = apiKey;
		mIsDebugMode = debug;
		mUpdateListeners = new ArrayList<OnScoreUpdatedListener>();
		//TODO: retrieve mappings from disk
		mIdMapping = new HashMap<String, String>();
		mScores = new HashMap<String, KloutScore>();
		mExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
	}
	
	/**
	 * Convenience method for {@link #getInstance(String, boolean)}.
	 * @param apiKey the Klout API key to use
	 * @return the global KloutScoreManager instance
	 */
	public static KloutScoreManager getInstance(String apiKey) {
		return getInstance(apiKey, false);
	}
	
	/**
	 * Obtain the global KloutScoreManager instance.
	 * @param apiKey the Klout API key to use
	 * @param debug specify whether we should print load times to the log
	 * @return the global KloutScoreManager instance
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
		// If we already have a cached score, and we aren't requerying, return the cached score
		if (haveScore(screenName) && !forceRequery) {
			scoreUpdate(mScores.get(screenName));
		} else if (haveScore(screenName) && forceRequery) {
			mExecutor.execute(new KloutScoreFetcher(screenName));
		}
		
		// If we already have the screenName -> Klout ID pair, no need to request ID from Klout
		if (getKloutId(screenName) != -1L) {
			mExecutor.execute(new KloutScoreFetcher(screenName));
		} else {
			//Fetch the Klout ID
			mExecutor.execute(new KloutIdFetcher(screenName));
		}
		
		
	}
	
	private void setApiKey(String apiKey) {
		mApiKey = apiKey;
	}
	
	private void scoreUpdate(KloutScore score) {
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
		return (mScores.get(screenName) != null);
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
		 * you are trying to receive, as KloutScoreManager uses multiple worker threads, and therefore can return out of request order.
		 * @param ks the most recently retrieved KloutScore
		 */
		public void onReceive(KloutScore ks);
	}
	
	private class KloutIdFetcher implements Runnable {
		private String mScreenName;
		
		public KloutIdFetcher(String screenName) {
			mScreenName = screenName;
		}
		
		@Override
		public void run() {
			//map Klout ID to Twitter name
			try {
				HttpGet get = new HttpGet("http://api.klout.com/v2/identity.json/tw?screenName=" + mScreenName + "&key=" + mApiKey);
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
				
				mIdMapping.put(mScreenName, kloutId);
				
				//Notify the system that we want to fetch the Klout score for this user
				mExecutor.execute(new KloutScoreFetcher(mScreenName));
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class KloutScoreFetcher implements Runnable {
		private String mScreenName;
		
		public KloutScoreFetcher(String screenName) {
			mScreenName = screenName;
		}
		
		@Override
		public void run() {
			try {
				String id = mIdMapping.get(mScreenName);
				if (id == null)
					throw new IllegalArgumentException("No mapping exists for \"" + mScreenName + "\"");
				
				HttpGet get = new HttpGet("http://api.klout.com/v2/user.json/" + id + "/score&key=" + mApiKey);
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
				double amplification = obj.getDouble("amplification");
				double trueReach = obj.getDouble("trueReach");
				double network = obj.getDouble("network");
				
				KloutScore k = new KloutScore(mScreenName, score, amplification, trueReach, network);
				//Put the new score in the map, unless we already have a mapping and we're just updating the score
				if (mScores.get(mScreenName) != null)
					mScores.get(mScreenName).updateScore(score, amplification, trueReach, network);
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
