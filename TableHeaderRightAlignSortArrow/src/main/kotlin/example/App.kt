package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true

  val key = "TableHeader.rightAlignSortArrow"
  val check = object : JCheckBox(key, UIManager.getBoolean(key)) {
    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        val b = UIManager.getLookAndFeelDefaults().getBoolean(key)
        isSelected = b
        updateSortAlign(table, key, b)
      }
    }
  }
  check.isOpaque = false
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    updateSortAlign(table, key, b)
  }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())
  mb.add(Box.createHorizontalGlue())
  mb.add(check)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

fun updateSortAlign(table: JTable, key: String?, b: Boolean) {
  UIManager.put(key, b)
  table.tableHeader.cursor = Cursor.getDefaultCursor()
  SwingUtilities.updateComponentTreeUI(table)
}

private object LookAndFeelUtil {
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

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
    UnsupportedLookAndFeelException::class
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
