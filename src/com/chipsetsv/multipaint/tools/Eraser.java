package com.chipsetsv.multipaint.tools;

import android.graphics.Color;
import android.graphics.Path;

public class Eraser extends Shape {

	public Eraser(Path path) {
		super(path);
		width = 30;
		color = Color.BLACK;
	}
	
	public Eraser(Path path, int color) {
		super(path);
		width = 30;
		this.color = color;
	}
	
	public Eraser(Path path, int color, int width) {
		super(path, color, width);
		
	}

	public Tools getToolType()
	{
		return Tools.Eraser;
	}
}
