package example

import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  UIManager.put("TableHeader.rightAlignSortArrow", false)
  val columnNames = arrayOf("Name", "CPU", "Memory", "Disk")
  val data = arrayOf(
    arrayOf("aaa", "1%", "1.6MB", "0MB/S"),
    arrayOf("bbb", "1%", "2.4MB", "3MB/S"),
    arrayOf("ccc", "2%", "0.3MB", "1MB/S"),
    arrayOf("ddd", "3%", "0.5MB", "2MB/S"),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = String::class.java
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      val cr = DefaultTableCellRenderer()
      cr.horizontalAlignment = SwingConstants.RIGHT
      val hr = SortIconLayoutHeaderRenderer(this)
      val cm = getColumnModel()
      for (i in 1..<cm.columnCount) {
        val tc = cm.getColumn(i)
        tc.headerRenderer = hr
        tc.cellRenderer = cr
      }
    }
  }
  table.autoCreateRowSorter = true
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class EmptyIcon : Icon {
  var width = 5
  var height = 5

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    // do nothing
  }

  override fun getIconWidth() = width

  override fun getIconHeight() = height
}

private class SortIconLayoutHeaderRenderer(
  component: Component,
) : TableCellRenderer {
  private val ascendingIcon = UIManager.getLookAndFeelDefaults().getIcon(ASCENDING)
  private val ascendingUri = getIconUri(ascendingIcon, component)
  private val descendingIcon = UIManager.getLookAndFeelDefaults().getIcon(DESCENDING)
  private val descendingUri = getIconUri(descendingIcon, component)
  private val emptyIcon = EmptyIcon()
  private val naturalUri = getIconUri(emptyIcon, component)

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    emptyIcon.width = ascendingIcon.iconWidth
    emptyIcon.height = ascendingIcon.iconHeight
    UIManager.put(ASCENDING, emptyIcon)
    UIManager.put(DESCENDING, emptyIcon)
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    UIManager.put(ASCENDING, ascendingIcon)
    UIManager.put(DESCENDING, descendingIcon)
    if (c is JLabel) {
      val sortOrder = getColumnSortOrder(table, column)
      val sortUri = when (sortOrder) {
        SortOrder.ASCENDING -> ascendingUri
        SortOrder.DESCENDING -> descendingUri
        else -> naturalUri
      }
      val v = 10
      val img = "<img src='%s'>".format(sortUri)
      val pct = "<td align='right'>%d%%".format(v)
      val fmt = "<html><table><tr><td>%s%s<tr><td><td align='right'>%s"
      c.text = fmt.format(img, pct, value)
    }
    return c
  }

  companion object {
    private const val ASCENDING = "Table.ascendingSortIcon"
    private const val DESCENDING = "Table.descendingSortIcon"

    fun getColumnSortOrder(
      table: JTable?,
      column: Int,
    ): SortOrder {
      var rv = SortOrder.UNSORTED
      if (table != null && table.rowSorter != null) {
        val sortKeys = table.rowSorter.sortKeys
        val mi = table.convertColumnIndexToModel(column)
        if (sortKeys.isNotEmpty() && sortKeys[0].column == mi) {
          rv = sortKeys[0].sortOrder
        }
      }
      return rv
    }
  }
}

fun getIconUri(
  icon: Icon,
  c: Component,
): URI? {
  val w = icon.iconWidth
  val h = icon.iconHeight
  val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = img.createGraphics()
  icon.paintIcon(c, g2, 0, 0)
  g2.dispose()
  return runCatching {
    val tmp = File.createTempFile("icon", ".png")
    tmp.deleteOnExit()
    ImageIO.write(img, "png", tmp)
    tmp.toURI()
  }.onFailure {
    UIManager.getLookAndFeel().provideErrorFeedback(c)
  }.getOrNull()
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
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
