package example

import java.awt.*
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

fun makeUI() = JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
  it.topComponent = JScrollPane(makeTable())
  it.bottomComponent = JLayer(JScrollPane(makeTable()), TableHeaderFillerLayerUI())
  it.resizeWeight = .5
  it.preferredSize = Dimension(320, 240)
}

private fun makeTable() = JTable(4, 3).also {
  it.autoResizeMode = JTable.AUTO_RESIZE_OFF
  it.autoCreateRowSorter = true
}

private class TableHeaderFillerLayerUI : LayerUI<JScrollPane>() {
  private val tempTable = JTable(DefaultTableModel(arrayOf(""), 0))
  private val filler = tempTable.tableHeader
  private val fillerColumn = tempTable.columnModel.getColumn(0)

  override fun updateUI(l: JLayer<out JScrollPane>?) {
    super.updateUI(l)
    SwingUtilities.updateComponentTreeUI(tempTable)
  }

  override fun paint(g: Graphics?, c: JComponent) {
    super.paint(g, c)
    val scroll = (c as? JLayer<*>)?.view as? JScrollPane ?: return
    val table = scroll.viewport.view as? JTable ?: return
    val header = table.tableHeader

    var width = header.width
    val cm = header.columnModel
    for (i in 0 until cm.columnCount) {
      width -= cm.getColumn(i).width
    }

    val pt = SwingUtilities.convertPoint(header, 0, 0, c)
    filler.setLocation(pt.x + header.width - width, pt.y)
    filler.setSize(width, header.height)
    fillerColumn.width = width

    SwingUtilities.paintComponent(g, filler, tempTable, filler.bounds)
  }

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.COMPONENT_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent?) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processComponentEvent(e: ComponentEvent, l: JLayer<out JScrollPane>) {
    val c = e.component as? JTableHeader ?: return
    l.repaint(c.bounds)
  }
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
    b: AbstractButton
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
