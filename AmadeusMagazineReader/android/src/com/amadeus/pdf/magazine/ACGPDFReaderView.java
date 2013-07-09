package com.amadeus.pdf.magazine;

import com.artifex.mupdfdemo.MuPDFReaderView;

import android.app.Activity;
import android.view.MotionEvent;

public class ACGPDFReaderView extends MuPDFReaderView {

	protected void onTap() {}

	public ACGPDFReaderView(Activity act) {
		super(act);
	}

	public boolean onSingleTapUp(MotionEvent e) {		
		onTap();		
		return super.onSingleTapUp(e);		
	}
}
