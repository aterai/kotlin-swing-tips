package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("prev1: Ctrl+1", 1, true),
    arrayOf("next1: Ctrl+2", 2, false),
    arrayOf("prev2: Ctrl+3", 3, true),
    arrayOf("next2: Ctrl+4", 4, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.fillsViewportHeight = true
  table.autoCreateRowSorter = true

  val tabs = JTabbedPane()
  tabs.addTab("JTable", JScrollPane(table))
  tabs.addTab("JTree", JScrollPane(JTree()))
  tabs.addTab("JSplitPane", JSplitPane())
  tabs.addTab("JButton", JButton("button"))

  val menu = JMenu("Sub")
  menu.add("Item 1")
  menu.add("Item 2")

  val popup = JPopupMenu()
  popup.add(menu)
  popup.add("Table Item 1")
  popup.add("Table Item 2")
  popup.add("Table Item 3")
  table.componentPopupMenu = popup

  EventQueue.invokeLater {
    val modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMask
    val ks1 = KeyStroke.getKeyStroke(KeyEvent.VK_1, modifiers)
    tabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks1, "prev1")
    val act1 = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val s = tabs.tabCount
        tabs.selectedIndex = (tabs.selectedIndex + s - 1) % s
      }
    }
    tabs.actionMap.put("prev1", act1)

    val ks2 = KeyStroke.getKeyStroke(KeyEvent.VK_2, modifiers)
    tabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks2, "next1")
    val act2 = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        tabs.selectedIndex = (tabs.selectedIndex + 1) % tabs.tabCount
      }
    }
    tabs.actionMap.put("next1", act2)

    val ks3 = KeyStroke.getKeyStroke(KeyEvent.VK_3, modifiers)
    tabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks3, "prev2")
    val act3 = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        tabs.dispatchEvent(MouseEvent(tabs, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, 1, false))
        val s = tabs.tabCount
        tabs.selectedIndex = (tabs.selectedIndex + s - 1) % s
      }
    }
    tabs.actionMap.put("prev2", act3)

    val ks4 = KeyStroke.getKeyStroke(KeyEvent.VK_4, modifiers)
    tabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks4, "next2")
    val act4 = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        MenuSelectionManager.defaultManager()
          .selectedPath
          .filterIsInstance<JPopupMenu>()
          .forEach { it.isVisible = false }
        tabs.selectedIndex = (tabs.selectedIndex + 1) % tabs.tabCount
      }
    }
    tabs.actionMap.put("next2", act4)
  }

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
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
