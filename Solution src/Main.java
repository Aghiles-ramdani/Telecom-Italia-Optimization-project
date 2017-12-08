import Heuristic.*;
public class Main {

	public static void main(String[] args){
		CoIoTeOptimizer optimizer;
		String inputFilePath = null, outputFilePath = null, solFilePath = null;
		boolean test=false;
		for(int i=0; i<args.length; i++) {
			if(args[i].equals("-i"))
				inputFilePath = args[++i];
			else if(args[i].equals("-o"))
				outputFilePath = args[++i];
			else if(args[i].equals("-s"))
				solFilePath = args[++i];
			else if(args[i].equals("-test"))
				test=true;
			else if(args[i].equals("-help")){
				System.out.println("Usage:\tjava -jar assignment.jar -i <input_file_path> -o <statistics_file_path> -s <solution_file_path> [-test]");
				return;
			}
		}
		
		optimizer=new CoIoTeOptimizer();
		if(!test){
			if(args.length<6){
				System.err.println("Not enough argments!");
				System.out.println("Usage:\tjava -jar assignment.jar -i <input_file_path> -o <statistics_file_path> -s <solution_file_path>");
				return;
			}
			String buff[]=inputFilePath.replace("\\", "/").split("/");
			String instanceName=buff[buff.length-1];
			instanceName=instanceName.substring(0, instanceName.length()-4);
			optimizer.execute(inputFilePath);
			optimizer.writeStatistics(outputFilePath, instanceName);
			optimizer.writeSolutionFile(solFilePath);
		}
		else{
			if(args.length<5){
				System.err.println("Not enough argments!");
				System.out.println("Usage:\tjava -jar assignment.jar -i <input_file_path> -o <statistics_file_path> -s <solution_file_path>");
				return;
			}
			switch(optimizer.test(inputFilePath, solFilePath)){
			case FEASIBLE:
				System.out.println("Solution is feasible");
				break;
			case NOT_FEASIBLE_DEMAND:
				System.out.println("Solution is not feasible: task demand constraint violated");
				break;
			case NOT_FEASIBLE_USER:
				System.out.println("Solution is not feasible: user constraint violated");
			default:
				break;
			}
		}
	}
}
