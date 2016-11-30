package datastructures;

import java.util.Arrays;
import java.util.Iterator;

public class Matrix implements Iterable<Vector> {	
	public int m, n;
	public Vector entries[];
	
	public Matrix(int rows, int columns) {
		m = rows;
		n = columns;
		
		entries = new Vector[m];
	}
	
	public Matrix(int rows, int columns, Double e[][]) {
		this(rows, columns);
		
		for (int m = 0; m < e.length; m++) {
			entries[m] = new Vector(this.n, e[m]);
		}
	}
	
	public Matrix(int rows, int columns, Double v) {
		this(rows, columns);
		
		for (int m = 0; m < this.m; m++) {
			entries[m] = new Vector(this.n, v);
		}
	}
	
	public Matrix(int rows, int columns, Vector v) {
		this(rows, columns);
		
		for (int m = 0; m < this.m; m++) {
			entries[m] = v.clone();
		}
	}
	
	public Vector row(int m) {
		return this.entries[m];
	}
	
	public Vector column(int n) {
		Vector c = new Vector(this.n);
		
		for (int i = 0; i < this.m; i++) {
			c.set(n, this.row(i).entry(n));
		}
		
		return c;
	}
	
	public void setRow(int m, Vector r) {
		this.entries[m] = r;
	}
	
	public void setColumn(int n, Vector c) {
		for (int i = 0; i < this.m; i++) {
			this.row(i).set(n, c.entry(n));
		}
	}
	
	public Double entry(int m, int n){
		return this.row(m).entry(n);
	}
	
	public void setEntry(int m, int n, Double v){
		this.row(m).set(n, v);
	}
	
	public void multiplyEntry(int m, int n, Double v){
		this.row(m).set(n, this.entry(m, n) * v);
	}
	
	public void divideEntry(int m, int n, Double v){
		this.multiplyEntry(m, n, 1.0 / v);
	}
	
	public void multiplyRow(int m, Double v){
		for (int j = 0; j < this.n; j++) {
			this.multiplyEntry(m, j, v);
		}	
	}
	
	public void divideRow(int m, Double v){
		this.multiplyRow(m, 1.0 / v);
	}
	
	public void multiplyColumn(int n, Double v){
		for (int i = 0; i < this.m; i++) {
			this.multiplyEntry(i, n, v);
		}	
	}
	
	public void divideColumn(int n, Double v){
		this.multiplyColumn(n, 1.0 / v);
	}
	
	public Matrix matrixMultiply(Matrix B) {
		Matrix C = new Matrix(this.m, B.n, 0.0);
		
		for (int m = 0; m < C.m; m++) {
			for (int n = 0; n < C.n; n++) {
				for (int i = 0; i < this.n; i++) {
					C.setEntry(m, n, C.entry(m, n) + this.entry(m, i) * B.entry(i, n));
				}
			}
		}
		
		return C;
	}
	
	public Matrix diagonalMatrix() {
		Matrix d = new Matrix(Math.max(this.m, this.n), Math.max(this.m, this.n), 0.0);
		Double val;
		
		for (int m = 0; m < Math.max(this.m, this.n); m++) {
			for (int n = 0; n < Math.max(this.m, this.n); n++) {
				if (m == n) {
					val = this.m > this.n ? this.entry(m, 0) : this.entry(0, m);
				} else {
					val = 0.0;
				}
				
				d.setEntry(m, n, val);
			}
		}
		
		return d;
	}
	
	public Matrix matrixAdd(Matrix B) {
		Matrix C = new Matrix(this.m, B.n, 0.0);
		
		for (int m = 0; m < C.m; m++) {
			for (int n = 0; n < C.n; n++) {
				C.setEntry(m, n, this.entry(m, n) + B.entry(m, n));
			}
		}
		
		return C;
	}
	
	public void add(Double v) {
		for (int m = 0; m < this.m; m++) {
			for (int n = 0; n < this.n; n++) {
				this.setEntry(m, n, this.entry(m, n) + v);
			}
		}
	}
	
	public Matrix transpose() {
		Matrix t = new Matrix(this.n, this.m, 0.0);
		int m = 0;
		
		for (Vector r: t) {
			for (int n = 0; n < t.n; n++) {
				r.set(n, this.entry(n, m));
			}
			
			m++;
		}
		
		return t;
	}
	
	public Matrix exp(Double e) {
		Matrix exp = new Matrix(this.m, this.n, 0.0);
		int m = 0;
		
		for (Vector r: exp) {
			for (int n = 0; n < exp.n; n++) {
				r.set(n, Math.pow((this.entry(m, n)), e));
			}
			
			m++;
		}
		
		return exp;
	}
	
	@Override
	public String toString() {
		String s = "";
		int i = 0;
		
		for (Vector r: this) {
			s += r;
					
			if (i < this.m - 1) {
				s += "\n";
			}
		}
		
		return s;
	}

	@Override
	public Iterator<Vector> iterator() {
		return Arrays.asList(this.entries).iterator();
	}
}
