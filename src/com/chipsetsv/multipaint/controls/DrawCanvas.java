package com.chipsetsv.multipaint.controls;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import com.chipsetsv.multipaint.connection.OnReceiveEvent;
import com.chipsetsv.multipaint.connection.OnSendEvent;
import com.chipsetsv.multipaint.tools.Eraser;
import com.chipsetsv.multipaint.tools.Pen;
import com.chipsetsv.multipaint.tools.Shape;
import com.chipsetsv.multipaint.tools.Stages;
import com.chipsetsv.multipaint.tools.Tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Toast;

public class DrawCanvas extends SurfaceView implements Callback {

	private CanvasThread canvasThread;
	private Paint mPaint;
	
	private volatile Bitmap bitmap;
	private volatile Canvas canvasBitmap;
	
	private Toolbar toolbar;
	private DisplayMetrics metrics;
		
	//private ArrayList<Shape> graphicObjects;
	private volatile Shape currentTool;
	private volatile Shape remoteTool;
	private volatile Path currentPath;
	private volatile Path remotePath;
	private volatile int paintColor;
	private volatile int backgroundColor;
	private volatile Tools currentToolType;
	private volatile Tools remoteToolType;
	private volatile boolean isClear = false;
	
	
	
	
	
	///////////// Messages
	private ReceiveListener receiverListener;
	public ReceiveListener getReceiveListener() {
		return receiverListener;
	}
	
	private OnSendEvent sendListener;
	public void setSendListener(OnSendEvent listener) {
		sendListener = listener;
	}
	public void removeSendListener() {
		sendListener = null;
	}
	
	private void sendMessage(String message) {
		ParseMessage(message, false);
		if (sendListener != null)
			sendListener.onSend(message);
	}
	
	private void sendMessage(Stages stage, Tools toolType, int color, int width, float x, float y) {
		float universalX = x;
		float universalY = y;
		if (metrics != null) {
			universalX = x / metrics.widthPixels;
			universalY = y / metrics.heightPixels;
		}
		sendMessage(stage.toString() + ";" + toolType.toString() + ";" + color + ";" + width + ";" 
											+ universalX + ";" + universalY);
	}
	
	////////////////////
	
	public DrawCanvas(Context context, AttributeSet attrs) {
		super(context, attrs);

		paintColor = Color.rgb(255, 100, 100);
		backgroundColor = Color.BLACK;
		//graphicObjects = new ArrayList<Shape>();
		
		mPaint = new Paint();
		InitializePaint();
		
		this.setFocusable(true);
		this.getHolder().addCallback(this);
		
		receiverListener = new ReceiveListener();
	}
	
	// Связь с тулбаром, установка параметров согласно ему
	public void setToolbar(Toolbar toolbar) {
		this.toolbar = toolbar;
		paintColor = toolbar.getPaintColor();
		backgroundColor = toolbar.getBackgroundColor();
		currentToolType = toolbar.getSelectedTool();
		this.toolbar.setOnPaintColorChangedListener(new Toolbar.OnPaintColorChangedListener() {
			@Override
			public void colorChanged(int color) {
				paintColor = color;
			}
		});
		this.toolbar.setOnBackgroundColorChangedListener(new Toolbar.OnBackgroundColorChangedListener() {
			@Override
			public void colorChanged(int color) {
				sendMessage(Stages.begin, Tools.Clear, color, 0, 0, 0);
			}
		});
		this.toolbar.setOnToolChangedListener(new Toolbar.OnToolChangedListener() {
			@Override
			public void toolChanged(Tools tool) {
				currentToolType = tool;
			}
		});
		this.toolbar.setOnSaveListener(new Toolbar.OnSaveListener() {
			
			@Override
			public void save() {
				savePicture();
			}
		});
	}
	
	// Установка метрик экрана для правильной отрисовки
	public void setMetrics(DisplayMetrics metrics) {
		this.metrics = metrics;
	}
	
	// Обработка приема сообщений
	public class ReceiveListener implements OnReceiveEvent
	{
		@Override
		public void onReceive(String msg) {
			synchronized (canvasThread.surfaceHolder) {
				ParseMessage(msg, true);
			}
		}		
	}
	//////////////////////////////////////////////////////////////
	// Message format: stage;toolType;color;width;x;y
	/////////////////////////////////////////////////////////////
	private void ParseMessage(String msg, boolean isRemote){
		Path path;
		Shape tool;
		Tools toolType;
		
		if (isRemote) {
			path = remotePath;
			tool = remoteTool;
			toolType = remoteToolType;
		}
		else {
			path = currentPath;
			tool = currentTool;
			toolType = currentToolType;
		}
		
		isClear = false;
		
		String[] datas = msg.split(";");
		Stages stage = Stages.valueOf(datas[0]);
		Tools newToolType = Tools.valueOf(datas[1]);
		float x = Float.parseFloat(datas[4]);
		float y = Float.parseFloat(datas[5]);
		int color = Integer.parseInt(datas[2]);
		int width = Integer.parseInt(datas[3]);
		
		if (metrics != null) {
			x = x * metrics.widthPixels;
			y = y * metrics.heightPixels;
			float widthf = width;
			widthf = widthf * ((widthf / metrics.widthPixels) * 80);
			width = (Math.round(widthf));
		}
		
		if (stage == Stages.begin) {
			
			if (newToolType == Tools.Clear) {
				backgroundColor = color;
				isClear = true;
				if (toolbar != null && isRemote) {
					toolbar.setBackgroundColor(color);
				}
			} else {
				path = new Path();
				path.moveTo(x, y);
				path.lineTo(x, y);
				
				if (tool == null || tool.isEnded() || tool.getToolType() != newToolType) {
					toolType = newToolType;
					switch (newToolType) {
					case Pen:
						tool = new Pen(path);
						break;
					case Eraser:
						tool = new Eraser(path);
						break;
					default:
						break;
					}
				}
				if (tool != null)
					tool.setParams(color, Math.round(width));
			}
			
		} else if (stage == Stages.processing) {
			path.lineTo(x, y);
		} else if (stage == Stages.end) {
			path.lineTo(x, y);

			if (tool != null)
				tool.ended();
		}
		
		if (isRemote) {
			remotePath = path;
			remoteTool = tool;
			remoteToolType = toolType;
		}
		else {
			currentPath = path;
			currentTool = tool;
			currentToolType = toolType;
		}
	}
	///////////////////////////////////////////////////////////////
	
	private void InitializePaint() {
    	mPaint.setDither(true);
	    mPaint.setColor(paintColor);
    	mPaint.setStyle(Paint.Style.STROKE);
    	mPaint.setStrokeJoin(Paint.Join.ROUND);
    	mPaint.setStrokeCap(Paint.Cap.ROUND);
    	mPaint.setStrokeWidth(3);		
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,  int height) {
		//mBitmap =  Bitmap.createBitmap (width, height, Bitmap.Config.ARGB_8888);
	}


	public void surfaceCreated(SurfaceHolder holder) {
		if (canvasThread == null || !canvasThread.isAlive()) {
			canvasThread = new CanvasThread(getHolder());
			canvasThread.setRunning(true);
			canvasThread.start();
		}
		// TODO: поправить восстановление активити
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		canvasThread.setRunning(false);
		while (retry) {
			try {
				canvasThread.join();
				retry = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {		
		if (currentTool != null)
		{
			canvas.drawPath(currentTool.getPath(), currentTool.getPaint(mPaint));
			if (currentTool.isEnded()) {
				currentTool.addEndCount();
				if (currentTool.isEndedPainted())
					currentTool = null;
			}
		}
		
		if (remoteTool != null)
		{
			canvas.drawPath(remoteTool.getPath(), remoteTool.getPaint(mPaint));
			if (remoteTool.isEnded()) {
				remoteTool.addEndCount();
				if (remoteTool.isEndedPainted())
					remoteTool = null;
			}
		}
		
		if (isClear) {
			canvas.drawColor(backgroundColor);
			
			if (currentTool != null)
				currentTool.getPath().reset();
			if (remoteTool != null)
				remoteTool.getPath().reset();
		}
		
		//int count = graphicObjects.size();
		
		//for(int i = 0; i < count; i++) {    		
    	//	Path currentGraphicObject = graphicObjects.get(i);
    		
    	//    canvas.drawPath(currentGraphicObject, mPaint);
    	//}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		synchronized (canvasThread.surfaceHolder) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					switch (currentToolType) {
					case Pen:
						sendMessage(Stages.begin, Tools.Pen, paintColor, toolbar.getToolWidth(), event.getX(), event.getY());
						break;
					case Eraser:
						sendMessage(Stages.begin, Tools.Eraser, backgroundColor, toolbar.getToolWidth(), event.getX(), event.getY());
						break;
					default:
						break;
				}
				
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				switch (currentToolType) {
					case Pen:
						sendMessage(Stages.processing, Tools.Pen, paintColor, toolbar.getToolWidth(), event.getX(), event.getY());
						break;
					case Eraser:
						sendMessage(Stages.processing, Tools.Eraser, backgroundColor, toolbar.getToolWidth(), event.getX(), event.getY());
						break;
					default:
						break;
				}
				
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				sendMessage(Stages.end, currentToolType, 0, 0, event.getX(), event.getY());
			}
		}
		return true;
	}
	
	

	private class CanvasThread extends Thread {
		
		private SurfaceHolder surfaceHolder;
		private boolean isRun = false;

		public CanvasThread(SurfaceHolder holder) {
			this.surfaceHolder = holder;
		}

		public void setRunning(boolean run) {
			this.isRun = run;
		}

		@Override
		public void run() {
			Canvas canvas;
			while (isRun) {
				canvas = null;
				try {
					canvas = this.surfaceHolder.lockCanvas();
					
					// Создание отдельной канвы, на которой рисуется аналог изображения для сохранения в битмап
					if (bitmap == null)
					{
						bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
						canvasBitmap = new Canvas(bitmap);
					}
					
					if (canvas != null) {
						synchronized (this.surfaceHolder) {
							// TODO: переработать принцип, так как получаются лишние итерации для определения конца отрисовки (isEndedCount)
							onDraw(canvasBitmap);
							//onDraw(canvas);
							canvas.drawBitmap(bitmap, 0, 0, mPaint);
						}
					}
					
				} finally {
					if (canvas != null) {
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
			}
		}
	}
	
	
	
	
	
	public void savePicture() {
		if (bitmap == null)
			return;
		try {
			SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
			String format = s.format(new java.util.Date());
			String filename = String.format("MultiPaint_%s.jpg", format);
			File file = new File(Environment.getExternalStorageDirectory(), filename);
			FileOutputStream fos = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			fos.close();
			Toast.makeText(getContext(), "Image saved on SD", Toast.LENGTH_LONG).show();
		} catch (FileNotFoundException e) {
			Log.e("DrawCanvas", "Error on saving image (File not found)");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("DrawCanvas", "Error on saving image (IO Exception)");
			e.printStackTrace();
		}
	}
	
}
