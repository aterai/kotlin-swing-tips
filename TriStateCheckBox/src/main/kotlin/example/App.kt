package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  private val checkBox = TriStateCheckBox("TriState JCheckBox")
  private val columnNames = arrayOf<Any>(Status.INDETERMINATE, "Integer", "String")
  private val data = arrayOf(
    arrayOf<Any>(true, 1, "BBB"),
    arrayOf<Any>(false, 12, "AAA"),
    arrayOf<Any>(true, 2, "DDD"),
    arrayOf<Any>(false, 5, "CCC"),
    arrayOf<Any>(true, 3, "EEE"),
    arrayOf<Any>(false, 6, "GGG"),
    arrayOf<Any>(true, 4, "FFF"),
    arrayOf<Any>(false, 7, "HHH")
  )
  private val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  private val table = object : JTable(model) {
    protected val CHECKBOX_COLUMN = 0
    protected var handler: HeaderCheckBoxHandler? = null

    override fun updateUI() {
      // [JDK-6788475] Changing to Nimbus LAF and back doesn't reset look and feel of JTable completely
      // https://bugs.openjdk.java.net/browse/JDK-6788475
      // XXX: set dummy ColorUIResource
      setSelectionForeground(ColorUIResource(Color.RED))
      setSelectionBackground(ColorUIResource(Color.RED))
      getTableHeader().removeMouseListener(handler)
      getModel()?.let { it.removeTableModelListener(handler) }

      super.updateUI()

      getModel()?.let {
        for (i in 0 until it.getColumnCount()) {
          val r = getDefaultRenderer(it.getColumnClass(i)) as? Component ?: continue
          SwingUtilities.updateComponentTreeUI(r)
        }
        getColumnModel()?.getColumn(CHECKBOX_COLUMN)?.apply {
          setHeaderRenderer(HeaderRenderer())
          setHeaderValue(Status.INDETERMINATE)
        }

        handler = HeaderCheckBoxHandler(this, CHECKBOX_COLUMN)
        it.addTableModelListener(handler)
        getTableHeader().addMouseListener(handler)
      }
    }

    override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
      val c = super.prepareEditor(editor, row, column)
      (c as? JCheckBox)?.also {
        it.setBackground(getSelectionBackground())
        it.setBorderPainted(true)
      }
      return c
    }
  }

  init {
    add(JTabbedPane().apply {
      val p = JPanel()
      p.add(checkBox)
      addTab("JCheckBox", p)
      addTab("JTableHeader", JScrollPane(table))
    })
    setPreferredSize(Dimension(320, 240))
  }
}

internal class HeaderRenderer : TableCellRenderer {
  private val check = TriStateCheckBox("Check All")

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val r = table.getTableHeader().getDefaultRenderer()
    val l = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel

    check.setOpaque(false)
    if (value is Status) {
      check.updateStatus(value)
    } else {
      check.setSelected(true)
    }
    l.setIcon(ComponentIcon(check))
    l.setText(null) // XXX: Nimbus???
    // System.out.println("getHeaderRect: " + table.getTableHeader().getHeaderRect(column));
    // System.out.println("getPreferredSize: " + l.getPreferredSize());
    // System.out.println("getMaximunSize: " + l.getMaximumSize());
    // System.out.println("----");
    // if (l.getPreferredSize().height > 1000) { // XXX: Nimbus???
    //   System.out.println(l.getPreferredSize().height);
    //   Rectangle rect = table.getTableHeader().getHeaderRect(column);
    //   l.setPreferredSize(new Dimension(0, rect.height));
    // }
    return l
  }
}

internal class TriStateActionListener : ActionListener {
  private var currentIcon: Icon? = null

  fun setIcon(icon: Icon?) {
    this.currentIcon = icon
  }

  override fun actionPerformed(e: ActionEvent) {
    val cb = e.getSource() as JCheckBox
    if (cb.isSelected()) {
      cb.getIcon()?.run {
        cb.setIcon(null)
        cb.setSelected(false)
      }
      // if (cb.getIcon() != null) {
      //   cb.setIcon(null)
      //   cb.setSelected(false)
      // }
    } else {
      cb.setIcon(currentIcon)
    }
  }
}

internal class TriStateCheckBox(title: String) : JCheckBox(title) {
  protected var listener: TriStateActionListener? = null
  private var currentIcon: Icon? = null

  fun updateStatus(s: Status) {
    when (s) {
      Status.SELECTED -> {
        setSelected(true)
        setIcon(null)
      }
      Status.DESELECTED -> {
        setSelected(false)
        setIcon(null)
      }
      Status.INDETERMINATE -> {
        setSelected(false)
        setIcon(currentIcon)
      }
      // else -> throw AssertionError("Unknown Status")
    }
  }

  override fun updateUI() {
    setIcon(null)
    removeActionListener(listener)
    super.updateUI()
    val iicon = IndeterminateIcon()
    val al = TriStateActionListener()
    al.setIcon(iicon)
    listener = al
    currentIcon = iicon
    addActionListener(listener)
    // if (getIcon() != null) {
    //   setIcon(currentIcon)
    // }
    getIcon()?.run {
      setIcon(currentIcon)
    }
  }
}

internal class IndeterminateIcon : Icon {
  private val icon = UIManager.getIcon("CheckBox.icon")

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    icon.paintIcon(c, g2, 0, 0)
    g2.setPaint(FOREGROUND)
    g2.fillRect(SIDE_MARGIN, (getIconHeight() - HEIGHT) / 2, getIconWidth() - SIDE_MARGIN - SIDE_MARGIN, HEIGHT)
    g2.dispose()
  }

  override fun getIconWidth() = icon.getIconWidth()

  override fun getIconHeight() = icon.getIconHeight()

  companion object {
    private val FOREGROUND = Color.BLACK // TEST: UIManager.getColor("CheckBox.foreground");
    private val SIDE_MARGIN = 4
    private val HEIGHT = 2
  }
}

internal class ComponentIcon(private val cmp: Component) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    SwingUtilities.paintComponent(g, cmp, c.getParent(), x, y, getIconWidth(), getIconHeight())
  }

  override fun getIconWidth() = cmp.getPreferredSize().width

  override fun getIconHeight() = cmp.getPreferredSize().height
}

internal enum class Status {
  SELECTED, DESELECTED, INDETERMINATE
}

// @see https://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/SwingSet3.java
internal object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.getName()

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val lafGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      menu.add(createLookAndFeelItem(lafInfo.getName(), lafInfo.getClassName(), lafGroup))
    }
    return menu
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.setActionCommand(lafClassName)
    lafItem.setHideActionText(true)
    lafItem.addActionListener {
      val m = lafGroup.getSelection()
      try {
        setLookAndFeel(m.getActionCommand())
      } catch (ex: ClassNotFoundException) {
        ex.printStackTrace()
      } catch (ex: InstantiationException) {
        ex.printStackTrace()
      } catch (ex: IllegalAccessException) {
        ex.printStackTrace()
      } catch (ex: UnsupportedLookAndFeelException) {
        ex.printStackTrace()
      }
    }
    lafGroup.add(lafItem)
    return lafItem
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(lookAndFeel: String) {
    val oldLookAndFeel = LookAndFeelUtil.lookAndFeel
    if (oldLookAndFeel != lookAndFeel) {
      UIManager.setLookAndFeel(lookAndFeel)
      LookAndFeelUtil.lookAndFeel = lookAndFeel
      updateLookAndFeel()
      // firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel);
    }
  }

  private fun updateLookAndFeel() {
    for (window in Frame.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
} /* Singleton */

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
