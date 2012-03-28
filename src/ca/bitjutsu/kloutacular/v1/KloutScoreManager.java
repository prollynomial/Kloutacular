package ca.bitjutsu.kloutacular.v1;

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
	private HashMap<String, KloutProfile> mProfiles;
	private ArrayList<OnProfileUpdatedListener> mUpdateListeners;
	private ExecutorService mExecutor;
	private String mApiKey;
	
	private KloutScoreManager(String apiKey, boolean debug) {
		mApiKey = apiKey;
		mIsDebugMode = debug;
		mUpdateListeners = new ArrayList<OnProfileUpdatedListener>();
		mProfiles = new HashMap<String, KloutProfile>();
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
			profileUpdate(mProfiles.get(screenName));
		} else {
			mExecutor.execute(new KloutScoreFetcher(screenName));
		}
	}
	
	/**
	 * Checks if we have a Klout score for the Twitter screen name in question.
	 * @param screenName the Twitter screen name to check
	 * @return whether or not we have a Klout score stored for this screen name
	 */
	//TODO: decide whether this is public or private
	private boolean haveScore(String screenName) {
		return (mProfiles.get(screenName) != null && mProfiles.get(screenName).getScore() > 0);
	}
	
	/**
	 * Convenience method for {@link #requestTopics(String, boolean)}.
	 * @param screenName the user to request topics for
	 */
	public void requestTopics(String screenName) {
		requestTopics(screenName, false);
	}
	
	/**
	 * Request a user's top topics.  Beware: the API will not always return the same number of topics.
	 * @param screenName the user to request topics for
	 * @param forceRequery force a refresh of the user's topics
	 */
	public void requestTopics(String screenName, boolean forceRequery) {
		if (haveTopics(screenName) && !forceRequery) {
			profileUpdate(mProfiles.get(screenName));
		} else {
			mExecutor.execute(new KloutTopicsFetcher(screenName));
		}
	}
	
	//TODO: should this be public or private?
	private boolean haveTopics(String screenName) {
		return (mProfiles.get(screenName) != null && mProfiles.get(screenName).getTopics() != null);
	}
	
	/**
	 * Convenience method for {@link #requestUser(String, boolean)}.
	 * @param screenName the user who we want user information about
	 */
	public void requestUser(String screenName) {
		requestUser(screenName, false);
	}
	
	/**
	 * Requests a user object from the Klout API.  A user object includes the score, score breakdown
	 * (amplification, network, true reach), class, class id, class description, user
	 * description, 1- and 5-day deltas, and the slope of the score graph.
	 * @param screenName the user who we want user information about
	 * @param forceRequery force a refresh of the user's information
	 */
	public void requestUser(String screenName, boolean forceRequery) {
		if (haveUser(screenName) && !forceRequery) {
			profileUpdate(mProfiles.get(screenName));
		} else {
			mExecutor.execute(new KloutUserFetcher(screenName));
		}
	}
	
	//TODO: should this be public or private?
	//XXX: this is not checking if we have the screen name mapped, this is checking if we have a "user object" returned by the Klout API
	private boolean haveUser(String screenName) {
		return (mProfiles.get(screenName) != null && mProfiles.get(screenName).getKloutClassDescription() != null);
	}
	
	/**
	 * Convenience method for {@link #requestInfluencedBy(String, boolean)}.
	 * @param screenName the user whose influencers are being requested
	 */
	public void requestInfluencedBy(String screenName) {
		requestInfluencedBy(screenName, false);
	}
	
	/**
	 * Request the user's influencers.
	 * @param screenName screenName the user whose influencers are being requested
	 * @param forceRequery force a refresh of the user's influencers
	 */
	public void requestInfluencedBy(String screenName, boolean forceRequery) {
		if (haveInfluencedBy(screenName) && !forceRequery) {
			profileUpdate(mProfiles.get(screenName));
		} else {
			mExecutor.execute(new KloutInfluencerFetcher(screenName, true));
		}
	}
	
	//TODO: should this be public or private?
	private boolean haveInfluencedBy(String screenName) {
		return (mProfiles.get(screenName) != null && mProfiles.get(screenName).getInfluencedBy() != null);
	}
	
	/**
	 * Convenience method for {@link #requestInfluencerOf(String, boolean)}. 
	 * @param screenName the user whose influencees are being requested
	 */
	public void requestInfluencerOf(String screenName) {
		requestInfluencerOf(screenName, false);
	}
	
	/**
	 * Request the user's influencees
	 * @param screenName the user whose influencees are being requested
	 * @param forceRequery force a refresh of the user's influencees
	 */
	public void requestInfluencerOf(String screenName, boolean forceRequery) {
		if (haveInfluencerOf(screenName) && !forceRequery) {
			profileUpdate(mProfiles.get(screenName));
		} else {
			mExecutor.execute(new KloutInfluencerFetcher(screenName, false));
		}
	}
	
	//TODO: should this be public or private?
	private boolean haveInfluencerOf(String screenName) {
		return (mProfiles.get(screenName) != null && mProfiles.get(screenName).getInfluencerOf() != null);
	}
	
	private void setApiKey(String apiKey) {
		mApiKey = apiKey;
	}
	
	private void profileUpdate(KloutProfile score) {
		for (OnProfileUpdatedListener l : mUpdateListeners) {
			l.onUpdate(score);
		}
	}
	
	/**
	 * Add a listener to be notified when scores are updated.
	 * @param l the listener to be registered
	 */
	public void addOnProfileUpdatedListener(OnProfileUpdatedListener l) {
		mUpdateListeners.add(l);
	}

	private void setDebugMode(boolean debug) {
		mIsDebugMode = debug;
	}
	
	private JSONObject makeRequest(String url) throws ClientProtocolException, IOException, IllegalStateException, JSONException {
		HttpGet get = new HttpGet(url);
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
		
		return new JSONObject(sb.toString());
	}

	/**
	 * 
	 * Listener interface for Klout profile updates.
	 * 
	 * @author Adam Carruthers (adam.carruthers@bitjutsu.ca)
	 * @since 0.1
	 * 
	 */
	public interface OnProfileUpdatedListener {
		/**
		 * Passes the result of the most recently <b>returned</b> request to the Klout API.  Be sure to check that this is the Klout profile
		 * you are trying to receive, as KloutScoreManager uses multiple worker threads, and therefore can return out of request order.
		 * @param kp the most recently retrieved KloutScore
		 */
		public void onUpdate(KloutProfile kp);
	}
	
	private class KloutScoreFetcher implements Runnable {
		private String mScreenName;
		
		public KloutScoreFetcher(String screenName) {
			mScreenName = screenName;
		}
		
		@Override
		public void run() {
			try {
				JSONObject obj = makeRequest("http://api.klout.com/1/klout.json?users=" + mScreenName + "&key=" + mApiKey);
				JSONArray users = obj.getJSONArray("users");
				
				double score = users.getJSONObject(0).getDouble("kscore");
				
				//Put the new score in the map, unless we already have a mapping and we're just adding/updating the score
				if (mProfiles.get(mScreenName) != null) {
					mProfiles.get(mScreenName).updateScore(score);
					profileUpdate(mProfiles.get(mScreenName));
				} else {
					KloutProfile k = new KloutProfile(mScreenName, score);
					mProfiles.put(mScreenName, k);
					profileUpdate(k);
				}
				
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
	
	private class KloutTopicsFetcher implements Runnable {
		private String mScreenName;
		
		public KloutTopicsFetcher(String screenName) {
			mScreenName = screenName;
		}
		
		@Override
		public void run() {
			try {
				JSONObject obj = makeRequest("http://api.klout.com/1/users/topics.json?users=" + mScreenName + "&key=" + mApiKey);
				JSONArray users = obj.getJSONArray("users");
				
				JSONArray topx = users.getJSONObject(0).getJSONArray("topics");
				ArrayList<String> topics = new ArrayList<String>();
				for (int i = 0; i < topx.length(); i++) {
					topics.add(topx.getString(i));
				}
				
				// Put the new topics in the map, unless we already have a mapping and we're just adding/updating the topics
				if (mProfiles.get(mScreenName) != null) {
					mProfiles.get(mScreenName).updateTopics(topics);
					profileUpdate(mProfiles.get(mScreenName));
				} else {
					KloutProfile k = new KloutProfile(mScreenName, topics);
					mProfiles.put(mScreenName, k);
					profileUpdate(k);
				}
				
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
	
	private class KloutUserFetcher implements Runnable {
		private String mScreenName;
		
		public KloutUserFetcher(String screenName) {
			mScreenName = screenName;
		}
		
		@Override
		public void run() {
			try {
				JSONObject obj = makeRequest("http://api.klout.com/1/users/show.json?users=" + mScreenName + "&key=" + mApiKey);
				JSONObject score = obj.getJSONArray("users").getJSONObject(0).getJSONObject("score");
				
				double kscore = score.getDouble("kscore");
				double slope = score.getDouble("slope");
				String userDesc = score.getString("description");
				int kclassid = score.getInt("kclass_id");
				String kclass = score.getString("kclass");
				String kclassDesc = score.getString("kclass_description");
				double network = score.getDouble("network_score");
				double amp = score.getDouble("amplification_score");
				double reach = score.getDouble("true_reach");
				double delta1 = score.getDouble("delta_1day");
				double delta5 = score.getDouble("delta_5day");
				
				// Put the new user in the map, unless we already have a mapping and we're just adding/updating the user
				if (mProfiles.get(mScreenName) != null) {
					mProfiles.get(mScreenName).updateUser(kscore, slope, userDesc, kclassid, kclass, kclassDesc,
															network, amp, reach, delta1, delta5);
					profileUpdate(mProfiles.get(mScreenName));
				} else {
					KloutProfile k = new KloutProfile(mScreenName, kscore, slope, userDesc, kclassid, kclass, kclassDesc,
														network, amp, reach, delta1, delta5);
					mProfiles.put(mScreenName, k);
					profileUpdate(k);
				}
				
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
	
	private class KloutInfluencerFetcher implements Runnable {
		private String mScreenName;
		private boolean mIsInfluencedBy;
		
		public KloutInfluencerFetcher(String screenName, boolean isInfluencedBy) {
			mScreenName = screenName;
			mIsInfluencedBy = isInfluencedBy;
		}
		
		@Override
		public void run() {
			try {
				JSONObject obj = makeRequest("http://api.klout.com/1/soi/influence" + (mIsInfluencedBy ? "d_by" : "r_of") + ".json?users=" + mScreenName + "&key=" + mApiKey);
				JSONArray influenceUsers = obj.getJSONArray("users").getJSONObject(0).getJSONArray(mIsInfluencedBy ? "influencers" : "influencees");
				
				ArrayList<KloutProfile> profiles = new ArrayList<KloutProfile>();
				for (int i = 0; i < influenceUsers.length(); i++) {
					String screenName = influenceUsers.getJSONObject(i).getString("twitter_screen_name");
					double kscore = influenceUsers.getJSONObject(i).getDouble("kscore");
					KloutProfile k = new KloutProfile(screenName, kscore);
					profiles.add(k);
					//Add to the map for future queries
					mProfiles.put(screenName, k);
				}
				
				// Put the new user in the map, unless we already have a mapping and we're just adding/updating the user
				if (mProfiles.get(mScreenName) != null) {
					if (mIsInfluencedBy)
						mProfiles.get(mScreenName).updateInfluencedBy(profiles);
					else
						mProfiles.get(mScreenName).updateInfluencerOf(profiles);
					
					profileUpdate(mProfiles.get(mScreenName));
				} else {
					KloutProfile k = new KloutProfile(mScreenName, profiles, mIsInfluencedBy);
					mProfiles.put(mScreenName, k);
					profileUpdate(k);
				}
				
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