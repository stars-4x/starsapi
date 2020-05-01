package org.starsautohost.racebuilder.craigstars;

/*
 * Recreated by Runar
 */
public enum LRT {
	IFE, TT, ARM, ISB, GR, UR, MA, NRSE, CE, OBRM, NAS, LSP, BET, RS;

	public int getIndex() {
		switch(this){
			case IFE:
				return 0;
			case TT:
				return 1;
			case ARM:
				return 2;
			case ISB:
				return 3;
			case GR:
				return 4;
			case UR:
				return 5;
			case MA:
				return 6;
			case NRSE:
				return 7;
			case CE:
				return 8;
			case OBRM:
				return 9;
			case NAS:
				return 10;
			case LSP:
				return 11;
			case BET:
				return 12;
			case RS:
				return 13;
			default:
				throw new IndexOutOfBoundsException("Wrong index for " + this);
		}
	}

	public static LRT fromIndex(int index) {
		switch(index){
		case 0:
			return IFE;
		case 1:
			return TT;
		case 2:
			return ARM;
		case 3:
			return ISB;
		case 4:
			return GR;
		case 5:
			return UR;
		case 6:
			return MA;
		case 7:
			return NRSE;
		case 8:
			return CE;
		case 9:
			return OBRM;
		case 10:
			return NAS;
		case 11:
			return LSP;
		case 12:
			return BET;
		case 13:
			return RS;
		default:
			throw new IndexOutOfBoundsException("Wrong index" + index);
		}
	}

}
