package alma.acs.gui.widgets;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import alma.acs.gui.util.threadsupport.EDTExecutor;

/*
ALMA - Atacama Large Millimiter Array
* Copyright (c) European Southern Observatory, 2013 
* 
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
* 
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
* 
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/

/**
 * <CODE>CheckList</CODE> is widget that shows a list of entries and a check box to enable/disable
 * each of them.
 * It has been written to replace <CODE>com.cosylab.gui.components.r2.JCheckList</CODE> that is not 
 * maintained anymore.
 * <P>
 * The user is presented with a list of items each of which has a associated check box:
 * each item in the list can be selectively enabled/disabled by using the checkbox.
 * <P> 
 * <CODE>CheckList</CODE> is a table of 2 columns: the first one is a boolean whose state
 * says if the item is selected or not. The second one is a not editable string.
 * <P>
 * The model of this widget accepts any not <code>null</code> java {@link Object}'s. 
 * The text shown in the second column is generated by invoking   {@link Object#toString()}.
 * 
 * @author  acaproni
 * @since   ACS 12.1
 */
public class CheckList extends JComponent {
	
	/**
	 * Each entry in the list is composed of a {@link Boolean} and a {@link Object}
	 * @author acaproni
	 *
	 */
	public class CheckListTableEntry {
		
		/**
		 * The activation state
		 */
		private boolean active;
		
		/**
		 * The entry
		 */
		private final Object item;
		
		/**
		 * Constructor
		 * 
		 * @param active The activation state
		 * @param item The item in the list
		 */
		CheckListTableEntry(boolean active, Object item) {
			if (item==null) {
				throw new IllegalArgumentException("Null entries are not allowed");
			}
			this.active=active;
			this.item=item;
		}
		
		/**
		 * Activate/deactivate the entry.
		 * 
		 * @param active if <code>true</code> the entry is active
		 */
		public void activate(boolean active) {
			this.active=active;
		}

		/**
		 * @return the activation state of the entry
		 */
		public boolean isActive() {
			return active;
		}

		/**
		 * Getter
		 * 
		 * @return the item
		 */
		public Object getItem() {
			return item;
		}
	}
	
	/**
	 * The table model for the <CODE>CheckList</CODE> widget.
	 * <P>
	 * Each entry is composed of the {@link Object} added to the widget
	 * and a {@link Boolean} representing is activation state.
	 * 
	 * @author acaproni
	 *
	 */
	private class CheckListTableModel extends AbstractTableModel {
		
		/**
		 * The items in the widget.
		 */
		private final List<CheckListTableEntry> items = Collections.synchronizedList(new Vector<CheckListTableEntry>());
		
		/**
		 * Add the passed object to the items of the list.
		 * <P>
		 * The object can't be <code>null</code>.
		 * 
		 * @param active The activation state
		 * @param obj The not <code>null</code> object to add
		 * @return the added entry
		 */
		public CheckListTableEntry addElement(boolean active, Object obj) {
			CheckListTableEntry ret=new CheckListTableEntry(active,obj);
			items.add(ret);
			EDTExecutor.instance().execute(new Runnable() {
				@Override
				public void run() {
					fireTableDataChanged();
				}
			});
			return ret;
		}

		/**
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		@Override
		public int getRowCount() {
			return items.size();
		}

		/**
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {
			return 2;
		}

		/** 
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			synchronized (items) {
				if (rowIndex<0 || rowIndex>=items.size()) {
					throw new IllegalArgumentException("Row index ("+rowIndex+") out of range (size is "+items.size()+")");
				}
				if (columnIndex==0) {
					return items.get(rowIndex).isActive();
				} else {
					return items.get(rowIndex).getItem().toString();
				}	
			}
		}
		
		/** 
		 * @see javax.swing.table.TableModel#isCellEditable(int, int)
		 */
		@Override
		public boolean isCellEditable(int row, int column) {
			return (column==0);
		}
		
		/** 
		 * @see javax.swing.table.TableModel#getColumnClass(int)
		 */
		@Override
		public Class getColumnClass(int column) {
			return (column==0)?Boolean.class: String.class;
		  }
		
		/**
		 * @return A array with all the items in the list
		 */
		public List<CheckListTableEntry> getEntries() {
			return items;
		}
		
		/**
		 * @return A list with all the active items in the list
		 */
		public List<CheckListTableEntry> getCheckedEntries() {
			Vector<CheckListTableEntry> ret = new Vector<CheckList.CheckListTableEntry>();
			synchronized (items) {
				for (CheckListTableEntry entry: items) {
					if (entry.isActive()) {
						ret.add(entry);
					}
				}
			}
			return ret;
		}
		
		/**
		 * @return A list with all the non-active items in the list
		 */
		public List<CheckListTableEntry> getUnCheckedEntries() {
			Vector<CheckListTableEntry> ret = new Vector<CheckList.CheckListTableEntry>();
			synchronized (items) {
				for (CheckListTableEntry entry: items) {
					if (!entry.isActive()) {
						ret.add(entry);
					}
				}
			}
			return ret;
		}
		
		/**
		 * Remove all the entries from the widget
		 */
		public void clear() {
			EDTExecutor.instance().execute(new Runnable() {
				@Override
				public void run() {
					items.clear();
					fireTableDataChanged();
				}
			});
		}
		
		/**
		 * @return The number of items whose state is active
		 */
		public int getActiveItemsNumber() {
			int ret=0;
			synchronized (items) {
				for (CheckListTableEntry entry: items) {
					if (entry.isActive()) {
						ret++;
					}
				}
			}
			return ret;
		}

		/**
		 * 
		 * @param index The index of the entry in the list
		 * @return the entry in the passed position
		 * 
		 * @see java.util.List#get(int)
		 */
		public CheckListTableEntry get(int index) {
			synchronized (items) {
				if (index<0 || index>=items.size()) {
					throw new IllegalArgumentException("Invalid index of item to get: "+index+"; size of list is "+items.size());
				}
			}
			return items.get(index);
		}
		
		/**
		 * Activate or deactivate all the items in the widget
		 * 
		 * @param active if <code>true</code> activate the items, otherwise deactivate
		 */
		public void activateAll(final boolean active) {
			EDTExecutor.instance().execute(new Runnable() {
				@Override
				public void run() {
					synchronized (items) {
						for (CheckListTableEntry entry: items) {
							entry.activate(active);
						}
					}
					fireTableDataChanged();
				}
			});			
		}

		/**
		 * @param index the index of the item to remove
		 * @return The removed item or <code>null</code> if the item was not in the list
		 * @see java.util.List#remove(int)
		 */
		public CheckListTableEntry removeEntry(final int index) {
			final class Remover implements Runnable {
				public CheckListTableEntry ret=null;
				/* (non-Javadoc)
				 * @see java.lang.Runnable#run()
				 */
				@Override
				public void run() {
					ret=items.remove(index);
					fireTableDataChanged();
				}
			}
			Remover remover = new Remover();
			try {
				EDTExecutor.instance().executeSync(remover);
			} catch (Throwable t) {
				System.err.println("Error remvoving item "+index);
				t.printStackTrace(System.err);
				return null;
			}
			return remover.ret;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int column) {
			if (column==0) {
				return "Activation state";
			} else {
				return "Object";
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
		 */
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			items.get(rowIndex).activate((Boolean)aValue);
			super.setValueAt(aValue, rowIndex, columnIndex);
		}
	}
	
	/**
	 * The model
	 */
	private final CheckListTableModel model = new CheckListTableModel();
	
	/**
	 * The table showing the entries
	 */
	private final JTable table = new JTable(model);
	
	/**
	 * Constructor
	 */
	public CheckList() {
		initGUI();
	}
	
	/**
	 * Initialize the GUI
	 */
	private void initGUI() {
		EDTExecutor.instance().execute(new Runnable() {
			public void run() {
				setLayout(new BorderLayout());
				add(new JScrollPane(table), BorderLayout.CENTER);
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
				FontMetrics fm = table.getFontMetrics(table.getFont());
				int strw = fm.stringWidth(model.getColumnName(0))+10;
				table.getColumnModel().getColumn(0).setPreferredWidth(strw);
				table.getColumnModel().getColumn(0).setMaxWidth(strw);
				table.getColumnModel().getColumn(0).setMinWidth(strw);
			}
		});
	}

	/**
	 * @return The entries in the list
	 * 
	 * @see alma.acs.gui.widgets.CheckList.CheckListTableModel#getEntries()
	 */
	public List<CheckListTableEntry> getEntries() {
		return model.getEntries();
	}

	/**
	 * @return
	 * @see alma.acs.gui.widgets.CheckList.CheckListTableModel#getCheckedEntries()
	 */
	public List getCheckedEntries() {
		return model.getCheckedEntries();
	}

	/**
	 * @return
	 * @see alma.acs.gui.widgets.CheckList.CheckListTableModel#getUnCheckedEntries()
	 */
	public List getUnCheckedEntries() {
		return model.getUnCheckedEntries();
	}

	/**
	 * Remove all the entries from the widget
	 */
	public void clear() {
		model.clear();
	}

	/**
	 * Add a element to the list with its initial activation state.
	 * 
	 * @param active The activation state of the item
	 * @param obj The not <code>null</code> item to add
	 * @return the added entry
	 * @see alma.acs.gui.widgets.CheckList.CheckListTableModel#addElement(boolean, java.lang.Object)
	 */
	public CheckListTableEntry addElement(boolean active, Object obj) {
		return model.addElement(active, obj);
	}
	
	/**
	 * @return The number of elements (regardless of their activation state) in the widget
	 */
	public int getItemsSize() {
		return model.getRowCount();
	}

	/**
	 * @return
	 * @see alma.acs.gui.widgets.CheckList.CheckListTableModel#getActiveItemsNumber()
	 */
	public int getActiveItemsNumber() {
		return model.getActiveItemsNumber();
	}
	
	/**
	 * @return The item selected by the user
	 */
	public CheckListTableEntry getSelectedEntry() {
		int selectedRow=table.getSelectedRow();
		if (selectedRow==-1) {
			return null;
		}
		return model.get(selectedRow);
	}
	
	/**
	 * 
	 * @return The number of the row selected by the user
	 */
	public int getSelectedIndex() {
		return table.getSelectedRow();
	}

	/**
	 * Activate ar deactivate all the itemes of the widget
	 * 
	 * @param active if <code>true</code> activate the items
	 * @see alma.acs.gui.widgets.CheckList.CheckListTableModel#activateAll(boolean)
	 */
	public void activateAll(boolean active) {
		model.activateAll(active);
	}

	/**
	 * Remove the entry in the passed position from the widget.
	 * 
	 * @param index The index of the item to remove
	 * @return The removed entry
	 * @see alma.acs.gui.widgets.CheckList.CheckListTableModel#removeEntry(int)
	 */
	public CheckListTableEntry removeEntry(int index) {
		return model.removeEntry(index);
	}
	
	/**
	 * Remove the selected entry from the widget
	 * 
	 * @return The removed entry or <code>null</code> if the entry was not found
	 */
	public CheckListTableEntry removeSelectedEntry() {
		int selectedRow=table.getSelectedRow();
		if (selectedRow==-1) {
			return null;
		}
		return removeEntry(selectedRow);
	}
}
