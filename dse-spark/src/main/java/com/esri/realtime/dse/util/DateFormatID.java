package com.esri.realtime.dse.util;

public class DateFormatID
{
	private int val;

	public DateFormatID(int val) {
		this.val = val;
	}
	public int value() {
		return val;
	}
	public void set(int newVal) {
		val = newVal;
	}

	public String toString() {
		return "" + val;
	}
}
