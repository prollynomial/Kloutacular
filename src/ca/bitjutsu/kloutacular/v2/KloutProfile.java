package ca.bitjutsu.kloutacular.v2;

import java.util.ArrayList;

/**
 * 
 * A data model that contains the user's Twitter screen name,
 * as well as their Klout score, influencers, influencees, and topics.
 * 
 * @author Adam Carruthers (adam.carruthers@bitjutsu.ca)
 * 
 */
public class KloutProfile {
	private String mScreenName;
	private double mScore;
	private double mDayChange;
	private double mWeekChange;
	private double mMonthChange;
	private ArrayList<KloutProfile> mInfluencedBy;
	private ArrayList<KloutProfile> mInfluencerOf;
	private ArrayList<Topic> mTopics;
	
	/* package */ KloutProfile(String screenName, double score, double daychange, double weekchange, double monthchange) {
		mScreenName = screenName;
		mScore = score;
		mDayChange = daychange;
		mWeekChange = weekchange;
		mMonthChange = monthchange;
	}
	
	/* package */ KloutProfile(String screenName, double score) {
		mScreenName = screenName;
		mScore = score;
	}
	
	/* package */ KloutProfile(String screenName, ArrayList<KloutProfile> influencers, ArrayList<KloutProfile> influencees) {
		mScreenName = screenName;
		mInfluencedBy = influencers;
		mInfluencerOf = influencees;
	}
	
	/* package */ KloutProfile(String screenName, ArrayList<Topic> topics) {
		mScreenName = screenName;
		mTopics = topics;
	}
	
	public String getScreenName() {
		return mScreenName;
	}
	
	public double getScore() {
		return mScore;
	}
	
	public double getDayChange() {
		return mDayChange;
	}
	
	public double getWeekChange() {
		return mWeekChange;
	}
	
	public double getMonthChange() {
		return mMonthChange;
	}
	
	public ArrayList<KloutProfile> getInfluencers() {
		return mInfluencedBy;
	}
	
	public ArrayList<KloutProfile> getInfluencees() {
		return mInfluencerOf;
	}
	
	public ArrayList<Topic> getTopics() {
		return mTopics;
	}
	
	// TODO: public or package?
	public void updateScore(double score, double daychange, double weekchange, double monthchange) {
		mScore = score;
		mDayChange = daychange;
		mWeekChange = weekchange;
		mMonthChange = monthchange;
	}
	
	// TODO: public or package?
	public void updateScore(double score) {
		mScore = score;
	}
	
	// TODO: public or package?
	public void updateScore(ArrayList<KloutProfile> influencers, ArrayList<KloutProfile> influencees) {
		mInfluencedBy = influencers;
		mInfluencerOf = influencees;
	}
	
	// TODO: public or package?
	public void updateScore(ArrayList<Topic> topics) {
		mTopics = topics;
	}
}
