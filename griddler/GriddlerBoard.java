package griddler;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseMotionAdapter;
import java.lang.Math;
import java.util.Calendar;
import java.util.Collections;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.undo.*;

import utils.MyPreferences;
import utils.TR;

/**
 *
 * @author zeroos
 */

public class GriddlerBoard extends JPanel{
	private GriddlerData data;
	private MyPreferences pref;
	private UndoManager undoManager = new UndoManager();
	private boolean paused = false;

	public static final int SINGLE = 0;
	public static final int BLOCK = 1;
	public static final int LINE = 2;

	//
	int hlCol = -2;
	int hlRow = -2;
	int leftColorVal = 1;
	int middleColorVal = -1;
	int rightColorVal = 0;
	int lastSelectedFieldX = 0;//the x coordinate of the last selected (clicked) field
	int lastSelectedFieldY = 0;//the y coordinate of the last selected (clicked) field
	int currentlySelectedFieldX = 0;//the x coordinate of the last selected (clicked) field
	int currentlySelectedFieldY = 0;//the y coordinate of the last selected (clicked) field
	boolean selectionInProgress = false;//the user is currently selecting fields (only if selectMode != SINGLE)
	int paintingColor = 0;//what color we are currently painting with (makes sense only if selectionInProgress)

	//
	boolean editMode = false;
	int selectMode = SINGLE;

	EventListenerList changeListenerList = new EventListenerList();
	EventListenerList hlChangeListenerList = new EventListenerList();
	EventListenerList undoableEditListenerList = new EventListenerList();

	AbstractAction finishAction;

	//sizes
	int fieldW;
	int fieldH;
	int offsetX;
	int offsetY;
	int gridOffsetX;
	int gridOffsetY;
	int linesOffsetX;
	int linesOffsetY;
	int gridW;
	int gridH;

	//colors
	Color lineColor;
	Color line5Color;
	Color hlLineColor;
	Color bgColor;

	//actions
	public Action undo = new AbstractAction(){
		public void actionPerformed(ActionEvent e) {
			undo();
		}
	};
	public Action redo = new AbstractAction(){
		public void actionPerformed(ActionEvent e) {
			redo();
		}
	};
	public Action nextSelectMode = new AbstractAction(){
		public void actionPerformed(ActionEvent e){
			nextSelectMode();
		}
	};




	public GriddlerBoard(){
		this(false);
	}
	public GriddlerBoard(boolean editMode){
		this(new GriddlerStaticData(), null, editMode);
	}
	public GriddlerBoard(GriddlerData data){
		this(data, null, false);
	}
	public GriddlerBoard(GriddlerData data, AbstractAction finishAction){
		this(data, finishAction, false);
	}
	public GriddlerBoard(GriddlerData data, AbstractAction finishAction, boolean editMode){
		this.data = data;
		this.editMode = editMode;
		this.finishAction = finishAction;
		pref = MyPreferences.getInstance();

		init();
	}
	
	public void calcGridSize(){
		gridOffsetX = offsetX+(data.getDesc().getLongestRow())*fieldW;
		gridOffsetY = offsetY+(data.getDesc().getLongestCol())*fieldH;
		linesOffsetX = gridOffsetX;
		linesOffsetY = gridOffsetY;
		gridW = data.getW()*fieldW;
		gridH = data.getH()*fieldH;
		if(editMode){
			gridOffsetX += fieldW;
			gridOffsetY += fieldH;
			//gridOffsetX = offsetX;
			//gridOffsetY = offsetY;

			linesOffsetX += 2*fieldW;
			linesOffsetY += 2*fieldH;
		}
		revalidate();
	}
	protected void init(){
		setOpaque(true);


		lineColor = new Color(pref.getInt("lineColor", new Color(0x66, 0x66, 0x66).getRGB()));
		line5Color = new Color(pref.getInt("line5Color", new Color(0x00, 0x00, 0x00).getRGB()));
		hlLineColor = new Color(pref.getInt("hlLineColor", new Color(0xff, 0xff, 0x00).getRGB()));
		bgColor = new Color(pref.getInt("bgColor", new Color (0xcc,0xcc,0xcc, 0x88).getRGB()));
		setBackground(bgColor);

		pref.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				lineColor = new Color(pref.getInt("lineColor", new Color(0x66, 0x66, 0x66).getRGB()));
				line5Color = new Color(pref.getInt("line5Color", new Color(0x00, 0x00, 0x00).getRGB()));
				hlLineColor = new Color(pref.getInt("hlLineColor", new Color(0xff, 0xff, 0x00).getRGB()));
				bgColor = new Color(pref.getInt("bgColor", new Color (0xcc,0xcc,0xcc, 0x88).getRGB()));
				setBackground(bgColor);
				repaint();
			}
		});

		fieldW = pref.getInt("fieldW", 20);
		fieldH = pref.getInt("fieldH", 20);
		offsetX = pref.getInt("offsetX", 0);
		offsetY = pref.getInt("offsetY", 0);

		calcGridSize();


		//set shortcuts
		final String shortcuts = pref.get("fields_shortcuts", "`1234567890qwertyuiopasdfghjkl");
		for(int i=0; i<shortcuts.length(); i++){
			getInputMap().put(KeyStroke.getKeyStroke(shortcuts.charAt(i)),
			                            "shortcut");
		}
		Action action  = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				shortcutPressed(shortcuts.indexOf(e.getActionCommand())-1);
		        }
		};
		getActionMap().put("shortcut", action);
		

		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE,0),"undo"); //backspace for undo
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,InputEvent.CTRL_MASK),"undo"); //ctrl+z for undo
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK),"redo"); //ctrl+shift+z for redo
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y,InputEvent.CTRL_MASK),"redo"); //ctrl+y for redo
		getActionMap().put("undo", undo);
		getActionMap().put("redo", redo);

		setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
		setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
		
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0),"nextSelectMode"); //tab for nextSelectMode
		getActionMap().put("nextSelectMode", nextSelectMode);
		

		setListeners();
	}
	private void setListeners(){
		//add listeners
		addMouseMotionListener(new MouseAdapter() {
			public void mouseMoved(MouseEvent e){
				if(paused) return;
				//set highlighted column and row, even when mouse is not over board
				int x = e.getX()-gridOffsetX;
				int y = e.getY()-gridOffsetY;
				
				setHlCol((int)Math.floor((float)x/fieldW));
				setHlRow((int)Math.floor((float)y/fieldH));
			}
			public void mouseDragged(MouseEvent e){
				if(paused) return;
				mouseMoved(e);
				if(	(e.getX() < gridOffsetX+gridW && e.getY() < gridOffsetY+gridH) &&
					(e.getX() > gridOffsetX       && e.getY() > gridOffsetY)){
					//position of mouse in fields
					final int x = (int)Math.floor((float)(e.getX()-gridOffsetX)/fieldW);
					final int y = (int)Math.floor((float)(e.getY()-gridOffsetY)/fieldH);
					if(x>=0 && y>=0){
						final int prevFieldVal = data.getFieldVal(x,y);
						final int newFieldVal;
						if((e.getModifiers()&e.BUTTON2_MASK) != 0){
							newFieldVal = middleColorVal;
						}else if((e.getModifiers()&e.BUTTON3_MASK) != 0){
							newFieldVal = rightColorVal;
						}else{//BUTTON1
							newFieldVal = leftColorVal;
						}
						if(selectMode == BLOCK && currentlySelectedFieldX == x && currentlySelectedFieldY == y) repaint();
						currentlySelectedFieldX = x;
						currentlySelectedFieldY = y;
						selectionInProgress = true;
						if(newFieldVal == prevFieldVal) return;
						if(selectMode == SINGLE){
						//generate undoable event
							fireUndoableEditEventOccured(new UndoableEditEvent(this, 
								new AbstractUndoableEdit(){
									public void undo(){
										super.undo();
										data.setFieldVal(prevFieldVal,x,y);
									}
									public void redo(){
										super.redo();
										data.setFieldVal(newFieldVal,x,y);
									}
								})
							);
							//set field value
							data.setFieldVal(newFieldVal,x,y);
						}

					}
				}
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e){
				if(paused){
			       		unpause();
					return;
				}
				//only when mouse is over grid
				if(	(e.getX() < gridOffsetX+gridW && e.getY() < gridOffsetY+gridH) &&
					(e.getX() > gridOffsetX       && e.getY() > gridOffsetY)){
					//position of mouse in fields
					final int x = (int)Math.floor((float)(e.getX()-gridOffsetX)/fieldW);
					final int y = (int)Math.floor((float)(e.getY()-gridOffsetY)/fieldH);
					if(x>=0 && y>=0){
						final int prevFieldVal = data.getFieldVal(x,y);
						final int newFieldVal;
						if(e.getButton() == e.BUTTON2){
							newFieldVal = middleColorVal;
						}else if(e.getButton() == e.BUTTON3){
							newFieldVal = rightColorVal;
						}else{//BUTTON1
							newFieldVal = leftColorVal;
						}
						lastSelectedFieldX = x;
						lastSelectedFieldY = y;
						paintingColor = newFieldVal;
						if(newFieldVal == prevFieldVal) return;
						//generate undoable event
						fireUndoableEditEventOccured(new UndoableEditEvent(this, 
							new AbstractUndoableEdit(){
								public void undo(){
									super.undo();
									data.setFieldVal(prevFieldVal,x,y);
								}
								public void redo(){
									super.redo();
									data.setFieldVal(newFieldVal,x,y);
								}
							})
						);
						//set field value
						data.setFieldVal(newFieldVal,x,y);	

					}
				//only when mouse over desc rows
				}else if(e.getX() > offsetX && e.getY() > gridOffsetY && e.getX() < gridOffsetX-(editMode?fieldW:0)){
					//position of mouse in px
					int x = e.getX()-offsetX;
					int y = e.getY()-gridOffsetY;
					//position of mouse in fields
					x = (int)Math.floor((float)x/fieldW);
					y = (int)Math.floor((float)y/fieldH);
					try{
						//in front of each line is a gap,
						//due to that they should be moved
						x-= data.getDesc().getLongestRow()-data.getDesc().getRow(y).size();

						DescField f = data.getDesc().getRow(y).get(x);
						if(e.getButton() == e.BUTTON1){
							setLeftColor(f.value);
							if(e.getClickCount() == 2){
								FieldsManager.getInstance((Window)getTopLevelAncestor(), getData(), f.value);
							}
						}else if(e.getButton() == e.BUTTON2){
							setMiddleColor(f.value);
						}else if(e.getButton() == e.BUTTON3){
							setRightColor(f.value);
						}

					}catch(IndexOutOfBoundsException ex){
					}

				//only when mouse over desc cols
				}else if(e.getX() > gridOffsetX && e.getY() > offsetY && e.getY() < gridOffsetY-(editMode?fieldH:0)){
					//position of mouse in px
					int x = e.getX()-gridOffsetX;
					int y = e.getY()-offsetY;
					//position of mouse in fields
					x = (int)Math.floor((float)x/fieldW);
					y = (int)Math.floor((float)y/fieldH);
					try{
						//in front of each line is a gap,
						//due to that they should be moved
						y-= data.getDesc().getLongestCol()-data.getDesc().getCol(x).size();

						DescField f = data.getDesc().getCol(x).get(y);
						if(e.getButton() == e.BUTTON1){
							setLeftColor(f.value);
							if(e.getClickCount() == 2){
								FieldsManager.getInstance((Window)getTopLevelAncestor(), getData(), f.value);
							}
						}else if(e.getButton() == e.BUTTON2)
							setMiddleColor(f.value);
						else if(e.getButton() == e.BUTTON3)
							setRightColor(f.value);
					}catch(IndexOutOfBoundsException ex){
					}

				}else if(editMode){
					if(e.getX() > gridOffsetX-fieldW && 
							e.getX() < gridOffsetX+gridW+fieldW &&
							e.getY() > gridOffsetY && 
							e.getY() < gridOffsetY+gridH){
						//add new col area
						if(e.getX() < gridOffsetX){
							//at the front
							data.addLeftCol();
						}else{
							//at the end
							data.addRightCol();
						}						
					}else if(e.getX() > gridOffsetX && 
							e.getX() < gridOffsetX+gridW &&
							e.getY() > gridOffsetY-fieldH &&
							e.getY() < gridOffsetY+gridH+fieldH){
						//add new row area
						if(e.getY() < gridOffsetY){
							//at the front
							data.addTopRow();
						}else{
							//at the end
							data.addBottomRow();
						}

					}
				}
			}
			public void mouseReleased(MouseEvent e){
				if(selectMode == BLOCK && selectionInProgress){
					undo(); //mouse was pressed one time and we have to undo it
					final int startX = currentlySelectedFieldX < lastSelectedFieldX?currentlySelectedFieldX:lastSelectedFieldX;
					final int startY = currentlySelectedFieldY < lastSelectedFieldY?currentlySelectedFieldY:lastSelectedFieldY;
					final int endX = currentlySelectedFieldX > lastSelectedFieldX?currentlySelectedFieldX:lastSelectedFieldX;
					final int endY = currentlySelectedFieldY > lastSelectedFieldY?currentlySelectedFieldY:lastSelectedFieldY;
					final int prevColor = paintingColor;
					int[][] tempGrid = new int[data.getW()][data.getH()];
					for(int x=startX; x<=endX; x++){
						for(int y=startY; y<=endY; y++){
							tempGrid[x][y] = data.getFieldVal(x,y);
							data.setFieldVal(paintingColor, x,y);
						}
					}
					final int[][] prevGrid = tempGrid;
					AbstractUndoableEdit edit = new AbstractUndoableEdit(){
						public void undo(){
							super.undo();
							for(int x=startX; x<=endX; x++){
								for(int y=startY; y<=endY; y++){
									data.setFieldVal(prevGrid[x][y], x,y);
								}
							}
						}
						public void redo(){
							super.redo();
							for(int x=startX; x<=endX; x++){
								for(int y=startY; y<=endY; y++){
									data.setFieldVal(prevColor, x,y);
								}
							}

						}
					};
					fireUndoableEditEventOccured(new UndoableEditEvent(this, edit));

				}
				selectionInProgress = false;
			}
		});
		addGriddlerDataListenerToData();
	}

	private void addGriddlerDataListenerToData(){
		data.addGriddlerDataListener(new GriddlerDataListener(){
			public void fieldChanged(int x, int y){
				if(x==-1){
					calcGridSize();
					repaint();
				}else repaint(gridOffsetX+x*fieldW, gridOffsetY+y*fieldH, fieldW, fieldH);
			}
			public void fieldsListChanged(){
				repaint();
			}
			public void descChanged(){
				//reinit!
				calcGridSize();
				repaint();
			}
			public void boardFinished(){
				if(finishAction != null) finishAction.actionPerformed(new ActionEvent(this, 0, ""));
			}
		});
	}


	int shortcutPrevColor = -3; //what color was on previous position
	int shortcutLastColor = -3; //what shortcut was previously pressed
	int shortcutCounter = 0;
	long shortcutLastTime = 0;
	public void shortcutPressed(int colorNum){
		long timeNow = Calendar.getInstance().getTimeInMillis();
		final int max_delay = 1000;
		if(timeNow-shortcutLastTime < max_delay && shortcutLastColor == colorNum){
			//multiple clicks
			shortcutCounter++;
			if(shortcutCounter%3 == 0){//foutrh click
				setRightColor(shortcutPrevColor);
				shortcutPrevColor = getLeftColorVal();
				setLeftColor(colorNum);
			}else if(shortcutCounter%3 == 1){//double click
				setLeftColor(shortcutPrevColor);
				shortcutPrevColor = getMiddleColorVal();
				setMiddleColor(colorNum);
			}else if(shortcutCounter%3 == 2){//triple click
				setMiddleColor(shortcutPrevColor);
				shortcutPrevColor = getRightColorVal();
				setRightColor(colorNum);
			}
		}else{
			//single click
			shortcutPrevColor = getLeftColorVal();
			setLeftColor(colorNum);
			shortcutCounter = 0;
		}
		shortcutLastTime = timeNow;
		shortcutLastColor = colorNum;
	}

	public void addHlChangeListener(ChangeListener l){
		hlChangeListenerList.add(ChangeListener.class, l);
	}
	public void removeHlChangeListener(ChangeListener l){
		hlChangeListenerList.remove(ChangeListener.class, l);
	}
	public void fireHlChange(){
		ChangeListener listeners[] =
			hlChangeListenerList.getListeners(ChangeListener.class);
		for(ChangeListener l: listeners){
			l.stateChanged(new ChangeEvent(this));
		}
	}

	public void addChangeListener(ChangeListener l){
		changeListenerList.add(ChangeListener.class, l);
	}
	public void removeChangeListener(ChangeListener l){
		changeListenerList.remove(ChangeListener.class, l);
	}
	public void fireChange(){
		ChangeListener listeners[] =
			changeListenerList.getListeners(ChangeListener.class);
		for(ChangeListener l: listeners){
			l.stateChanged(new ChangeEvent(this));
		}
	}
	public void addUndoableEditListener(UndoableEditListener l){
		undoableEditListenerList.add(UndoableEditListener.class, l);
	}
	public void removeUndoableEditListener(UndoableEditListener l){
		undoableEditListenerList.remove(UndoableEditListener.class, l);
	}
	public void fireUndoableEditEventOccured(UndoableEditEvent e){
		undoManager.addEdit(e.getEdit());
		UndoableEditListener listeners[] = 
			undoableEditListenerList.getListeners(UndoableEditListener.class);
		for(UndoableEditListener l: listeners){
			l.undoableEditHappened(e);
		}
	}


	public int getLeftColorVal(){
		return leftColorVal;
	}
	public int getMiddleColorVal(){
		return middleColorVal;
	}
	public int getRightColorVal(){
		return rightColorVal;
	}

	public void setLeftColor(int c){
		if(c<data.getFields().length && c>-2){
			fireChange();
			leftColorVal = c;
		}
	}
	public void setMiddleColor(int c){
		if(c<data.getFields().length && c>-2){
			fireChange();
			middleColorVal = c;
		}
	}
	public void setRightColor(int c){
		if(c<data.getFields().length && c>-2){
			fireChange();
			rightColorVal = c;
		}
	}

	public void setFieldW(int w){
		fieldW = w;
		calcGridSize();
		repaint();
	}
	public void setFieldH(int h){
		fieldH = h;
		calcGridSize();
		repaint();
	}
	public void setSelectMode(int s){
		selectMode = s;
		fireChange();
	}
	public int getSelectMode(){
		return selectMode;
	}
	public void nextSelectMode(){
		setSelectMode((getSelectMode()+1)%LINE);
	}

	public GriddlerData getData(){
		return data;
	}
	public void setData(GriddlerData gd){
		for(GriddlerDataListener listener: data.getGriddlerDataListeners()){
			gd.addGriddlerDataListener(listener);
		}
		data = gd;

		calcGridSize();
		repaint();
		revalidate();
	}
	
	public int getHlRow(){
		return hlRow;
	}
	public int getHlCol(){
		return hlCol;
	}
	public void setHlCol(int n){
		//repaint previous area
		if(hlCol == n) return;
		if(hlCol != -2) repaint(gridOffsetX+hlCol*fieldW-1, offsetY, fieldW+3, 1+gridH+gridOffsetY+(editMode?fieldW:0));

		if(n < 0 || n >= data.getW()) hlCol = -2;
		else{
		       	hlCol = n;
			//repaint new area
			repaint(gridOffsetX+hlCol*fieldW-1, offsetY, fieldW+3, 1+gridH+gridOffsetY+(editMode?fieldW:0));
		}
		fireHlChange();
	}
	public void setHlRow(int n){
		//repaint previous area
		if(hlRow == n) return;
		if(hlRow != -2) repaint(offsetX, gridOffsetY+hlRow*fieldH-1, 1+gridW+gridOffsetX+(editMode?fieldH:0), gridH+3);

		if(n < 0 || n >= data.getH()) hlRow = -2;
		else{
			hlRow = n;
			//repaint new area
			repaint(offsetX, gridOffsetY+hlRow*fieldH-1, gridW+gridOffsetX+(editMode?fieldH:0), gridH+3);
		}
		fireHlChange();
	}
	public void setEditMode(boolean editMode){
		this.editMode = editMode;

		calcGridSize();
		repaint();
		revalidate();
	}
	public boolean getEditMode(){
		return this.editMode;
	}


	protected void paintComponent(Graphics g){
		super.paintComponent(g);
		if(!paused){
			paintGrid(g);
			paintDesc((Graphics2D)g);
			paintLines(g);
		}else{
			paintPauseMsg(g);
		}
	}
	protected void paintGrid(Graphics g){
		for(int y=0; y<data.getH(); y++){
			for(int x=0; x<data.getW(); x++){
				Field f;
				if(selectMode == BLOCK && selectionInProgress){
					if(((x >= lastSelectedFieldX && x <= currentlySelectedFieldX) || 
					    (x <= lastSelectedFieldX && x >= currentlySelectedFieldX)) &&
					   ((y >= lastSelectedFieldY && y <= currentlySelectedFieldY) || 
					    (y <= lastSelectedFieldY && y >= currentlySelectedFieldY))){
							f = data.getField(paintingColor);
					}else{
						f = data.getField(x,y, !editMode);
					}
				}else{
					f = data.getField(x,y, !editMode);
				}
				f.paint(g,
					gridOffsetX+x*fieldW,
					gridOffsetY+y*fieldH,
					fieldW,
					fieldH
				);

			}
		}
	}

	protected void paintDesc(Graphics2D g){
		Color descColor = new Color(pref.getInt("descColor", new Color(0x00, 0x00, 0x00).getRGB()));

		int descOffsetY;
		int descOffsetX;
		if(editMode){
			descOffsetY = this.gridOffsetY - fieldH;
			descOffsetX = this.gridOffsetX - fieldW;
		}else{
			descOffsetY = this.gridOffsetY;
			descOffsetX = this.gridOffsetX;
		}
		for(int x=0; x<data.getDesc().getColsSize(); x++){
			int lineStart = descOffsetY-data.getDesc().getCol(x).size()*fieldH;
			int linePos = descOffsetX+x*fieldW;
			if(editMode) linePos += fieldW;
			for(int y=0; y<data.getDesc().getCol(x).size(); y++){
				DescField f = data.getDesc().getCol(x).get(y);
				Field field;
				try{
					field = data.getField(f.value);
				}catch(IndexOutOfBoundsException e){
					field = data.getField(0);
				}
				f.paint(g, field, linePos, lineStart+y*fieldH, fieldW, fieldH);
								g.setColor(lineColor);
				g.drawLine(linePos,
					lineStart+fieldH*y,
					linePos+fieldW,
					lineStart+fieldH*y);

			}
		}
		for(int y=0; y<data.getDesc().getRowsSize(); y++){
			int lineStart = descOffsetX-data.getDesc().getRow(y).size()*fieldW;
			int linePos = descOffsetY+y*fieldH;
			if(editMode) linePos += fieldH;
			for(int x=0; x<data.getDesc().getRow(y).size(); x++){
				DescField f = data.getDesc().getRow(y).get(x);
				Field field;
				try{
					field = data.getField(f.value);
				}catch(IndexOutOfBoundsException e){
					field = data.getField(0);
				}
				f.paint(g, field, lineStart+x*fieldW, linePos, fieldW, fieldH);

				g.setColor(lineColor);
				g.drawLine(lineStart+fieldW*x,
					linePos,
					lineStart+fieldW*x,
					linePos+fieldH);

			}
		}
	}

	protected void paintLines(Graphics g){
		g.setColor(lineColor);

		//draw border

		//left
		g.drawLine(	gridOffsetX-(editMode?fieldW:0),
				gridOffsetY,
				gridOffsetX-(editMode?fieldW:0),
				gridOffsetY+gridH);
		//right
		g.drawLine(	gridOffsetX+gridW+(editMode?fieldW:0),
				gridOffsetY,
				gridOffsetX+gridW+(editMode?fieldW:0),
				gridOffsetY+gridH);
		//bottom
		g.drawLine(	gridOffsetX,
				gridOffsetY+gridH+(editMode?fieldH:0),
				gridOffsetX+gridW,
				gridOffsetY+gridH+(editMode?fieldH:0));
		//top
		g.drawLine(	gridOffsetX,
				gridOffsetY-(editMode?fieldH:0),
				gridOffsetX+gridW,
				gridOffsetY-(editMode?fieldH:0));



		//draw grid
		g.setColor(lineColor);
		int prevLineL = 0;
		for(int y=0; y<=data.getH(); y++){
			//horizontal lines
			int lineL;
			//count line length
			if(y!=data.getH()){
				try{
					lineL = data.getDesc().getRow(y).size();
				}catch(IndexOutOfBoundsException e){
					lineL = 0;
				}
				if(editMode) lineL += 2;
			}else{
				lineL = prevLineL;
			}
			//draw line
			
			if(y%5==0) g.setColor(line5Color);


			g.drawLine(	linesOffsetX-(prevLineL>lineL?prevLineL:lineL)*fieldW,
					gridOffsetY+y*fieldH,
					linesOffsetX+data.getW()*fieldW,
					gridOffsetY+y*fieldH);

			if(y%5==0){
				g.drawLine(	linesOffsetX-(prevLineL>lineL?prevLineL:lineL)*fieldW,
						gridOffsetY+y*fieldH+1,
						linesOffsetX+data.getW()*fieldW,
						gridOffsetY+y*fieldH+1);
				g.setColor(lineColor);
			}

			prevLineL = lineL;
		}
		prevLineL = 0;
		for(int x=0; x<=data.getW(); x++){
			//vertical lines
			int lineL;
			if(x!=data.getW()){
				try{
					lineL = data.getDesc().getCol(x).size();
				}catch(IndexOutOfBoundsException e){
					lineL = 0;
				}
				if(editMode) lineL += 2;
			}else{
				lineL = prevLineL;
			}

			if(x%5==0) g.setColor(line5Color);
			g.drawLine(	gridOffsetX+x*fieldW,
					linesOffsetY-(prevLineL>lineL?prevLineL:lineL)*fieldH,
					gridOffsetX+x*fieldW,
					linesOffsetY+data.getH()*fieldH);
			prevLineL = lineL;
			if(x%5==0){
				g.drawLine(	gridOffsetX+x*fieldW+1,
						linesOffsetY-(prevLineL>lineL?prevLineL:lineL)*fieldH,
						gridOffsetX+x*fieldW+1,
						linesOffsetY+data.getH()*fieldH);

				g.setColor(lineColor);
			}
		}
		//draw highlighted lines
		g.setColor(hlLineColor);
		int lineL;
		if(hlRow>=0){
			try{
				lineL = data.getDesc().getRow(hlRow).size();
			}catch(IndexOutOfBoundsException e){
				lineL = 0;
			}
			if(editMode) lineL += 2;
			g.fillRect(	linesOffsetX-lineL*fieldW,
					gridOffsetY+hlRow*fieldH,
					gridW+lineL*fieldW+1,
					1);
			g.fillRect(	linesOffsetX-lineL*fieldW,
					gridOffsetY+(hlRow+1)*fieldH,
					gridW+lineL*fieldW+1,
					1);
		}
		if(hlCol>=0){
			try{
				lineL = data.getDesc().getCol(hlCol).size();
			}catch(IndexOutOfBoundsException e){
				lineL = 0;
			}
			if(editMode) lineL += 2;
			g.fillRect(	gridOffsetX+hlCol*fieldW,
					linesOffsetY-lineL*fieldH,
					1,
					gridH+lineL*fieldH+1);
			g.fillRect(	gridOffsetX+(hlCol+1)*fieldW,
					linesOffsetY-lineL*fieldH,
					1,
					gridH+lineL*fieldH+1);
		}
	}


	public void paintPauseMsg(Graphics g){
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
		g.drawString(TR.t("PAUSED"), 20, 30);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
		g.drawString(TR.t("Click anywhere on the board to unpause."), 20, 60);
	}
	public void pause(){
		paused = true;
		repaint();
	}
	public void unpause(){
		paused = false;
		repaint();
	}
	public void togglePause(){
		if(paused) unpause();
		else pause();
	}


	public boolean canUndo(){
		return undoManager.canUndo();
	}
	public void undo(){
		if(canUndo()) undoManager.undo();
	}
	public boolean canRedo(){
		return undoManager.canRedo();
	}
	public void redo(){
		if(canRedo()) undoManager.redo();
	}
	public Dimension getPreferredSize(){
		return new Dimension(gridOffsetX+gridW+(editMode?fieldW:0),gridOffsetY+gridH+(editMode?fieldH:0));
	}
	public Dimension getMinimumSize(){
		return getPreferredSize();
	}
}
