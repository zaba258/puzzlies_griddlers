package griddler;


/**
 *
 * @author zeroos
 */

public interface GriddlerData extends Cloneable{
	boolean isInitialized();

	int getW();
	int getH();

	int[][] getGrid();
	int getFieldVal(int x, int y);

	int[] getRow(int n);
	void setRow(int n, int[] row);
	int[] getCol(int n);
	void setCol(int n, int[] col);

	void setGrid(int[][] grid);
	void setFieldVal(int v, int x, int y);

	Field getField(int n);
	Field getField(int n, boolean emptyFields);
	Field getField(int x, int y);
	Field getField(int x, int y, boolean emptyFields);
	Field[] getFields();



	Desc getDesc();

	void addGriddlerDataListener(GriddlerDataListener l);
	void removeGriddlerDataListener(GriddlerDataListener l);
        GriddlerDataListener[] getGriddlerDataListeners();



	String toXML();
	String getBoardDataMD5();

	GriddlerData clone();
//	Object clone();


	//if data is editable:
	void addField(Field c);
	void setField(int i, Field c);
	void addCol(int pos);
	void addRow(int pos);
	void addLeftCol();
	void addRightCol();
	void addTopRow();
	void addBottomRow();

	//normalize
	void crop();
	void genDesc();
	public void delUnusedFields();

	//solving
	int checkBoardFinished();
	int checkBoardFinished(boolean changeDesc);
}