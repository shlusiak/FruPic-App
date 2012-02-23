package de.saschahlusiak.frupic.model;

import java.io.File;
import java.io.Serializable;


import android.content.Context;
import android.util.Log;

public class Frupic implements Serializable {

	private static final long serialVersionUID = 12345L;

	static private final String tag = Frupic.class.getSimpleName();
	
	int id;
	String full_url, thumb_url;
	String date;
	String username;
	String tags[];
	
	Frupic() {
		this.tags = null;
		this.username = null;
		this.date = null;
		this.full_url = null;
		this.thumb_url = null;
		this.id = 0;
		
	}
	
	public String getUsername() {
		if (username == null)
			return "";
		return username;
	}
	
	public String getDate() {
		return date;
	}
	
	public int getId() {
		return id;
	}
	
	public String getUrl() {
		return "http://frupic.frubar.net/" + id;
	}
	public String getFullUrl() {
		return full_url;
	}
	
	public File getCachedFile(Context context) {
		return new File(FrupicFactory.getCacheFileName(context, this, false));
	}
	
	public String getFileName(boolean thumb) {
		if (!thumb)
			return new File(full_url).getName();
		return "frupic_" + id + (thumb ? "_thumb" : "");
	}
	
	public String[] getTags() {
		return tags;
	}
	
	/**
	 * Connects all tags to one string of the form "[tag1, tag2, tag3]"
	 */
	public String getTagsString() {
		if (tags == null)
			return "---";
		String s = "";
		
		for (String s2: tags) {
			if (s.length() > 0)
				s += ", ";
			s += s2;
		}
		
		return "[" + s + "]";
	}
}
