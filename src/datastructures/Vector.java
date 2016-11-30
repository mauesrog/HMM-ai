package datastructures;

public class Vector {
	public Double entries[];
	public int n;
	
	public Vector(int columns) {
		n = columns;
	}
	
	public Vector(int columns, Double e[]) {
		this(columns);
		
		entries = e;
	}
	
	public Vector(int columns, Double v) {
		this(columns);
		
		entries = new Double[this.n];
		
		for (int n = 0; n < entries.length; n++) {
			entries[n] = v;
		}
	}
	
	public void set(int n, Double v) {
		entries[n] = v;
	}
	
	public void replaceEntries(Double e[]) {
		entries = e;
	}
	
	public Double entry(int n) {
		return entries[n];
	}
	
	public Double norm() {
		Double norm = 0.0;
		
		for (Double v: this.entries) {
			norm += v;
		}
		
		return norm;
	}
	
	public Vector clone() {
		return new Vector(this.n, this.entries.clone());
	}
	
	@Override
	public String toString() {
		String s = "";
		
		for (int i = 0; i < this.n; i++) {
			s += Math.round(this.entry(i) * 100.0) / 100.0;
			
			if (i < this.n - 1) {
				s += "  ";
			}
		}
		
		return s;
	}
}
