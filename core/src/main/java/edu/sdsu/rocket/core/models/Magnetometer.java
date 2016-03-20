package edu.sdsu.rocket.core.models;

import java.util.concurrent.atomic.AtomicInteger;

import com.badlogic.gdx.math.Vector3;
import edu.sdsu.rocket.core.helpers.AtomicFloat;

public class Magnetometer {

	private final AtomicFloat scalingFactor = new AtomicFloat(1f);
	private final AtomicInteger rawX = new AtomicInteger();
	private final AtomicInteger rawY = new AtomicInteger();
	private final AtomicInteger rawZ = new AtomicInteger();
	
	public void setScalingFactor(float scalingFactor) {
		this.scalingFactor.set(scalingFactor);
	}
	
	public float getScalingFactor() {
		return scalingFactor.get();
	}
	
	public void setRawX(int value) {
		this.rawX.set(value);
	}
	
	public int getRawX() {
		return rawX.get();
	}
	
	public void setRawY(int value) {
		this.rawY.set(value);
	}
	
	public int getRawY() {
		return rawY.get();
	}
	
	public void setRawZ(int value) {
		this.rawZ.set(value);
	}
	
	public int getRawZ() {
		return rawZ.get();
	}
	
	/**
	 * Sets the latest magnetometer values in the provided Vector3 object.
	 * 
	 * Units are in Gauss.
	 * 
	 * @param v
	 */
	public void get(Vector3 v) {
		v.set(getRawX(), getRawY(), getRawZ()).scl(getScalingFactor());
	}
	
}
