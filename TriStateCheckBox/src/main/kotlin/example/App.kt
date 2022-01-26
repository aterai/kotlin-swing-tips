package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn

private const val CHECKBOX_COLUMN = 0
private val checkBox = TriStateCheckBox("TriState JCheckBox")
private val columnNames = arrayOf(Status.INDETERMINATE, "Integer", "String")
private val data = arrayOf(
  arrayOf(true, 1, "BBB"),
  arrayOf(false, 12, "AAA"),
  arrayOf(true, 2, "DDD"),
  arrayOf(false, 5, "CCC"),
  arrayOf(true, 3, "EEE"),
  arrayOf(false, 6, "GGG"),
  arrayOf(true, 4, "FFF"),
  arrayOf(false, 7, "HHH")
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
    model?.removeTableModelListener(handler)
    super.updateUI()

    model?.also {
      for (i in 0 until it.columnCount) {
        val r = getDefaultRenderer(it.getColumnClass(i)) as? Component ?: continue
        SwingUtilities.updateComponentTreeUI(r)
      }
      handler = HeaderCheckBoxHandler(this, CHECKBOX_COLUMN)
      it.addTableModelListener(handler)
      getTableHeader().addMouseListener(handler)
    }
    getColumnModel().getColumn(CHECKBOX_COLUMN).also {
      it.headerRenderer = HeaderRenderer()
      it.headerValue = Status.INDETERMINATE
    }
  }

  override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
    val c = super.prepareEditor(editor, row, column)
    (c as? JCheckBox)?.also {
      it.background = getSelectionBackground()
      it.isBorderPainted = true
    }
    return c
  }
}

fun makeUI() = JTabbedPane().also {
  val mb = JMenuBar()
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())
  EventQueue.invokeLater { it.rootPane.jMenuBar = mb }

  val p = JPanel()
  p.add(checkBox)
  it.addTab("JCheckBox", p)
  it.addTab("JTableHeader", JScrollPane(table))
  it.preferredSize = Dimension(320, 240)
}

private class HeaderRenderer : TableCellRenderer {
  private val check = TriStateCheckBox("Check All")

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

    check.isOpaque = false
    if (value is Status) {
      check.updateStatus(value)
    } else {
      check.isSelected = true
    }
    (c as? JLabel)?.also {
      it.icon = ComponentIcon(check)
      it.text = null // XXX: Nimbus???
    }
    return c
  }
}

private class TriStateActionListener : ActionListener {
  private var currentIcon: Icon? = null

  fun setIcon(icon: Icon?) {
    this.currentIcon = icon
  }

  override fun actionPerformed(e: ActionEvent) {
    val cb = e.source as? JCheckBox ?: return
    if (cb.isSelected) {
      cb.icon?.run {
        cb.icon = null
        cb.isSelected = false
      }
    } else {
      cb.icon = currentIcon
    }
  }
}

private class TriStateCheckBox(title: String) : JCheckBox(title) {
  private var listener: TriStateActionListener? = null
  private var currentIcon: Icon? = null

  fun updateStatus(s: Status) {
    when (s) {
      Status.SELECTED -> {
        isSelected = true
        icon = null
      }
      Status.DESELECTED -> {
        isSelected = false
        icon = null
      }
      Status.INDETERMINATE -> {
        isSelected = false
        icon = currentIcon
      }
      // else -> throw AssertionError("Unknown Status")
    }
  }

  override fun updateUI() {
    icon = null
    removeActionListener(listener)
    super.updateUI()
    val indeterminateIcon = IndeterminateIcon()
    val al = TriStateActionListener()
    al.setIcon(indeterminateIcon)
    listener = al
    currentIcon = indeterminateIcon
    addActionListener(listener)
    icon?.run {
      icon = currentIcon
    }
  }
}

private class IndeterminateIcon : Icon {
  private val icon = UIManager.getIcon("CheckBox.icon")

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    icon.paintIcon(c, g2, 0, 0)
    g2.paint = FOREGROUND
    g2.fillRect(SIDE_MARGIN, (iconHeight - HEIGHT) / 2, iconWidth - SIDE_MARGIN - SIDE_MARGIN, HEIGHT)
    g2.dispose()
  }

  override fun getIconWidth() = icon.iconWidth

  override fun getIconHeight() = icon.iconHeight

  companion object {
    private val FOREGROUND = Color.BLACK // TEST: UIManager.getColor("CheckBox.foreground");
    private const val SIDE_MARGIN = 4
    private const val HEIGHT = 2
  }
}

private class ComponentIcon(private val cmp: Component) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    SwingUtilities.paintComponent(g, cmp, c.parent, x, y, iconWidth, iconHeight)
  }

  override fun getIconWidth() = cmp.preferredSize.width

  override fun getIconHeight() = cmp.preferredSize.height
}

private enum class Status {
  SELECTED, DESELECTED, INDETERMINATE
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val lafGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      menu.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafGroup))
    }
    return menu
  }

  private fun createLookAndFeelItem(
    lafName: String,
    lafClassName: String,
    lafGroup: ButtonGroup
  ): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.hideActionText = true
    lafItem.addActionListener { e ->
      val m = lafGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
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
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
}

private class HeaderCheckBoxHandler(
  val table: JTable,
  val targetColumnIndex: Int
) : MouseAdapter(), TableModelListener {
  override fun tableChanged(e: TableModelEvent) {
    if (e.type == TableModelEvent.UPDATE && e.column == targetColumnIndex) {
      val vci = table.convertColumnIndexToView(targetColumnIndex)
      val column = table.columnModel.getColumn(vci)
      val status = column.headerValue
      val m = table.model
      if (m is DefaultTableModel && fireUpdateEvent(m, column, status)) {
        val h = table.tableHeader
        h.repaint(h.getHeaderRect(vci))
      }
    }
  }

  private fun fireUpdateEvent(m: DefaultTableModel, column: TableColumn, status: Any): Boolean {
    return if (Status.INDETERMINATE == status) {
      // val l = (m.getDataVector() as Vector<*>).stream()
      //     .map { v -> (v as Vector<*>).get(targetColumnIndex) as Boolean }
      //     .distinct()
      //     .collect(Collectors.toList())
      val l = m.dataVector.mapNotNull { (it as? List<*>)?.get(targetColumnIndex) as? Boolean }.distinct()
      val isOnlyOneSelected = l.size == 1
      if (isOnlyOneSelected) {
        // column.setHeaderValue(if (l.get(0)) Status.SELECTED else Status.DESELECTED)
        column.headerValue = if (l.first()) Status.SELECTED else Status.DESELECTED
        true
      } else {
        false
      }
    } else {
      column.headerValue = Status.INDETERMINATE
      true
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val header = e.component as? JTableHeader ?: return
    val tbl = header.table
    val columnModel = tbl.columnModel
    val m = tbl.model
    val vci = columnModel.getColumnIndexAtX(e.x)
    val mci = tbl.convertColumnIndexToModel(vci)
    if (mci == targetColumnIndex && m.rowCount > 0) {
      val column = columnModel.getColumn(vci)
      val b = Status.DESELECTED === column.headerValue
      for (i in 0 until m.rowCount) {
        m.setValueAt(b, i, mci)
      }
      column.headerValue = if (b) Status.SELECTED else Status.DESELECTED
      // header.repaint()
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
