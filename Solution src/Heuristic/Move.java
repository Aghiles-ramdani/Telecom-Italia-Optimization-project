package Heuristic;

public class Move{

	int m,t,i,j,c,cu;
	float cuc;
	public Move(int m, int t, int i, int j, int c, int cu, float cuc) {
		super();
		this.m = m;
		this.t = t;
		this.i = i;
		this.j = j;
		this.c = c;			//costo per task moltiplicato 100 * numero di task degli utenti di tipo diverso da this.m
		this.cu=cu; 		//costo per mossa (arrotondato)
		this.cuc=cuc;		//cost per mossa (non arrotondato)
	}
	
	@Override
	public boolean equals(Object arg0) {
		Move x=(Move) arg0;
		if(x.m==m&&x.t==t&&x.i==i&&x.j==j)
			return true;
		return false;
	}
	
	
}
