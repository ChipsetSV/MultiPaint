package com.chipsetsv.multipaint.controls;

import yuku.ambilwarna.AmbilWarnaDialog;

import com.chipsetsv.multipaint.R;
import com.chipsetsv.multipaint.tools.Tools;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Toolbar extends LinearLayout {

	private int height = 50;
	
	private ImageButton buttonPen;
	private ImageButton buttonEraser;
	
	private int color = Color.RED;
	private ImageButton buttonColor;
	
	private int bgColor = Color.BLACK;
	private ImageButton buttonBackgroundColor;
	
	private int width = 10;
	private TextView textWidth;
	
	private ImageButton buttonWidthInc;
	private ImageButton buttonWidthDec;
	
	private ImageButton buttonSave;
	
	private Tools selectedTool = Tools.Pen;
	
	
	/////////// Properties
	
	public Tools getSelectedTool() {
		return selectedTool;
	}
	
	public int getPaintColor() {
		return color;
	}
	
	public int getBackgroundColor() {
		return bgColor;
	}
	
	public int getToolWidth() {
		return width;
	}
	
	public void setBackgroundColor(int newColor) {
		bgColor = newColor;
		UpdateControls();
	}
	/////////////////
	
	public Toolbar(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		float dips = height;
		float scale = getContext().getResources().getDisplayMetrics().density;
		height = Math.round(dips * scale);
		
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, height));
		
		buttonPen = new ImageButton(context);
		buttonPen.setBackgroundResource(R.drawable.tool_button);
		buttonPen.setImageDrawable(context.getResources().getDrawable(R.drawable.brush));
		buttonPen.setOnClickListener(new OnPenClickListener());
		buttonPen.setLayoutParams(new LayoutParams(height, height));
		
		buttonEraser = new ImageButton(context);
		buttonEraser.setBackgroundResource(R.drawable.tool_button);
		buttonEraser.setImageDrawable(context.getResources().getDrawable(R.drawable.eraser));
		buttonEraser.setOnClickListener(new OnEraserClickListener());
		buttonEraser.setLayoutParams(new LayoutParams(height, height));
		
		textWidth = new TextView(context);
		textWidth.setLayoutParams(new LayoutParams(height, height));
		textWidth.setBackgroundResource(R.drawable.tool_button);
		textWidth.setTextSize(30);
		textWidth.setGravity(Gravity.CENTER);
		
			
		buttonWidthInc = new ImageButton(context);
		buttonWidthInc.setLayoutParams(new LayoutParams(height, height));
		buttonWidthInc.setBackgroundResource(R.drawable.tool_button);
		buttonWidthInc.setImageDrawable(context.getResources().getDrawable(R.drawable.round_plus_icon));
		buttonWidthInc.setOnClickListener(new OnWidthIncClickListener());
		buttonWidthDec = new ImageButton(context);
		buttonWidthDec.setLayoutParams(new LayoutParams(height, height));
		buttonWidthDec.setBackgroundResource(R.drawable.tool_button);
		buttonWidthDec.setImageDrawable(context.getResources().getDrawable(R.drawable.round_minus_icon));
		buttonWidthDec.setOnClickListener(new OnWidthDecClickListener());
		
		buttonColor = new ImageButton(context);
		buttonColor.setLayoutParams(new LayoutParams(height, height));
		buttonColor.setBackgroundResource(R.drawable.tool_button);
		buttonColor.setImageDrawable(context.getResources().getDrawable(R.drawable.palette));
		buttonColor.setOnClickListener(new OnColorClickListener());
		
		buttonBackgroundColor = new ImageButton(context);
		buttonBackgroundColor.setLayoutParams(new LayoutParams(height, height));
		buttonBackgroundColor.setBackgroundResource(R.drawable.tool_button);
		buttonBackgroundColor.setImageDrawable(context.getResources().getDrawable(R.drawable.palette));
		buttonBackgroundColor.setOnClickListener(new OnBackgroundColorClickListener());
		
		buttonSave = new ImageButton(context);
		buttonSave.setLayoutParams(new LayoutParams(height, height));
		buttonSave.setBackgroundResource(R.drawable.tool_button);
		buttonSave.setImageDrawable(context.getResources().getDrawable(R.drawable.round_plus_icon));
		buttonSave.setOnClickListener(new OnSaveClickListener());
		
		this.addView(buttonPen);
		this.addView(buttonEraser);
		this.addView(buttonWidthInc);
		this.addView(textWidth);
		this.addView(buttonWidthDec);
		this.addView(buttonColor);
		this.addView(buttonBackgroundColor);
		this.addView(buttonSave);
		
		UpdateControls();
	}
	
	public void UpdateControls() {
		for (int i = 0; i < this.getChildCount(); i++) {
			View obj = this.getChildAt(i);
			obj.setEnabled(true);
		}
		
		switch (selectedTool) {
		case Pen:
			buttonPen.setEnabled(false);
			break;
		case Eraser:
			buttonEraser.setEnabled(false);
			break;
		default:
			break;
		}
		
		buttonColor.setColorFilter(color, Mode.SRC_IN);
		buttonBackgroundColor.setColorFilter(bgColor, Mode.SRC_IN);
		
		textWidth.setText(String.valueOf(width));
	}
	
	
	/////////////// Toolbar Listeners
	
	class OnPenClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			selectedTool = Tools.Pen;
			if (mToolChangedListener != null)
				mToolChangedListener.toolChanged(selectedTool);
			UpdateControls();
		}
	}
	
	class OnEraserClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			selectedTool = Tools.Eraser;
			if (mToolChangedListener != null)
				mToolChangedListener.toolChanged(selectedTool);
			UpdateControls();
		}
	}

	class OnWidthIncClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			width++;
			if (mWidthChangedListener != null)
				mWidthChangedListener.widthChanged(width);
			UpdateControls();
		}
	}
	
	class OnWidthDecClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			width--;
			if (mWidthChangedListener != null)
				mWidthChangedListener.widthChanged(width);
			UpdateControls();
		}
	}
	
	class OnColorClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			AmbilWarnaDialog dialog = new AmbilWarnaDialog(getContext(), color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
				
				@Override
				public void onOk(AmbilWarnaDialog dialog, int newColor) {
					color = newColor;
					if (mPaintColorChangedListener != null)
						mPaintColorChangedListener.colorChanged(newColor);
					UpdateControls();
				}
				
				@Override
				public void onCancel(AmbilWarnaDialog dialog) {
					// TODO Auto-generated method stub
					
				}
			});
			dialog.show();
		}
	}
	
	class OnBackgroundColorClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			AmbilWarnaDialog dialog = new AmbilWarnaDialog(getContext(), bgColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
				
				@Override
				public void onOk(AmbilWarnaDialog dialog, int newColor) {
					bgColor = newColor;
					if (mBackgroundColorChangedListener != null)
						mBackgroundColorChangedListener.colorChanged(newColor);
					UpdateControls();
				}
				
				@Override
				public void onCancel(AmbilWarnaDialog dialog) {
					// TODO Auto-generated method stub
					
				}
			});
			dialog.show();
		}
	}


	class OnSaveClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (mSaveListener != null)
				mSaveListener.save();
		}
	}
	
	//////////////
	
	
	
	///////////// Toolbar Events
		
	public interface OnPaintColorChangedListener {
        void colorChanged(int color);
    }
    private OnPaintColorChangedListener mPaintColorChangedListener;	
    public void setOnPaintColorChangedListener(OnPaintColorChangedListener l) {
    	mPaintColorChangedListener = l;
    }
	
    public interface OnBackgroundColorChangedListener {
        void colorChanged(int color);
    }
    private OnBackgroundColorChangedListener mBackgroundColorChangedListener;
    public void setOnBackgroundColorChangedListener(OnBackgroundColorChangedListener l) {
    	mBackgroundColorChangedListener = l;
    }
    
    public interface OnToolChangedListener {
        void toolChanged(Tools tool);
    }
    private OnToolChangedListener mToolChangedListener;
    public void setOnToolChangedListener(OnToolChangedListener l) {
    	mToolChangedListener = l;
    }
    
    public interface OnWidthChangedListener {
        void widthChanged(int width);
    }
    private OnWidthChangedListener mWidthChangedListener;
    public void setOnWidthChangedListener(OnWidthChangedListener l) {
    	mWidthChangedListener = l;
    }
    
    public interface OnSaveListener {
        void save();
    }
    private OnSaveListener mSaveListener;
    public void setOnSaveListener(OnSaveListener l) {
    	mSaveListener = l;
    }
    
	////////////////////////
}
