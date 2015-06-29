package de.unibayreuth.bayeosloggerapp.tools;


public class Tuple<X, Y> {
	public X x;
	public Y y;

	public Tuple(X x, Y y) {
		this.x = x;
		this.y = y;
	}

	public X getFirst() {
		return x;
	}

	public Y getSecond() {
		return y;
	}

	public void setFirst(X x) {
		this.x = x;
	}

	public void setSecond(Y y) {
		this.y = y;
	}

}
