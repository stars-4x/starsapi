package org.starsautohost.racebuilder.craigstars;

import javax.swing.JRadioButton;

public enum PRT {
	HE, SS, WM, CA, IS, SD, PP, IT, AR, JoaT;

	public int getIndex() {
		switch(this){
			case HE:
				return 0;
			case SS:
				return 1;
			case WM:
				return 2;
			case CA:
				return 3;
			case IS:
				return 4;
			case SD:
				return 5;
			case PP:
				return 6;
			case IT:
				return 7;
			case AR:
				return 8;
			case JoaT:
				return 9;
			default:
				throw new IndexOutOfBoundsException("Wrong index for " + this); //Should not happen
		}
	}

	public static PRT fromIndex(int index) {
		switch (index){
		case 0:
			return HE;
		case 1:
			return SS;
		case 2:
			return WM;
		case 3:
			return CA;
		case 4:
			return IS;
		case 5:
			return SD;
		case 6:
			return PP;
		case 7:
			return IT;
		case 8:
			return AR;
		case 9:
			return JoaT;
		default:
			throw new IndexOutOfBoundsException("Wrong index" + index);
		}
	}

}
