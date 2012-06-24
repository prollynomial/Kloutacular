package ca.bitjutsu.kloutacular.v2;

/**
 * 
 * A data model for a Klout topic.
 * 
 * @author Adam Carruthers (adam.carruthers@bitjutsu.ca)
 *
 */
public class Topic {
	private long mId;
	private String mName;
	private String mDisplayName;
	private String mSlug;
	private String mImageUrl;
	
	/* package */ Topic(long id, String name, String displayName, String slug, String imageUrl) {
		mId = id;
		mName = name;
		mDisplayName = displayName;
		mSlug = slug;
		mImageUrl = imageUrl;
	}
	
	/**
	 * The unique id for the Klout topic.
	 * @return the topic id
	 */
	public long getId() {
		return mId;
	}
	
	/**
	 * A friendly name for the topic.
	 * @return the topic display name
	 */
	public String getDisplayName() {
		return mDisplayName; 
	}
	
	/**
	 * A less-friendly name for the topic.
	 * @return the topic name
	 */
	public String getName() {
		return mName;
	}
	
	/**
	 * A helper to build a URL for the topic: http://klout.com/#/topic/{slug}.
	 * @return the slug
	 */
	public String getSlug() {
		return mSlug;
	}
	
	/**
	 * URL to Klout's image for the topic.
	 * @return the URL pointing to the image
	 */
	public String getImageUrl() {
		return mImageUrl;
	}
}
