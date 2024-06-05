package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  UIManager.put("TabbedPane.tabInsets", Insets(1, 4, 0, 4))
  UIManager.put("TabbedPane.selectedTabPadInsets", Insets(1, 1, 1, 1))
  UIManager.put("TabbedPane.tabAreaInsets", Insets(3, 2, 0, 2))
  UIManager.put("TabbedPane.selectedLabelShift", 0)
  UIManager.put("TabbedPane.labelShift", 0)

  val tabs = UnderlineFocusTabbedPane()
  tabs.addTab("JTree", ColorIcon(Color.RED), JScrollPane(JTree()))
  tabs.addTab("JTextArea", ColorIcon(Color.GREEN), JScrollPane(JTextArea()))
  tabs.addTab("JTable", ColorIcon(Color.BLUE), JScrollPane(JTable(8, 3)))
  tabs.addTab("JSplitPane", ColorIcon(Color.ORANGE), JScrollPane(JSplitPane()))
  tabs.selectedIndex = -1
  EventQueue.invokeLater { tabs.selectedIndex = 0 }

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private class UnderlineFocusTabbedPane : JTabbedPane() {
  override fun updateUI() {
    UIManager.put("TabbedPane.focus", ALPHA_ZERO)
    super.updateUI()
    addChangeListener { e ->
      (e.source as? JTabbedPane)
        ?.takeIf { it.tabCount > 0 }
        ?.also {
          val idx = it.selectedIndex
          for (i in 0..<it.tabCount) {
            (it.getTabComponentAt(i) as? JComponent)?.also { tab ->
              val color = if (i == idx) SELECTION_COLOR else ALPHA_ZERO
              tab.border = BorderFactory.createMatteBorder(0, 0, 3, 0, color)
            }
          }
        }
    }
  }

  override fun insertTab(
    title: String?,
    icon: Icon?,
    c: Component?,
    tip: String?,
    index: Int,
  ) {
    super.insertTab(title, icon, c, tip, index)
    setTabComponentAt(index, JLabel(title, icon, CENTER))
  }

  companion object {
    private val ALPHA_ZERO = Color(0x0, true)
    private val SELECTION_COLOR = Color(0x00_AA_FF)
  }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 3, iconHeight - 3)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
