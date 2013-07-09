package com.amadeus.pdf.magazine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ArticleListModel {
	public String Category;
	public ArrayList<ACGMagazineArticle> Articles;
	
	public ArticleListModel() {
		Articles = new ArrayList<ACGMagazineArticle>();
	}
	
	public static ArrayList<ArticleListModel> getListModels(ArrayList<ACGMagazineArticle> articles) {
		ArrayList<ArticleListModel> models = new ArrayList<ArticleListModel>();
		
		for (ACGMagazineArticle article : articles) {
			ArticleListModel model = null;
			for (ArticleListModel existingModel : models) {
				if (existingModel.Category.contentEquals(article.getCategory())) {
					model = existingModel;
					break;
				}
			}
			
			if (model == null) {
				model = new ArticleListModel();
				model.Category = article.getCategory();
				models.add(model);
			}
			
			model.Articles.add(article);
		}
		
		if (models.size() > 0)
			sortModels(models);
		
		return models;
	}
	
	public enum ArticleListModelComparator implements Comparator<ArticleListModel> {
		Category_SORT {
	        public int compare(ArticleListModel o1, ArticleListModel o2) {
	            return o1.Category.compareTo(o2.Category);
	        }};

	    public static Comparator<ArticleListModel> ascending(final Comparator<ArticleListModel> other) {
	        return new Comparator<ArticleListModel>() {
	            public int compare(ArticleListModel o1, ArticleListModel o2) {
	                return other.compare(o1, o2);
	            }
	        };
	    }

	    public static Comparator<ArticleListModel> getComparator(final ArticleListModelComparator... multipleOptions) {
	        return new Comparator<ArticleListModel>() {
	            public int compare(ArticleListModel o1, ArticleListModel o2) {
	                for (ArticleListModelComparator option : multipleOptions) {
	                    int result = option.compare(o1, o2);
	                    if (result != 0) {
	                        return result;
	                    }
	                }
	                return 0;
	            }
	        };
	    }
	}
	
	public static void sortModels(ArrayList<ArticleListModel> _issues) {
		Collections.sort(_issues, ArticleListModelComparator.ascending(ArticleListModelComparator.getComparator(ArticleListModelComparator.Category_SORT)));
	}
}
