package com.chipsetsv.multipaint.tools;

import android.graphics.Color;
import android.graphics.Path;

public class Pen extends Shape {

	public Pen(Path path) {
		super(path);
		width = 3;
		color = Color.WHITE;
	}
	
	public Pen(Path path, int color) {
		super(path);
		width = 3;
		this.color = color;
	}
	
	public Pen(Path path, int color, int width) {
		super(path, color, width);
		
	}

	public Tools getToolType()
	{
		return Tools.Pen;
	}
}
