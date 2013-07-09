package com.amadeus.pdf.magazine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.FilterQueryProvider;
import android.widget.TextView;

public class ArticleSuggestionsAdapter extends CursorAdapter {

	ArrayList<ACGMagazineArticle> mArticles;
	ArrayList<ACGMagazineArticle> mFilteredArticles;

	private static final String[] COLUMNS = {
		BaseColumns._ID
	};
	
    public ArticleSuggestionsAdapter(Context context, ArrayList<ACGMagazineArticle> objects) {	
        super(context, new MatrixCursor(COLUMNS), 0);
        
        mArticles = objects;
		mFilteredArticles = new ArrayList<ACGMagazineArticle>();
		
		setFilterQueryProvider(new FilterQueryProvider() {

			@Override
			public Cursor runQuery(CharSequence query) {
				if (query.length() >= 3) 
					mFilteredArticles = filterArticleList(mArticles, Arrays.asList(query.toString().split(" ")));
	    		else 
	    			mFilteredArticles = new ArrayList<ACGMagazineArticle>();
				
				// Convert to cursor... must be a better way to do this... who cares though, this operation is fairly painless.
				MatrixCursor cursor = new MatrixCursor(COLUMNS);
				for (int i = 0; i < mFilteredArticles.size(); i++) {
					cursor.addRow(new String [] { String.valueOf(i) });
				}
					
				return cursor;
			}
			
			private ArrayList<ACGMagazineArticle> filterArticleList(ArrayList<ACGMagazineArticle> _articles, List<String> _searchTerms) {
				ArrayList<ACGMagazineArticle> models = new ArrayList<ACGMagazineArticle>();
				
				for (ACGMagazineArticle article : _articles){
					ArrayList<String> matchedTerms = new ArrayList<String>();
					for (String keyword : article.getKeywords()){ 
						for (String searchTerm : _searchTerms) {
							if (matchedTerms.contains(searchTerm))
								continue;
							if (like(keyword, "%" + searchTerm + "%")) {
								if (!matchedTerms.contains(searchTerm)) {
									matchedTerms.add(searchTerm);
								}
							}
						}
					}
					
					for (String searchTerm : _searchTerms) {
						if (matchedTerms.contains(searchTerm))
							continue;
						if (like(article.getTitle(), "%" + searchTerm + "%")) {
							if (!matchedTerms.contains(searchTerm)) {
								matchedTerms.add(searchTerm);
							}
						}
					}
					
					if (matchedTerms.size() == _searchTerms.size()) 
						models.add(article);
				}
				
				return models;
			}
			
			private boolean like(String str, String expr) {
			    expr = expr.toLowerCase(); // ignoring locale for now
			    expr = expr.replace(".", "\\."); // "\\" is escaped to "\" (thanks, Alan M)
			    // ... escape any other potentially problematic characters here
			    expr = expr.replace("?", ".");
			    expr = expr.replace("%", ".*");
			    str = str.toLowerCase();
			    return str.matches(expr);
			}
			
		});
    }
    
    @Override
	public View newView(Context arg0, Cursor cursor, ViewGroup arg2) {
		LayoutInflater inflater = LayoutInflater.from(arg0);
		View v = inflater.inflate(R.layout.suggestion_list_item, null);
		
		return v;
	}
    
    @Override
	public void bindView(View convertView, Context arg1, Cursor cursor) {   	
    	ACGMagazineArticle article = mFilteredArticles.get(cursor.getPosition());		
		TextView title = (TextView)convertView.findViewById(R.id.textViewArticleTitle);
		title.setText(article.getTitle());
		TextView category = (TextView)convertView.findViewById(R.id.textViewArticleCategory);
		category.setText(article.getCategory());
	}

    public ACGMagazineArticle getFilteredItem(int position) {
    	return mFilteredArticles.get(position);
    }
}
