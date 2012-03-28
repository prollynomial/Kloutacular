package ca.bitjutsu.kloutacular.v1;

import java.util.ArrayList;

/**
 * A data model for holding a user's Klout information.  This includes their influencers, who
 * they are influenced by, their Twitter handle, their Klout score, their Klout score breakdown
 * (amplification, network, true reach) their Klout class (networker, observer, etc), the
 * descriptions of their class, as well as their 1- and 5-day score changes.
 * 
 * @author Adam Carruthers (adam.carruthers@bitjutsu.ca)
 *
 */
public class KloutProfile {
	private String mScreenName;
	private double mScore;
	private double mAmplification;
	private double mNetwork;
	private double mTrueReach;
	private ArrayList<KloutProfile> mInfluencedBy;
	private ArrayList<KloutProfile> mInfluencerOf;
	private double mSlope;
	private String mUserDescription;
	private String mClass;
	private String mClassDescription;
	private int mClassId;
	private double mOneDayDelta;
	private double mFiveDayDelta;
	private ArrayList<String> mTopics;
	
	/**
	 * Constructor used for basic Klout score requests.
	 * @param screenName the user's Twitter handle
	 * @param score the user's Klout score
	 */
	/* package */ KloutProfile(String screenName, double score) {
		mScreenName = screenName;
		mScore = score;
	}
	
	/**
	 * Constructor used for "influencer" requests.
	 * @param screenName
	 * @param influencerOf
	 */
	/* package */ KloutProfile(String screenName, ArrayList<KloutProfile> influencers, boolean isInfluencedBy) {
		mScreenName = screenName;
		if (isInfluencedBy)
			mInfluencedBy = influencers;
		else
			mInfluencerOf = influencers;
	}
	
	/**
	 * Constructor for "user" requests.
	 * @param screenName
	 * @param score
	 * @param slope
	 * @param userDesc
	 * @param kclassId
	 * @param kclass
	 * @param kclassDesc
	 * @param network
	 * @param amp
	 * @param reach
	 * @param delta1
	 * @param delta5
	 */
	/* package */ KloutProfile(String screenName, double score, double slope, String userDesc, int kclassId, String kclass,
								String kclassDesc, double network, double amp, double reach, double delta1, double delta5) {
		mScreenName = screenName;
		mScore = score;
		mSlope = slope;
		mUserDescription = userDesc;
		mClassId = kclassId;
		mClass = kclass;
		mClassDescription = kclassDesc;
		mNetwork = network;
		mAmplification = amp;
		mTrueReach = reach;
		mOneDayDelta = delta1;
		mFiveDayDelta = delta5;
	}
	
	/**
	 * Constructor used for "topics" requests.
	 * @param screenName
	 * @param topics
	 */
	/* package */ KloutProfile(String screenName, ArrayList<String> topics) {
		mScreenName = screenName;
		mTopics = topics;
	}
	
	/* package */ void updateUser(double score, double slope, String userDesc, int kclassId, String kclass,
								String kclassDesc, double network, double amp, double reach, double delta1, double delta5) {
		mScore = score;
		mSlope = slope;
		mUserDescription = userDesc;
		mClassId = kclassId;
		mClass = kclass;
		mClassDescription = kclassDesc;
		mNetwork = network;
		mAmplification = amp;
		mTrueReach = reach;
		mOneDayDelta = delta1;
		mFiveDayDelta = delta5;
	}
	
	/* package */ void updateScore(double score) {
		mScore = score;
	}
	
	/* package */ void updateInfluencedBy(ArrayList<KloutProfile> influencedBy) {
		mInfluencedBy = influencedBy;
	}
	
	/* package */ void updateInfluencerOf(ArrayList<KloutProfile> influencerOf) {
		mInfluencerOf = influencerOf;
	}
	
	/* package */ void updateTopics(ArrayList<String> topics) {
		mTopics = topics;
	}
	
	/**
	 * Get the user's screen name.
	 * @return the user's screen name
	 */
	public String getScreenName() {
		return mScreenName;
	}
	
	/**
	 * Get the user's Klout score.
	 * @return the user's Klout score or <code>0.0</code> if {@link KloutScoreManager#requestUser(String)}
	 * or {@link KloutScoreManager#requestKlout(String)} were not used to fetch this user's information. 
	 */
	public double getScore() {
		return mScore;
	}
	
	/**
	 * Get the users who are influenced by this user.
	 * @return the users who are influenced by this user or <code>null</code> if
	 * {@link KloutScoreManager#requestInfluencedBy(String)} was not used to fetch this user's information. 
	 */
	public ArrayList<KloutProfile> getInfluencedBy() {
		return mInfluencedBy;
	}
	
	/**
	 * Get the users who influence this user.
	 * @return the users who are influenced by this user or <code>null</code> if
	 * {@link KloutScoreManager#requestInfluencerOf(String)} was not used to fetch this user's information. 
	 */
	public ArrayList<KloutProfile> getInfluencerOf() {
		return mInfluencerOf;
	}
	
	/**
	 * Get the slope of the user's Klout score graph.
	 * @return the user's graph slope or <code>0.0</code> if {@link KloutScoreManager#requestUser(String)}
	 * was not used to fetch this user's information.
	 */
	public double getSlope() {
		return mSlope;
	}
	
	/**
	 * Get the user's amplification score.
	 * @return the user's amplification score or <code>0.0</code> if {@link KloutScoreManager#requestUser(String)}
	 * was not used to fetch this user's information.
	 */
	public double getAmplification() {
		return mAmplification;
	}
	
	/**
	 * Get the user's true reach score.
	 * @return the user's true reach score or <code>0.0</code> if {@link KloutScoreManager#requestUser(String)}
	 * was not used to fetch this user's information.
	 */
	public double getTrueReach() {
		return mTrueReach;
	}
	
	/**
	 * Get the user's network score.
	 * @return the user's network score or <code>0.0</code> if {@link KloutScoreManager#requestUser(String)}
	 * was not used to fetch this user's information.
	 */
	public double getNetwork() {
		return mNetwork;
	}
	
	/**
	 * Get the user's Klout class ID, which corresponds to their Klout class ({@link #getKloutClass()}).
	 * @return the user's Klout class ID or <code>0</code> if {@link KloutScoreManager#requestUser(String)}
	 * was not used to fetch this user's information.
	 */
	public int getKloutClassId() {
		return mClassId;
	}
	
	/**
	 * Get the user's five day delta; their Klout score change over the last five days.
	 * @return the user's five day delta or <code>0.0</code> if {@link KloutScoreManager#requestUser(String)}
	 * was not used to fetch this user's information.
	 */
	public double getFiveDayDelta() {
		return mFiveDayDelta;
	}
	
	/**
	 * Get the user's one day delta; their Klout score change over the last day.
	 * @return the user's one day delta or <code>0.0</code> if {@link KloutScoreManager#requestUser(String)}
	 * was not used to fetch this user's information.
	 */
	public double getOneDayDelta() {
		return mOneDayDelta;
	}
	
	/**
	 * Get the user's Klout class description for their corresponding Klout class (networker, observer, etc).
	 * @return the user's Klout class description or <code>null</code> if {@link KloutScoreManager#requestUser(String)}
	 * was not used to fetch this user's information.
	 */
	public String getKloutClassDescription() {
		return mClassDescription;
	}
	
	/**
	 * Get the user's description according to Klout.
	 * @return the user's description or <code>null</code> if {@link KloutScoreManager#requestUser(String)}
	 * was not used to fetch this user's information.
	 */
	public String getUserDescription() {
		return mUserDescription;
	}
	
	/**
	 * Get the user's Klout class (networker, observer, etc).
	 * @return the user's Klout class or <code>null</code> if {@link KloutScoreManager#requestUser(String)}
	 * was not used to fetch this user's information.
	 */
	public String getKloutClass() {
		return mClass;
	}
	
	/**
	 * Get the topics that the user is influential about.
	 * @return the user's topics or <code>null</code> if {@link KloutScoreManager#requestTopics(String)}
	 * was not used to fetch this user's information.
	 */
	public ArrayList<String> getTopics() {
		return mTopics;
	}
}
