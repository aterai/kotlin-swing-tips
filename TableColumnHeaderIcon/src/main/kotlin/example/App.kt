package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val icons = arrayOf(
    cl.getResource("example/wi0062-16.png"),
    cl.getResource("example/wi0063-16.png"),
    cl.getResource("example/wi0064-16.png")
  )
  val columnNames = arrayOf("Column1", "Column2", "Column3")
  val table = JTable(DefaultTableModel(columnNames, 8))
  val m = table.columnModel
  for (i in 0 until m.columnCount) {
    val icon = icons[i]
    val name = columnNames[i]
    val hv = "<html><table cellpadding='0' cellspacing='0'><td><img src='$icon'/></td>&nbsp;$name"
    m.getColumn(i).headerValue = hv
  }
  table.autoCreateRowSorter = true

  val mb = JMenuBar()
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.preferredSize = Dimension(320, 240)
  }
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
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
