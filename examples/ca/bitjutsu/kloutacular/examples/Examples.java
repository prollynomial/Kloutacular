package ca.bitjutsu.kloutacular.examples;

import ca.bitjutsu.kloutacular.v1.KloutProfile;
import ca.bitjutsu.kloutacular.v1.KloutScoreManager;
import ca.bitjutsu.kloutacular.v1.KloutScoreManager.OnProfileUpdatedListener;

/*
 * Serves as a command line Klout API utility.
 */
public class Examples {	
	public static void main(String args[]) {
		if (args.length < 3) {
			System.out.println("Usage: \"java Examples <apikey> <user/score/influencer_of/influenced_by/topics> <user1> [user2...]\"");
			return;
		}
		
		if (!(args[1].equals("user") || args[1].equals("score")
				|| args[1].equals("influencer_of") || args[1].equals("influenced_by") || args[1].equals("topics"))) {
			System.out.println("Request type \"" + args[1] + "\" not recognized.");
			return;
		}
		
		String apiKey = args[0];
		String requestType = args[1];
		
		// Obtain a KloutScoreManager instance.
		KloutScoreManager kloutMan = KloutScoreManager.getInstance(apiKey);
		
		// This listener is called when KloutScoreManager receives results
		// from the Klout API and updates or adds a KloutProfile
		kloutMan.addOnProfileUpdatedListener(new OnProfileUpdatedListener() {
			public void onUpdate(KloutProfile kp) {
				System.out.println(kp);
			}
		});
		
		// Iterate through users passed and perform the action specified on that user.
		for (int i = 2; i < args.length; i++) {
			if (requestType.equals("user"))
				kloutMan.requestUser(args[i]);
			
			if (requestType.equals("score"))
				kloutMan.requestKlout(args[i]);
			
			if (requestType.equals("influencer_of"))
				kloutMan.requestInfluencerOf(args[i]);
			
			if (requestType.equals("influencedBy"))
				kloutMan.requestInfluencedBy(args[i]);
			
			if (requestType.equals("topics"))
				kloutMan.requestTopics(args[i]);
		}
	}
}
