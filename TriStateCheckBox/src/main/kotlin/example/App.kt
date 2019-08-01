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
    @Transient
    protected var handler: HeaderCheckBoxHandler? = null

    override fun updateUI() {
      // [JDK-6788475] Changing to Nimbus LAF and back doesn't reset look and feel of JTable completely
      // https://bugs.openjdk.java.net/browse/JDK-6788475
      // XXX: set dummy ColorUIResource
      setSelectionForeground(ColorUIResource(Color.RED))
      setSelectionBackground(ColorUIResource(Color.RED))
      getTableHeader()?.removeMouseListener(handler)
      getModel()?.removeTableModelListener(handler)
      super.updateUI()

      getModel()?.also {
        for (i in 0 until it.getColumnCount()) {
          val r = getDefaultRenderer(it.getColumnClass(i)) as? Component ?: continue
          SwingUtilities.updateComponentTreeUI(r)
        }
        handler = HeaderCheckBoxHandler(this, CHECKBOX_COLUMN)
        it.addTableModelListener(handler)
        getTableHeader().addMouseListener(handler)
      }
      getColumnModel().getColumn(CHECKBOX_COLUMN).also {
        it.setHeaderRenderer(HeaderRenderer())
        it.setHeaderValue(Status.INDETERMINATE)
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
    add(JTabbedPane().also {
      val p = JPanel()
      p.add(checkBox)
      it.addTab("JCheckBox", p)
      it.addTab("JTableHeader", JScrollPane(table))
    })
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    private const val CHECKBOX_COLUMN = 0
  }
}

internal class HeaderRenderer : TableCellRenderer {
  private val check = TriStateCheckBox("Check All")

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val r = table.getTableHeader().getDefaultRenderer()
    val c = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

    check.setOpaque(false)
    if (value is Status) {
      check.updateStatus(value)
    } else {
      check.setSelected(true)
    }
    (c as? JLabel)?.also {
      it.setIcon(ComponentIcon(check))
      it.setText(null) // XXX: Nimbus???
    }
    return c
  }
}

internal class TriStateActionListener : ActionListener {
  private var currentIcon: Icon? = null

  fun setIcon(icon: Icon?) {
    this.currentIcon = icon
  }

  override fun actionPerformed(e: ActionEvent) {
    val cb = e.getSource() as? JCheckBox ?: return
    if (cb.isSelected()) {
      cb.getIcon()?.run {
        cb.setIcon(null)
        cb.setSelected(false)
      }
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
    private const val SIDE_MARGIN = 4
    private const val HEIGHT = 2
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
      runCatching {
        setLookAndFeel(m.getActionCommand())
      }.onFailure {
        it.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
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
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
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
