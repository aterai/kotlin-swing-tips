package example

import java.awt.*
import javax.swing.*
import kotlin.math.max

fun makeUI(): Component {
  val tabbedPane = CenteredTabbedPane()
  tabbedPane.addTab("JTree", JScrollPane(JTree()))
  tabbedPane.addTab("JSplitPane", JSplitPane())
  tabbedPane.addTab("JTable", JScrollPane(JTable(5, 3)))
  tabbedPane.setComponentPopupMenu(TabbedPanePopupMenu())
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private class CenteredTabbedPane : JTabbedPane() {
  override fun doLayout() {
    val placement = getTabPlacement()
    if (placement == TOP || placement == BOTTOM) {
      EventQueue.invokeLater { updateTabAreaMargins() }
    }
    super.doLayout()
  }

  private fun updateTabAreaMargins() {
    val allWidth = (0 until tabCount).sumOf { getBoundsAt(it).width }
    val r = SwingUtilities.calculateInnerArea(this, null)
    val w2 = max(0, (r.width - allWidth) / 2)
    val ins = Insets(3, w2, 4, 0)
    val d = UIDefaults()
    d.put("TabbedPane:TabbedPaneTabArea.contentMargins", ins)
    putClientProperty("Nimbus.Overrides", d)
    putClientProperty("Nimbus.Overrides.InheritDefaults", true)
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
        val tabIdx = it.selectedIndex
        val title = it.getTitleAt(tabIdx)
        val cmp = it.getComponentAt(tabIdx)
        it.removeAll()
        it.addTab(title, cmp)
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    (c as? JTabbedPane)?.also {
      closePage.isEnabled = it.indexAtLocation(x, y) >= 0
      closeAll.isEnabled = it.tabCount > 0
      closeAllButActive.isEnabled = it.tabCount > 0
      super.show(c, x, y)
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
