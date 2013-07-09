package com.amadeus.pdf.magazine;

import java.util.ArrayList;

public interface ACGMagazineArticle {
	
	public String getSubject();
	public String getTitle();
	public String getCategory();	
	public ArrayList<String> getKeywords();
	public int getFirstPage();
	
}

