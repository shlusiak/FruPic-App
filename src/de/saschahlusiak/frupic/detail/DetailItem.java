package de.saschahlusiak.frupic.detail;

public class DetailItem {
	String title, value;
	
	DetailItem(String title) {
		this.title = title;
		this.value = "";
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return title;
	}
	
	public String getValue() {
		return value;
	}
}