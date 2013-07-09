package com.amadeus.pdf.magazine;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class ArticleListAdapter extends BaseExpandableListAdapter {

	public Context mContext;
	public ArrayList<ArticleListModel> mModels;
	
	public ArticleListAdapter(Context context, ArrayList<ACGMagazineArticle> articles) {
		mContext = context;
		mModels = ArticleListModel.getListModels(articles);
	}
	
	@Override
	public Object getChild(int arg0, int arg1) {
		return mModels.get(arg0).Articles.get(arg1);
	}

	@Override
	public long getChildId(int arg0, int arg1) {
		return arg1;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		ACGMagazineArticle article = mModels.get(groupPosition).Articles.get(childPosition);
		
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.acg_article_list_child_item, null);
		}
		
		TextView title = (TextView)convertView.findViewById(R.id.textViewArticleTitle);
		title.setText(article.getTitle());

		return convertView;
	}

	@Override
	public int getChildrenCount(int arg0) {
		return mModels.get(arg0).Articles.size();
	}

	@Override
	public Object getGroup(int arg0) {
		return mModels.get(arg0);
	}

	@Override
	public int getGroupCount() {
		return mModels.size();
	}

	@Override
	public long getGroupId(int arg0) {
		return arg0;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		ArticleListModel model = mModels.get(groupPosition);
		
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.acg_article_list_group_item, null);
		}
		
		TextView title = (TextView)convertView.findViewById(R.id.textViewCategory);
		title.setText(model.Category);
		
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int arg0, int arg1) {
		return true;
	}
}
