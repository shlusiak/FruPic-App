package de.saschahlusiak.frupic.model;

import java.io.File;
import java.io.Serializable;

import de.saschahlusiak.frupic.db.FrupicDB;

import android.database.Cursor;
import android.util.Log;

public class Frupic implements Serializable {

	private static final long serialVersionUID = 12345L;
	
	public static final int FLAG_NEW = 0x01;
	public static final int FLAG_FAV = 0x02;

	int id;
	int flags;
	String full_url, thumb_url;
	String date;
	String username;
	String tags[];
	Object tag;
	
	public Frupic() {
		this.tags = null;
		this.username = null;
		this.date = null;
		this.full_url = null;
		this.thumb_url = null;
		this.id = 0;
		this.tag = null;
		this.flags = 0;
	}
	
	public Frupic(Cursor cursor) {
		this();
		fromCursor(cursor);
	}
	
	public void fromCursor(Cursor cursor) {
		this.id = cursor.getInt(FrupicDB.ID_INDEX);
		this.full_url = cursor.getString(FrupicDB.FULLURL_INDEX);
		this.thumb_url = cursor.getString(FrupicDB.THUMBURL_INDEX);
		this.username = cursor.getString(FrupicDB.USERNAME_INDEX);
		this.date = cursor.getString(FrupicDB.DATE_INDEX);
		this.flags = cursor.getInt(FrupicDB.FLAGS_INDEX);
		String s = cursor.getString(FrupicDB.TAGS_INDEX);
		if (s != null) {
			if (s.length() > 0)
				this.tags = s.split(", ");
			else
				this.tags = null;
			if (this.tags != null && this.tags.length == 0)
				this.tags = null;
		}
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
	

	public String getThumbUrl() {
		return thumb_url;
	}
	
	public File getCachedFile(FrupicFactory factory, boolean thumb) {
		return new File(FrupicFactory.getCacheFileName(factory, this, thumb));
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
			return null;
		String s = "";
		
		for (String s2: tags) {
			if (s.length() > 0)
				s += ", ";
			s += s2;
		}
		
		return "[" + s + "]";
	}
	
	public void setFlags(int newFlags) {
		this.flags = newFlags;
	}

	public int getFlags() {
		return flags;
	}
	
	public boolean hasFlag(int flag) {
		return ((flags & flag) != 0);
	}
	
	public boolean isAnimated() {
		return full_url.endsWith(".gif") || full_url.endsWith(".GIF");
	}
}
