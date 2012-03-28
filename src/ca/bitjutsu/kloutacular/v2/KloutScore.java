package ca.bitjutsu.kloutacular.v2;

/**
 * 
 * KloutScore is a data model that contains the user's Twitter screen name,
 * their Klout score, and all of the Klout sub-scores.
 * 
 * @author Adam Carruthers (adam.carruthers@bitjutsu.ca)
 * 
 */
public class KloutScore {
	private String mScreenName;
	private double mScore;
	private double mAmplification;
	private double mTrueReach;
	private double mNetwork;
	
	/**
	 * Constructor
	 * @param screenName the user's Twitter screen name
	 * @param score the user's Klout score
	 * @param amplification the user's amplification score
	 * @param trueReach the user's true reach score
	 * @param network the user's network score
	 */
	public KloutScore(String screenName, double score, double amplification, double trueReach, double network) {
		mScreenName = screenName;
		mScore = score;
		mAmplification = amplification;
		mTrueReach = trueReach;
		mNetwork = network;
	}
	
	/**
	 * Obtain the user's screen name
	 * @return the user's Twitter screen name
	 */
	public String getScreenName() {
		return mScreenName;
	}
	
	public double getScore() {
		return mScore;
	}
	
	public double getAmplification() {
		return mAmplification;
	}
	
	public double getTrueReach() {
		return mTrueReach;
	}
	
	public double getNetwork() {
		return mNetwork;
	}
	
	public void updateScore(double score, double amplification, double trueReach, double network) {
		mScore = score;
		mAmplification = amplification;
		mTrueReach = trueReach;
		mNetwork = network;
	}
}
