package griddler;

/**
 *
 * @author zeroos
 */
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;


public class GriddlerSolver{
	public static final boolean VERBOSE = true;

	GriddlerBoard board;
	GriddlerData data;
	String stepDesc = "";
	static int assumptionCounter = 0;

	public GriddlerSolver(Desc desc){
	}
	public GriddlerSolver(GriddlerBoard board){
		this.data = board.getData();
		this.board = board;
	}
	public GriddlerSolver(GriddlerData data){
		this.data = data;
//		grid = data.getGrid();
//		desc = data.getDesc();
	}
	
	public void setData(GriddlerData data){
		this.data = data;
	}
	public GriddlerData getData(){
		return this.data;
	}
	public void reinit(){
		if(board != null) setData(board.getData());
	}

	public String getStepDesc(){
		return stepDesc;
	}
	public void setStepDesc(String desc){
		stepDesc = desc;
	}

	public boolean isSolvable(){
		try{
			solve();
		}catch(UnsolvableException e){
			return false;
		}
		return true;

	}
	public String descNextStep(){
		nextStep();
		return getStepDesc();
	}
	public void nextStep(){
		try{
			solve(1);
		}catch(UnsolvableException e){
			if(VERBOSE) System.out.println("Sry, unsolvable.");
		}
	}

	public void solve() throws UnsolvableException{
		solve(-1);
	}
	public void solve(int numberOfSteps) throws UnsolvableException{
		try{
			if(numberOfSteps == 0) return;
			boolean changed = false;
	
/*			while(forEachRowAndColumn(new SimpleBoxesAlgorithm(), numberOfSteps)){
				if(VERBOSE) System.out.println("Simple boxes: board changed.");
				numberOfSteps--;
				changed = true;
				if(numberOfSteps == 0) return;
			}
			while(forEachRowAndColumn(new SimpleSpacesAlgorithm(), numberOfSteps)){
				if(VERBOSE) System.out.println("Simple spaces: board changed.");
				numberOfSteps--;
				changed = true;
				if(numberOfSteps == 0) return;
			}*/
			while(forEachRowAndColumn(new CompleteLineAlgorithm(), numberOfSteps)){
				if(VERBOSE) System.out.println("CompleteLine: board changed.");
				numberOfSteps--;
				changed = true;
				if(numberOfSteps == 0) return;
			}

			if(data.checkBoardFinished(false) == 1){
				if(VERBOSE) System.out.println("Board finished.");
				return;
			}
			makeAssumption();
			if(data.checkBoardFinished(false) != 1) throw new UnsolvableException(UnsolvableException.CONTRADICTION); //end of board, no solutions
		}catch(UnsolvableException e){
			if(e.getReason() == e.MULTIPLE_SOLUTIONS) if(VERBOSE) System.out.println("Multiple solutions");
			else if(e.getReason() == e.CONTRADICTION){
				if(VERBOSE) System.out.println("Contradiction");
//				e.printStackTrace();
			}
			throw e;
		}
	}

	private void makeAssumption() throws UnsolvableException{
		for(int x=0; x<data.getW(); x++){
			for(int y=0; y<data.getH(); y++){
				//find an unfilled field
				if(data.getFieldVal(x,y) == -1){
					if(VERBOSE) System.out.println("-1!");
					//copy data
					GriddlerData solution = null;
					for(int i=0; i<data.getFields().length; i++){
						GriddlerData newData = data.clone();
						//for each field value
						newData.setFieldVal(i, x, y);
						assumptionCounter++;
						int assumptionNum = assumptionCounter;
						if(VERBOSE) System.out.println("Assumption " + assumptionCounter + ": field " + x + "x"+ y + " set to " + i);
						if(data.checkBoardFinished(false) == -1){
							if(VERBOSE) System.out.println(" ^^^ instantly failed");
							continue;
						}
						GriddlerSolver newSolver = new GriddlerSolver(newData);
						try{
							newSolver.solve(); //if unsolveble throws exception
							if(solution != null){
								throw new UnsolvableException(UnsolvableException.MULTIPLE_SOLUTIONS);
							}
							solution = newSolver.getData().clone();
							if(VERBOSE) System.out.println(assumptionNum + " success");
						}catch(UnsolvableException e){
							if(VERBOSE) System.out.println(assumptionNum + " failed");
						}
					}
					if(solution != null) this.data.setGrid(solution.getGrid());
					return;
				}
			}
		}
//		if(!data.checkBoardFinished(false)) throw new UnsolvableException(UnsolvableException.CONTRADICTION);
		return;
	}


	private boolean forEachRowAndColumn(SolvingAlgorithm a) throws UnsolvableException{
		return forEachRowAndColumn(a, -1);
	}
	private boolean forEachRowAndColumn(SolvingAlgorithm a, int stepLimit) throws UnsolvableException{
		boolean stepPerformed = false;
		for(int i=0; i<data.getH() && stepLimit!=0; i++){
			try{
				if(a.solve(data.getRow(i), data.getDesc().getRow(i))){
					data.setRow(i, a.getNewFieldSet());
					stepLimit--;
					stepPerformed = true;
				}
			}catch(IndexOutOfBoundsException e){
				if(VERBOSE) System.out.println("out");
//				e.printStackTrace();
			}
		}
		if(stepPerformed) return stepPerformed;
		for(int i=0; i<data.getW() && stepLimit!=0; i++){
			try{
				if(a.solve(data.getCol(i), data.getDesc().getCol(i))){
					data.setCol(i, a.getNewFieldSet());
					stepLimit--;
					stepPerformed = true;
				}
			}catch(IndexOutOfBoundsException e){
				if(VERBOSE) System.out.println("out");
//				e.printStackTrace();
			}
		}
		return stepPerformed;

	}


	private class SimpleBoxesAlgorithm extends SolvingAlgorithm{
		public boolean solve(int[] fs, ArrayList<DescField> ds) throws UnsolvableException{
			boolean changed = false;
			//create a temprorary array and try to fill it with condensed boxes
			int[] leftTempTab = new int[fs.length];
			int[] rightTempTab = new int[fs.length];
			for(int i=0; i<leftTempTab.length; i++) leftTempTab[i] = -1;
			for(int i=0; i<rightTempTab.length; i++) rightTempTab[i] = -1;

			int tempTabPos = 0;
			for(int descTabPos = 0; descTabPos < ds.size(); descTabPos++){
				DescField d = ds.get(descTabPos);
				for(int descFieldPos=0; descFieldPos<d.getLength(); descFieldPos++){
					leftTempTab[tempTabPos++] = descTabPos;
				}
				if(descTabPos+1 < ds.size() && d.getValue() == ds.get(descTabPos+1).getValue()){//if the same color of two blocks
					leftTempTab[tempTabPos++] = -1;
				}
			}
			tempTabPos = rightTempTab.length-1;
			for(int descTabPos = ds.size()-1; descTabPos >= 0; descTabPos--){
				DescField d = ds.get(descTabPos);
				for(int descFieldPos=0; descFieldPos<d.getLength(); descFieldPos++){
					rightTempTab[tempTabPos--] = descTabPos;
				}
				if(descTabPos > 0 && d.getValue() == ds.get(descTabPos-1).getValue()){//if the same color of two blocks
					rightTempTab[tempTabPos--] = -1;
				}
			}
			for(int i=0; i<fs.length; i++){
				//search for fields with the same number in two arrays
				if(leftTempTab[i] == rightTempTab[i] && leftTempTab[i] != -1){

					if(fs[i] > 0 && fs[i] != ds.get(rightTempTab[i]).value){
						//at i there is something else than should be
						throw new UnsolvableException(UnsolvableException.CONTRADICTION);
					}else if(fs[i] <= 0 || fs[i] != ds.get(rightTempTab[i]).value){
						//if fs[i] is set to the correct value, this code won't be executed
						changed = true;
						fs[i] = ds.get(rightTempTab[i]).value;
					}
				}
			}
			if(changed) setNewFieldSet(fs);
			return changed;
		}
	}

	private class SimpleSpacesAlgorithm extends SolvingAlgorithm{
		public boolean solve(int[] fs, ArrayList<DescField> ds) throws UnsolvableException{
			boolean changed = false;
			return changed;
		}
	}

	private class CompleteLineAlgorithm extends SolvingAlgorithm{
		ArrayList<int[]> possibleSolutions;
		boolean newFieldFound;
		int[] availableFieldValues;
		int[] fs;
		ArrayList<DescField> ds;

		public boolean solve(int[] fs, ArrayList<DescField> ds) throws UnsolvableException{
			if(ds.size()==0) return false;
			possibleSolutions = new ArrayList<int[]>();
			newFieldFound = false;
			availableFieldValues = getAvailableFieldValues(ds);
			this.fs = fs;
			this.ds = ds;

			int[] solution = new int[fs.length];
			System.arraycopy(fs, 0, solution, 0, fs.length);

			
			if(VERBOSE) System.out.println("Possible solutions before solve: " + possibleSolutions.size());
			solve(0,0);


			if(VERBOSE) System.out.println("So far:");
			for(int j=0; j<solution.length;j++){
				if(VERBOSE) System.out.print(solution[j] + ",");
			}
			if(VERBOSE) System.out.println();
			if(VERBOSE) System.out.println("Possible solutions: " + possibleSolutions.size());
			for(int i=0; i<possibleSolutions.size(); i++){
				for(int j=0; j<possibleSolutions.get(i).length;j++){
					if(VERBOSE) System.out.print(possibleSolutions.get(i)[j] + ",");
				}
				if(VERBOSE) System.out.println();
			}

			//checks each possible sollution to determine new fields
			for(int i=0; i<solution.length; i++){//for each field
				int possibleFieldValue=-1;
				if(solution[i] != -1){//known value
					continue;
				}
				for(int s=0; s<possibleSolutions.size(); s++){//for each solution
					int[] sol = possibleSolutions.get(s);
					if(s==0){
						possibleFieldValue = sol[i];
					}else if(possibleFieldValue != sol[i]){
						possibleFieldValue = -1;
						break;
					}
				}
				if(possibleFieldValue != -1){//possible solution was found
					solution[i] = possibleFieldValue;
					newFieldFound = true;
				}
			}
			setNewFieldSet(solution);
			return newFieldFound;



		}

		public void solve(int pos, int descPos) throws UnsolvableException{
			if(VERBOSE) System.out.println("solve("+pos+","+descPos+");");
			int rightPadding = 0;

			int blockLength = ds.get(descPos).getLength();
			int blockValue = ds.get(descPos).getValue();

			//count rightPadding
			int previousValue = blockValue;
			for(int i=descPos+1; i<ds.size();i++){
				if(ds.get(i).getValue() == previousValue) rightPadding++;
				rightPadding+=ds.get(i).getLength();
				previousValue = ds.get(i).getValue();
			}


			if(VERBOSE) System.out.println("A: " + rightPadding);
			for(int i=pos; i<=fs.length-rightPadding-blockLength; i++){
				int isPossible = isPossible(descPos, i, pos);
				if(isPossible == 1){
					int old_data[] = new int[blockLength];
					for(int j=0; j<blockLength; j++){
						old_data[j] = fs[i+j];
						fs[i+j] = blockValue;
					}
					if(descPos+1 < ds.size()){
						if(ds.get(descPos+1).getValue() == blockValue){
							solve(i+blockLength+1, descPos+1);
						}else{
							solve(i+blockLength, descPos+1);
						}
					}else{
						markPossibleSolution();
					}
					//restore previous data
					for(int j=0; j<blockLength; j++){
						fs[i+j] = old_data[j];
					}
				}else if(isPossible == -1){
					break;
				}
			}	
		}
		public int isPossible(int descPos, int pos, int startPos){
			//checks if it is possible to insert a block from descPos into a pos,
			//assumes that the grid start at startPos AND that it is called for each possibility 
				//(you SHOULD NOT expect it to work if you pass something that's in the middle)

			//returns -1 if it is NOT possible and there is no point in moving further [ie the block was skipped]
			//returns 0 if it is NOT possible, but you should move further
			//returns 1 if it is possible

			int blockLength = ds.get(descPos).getLength();
			int blockValue = ds.get(descPos).getValue();


			//check if no blocks were skipped
			if(pos>startPos){
				if(fs[pos-1]==blockValue){
					if(VERBOSE) System.out.println("S: skipped; pos: " + pos + " startPos: " + startPos);
					return -1;
				}
			}

			//check if not covers any different colors
			for(int i=pos; i<pos+blockLength; i++){
				if(fs[i] != -1 && fs[i] != blockValue){
					if(VERBOSE) System.out.println("S: different color");
					return 0;
				}
			}
			//check if not collides with any other block of the same value
			try{
				if(fs[pos-1] == blockValue || fs[pos+blockLength+1] == blockValue){
					if(VERBOSE) System.out.println("S: collision");
					return 0;
				}
			}catch(ArrayIndexOutOfBoundsException e){ }

			if(VERBOSE) System.out.println("S: Possible");
			return 1;

		}
		public void markPossibleSolution(){
			int[] solution = new int[fs.length];
			System.arraycopy(fs, 0, solution, 0, fs.length);
			possibleSolutions.add(solution);
		}
	}

	private abstract class SolvingAlgorithm{
		int[] newFieldSet = new int[]{};

		//returns true if new fields were found
		public abstract boolean solve(int[] fieldSet, ArrayList<DescField> descFieldSet) throws UnsolvableException;
		int[] getAvailableFieldValues(ArrayList<DescField> ds){
			Set valuesSet = new HashSet();
			for(int i=0; i<ds.size(); i++){
				valuesSet.add(ds.get(i).getValue());
			}
			int[] values = new int[valuesSet.size()];
			Iterator it = valuesSet.iterator();
			for(int i=0; i<values.length; i++){
				values[i] = (Integer)it.next();
			}
			return values;
		}
		public int[] getNewFieldSet(){
			return newFieldSet;
		}
		public void setNewFieldSet(int[] f){
			newFieldSet = f;
		}
	}
}
