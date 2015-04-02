package com.chipsetsv.multipaint.tools;

import android.graphics.Paint;
import android.graphics.Path;

public class Shape {
	protected Path path;
	protected int color;
	protected int width;
	
	protected boolean isEnded;
	protected int endCount = 0;
	
	public Shape(Path path) {
		this.path = path;
		isEnded = false;
	}
	
	public Shape(Path path, int color, int width) {
		this.path = path;
		this.color = color;
		this.width = width;
		isEnded = false;
	}
	
	public Path getPath() {
		return path;
	}
	
	public int getColor() {
		return color;
	}
	
	public int getWidth() {
		return width;
	}
	
	public Paint getPaint(Paint paint)
	{
		paint.setColor(color);
		paint.setStrokeWidth(width);
		return paint;
	}
	
	public void setParams(int color, int width) {
		this.color = color;
		this.width = width;
	}
	
	public Tools getToolType()
	{
		return Tools.None;
	}
	
	
	// �������� ��� �����������, ��������� �� ��� ������ ��� ���������
	// ������� ��������� ��� ���������� �������� ���������� �������� ������,
	// ��� ��� �� ����� ���������� ��� ����� ���������� 3 ����� (����������������)
	public void ended() {
		isEnded = true;
	}
	
	public boolean isEnded() {
		return isEnded;
	}
	
	public int getEndCount() {
		return endCount;
	}
	
	public void addEndCount() {
		endCount++;
	}
	
	public boolean isEndedPainted() {
		return endCount == 3;
	}
}
