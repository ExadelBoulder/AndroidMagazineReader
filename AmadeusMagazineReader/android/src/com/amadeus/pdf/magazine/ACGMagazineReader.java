package com.amadeus.pdf.magazine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnSuggestionListener;

import com.amadeus.pdf.magazine.R;
import com.artifex.mupdfdemo.AsyncTask;
import com.artifex.mupdfdemo.MuPDFAlert;
import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.MuPDFPageAdapter;
import com.artifex.mupdfdemo.MuPDFReaderView;
import com.artifex.mupdfdemo.SafeAnimatorInflater;
import com.artifex.mupdfdemo.SearchTaskResult;
import com.artifex.mupdfdemo.MuPDFAlert.ButtonPressed;

class ThreadPerTaskExecutor implements Executor {
	public void execute(Runnable r) {
		new Thread(r).start();
	}
}

/* Built using com.amadeus.pdf.mupdf.MuPDFActivity */

public class ACGMagazineReader extends SherlockActivity
{	
	public static final String INTENT_EXTRA_INITIAL_PAGE = "INTENT_EXTRA_INITIAL_PAGE";
	private int	mInitialPage;	
	public static final String INTENT_EXTRA_PDF_LABEL = "INTENT_EXTRA_PDF_LABEL";
	private String mPdfLabel;
	public static final String INTENT_EXTRA_ARTICLES = "INTENT_EXTRA_ARTICLES";
	private ArrayList<ACGMagazineArticle> mMagazineArticles;
	private ArrayList<ACGMagazineArticle> mFilteredArticles;
	public static final String INTENT_EXTRA_AD_FILE_NAME_MOBILE = "INTENT_EXTRA_AD_FILE_NAME_MOBILE";
	private String mMobileAdFileName;
	public static final String INTENT_EXTRA_AD_FILE_NAME_TABLET = "INTENT_EXTRA_AD_FILE_NAME_TABLET";
	private String mTabletAdFileName;
	public static final String INTENT_EXTRA_AD_URL = "INTENT_EXTRA_AD_URL";
	private String mAdUrl;

	private final int    OUTLINE_REQUEST=0;
	private final int    PRINT_REQUEST=1;
	private MuPDFCore    core;
	private String       mFileName;
	private ACGPDFReaderView mDocView;
	
	private boolean      mActionsVisible;
	private View         mPageSliderView;
	private EditText     mPasswordView;
	private SeekBar      mPageSlider;
	private int          mPageSliderRes;	
	private TextView     mPageNumberView;
	private TextView     mInfoView;
	private ImageButton	 mAd;
	
	private AlertDialog.Builder mAlertBuilder;
	private final Handler mHandler = new Handler();
	private boolean mAlertsActive= false;
	private AsyncTask<Void,Void,MuPDFAlert> mAlertTask;
	private AlertDialog mAlertDialog;
	
	private ArticleSuggestionsAdapter mSuggestionsAdapter;
	private ExpandableListView mArticles;
	private LinearLayout mArticlesLayout;
	private ArticleListAdapter mArticlesAdapter;

	/** Called when the activity is first created. */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_ACTION_BAR_OVERLAY);
		
		mAlertBuilder = new AlertDialog.Builder(this);

		if (core == null) {
			Intent intent = getIntent();
			mInitialPage = intent.getIntExtra(INTENT_EXTRA_INITIAL_PAGE, 0);
			mPdfLabel = intent.getStringExtra(INTENT_EXTRA_PDF_LABEL);
			mMobileAdFileName = intent.getStringExtra(INTENT_EXTRA_AD_FILE_NAME_MOBILE);
			mTabletAdFileName = intent.getStringExtra(INTENT_EXTRA_AD_FILE_NAME_TABLET);
			mAdUrl = intent.getStringExtra(INTENT_EXTRA_AD_URL);
			
			try
			{
				Serializable serialized = intent.getSerializableExtra(INTENT_EXTRA_ARTICLES);
				if (serialized != null) 
					mMagazineArticles = (ArrayList<ACGMagazineArticle>)serialized;
			}
			catch (ClassCastException ex) {
				ex.printStackTrace();
			}
			
			byte buffer[] = null;
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				Uri uri = intent.getData();
				if (uri.toString().startsWith("content://")) {
					// Handle view requests from the Transformer Prime's file manager
					// Hopefully other file managers will use this same scheme, if not
					// using explicit paths.
					Cursor cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
					if (cursor.moveToFirst()) {
						String str = cursor.getString(0);
						String reason = null;
						if (str == null) {
							try {
								InputStream is = getContentResolver().openInputStream(uri);
								int len = is.available();
								buffer = new byte[len];
								is.read(buffer, 0, len);
								is.close();
							}
							catch (java.lang.OutOfMemoryError e)
							{
								System.out.println("Out of memory during buffer reading");
								reason = e.toString();
							}
							catch (Exception e) {
								reason = e.toString();
							}
							if (reason != null)
							{
								buffer = null;
								Resources res = getResources();
								AlertDialog alert = mAlertBuilder.create();
								setTitle(String.format(res.getString(R.string.cannot_open_document_Reason), reason));
								alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
												finish();
											}
										});
								alert.show();
								return;
							}
						} else {
							uri = Uri.parse(str);
						}
					}
				}
				if (buffer != null) {
					core = openBuffer(buffer);
				} else {
					core = openFile(Uri.decode(uri.getEncodedPath()));
				}
				SearchTaskResult.set(null);
				if (core.countPages() == 0)
					core = null;
			}
			if (core != null && core.needsPassword()) {
				requestPassword(savedInstanceState);
				return;
			}
		}
		if (core == null)
		{
			AlertDialog alert = mAlertBuilder.create();
			alert.setTitle(R.string.cannot_open_document);
			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			alert.show();
			return;
		}

		createUI(savedInstanceState);
	}
	 
	public void requestPassword(final Bundle savedInstanceState) {
		mPasswordView = new EditText(this);
		mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

		AlertDialog alert = mAlertBuilder.create();
		alert.setTitle(R.string.enter_password);
		alert.setView(mPasswordView);
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (core.authenticatePassword(mPasswordView.getText().toString())) {
					createUI(savedInstanceState);
				} else {
					requestPassword(savedInstanceState);
				}
			}
		});
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		alert.show();
	}


	public void createUI(Bundle savedInstanceState) {
		if (core == null)
			return;

		// Now create the UI.
		// First create the document view
		mDocView = new ACGPDFReaderView(this) {
			@Override
			protected void onMoveToChild(int i) {
				if (core == null)
					return;
				
				mPageNumberView.setText(String.format("%d / %d", i + 1,
						core.countPages()));
				mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
				mPageSlider.setProgress(i * mPageSliderRes);
				
				super.onMoveToChild(i);
			}

			@Override
			protected void onTap() {
				if (mArticlesLayout.getVisibility() == View.VISIBLE)
					slideUpArticles();
			}
			
			@Override
			protected void onTapMainDocArea() {
				if (!mActionsVisible) {
					showActions();
				} else {
					hideActions();	
				}
			}

			@Override
			protected void onDocMotion() {
				hideActions();
			}

		};
		mDocView.setAdapter(new MuPDFPageAdapter(this, core));		

		// Make the slider overlay
		makeOverlays();	
        
		// Set up the page slider
		int smax = Math.max(core.countPages()-1,1);
		mPageSliderRes = ((10 + smax - 1)/smax) * 2;

		// Set the PDF Label
		if (mPdfLabel != null) 
			getSupportActionBar().setTitle(mPdfLabel);
		else
			getSupportActionBar().setTitle(mFileName);

		// Activate the page slider.
		mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				mDocView.setDisplayedViewIndex((seekBar.getProgress()+mPageSliderRes/2)/mPageSliderRes);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				if (mArticlesLayout.getVisibility() == View.VISIBLE)
					slideUpArticles();
			}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				updatePageNumView((progress+mPageSliderRes/2)/mPageSliderRes);
			}
		});

		// Set the initial page
		if (mInitialPage > 0) 
			mDocView.setDisplayedViewIndex(mInitialPage - 1);
		else 
			mDocView.setDisplayedViewIndex(0);	

		// Stick the document view and the buttons overlay into a parent view
		RelativeLayout layout = new RelativeLayout(this);
		layout.addView(mDocView);
		layout.addView(mPageSliderView);
		layout.setBackgroundResource(R.drawable.tiled_background);
		//layout.setBackgroundResource(R.color.canvas);
		setContentView(layout);
		
		slideUpArticles();
		
		mArticlesAdapter = new ArticleListAdapter(this, mMagazineArticles);
		mArticles.setAdapter(mArticlesAdapter);
		for (int i = 0; i < mArticlesAdapter.getGroupCount(); i++)
			mArticles.expandGroup(i);
		/*mArticles.setOnGroupClickListener(new OnGroupClickListener() {
			  @Override
			  public boolean onGroupClick(ExpandableListView parent, View v,int groupPosition, long id) { 
			    return true; // This way the expander cannot be collapsed
			  }
		});*/
		mArticles.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView arg0, View arg1, int arg2, int arg3, long arg4) {
				int articlePage = ((ACGMagazineArticle)mArticlesAdapter.getChild(arg2, arg3)).getFirstPage();
				if (articlePage > 0) 
					mDocView.setDisplayedViewIndex(articlePage - 1);
				else 
					mDocView.setDisplayedViewIndex(0);								
				
				hideActions();				
				return true;
			}
			
		});
		mArticles.setGroupIndicator(null);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case OUTLINE_REQUEST:
			if (resultCode >= 0)
				mDocView.setDisplayedViewIndex(resultCode);
			break;
		case PRINT_REQUEST:
			if (resultCode == RESULT_CANCELED)
				showInfo(getString(R.string.print_failed));
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onDestroy()
	{
		if (core != null)
			core.onDestroy();
		if (mAlertTask != null) {
			mAlertTask.cancel(true);
			mAlertTask = null;
		}
		core = null;
		super.onDestroy();
	}

	private void showActions() {
		if (core == null)
			return;
		if (!mActionsVisible) {
			mActionsVisible = true;
			
			// Show the action bar.
			getSupportActionBar().show();
			
			mAd.setVisibility(View.GONE);
			
			// Update page number text and slider
			int index = mDocView.getDisplayedViewIndex();
			updatePageNumView(index);
			mPageSlider.setMax((core.countPages()-1)*mPageSliderRes);
			mPageSlider.setProgress(index*mPageSliderRes);

			// Show the page slider.
			Animation anim = new TranslateAnimation(0, 0, mPageSlider.getHeight(), 0);
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPageSlider.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mPageNumberView.setVisibility(View.VISIBLE);
				}
			});
			mPageSlider.startAnimation(anim);
		}
	}

	private void hideActions() {
		if (mActionsVisible) {
			mActionsVisible = false;
			
			hideKeyboard();

			// Hide the action bar.
			getSupportActionBar().hide();
			
			// Hide the articles list
			mArticlesLayout.setVisibility(View.GONE);
			
			// Hide the page slider.
			Animation anim = new TranslateAnimation(0, 0, 0, mPageSlider.getHeight());
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPageNumberView.setVisibility(View.INVISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mPageSlider.setVisibility(View.INVISIBLE);
					
					Animation fadeIn = new AlphaAnimation(0, 1);
					fadeIn.setDuration(200);
					fadeIn.setAnimationListener(new AnimationListener() {

						@Override
						public void onAnimationEnd(Animation animation) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void onAnimationStart(Animation animation) {
							mAd.setVisibility(View.VISIBLE);
						}
						
					});
					mAd.startAnimation(fadeIn);
					
				}
			});
			mPageSlider.startAnimation(anim);
		}
	}
	
	private void slideUpArticles() {
		Animation articlesAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, -1.0f);
		articlesAnim.setDuration(300);
		articlesAnim.setAnimationListener(new Animation.AnimationListener() {

			@Override
			public void onAnimationEnd(Animation arg0) {
				mArticlesLayout.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onAnimationStart(Animation arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		mArticlesLayout.startAnimation(articlesAnim);
	}
	
	private void slideDownArticles() {
		mArticles.smoothScrollToPosition(0);
		
		Animation articlesAnim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, -1.0f,Animation.RELATIVE_TO_SELF, 0.0f);
		articlesAnim.setDuration(300);
		articlesAnim.setAnimationListener(new Animation.AnimationListener() {

			@Override
			public void onAnimationEnd(Animation arg0) {
				
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onAnimationStart(Animation arg0) {
				
				mArticlesLayout.setVisibility(View.VISIBLE);
				mArticles.setVisibility(View.VISIBLE);
				mArticlesLayout.requestFocus();
			}
		});
		mArticlesLayout.startAnimation(articlesAnim);
	}

	private void updatePageNumView(int index) {
		if (core == null)
			return;
		mPageNumberView.setText(String.format("%d / %d", index+1, core.countPages()));
	}

	private void showInfo(String message) {
		mInfoView.setText(message);

		int currentApiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentApiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			SafeAnimatorInflater safe = new SafeAnimatorInflater((Activity)this, R.animator.info, (View)mInfoView);
		} else {
			mInfoView.setVisibility(View.VISIBLE);
			mHandler.postDelayed(new Runnable() {
				public void run() {
					mInfoView.setVisibility(View.INVISIBLE);
				}
			}, 500);
		}
	}

	private void makeOverlays() {
		mPageSliderView = getLayoutInflater().inflate(R.layout.acg_overlay,null);
		mPageSlider = (SeekBar)mPageSliderView.findViewById(R.id.pageSlider);
		mPageNumberView = (TextView)mPageSliderView.findViewById(R.id.pageNumber);
		mInfoView = (TextView)mPageSliderView.findViewById(R.id.info);
		mArticles = (ExpandableListView)mPageSliderView.findViewById(R.id.listViewArticles);
		mArticlesLayout = (LinearLayout)mPageSliderView.findViewById(R.id.layoutArticles);
		mArticlesLayout.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus && mArticlesLayout.getVisibility() == View.VISIBLE)
					slideUpArticles();
			}
			
		});
		
		mPageNumberView.setVisibility(View.INVISIBLE);
		mInfoView.setVisibility(View.INVISIBLE);
		mPageSlider.setVisibility(View.INVISIBLE);
		
		int actionBarHeight = (int)getResources().getDimension(R.dimen.abs__action_bar_default_height);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mArticlesLayout.getLayoutParams());
		params.setMargins(0, actionBarHeight, 0, 120);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
		mArticlesLayout.setLayoutParams(params);
		
		// Setup the ad
		mAd = (ImageButton) mPageSliderView.findViewById(R.id.imageButtonMagazineAd);
        InputStream imageStream = null;
        if (mAd.getTag().toString().contentEquals("mobile")) 
        	imageStream = getStreamFromFile(mMobileAdFileName);
        else 
        	imageStream = getStreamFromFile(mTabletAdFileName);
        
        if (imageStream != null)
        	mAd.setImageDrawable(Drawable.createFromStream(imageStream, "src"));
        
        mAd.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (mAdUrl != null && !mAdUrl.contentEquals("")) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mAdUrl));
					startActivity(browserIntent);
				}
			}

        });
	}

	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mPageSliderView.getWindowToken(), 0);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mActionsVisible)
			hideActions();
		else 
			showActions();
		
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onStart() {
		if (core != null)
		{
			core.startAlerts();
			createAlertWaiter();
		}

		super.onStart();
	}

	@Override
	protected void onStop() {
		if (core != null)
		{
			destroyAlertWaiter();
			core.stopAlerts();
		}

		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (core.hasChanges()) {
			DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if (which == AlertDialog.BUTTON_POSITIVE)
						core.save();

					finish();
				}
			};
			AlertDialog alert = mAlertBuilder.create();
			alert.setTitle("MuPDF");
			alert.setMessage(getString(R.string.document_has_changes_save_them_));
			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), listener);
			alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), listener);
			alert.show();
		} else {
			super.onBackPressed();
		}
	}
	
	public void createAlertWaiter() {
		mAlertsActive = true;
		// All mupdf library calls are performed on asynchronous tasks to avoid stalling
		// the UI. Some calls can lead to javascript-invoked requests to display an
		// alert dialog and collect a reply from the user. The task has to be blocked
		// until the user's reply is received. This method creates an asynchronous task,
		// the purpose of which is to wait of these requests and produce the dialog
		// in response, while leaving the core blocked. When the dialog receives the
		// user's response, it is sent to the core via replyToAlert, unblocking it.
		// Another alert-waiting task is then created to pick up the next alert.
		if (mAlertTask != null) {
			mAlertTask.cancel(true);
			mAlertTask = null;
		}
		if (mAlertDialog != null) {
			mAlertDialog.cancel();
			mAlertDialog = null;
		}
		mAlertTask = new AsyncTask<Void,Void,MuPDFAlert>() {

			@Override
			protected MuPDFAlert doInBackground(Void... arg0) {
				if (!mAlertsActive)
					return null;

				return core.waitForAlert();
			}

			@Override
			protected void onPostExecute(final MuPDFAlert result) {
				// core.waitForAlert may return null when shutting down
				if (result == null)
					return;
				final MuPDFAlert.ButtonPressed pressed[] = new MuPDFAlert.ButtonPressed[3];
				for(int i = 0; i < 3; i++)
					pressed[i] = MuPDFAlert.ButtonPressed.None;
				DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mAlertDialog = null;
						if (mAlertsActive) {
							int index = 0;
							switch (which) {
							case AlertDialog.BUTTON1: index=0; break;
							case AlertDialog.BUTTON2: index=1; break;
							case AlertDialog.BUTTON3: index=2; break;
							}
							result.buttonPressed = pressed[index];
							// Send the user's response to the core, so that it can
							// continue processing.
							core.replyToAlert(result);
							// Create another alert-waiter to pick up the next alert.
							createAlertWaiter();
						}
					}
				};
				mAlertDialog = mAlertBuilder.create();
				mAlertDialog.setTitle(result.title);
				mAlertDialog.setMessage(result.message);
				switch (result.iconType)
				{
				case Error:
					break;
				case Warning:
					break;
				case Question:
					break;
				case Status:
					break;
				}
				switch (result.buttonGroupType)
				{
				case OkCancel:
					mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.cancel), listener);
					pressed[1] = MuPDFAlert.ButtonPressed.Cancel;
				case Ok:
					mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.okay), listener);
					pressed[0] = MuPDFAlert.ButtonPressed.Ok;
					break;
				case YesNoCancel:
					mAlertDialog.setButton(AlertDialog.BUTTON3, getString(R.string.cancel), listener);
					pressed[2] = MuPDFAlert.ButtonPressed.Cancel;
				case YesNo:
					mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.yes), listener);
					pressed[0] = MuPDFAlert.ButtonPressed.Yes;
					mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.no), listener);
					pressed[1] = MuPDFAlert.ButtonPressed.No;
					break;
				}
				mAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						mAlertDialog = null;
						if (mAlertsActive) {
							result.buttonPressed = MuPDFAlert.ButtonPressed.None;
							core.replyToAlert(result);
							createAlertWaiter();
						}
					}
				});

				mAlertDialog.show();
			}
		};

		mAlertTask.executeOnExecutor(new ThreadPerTaskExecutor());
	}

	public void destroyAlertWaiter() {
		mAlertsActive = false;
		if (mAlertDialog != null) {
			mAlertDialog.cancel();
			mAlertDialog = null;
		}
		if (mAlertTask != null) {
			mAlertTask.cancel(true);
			mAlertTask = null;
		}
	}

	private MuPDFCore openFile(String path)
	{
		int lastSlashPos = path.lastIndexOf('/');
		mFileName = new String(lastSlashPos == -1
					? path
					: path.substring(lastSlashPos+1));
		System.out.println("Trying to open "+path);
		try
		{
			core = new MuPDFCore(this, path);
		}
		catch (Exception e)
		{
			System.out.println(e);
			return null;
		}
		return core;
	}

	private MuPDFCore openBuffer(byte buffer[])
	{
		System.out.println("Trying to open byte buffer");
		try
		{
			core = new MuPDFCore(this, buffer);
		}
		catch (Exception e)
		{
			System.out.println(e);
			return null;
		}
		return core;
	}
	
	@Override
    public boolean onCreateOptionsMenu(final Menu menu) {

		final MenuItem searchItem = menu.add("Search");
		
        menu.add("Articles")
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		//Create the search view
        final SearchView searchView = new SearchView(getSupportActionBar().getThemedContext());
        searchView.setQueryHint("Search this issue");
        AutoCompleteTextView searchText = (AutoCompleteTextView) searchView.findViewById(R.id.abs__search_src_text);
        searchText.setHintTextColor(Color.LTGRAY);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {           

                return true;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
            	mSuggestionsAdapter.runQueryOnBackgroundThread(newText);
        	    return true;
            }
        });

        mSuggestionsAdapter = new ArticleSuggestionsAdapter(this, mMagazineArticles);
        searchView.setSuggestionsAdapter(mSuggestionsAdapter);
        searchView.setOnSuggestionListener(new OnSuggestionListener() {

			@Override
			public boolean onSuggestionClick(int position) {
				int articlePage = mSuggestionsAdapter.getFilteredItem(position).getFirstPage();
				if (articlePage > 0) 
					mDocView.setDisplayedViewIndex(articlePage - 1);
				else 
					mDocView.setDisplayedViewIndex(0);	
								
				searchItem.collapseActionView();
				searchView.setQuery("", true);
				
				hideActions();				
				return true;
			}

			@Override
			public boolean onSuggestionSelect(int position) {
				return true;
			}
        	
        });

        searchItem.setActionView(searchView).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);          
        
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle() == "Articles") {
			if (mArticlesLayout.getVisibility() == View.VISIBLE)
				slideUpArticles();
			else
				slideDownArticles();
		}

		return false;
	}
	
	public InputStream getStreamFromFile(String _fileName) {
		try {
			File f = getFile(_fileName);
			if (f != null)
				return new java.io.FileInputStream(f.getAbsolutePath());
			else
				return null;
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	public  File getFile(String _fileName) {
		String path = getExternalFilesDir(null).toString();
		File f = new File(path + "/" + _fileName);
		if (f.exists())
			return f;
		else 
			return null;
	}
}
