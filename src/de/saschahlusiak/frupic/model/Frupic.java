package de.saschahlusiak.frupic.model;

public class Frupic {
	int id;
	String url;
	String date;
	String username;
	boolean thumb;
	
	Frupic(int id, String url, String date, String username) {
		this.id = id;
		this.url = url;
		this.date = date;
		this.username = username;
		thumb = false;
	}
	Frupic() {
	}
	
	public int getId() {
		return id;
	}
	
	public String getUrl() {
		if (!thumb)
			return url;
		return "http://frupic.frubar.net/thumbs/" + id + ".png";
	}
}
