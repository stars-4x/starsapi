package org.starsautohost.racebuilder.craigstars;

public class Hab{ // extends AbstractCSObject {
	private int grav;
	private int temp;
	private int rad;

	public Hab() {
	    grav = temp = rad = 0;
	}

	public Hab(int grav, int temp, int rad) {
		super();
		this.grav = grav;
		this.temp = temp;
		this.rad = rad;
	}
	
	public int getAtIndex(int index) {
	    if (index == 0) {
	        return grav;
	    } else if (index == 1) {
	        return temp;
	    } else if (index == 2) {
	        return rad;
	    }
	    throw new IndexOutOfBoundsException("Habitabiilty index must be between 0 and 2, was: " + index);
	}
	
	public void setAtIndex(int index, int value) {
        if (index == 0) {
            setGrav(value);
        } else if (index == 1) {
            setTemp(value);
        } else if (index == 2) {
            setRad(value);
        } else {
            throw new IndexOutOfBoundsException("Habitabiilty index must be between 0 and 2, was: " + index);
        }
	}

	public int getGrav() {
		return grav;
	}

	public void setGrav(int grav) {
		this.grav = grav;
	}

	public int getTemp() {
		return temp;
	}

	public void setTemp(int temp) {
		this.temp = temp;
	}

	public int getRad() {
		return rad;
	}

	public void setRad(int rad) {
		this.rad = rad;
	}

}
