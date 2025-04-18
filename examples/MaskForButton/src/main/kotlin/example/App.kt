package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  val log = JTextArea()
  log.isEditable = false
  log.append("MouseInfo.getNumberOfButtons: ${MouseInfo.getNumberOfButtons()}\n")
  val tabbedPane = JTabbedPane()
  tabbedPane.componentPopupMenu = TabbedPanePopupMenu()
  val help = JLabel("Close a tab by the middle mouse button clicking.")
  tabbedPane.addTab("Help", help)
  tabbedPane.addTab("Title a", JLabel("JLabel a"))
  tabbedPane.addTab("Title b", JLabel("JLabel b"))
  tabbedPane.addTab("Title c", JLabel("JLabel c"))
  tabbedPane.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      val button = e.button
      val mask = if (button == 0) "NOBUTTON" else "BUTTON$button"
      log.append("$mask\n")
      val isDouble = e.clickCount >= 2
      val isLeftDouble = SwingUtilities.isLeftMouseButton(e) && isDouble
      val isMiddle = SwingUtilities.isMiddleMouseButton(e)
      (e.component as? JTabbedPane)?.also {
        val idx = it.indexAtLocation(e.x, e.y)
        if (idx >= 0 && (isMiddle || isLeftDouble)) {
          it.remove(idx)
        }
      }
    }

    override fun mousePressed(e: MouseEvent) {
      val mousePressed = SwingUtilities.isMiddleMouseButton(e)
      log.append("Middle mousePressed: $mousePressed\n")
    }

    override fun mouseReleased(e: MouseEvent) {
      val mouseReleased = SwingUtilities.isMiddleMouseButton(e)
      log.append("Middle mouseReleased: $mouseReleased\n")
    }
  })

  return JPanel(GridLayout(2, 1)).also {
    it.add(tabbedPane)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TabbedPanePopupMenu : JPopupMenu() {
  private var count = 0
  private val closePage: JMenuItem
  private val closeAll: JMenuItem
  private val closeAllButActive: JMenuItem

  init {
    add("New tab").addActionListener {
      (invoker as? JTabbedPane)?.also {
        it.addTab("Title: $count", JLabel("Tab: $count"))
        it.selectedIndex = it.tabCount - 1
        count++
      }
    }
    addSeparator()

    closePage = add("Close")
    closePage.addActionListener {
      (invoker as? JTabbedPane)?.also {
        it.remove(it.selectedIndex)
      }
    }
    addSeparator()

    closeAll = add("Close all")
    closeAll.addActionListener {
      (invoker as? JTabbedPane)?.removeAll()
    }

    closeAllButActive = add("Close all bat active")
    closeAllButActive.addActionListener {
      (invoker as? JTabbedPane)?.also {
        val idx = it.selectedIndex
        val title = it.getTitleAt(idx)
        val cmp = it.getComponentAt(idx)
        it.removeAll()
        it.addTab(title, cmp)
      }
    }
  }

  override fun show(
    c: Component,
    x: Int,
    y: Int,
  ) {
    if (c is JTabbedPane) {
      closePage.isEnabled = c.indexAtLocation(x, y) >= 0
      closeAll.isEnabled = c.tabCount > 0
      closeAllButActive.isEnabled = c.tabCount > 0
      super.show(c, x, y)
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
