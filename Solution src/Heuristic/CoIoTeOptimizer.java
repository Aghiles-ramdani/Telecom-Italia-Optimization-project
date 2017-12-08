package Heuristic;
import java.io.*;
import java.util.concurrent.Semaphore;

public class CoIoTeOptimizer implements Runnable{
	private long loadStartTime=-1, loadEndTime=-1, initializeStartTime=-1, initializeEndTime, solveStartTime=-1, solveEndTime=-1;
	private static int validMoves, usedMoves, lastIndex;
	private static int nCells=0, nTP=0, nUT=0;
	private static int[] nTasks;				//numero totale di task per ogni cella
	private static int[] nRTasks;  				//numero di task rimanenti per ogni cella
	private static int[] nTPU;					//numero di task che un utente può compiere
	private static int[][][] totUsers;		//il numero di utenti totale di tipo m (primo indice) al tempo t (secondo indice) nella cella di partenza i (terzo indice)
	private static int[][][] freeUsers;			//il numero di utenti rimanenti di tipo m (primo indice) al tempo t (secondo indice) nella cella di partenza i (terzo indice)
	private static int[][][][] usageMatrix;		//per ogni mossa m t i j, contine quanti utenti hanno eseguito la mossa
	private static int[][][][] costMatrix;		//matrice dei costi per tutte le mosse (arrotondato all'intero inferiore)
	private static float[][][][] costMatrixD;	//matrice di costi per ogni mossa (non arrotondata)
	private static Move[] costTArray;			//array che contiene tutte le Move (classe) valide. L'array viene ordinato per costo.
	private static int maxCost;					//costo massimo in costTArray. Serve per l'ordinamento.
	
	private static Semaphore[] semaphores=new Semaphore[8];
	private	static Thread Threads[]=new Thread[8];
	
	
	private int[] lastIndexes;					//per ogni destinazione j, contiene qual è l'ultimo indice di costTArray che corrisponde una mossa utilizzata
	
	private int[][][][] usageMatrixB;
	private int[][][] freeUsersB;
	private int[] nRTasksB;
	private int usedMovesB;
	private int lastIndexB;
	private int[] lastIndexesB;
	public enum FeasibilityState{
		FEASIBLE, NOT_FEASIBLE_DEMAND, NOT_FEASIBLE_USER, FEASIBILITY_TEST_FAILED;
	}
	
	public CoIoTeOptimizer(){
	}
	
	public void run()  {
		switch (Thread.currentThread().getName()){
		
			case "Counting Sort":
				int i;
				int[] C=new int[maxCost+1];
			    Move[] B=new Move[validMoves];
			    for(i=0; i<validMoves; i++)
			        C[costTArray[i].c]++;
			    for(i=1; i<maxCost+1; i++)
			        C[i]+=C[i-1];
			    for(i=validMoves-1; i>=0; i--){
			        B[--C[costTArray[i].c]]=costTArray[i];
			    }
			    for(i=0; i<validMoves; i++)
			    	costTArray[i]=B[i];
				semaphores[0].release();
				break;
		}
	}
	
	
	private long inputLoadET(String path) throws FileNotFoundException, IOException{
		loadStartTime=System.currentTimeMillis();  
		inputLoad(path);
		loadEndTime=System.currentTimeMillis();
		return loadEndTime - loadStartTime;
	}
	private boolean inputLoad(String path) throws FileNotFoundException, IOException{  //effettua solo la lettura, inizializzando nCells, nUT, nTP, nTPU, costMatrixD, nTasks, totalPeople
		int i, j, k, h;
		BufferedReader br=new BufferedReader(new FileReader(path));
		String[] buff;
		buff=br.readLine().split(" ");
		nCells=Integer.parseInt(buff[0]);
		nTP=Integer.parseInt(buff[1]);
		nUT=Integer.parseInt(buff[2]);
		nTPU=new int[nUT];
		br.readLine();
		buff=br.readLine().split(" ");
		for(i=0; i<nUT; i++)
			nTPU[i]=Integer.parseInt(buff[i]);
		
		br.readLine();						//Reading Costs
		costMatrixD=new float[nUT][nTP][nCells][nCells];
		for(i=0; i<nUT; i++)	{			
			for(j=0; j<nTP; j++){
				br.readLine();
				for(k=0; k<nCells; k++){
					buff=br.readLine().split(" ");
					for(h=0; h<nCells; h++){
						if(k==h){
							costMatrixD[i][j][k][h]=Float.MAX_VALUE;
							continue;
						}
						costMatrixD[i][j][k][h]=Float.parseFloat(buff[h]);
					}
				}
			}
		}
		br.readLine();						//Reading Number of Tasks
		nTasks=new int[nCells];
		buff=br.readLine().split(" ");
		for(i=0; i<nCells; i++){
			nTasks[i]=Integer.parseInt(buff[i]);
		}
		
		br.readLine();						//Reading number of Users
		totUsers=new int[nUT][nTP][nCells];
		for(i=0; i<nUT; i++){			
			for(j=0; j<nTP; j++){
				br.readLine();
				buff=br.readLine().split(" ");
				for(k=0; k<nCells; k++){
					totUsers[i][j][k]=Integer.parseInt(buff[k]);
				}
			}
		}
		br.close();
		return true;
	}
	
	private long initializeET() throws InterruptedException{					//calcola il tempo di inizializzazione
		initializeStartTime=System.currentTimeMillis();
		initialize();
		initializeEndTime=System.currentTimeMillis();
		return initializeEndTime - initializeStartTime;
	}
	private void initialize() throws InterruptedException {					//inizializza le variabili di suporto, ordina costTArray, e trova la soluzione iniziale
		int UC=0;		//celle da completare (nRTask>0)
		
		int i, j, k, h;
		costTArray=new Move[nUT*nTP*nCells*nCells];
		nRTasks=new int[nCells];
		costMatrix=new int[nUT][nTP][nCells][nCells];
		for(validMoves=0, i=0; i<nUT; i++){					//itero su tipo di utente (i=tipo utente)	
			for(j=0; j<nTP; j++){							//itero su periodi di tempo (j=periodo di tempo)	
				for(k=0; k<nCells; k++){					// itero su celle di partenza
					for(h=0; h<nCells; h++){				// itero su celle di arrivo
						costMatrix[i][j][k][h]=(int) (costMatrixD[i][j][k][h]);
						if(nTasks[h]!=0&&totUsers[i][j][k]!=0){  
							costTArray[validMoves]=new Move(i, j, k, h, costMatrix[i][j][k][h], costMatrix[i][j][k][h], costMatrixD[i][j][k][h]);
							for(int x=0; x<nUT; x++){
								if(x!=i)
									costTArray[validMoves].c*=nTPU[x]; //trucchetto
							}
							maxCost=Math.max(maxCost, costTArray[validMoves].c);
							validMoves++;
						}
					}
				}
			}
		}
		
		lastIndexes=new int[nCells];				//conterrà, per ogni destinazione j, l'utlimo indice dell'array costTArray, la cui mossa viene utilizzata
		semaphores[0]=new Semaphore(1);
		semaphores[0].acquire();
		Threads[0]=new Thread(this, "Counting Sort");		//lancio il thread che gestisce l'ordinamento
		Threads[0].start();
		
		for(i=0; i<nCells; i++){
			nRTasks[i]=nTasks[i];					//copio il numero totale di task per ogni cella, nel vettore che contine il numero rimanete: all'inizio sono uguali!
			if(nTasks[i]>0)
				UC++;								//calcolo quante celle hanno task da svolgere
		}
		
		
		freeUsers=new int[nUT][nTP][nCells];
		for(i=0; i<nUT; i++){			
			for(j=0; j<nTP; j++){
				for(k=0; k<nCells; k++){
					freeUsers[i][j][k]=totUsers[i][j][k];			//copio il numero totale di utenti per ogni sorgente m t i, nel vettore che contine il numero rimanete: all'inizio sono uguali!
				}
			}
		}
		
		
		usageMatrix=new int[nUT][nTP][nCells][nCells];
		
		
		usedMoves=0;
		lastIndex=-1;								//conterrà l'utlimo indice dell'array costTArray, la cui mossa viene utilizzata
		semaphores[0].acquire();
		semaphores[0].release();
		
		Move x;
		for(i=0; i<validMoves; i++){					//qui comincia il calcolo della soluzione iniziale
			x=costTArray[i];
			if(UC==0)
				break;
			if(x==null){
				System.err.println("Esaurite il numero di mosse valide! Problema non risolvibile!");
				break;
			}
			if(nRTasks[x.j]<=0)
				continue;
			j=(int) Math.ceil((Math.max(nRTasks[x.j], (double) 0)/nTPU[x.m]));
			int n = Math.min(freeUsers[x.m][x.t][x.i], j);
			if(n==0)
				continue;
			
			useMove(i, n);
			if(nRTasks[x.j]<=0){
				UC--;
//				if(nRTasks[x.j]<0)
//					taskOverheadFixing();
			}
		}
			
	}
	
	private long optimizeET(){
		solveStartTime=System.currentTimeMillis();
		optimize();
		solveEndTime=System.currentTimeMillis();
		return solveEndTime-solveStartTime;
	}
	private void optimize(){
		taskOverheadFixing();
		conflictSolver();
	}
	
	private void taskOverheadFixing(){
		int i, j, k, h, index=0, nTaskDaSvolgere;
		Move x, y;
		boolean[] UTEvaluated=new boolean[nUT];
		boolean stop=false;
		for(j=0; j<nCells; j++){
			if(nRTasks[j]>=0)									//se non c'è overhead salto questa destinazione
				continue;
			
			for(i=lastIndexes[j]; i>=0; i--){					//partendo dall'ultimo indice utilizzato dalla destinazione j, scorro indietro l'array per trovare l'ultima mossa utilizzata (in realtà è sempre la prima, ma prima l'ultimo indice non lo stavo memorizzando per ogni destinazione ma per l'array completo
				x=costTArray[i];
				if(x.j==j){										//if non più necessario utilizzando lastIndexes invece di lastIndex
					if(usageMatrix[x.m][x.t][x.i][x.j]>0){
						index=i;
						break;
					}
				}
			}
			if(i==-1)
				continue;
			y=costTArray[index];
			nTaskDaSvolgere=nTPU[y.m]+nRTasks[j];					//numero task da svolgere 	
			for(h=0; h<nUT; h++){
				if(nTPU[h]<nTPU[y.m])
					UTEvaluated[h]=false;
				else
					UTEvaluated[h]=true;
			}
			for(k=index; k<validMoves; k++){
				x=costTArray[k];
				if(x.j!=j)
					continue;
				if(nTPU[x.m]>=nTPU[y.m])
					continue;
				i=1;
				while(i*nTPU[x.m]<nTaskDaSvolgere)
					i++;
				if(freeUsers[x.m][x.t][x.i]<i){	
					continue;
				}
				if(i*x.cu<y.cu){
					
					useMove(index, -1);
					useMove(k, i);
//					usageMatrix[x.m][x.t][x.i][x.j]+=i;
//					nRTasks[x.j]-=i*nTPU[x.m];
//					freeUsers[x.m][x.t][x.i]-=i;
					if(freeUsers[y.m][y.t][y.i]>=totUsers[y.m][y.t][y.i])
						break;
					if(nRTasks[x.j]<0){
						y=x;
						index=k;
						continue;
					}
				}
				else{
					UTEvaluated[x.m]=true;
					stop=true;
					for(h=0; h<nUT; h++)
						if(UTEvaluated[h]==false)
							stop=false;
				}
				if(stop){
					stop=false;
					break;
				}
				else if(nRTasks[y.j]>=0)
					break;
				
			}
		}
	}
	
	private void conflictSolver(){
		int i, h;							//i è l'indice della mossa da migliorare
											//j è l'indice della mossa da ridurre
		double COBJ, NOBJ;					//current objective function and new objective function
		boolean findFirst;
		Move moveToImprove, moveToReduce;
		for(i=-1; i<lastIndex; ){
			if(System.currentTimeMillis()-solveStartTime>4900)
				break;
			i=nextConflict(i);
			if(i==-1)
				return;
//			System.out.println(costTArray[i].m + " " + costTArray[i].t + " " + costTArray[i].i + " " + costTArray[i].j);
			moveToImprove=costTArray[i];
			findFirst=true;
			for(h=0; h<lastIndex; ){
				if(findFirst){
					h=lastMoveUsingSource(i);
					findFirst=false;
				}
				else
					h=previousMoveUsingSource(h);
				if(h==i)
					continue;
				if(h==-1)
					break;
				moveToReduce=costTArray[h];
//				System.out.println(costTArray[h].m + " " + costTArray[h].t + " " + costTArray[h].i + " " + costTArray[h].j);
				COBJ=ObjectiveFunction();
				while(true){
					if(usageMatrix[moveToReduce.m][moveToReduce.t][moveToReduce.i][moveToReduce.j]<=0)
						break;
					backup();
					useMove(h, -usageMatrix[moveToReduce.m][moveToReduce.t][moveToReduce.i][moveToReduce.j]);
					useMove(i, freeUsers[moveToImprove.m][moveToImprove.t][moveToImprove.i]);
					removeExtraMoves(moveToImprove.j);
					taskOverheadFixing();
					completeDest(moveToReduce.j);
					taskOverheadFixing();
					NOBJ=ObjectiveFunction();
					if(COBJ<=NOBJ||(!isFeasible())){
						backtrack();
						break;
					}
					COBJ=NOBJ;
				}
				
					
			}
			
		}
	}
	private int nextConflict(int index){
		int i;//, j;
		Move x;
		Move last;//, penultima;
		for(i=index+1; i<lastIndex; i++){
			x=costTArray[i];
			if(usageMatrix[x.m][x.t][x.i][x.j]>=totUsers[x.m][x.t][x.i])  //se li ho usati tutti 
				continue;
			if(lastIndexes[x.j]<=i)  //se è l'ultima mossa per completare la cella di destinazione j 
				continue;
			last=costTArray[lastIndexes[x.j]];
			if(nRTasks[x.j]-nTPU[x.m]+usageMatrix[last.m][last.t][last.i][last.j]*nTPU[last.m]<0)
				continue;
//			j=previuosUsedMoveOfDest(lastIndexes[x.j]);
//			if(j!=-1){
//				penultima=costTArray[j];
//				if(penultima.equals(x)){
//					if(nRTasks[x.j]-nTPU[penultima.m]+usageMatrix[last.m][last.t][last.i][last.j]*nTPU[last.m]<0)
//						continue;
//				}
//			}
			return i;
		}
		return -1;
	}
	
	private void backup(){
		int i, j, k, h;
		usageMatrixB=new int[nUT][nTP][nCells][nCells];
		freeUsersB=new int[nUT][nTP][nCells];
		nRTasksB=new int[nCells];
		lastIndexesB=new int[nCells];
		usedMovesB=usedMoves;
		lastIndexB=lastIndex;
		for(i=0; i<nUT; i++){
			for(j=0; j<nTP; j++){
				for(k=0; k<nCells; k++){
					freeUsersB[i][j][k]=freeUsers[i][j][k];
					for(h=0; h<nCells; h++){
						usageMatrixB[i][j][k][h]=usageMatrix[i][j][k][h];
					}
				}
			}
		}
		for(k=0; k<nCells; k++){
			nRTasksB[k]=nRTasks[k];
			lastIndexesB[k]=lastIndexes[k];
		}
	}
	private void backtrack(){
		int i, j, k, h;
		for(i=0; i<nUT; i++){
			for(j=0; j<nTP; j++){
				for(k=0; k<nCells; k++){
					freeUsers[i][j][k]=freeUsersB[i][j][k];
					for(h=0; h<nCells; h++){
						usageMatrix[i][j][k][h]=usageMatrixB[i][j][k][h];
					}
				}
			}
		}
		for(k=0; k<nCells; k++){
			nRTasks[k]=nRTasksB[k];
			lastIndexes[k]=lastIndexesB[k];
		}
		
		lastIndex=lastIndexB;
		usedMoves=usedMovesB;
	}
	
	private int lastMoveUsingSource(int index){					//fornisce la prima mossa a partire dall'indice indicato con la stessa sorgente della mossa corrispondente all'indice
		int i;
		Move x, y=costTArray[index];
		for(i=lastIndex; i>=0; i--){
			x=costTArray[i];
			if(x.m!=y.m||x.t!=y.t||x.i!=y.i)
				continue;							//continui finchè non ne trovi una con la stessa cella di partenza stesso tipo e stesso tempo
			if(usageMatrix[x.m][x.t][x.i][x.j]<=0)
				continue;							//continuo finchè non ne trovo una che sto usando
			return i;
		}
		return -1;
	}
	private int previousMoveUsingSource(int index){					//fornisce la prima mossa a partire dall'indice indicato con la stessa sorgente della mossa corrispondente all'indice
		int i;
		Move x, y=costTArray[index];
		for(i=index-1; i>=0; i--){
			x=costTArray[i];
			if(x.m!=y.m||x.t!=y.t||x.i!=y.i)
				continue;
			if(usageMatrix[x.m][x.t][x.i][x.j]<=0)
				continue;
			return i;
		}
		return -1;
		
	}
	private int previuosUsedMoveOfDest(int index){
		int i;
		Move x, y=costTArray[index];
		for(i=index-1; i>=0; i--){
			x=costTArray[i];
			if(x.j!=y.j)
				continue;
			if(usageMatrix[x.m][x.t][x.i][x.j]<=0)
				continue;
			return i;
		}
		return -1;
		
	}
	
	private void removeExtraMoves(int dest){
		int i;
		while(nRTasks[dest]<0){
			i=lastIndexes[dest];
			useMove(lastIndexes[dest], -1);
			if(nRTasks[dest]>0){
				useMove(i, 1);
				break;
			}
		}
		
	}
	private void completeDest(int dest){
		int i;
		Move x;
		for(i=Math.max(lastIndexes[dest], 0); i<validMoves&&nRTasks[dest]>0; i++){
			x=costTArray[i];
			if(x.j!=dest)
				continue;
			int n = Math.min(freeUsers[x.m][x.t][x.i], (int) Math.ceil((Math.max(nRTasks[x.j], (double) 0)/nTPU[x.m])));
			if(n==0)
				continue;
			usedMoves++;
			
			useMove(i, n);
		}
	}
	
	private void updateLastIndex(){
		for(int i=0; i<nCells; i++){
			if(lastIndex<lastIndexes[i])
				lastIndex=lastIndexes[i];
		}
	}

	public FeasibilityState test(String inputFilePath, String solFilePath){
		try {
			solLoad(solFilePath);
		} catch (FileNotFoundException e) {
			System.err.println("Cannot find solution file!");
			return FeasibilityState.FEASIBILITY_TEST_FAILED;
		} catch (IOException e) {
			System.err.println("Unknown file format!");
			return FeasibilityState.FEASIBILITY_TEST_FAILED;
		}
		try {
			inputLoadET(inputFilePath);
		} catch (FileNotFoundException e) {
			System.err.println("Cannot find input file!");
			return FeasibilityState.FEASIBILITY_TEST_FAILED;
		} catch (IOException e) {
			System.err.println("Unknown file format!");
			return FeasibilityState.FEASIBILITY_TEST_FAILED;
		}
		return feasibilityState();
		
	}
	private void solLoad(String solFilePath) throws IOException {
		int i, j, m, t;
		
		BufferedReader br=new BufferedReader(new FileReader(solFilePath));
		String buff;
		String[] split;
		buff=br.readLine();
		split=buff.split(";");
		nUT=Integer.parseInt(split[1]);
		nTP=Integer.parseInt(split[2]);
		nCells=Integer.parseInt(split[0]);
		
		usageMatrix=new int[nUT][nTP][nCells][nCells];
		while((buff=br.readLine())!=null){
			split=buff.split(";");
			i=Integer.parseInt(split[0]);
			j=Integer.parseInt(split[1]);
			m=Integer.parseInt(split[2]);
			t=Integer.parseInt(split[3]);
			usageMatrix[m][t][i][j]=Integer.parseInt(split[4]);
		}
		br.close();
	}

	public void execute(String inputFilePath){
		try {
			inputLoadET(inputFilePath);
			initializeET();
			if(!isFeasible()){
				System.err.println("Could not find a feasible initial solution!");
				return;
			}
			optimizeET();
		} catch (FileNotFoundException e) {
			System.err.println("Cannot find input file!");
		} catch (IOException e) {
			System.err.println("Unknown file format!");
		} catch (InterruptedException e) {
			System.err.println("Unexpected interrupt");
		}
		
	}
	public void writeSolutionFile(String solutionFilePath){
		File file=new File(solutionFilePath);
		file.delete();
		PrintStream solutionFile;
		try {
			solutionFile = new PrintStream(solutionFilePath);
		} catch (FileNotFoundException e) {
			System.err.println("Cannot create solution file!");
			return;
		}
		solutionFile.println(nCells+";"+nUT+";"+nTP);
		int m, t, i, j;
		for(i=0; i<nCells; i++){
			for(j=0; j<nCells; j++){
				for(m=0; m<nUT; m++){
					for(t=0; t<nTP; t++){
						if(usageMatrix[m][t][i][j]>0)
							solutionFile.println(i+";"+j+";"+m+";"+t+";"+usageMatrix[m][t][i][j]);
					}
				}
			}
		}
		solutionFile.close();

	}
	public void writeStatistics(String outputFilePath, String instanceName){
		File file=new File(outputFilePath);
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.out.println("Statistics file does not exist and cannot be created!");
			}
		}
		BufferedWriter outputFile;
		try {
			outputFile = new BufferedWriter(new FileWriter(file.getAbsoluteFile(), true));
		} catch (IOException e1) {
			System.out.println("Statistics file cannot be opened!");
			return;
		}
		try {
			if(isFeasible())
				outputFile.write(instanceName+";"+String.format("%.3f", ((float)(initializeEndTime-initializeStartTime+solveEndTime-solveStartTime))/1000)+";"+((int) ObjectiveFunction())+";"+nUserOfType(0)+";"+nUserOfType(1)+";"+nUserOfType(2)+"\n");
			else
				outputFile.write(instanceName+";?;?;?;?;?\n");
		} catch (IOException e) {
			System.out.println("Statistics file cannot be written!");
			try {
				outputFile.close();
			} catch (IOException e1) {
				System.out.println("Failed closing statistics file!");
				
			}
			return;
		}
		try {
			outputFile.close();
		} catch (IOException e) {
			System.out.println("Failed closing statistics file!");
			return;
		}
	}
	
	private int nUserOfType(int m){
		int a=0, t, i;
		for(t=0; t<nTP; t++){
			for(i=0; i<nCells; i++){
				a+=totUsers[m][t][i]-freeUsers[m][t][i];
			}
		}
		return a;
	}
	private void useMove(int i, int n){
		Move x=costTArray[i];
		if(n>0){
			if(usageMatrix[x.m][x.t][x.i][x.j]<=0)
				usedMoves++;
		}
		usageMatrix[x.m][x.t][x.i][x.j]+=n;
		freeUsers[x.m][x.t][x.i]-=n;
		nRTasks[x.j]-=n*nTPU[x.m];
		if(n>0){
			lastIndexes[x.j]=Math.max(lastIndexes[x.j], i);
			lastIndex=Math.max(lastIndex, lastIndexes[x.j]);
		}
		else if (n<0){
			if(usageMatrix[x.m][x.t][x.i][x.j]<=0){
				usedMoves--;
				int j=previuosUsedMoveOfDest(lastIndexes[x.j]);
				if(lastIndex==lastIndexes[x.j]){
					lastIndexes[x.j]=j;
					updateLastIndex();
				}
				else
					lastIndexes[x.j]=j;
			}	
		}
			
	}

	public double ObjectiveFunction(){
		int a=0;
		int i, j, k, h;
		for(i=0; i<nUT; i++){
			for(j=0; j<nTP; j++){
				for(k=0; k<nCells; k++){
					for(h=0; h<nCells; h++){
						a+=usageMatrix[i][j][k][h]*costMatrix[i][j][k][h];
					}
				}
			}
		}
		return (double) a;
	}
	private boolean isFeasible(){
		boolean feasible=true;
		int a=0;
		int i, j, k, h;
		for(h=0; h<nCells&&feasible; h++){
			a=0;
			for(i=0; i<nUT&&feasible; i++){
				for(j=0; j<nTP&&feasible; j++){
					for(k=0; k<nCells&&feasible; k++){
						a+=usageMatrix[i][j][k][h]*nTPU[i];
					}
				}
			}
			if(a<nTasks[h])
				feasible=false;
		}
		for(k=0; k<nCells&&feasible; k++){
			for(i=0; i<nUT&&feasible; i++){
				for(j=0; j<nTP&&feasible; j++){
					a=0;
					for(h=0; h<nCells&&feasible; h++){
						a+=usageMatrix[i][j][k][h];
					}
					if(a>totUsers[i][j][k])
						feasible=false;
				}
			}
			
		}
		return feasible;
	}
	
	public FeasibilityState feasibilityState(){
		int a=0;
		int i, j, k, h;
		for(h=0; h<nCells; h++){
			a=0;
			for(i=0; i<nUT; i++){
				for(j=0; j<nTP; j++){
					for(k=0; k<nCells; k++){
						a+=usageMatrix[i][j][k][h]*nTPU[i];
					}
				}
			}
			if(a<nTasks[h])
				return FeasibilityState.NOT_FEASIBLE_DEMAND;
		}
		for(k=0; k<nCells; k++){
			for(i=0; i<nUT; i++){
				for(j=0; j<nTP; j++){
					a=0;
					for(h=0; h<nCells; h++){
						a+=usageMatrix[i][j][k][h];
					}
					if(a>totUsers[i][j][k])
						return FeasibilityState.NOT_FEASIBLE_USER;
				}
			}
			
		}
		return FeasibilityState.FEASIBLE;
	}
}