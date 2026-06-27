package compucrash;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.swing.ImageIcon;

public class CButtonFactory {

	private static CButtonFactory uniqueInstance = null;
	private Hashtable cButtons = new Hashtable();

	private CButtonFactory() {
		uniqueInstance = this;
		try {
			ResultSet rset =
				CDataManager.getInstance().getButtons();
			while (rset.next()) {
				CProperties cbp = new CProperties();
				cbp.put("bez", rset.getString(2));
				cbp.put("label", rset.getString(3));
				cbp.put("icon", new ImageIcon(rset.getString(4)));
				cbp.put("position", new Integer(rset.getInt(5)));
				cbp.put("tooltip", rset.getString(6));
				cbp.put("command", rset.getString(7));
				cbp.put("mnemonic", rset.getString(8));				
				cButtons.put((String)cbp.get("bez"),cbp);
			}
		} catch (SQLException e) {
			System.out.println(e);
		}
	}

	public void dispose() {
		uniqueInstance = null;
	}
	
	public CProperties getCButtonProperty(String bez) {
		return (CProperties)cButtons.get("bez");
	}
	
	public static CButtonFactory getCButtonFactory() {
		if (uniqueInstance == null) return new CButtonFactory();
		return uniqueInstance;
	}
	
	public static CButton getButton(String bez) {	
		if ((CProperties)getCButtonFactory().cButtons.get(bez) == null) {
			CProperties cbp = new CProperties();
			cbp.put("bez", bez);
			cbp.put("label", bez);
			cbp.put("icon", new ImageIcon());
			cbp.put("position", new Integer(0));
			cbp.put("tooltip", null);
			cbp.put("command", null);
			return new CButton(cbp);
		}
		return new CButton((CProperties)getCButtonFactory().cButtons.get(bez));
	}
	
	class CButtonProperty {
		private String bez;
		private String label;
		private ImageIcon icon;
		private int position;
		private String toolTip;

		public CButtonProperty() {
		}

		/**
		 * 
		 * @uml.property name="bez"
		 */
		public String getBez() {
			return bez;
		}

		/**
		 * 
		 * @uml.property name="icon"
		 */
		public ImageIcon getIcon() {
			return icon;
		}

		/**
		 * 
		 * @uml.property name="label"
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * 
		 * @uml.property name="position"
		 */
		public int getPosition() {
			return position;
		}

		/**
		 * 
		 * @uml.property name="toolTip"
		 */
		public String getToolTip() {
			return toolTip;
		}

		/**
		 * 
		 * @uml.property name="bez"
		 */
		public void setBez(String string) {
			bez = string;
		}

		/**
		 * 
		 * @uml.property name="icon"
		 */
		public void setIcon(ImageIcon icon) {
			this.icon = icon;
		}

		/**
		 * 
		 * @uml.property name="label"
		 */
		public void setLabel(String string) {
			label = string;
		}

		/**
		 * 
		 * @uml.property name="position"
		 */
		public void setPosition(int i) {
			position = i;
		}

		/**
		 * 
		 * @uml.property name="toolTip"
		 */
		public void setToolTip(String string) {
			toolTip = string;
		}

	}
}
