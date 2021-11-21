package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicTabbedPaneUI

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
    super.updateUI()
    // isFocusable = false
    if (getUI() is WindowsTabbedPaneUI) {
      setUI(object : WindowsTabbedPaneUI() {
        override fun paintFocusIndicator(
          g: Graphics,
          tabPlacement: Int,
          rects: Array<Rectangle>,
          tabIndex: Int,
          iconRect: Rectangle,
          textRect: Rectangle,
          isSelected: Boolean
        ) {
          super.paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect, textRect, false)
        }
      })
    } else {
      setUI(object : BasicTabbedPaneUI() {
        override fun paintFocusIndicator(
          g: Graphics,
          tabPlacement: Int,
          rects: Array<Rectangle>,
          tabIndex: Int,
          iconRect: Rectangle,
          textRect: Rectangle,
          isSelected: Boolean
        ) {
          super.paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect, textRect, false)
        }
      })
    }
    addChangeListener { e ->
      (e.source as? JTabbedPane)
        ?.takeIf { it.tabCount > 0 }
        ?.also {
          val idx = it.selectedIndex
          for (i in 0 until it.tabCount) {
            val c = it.getTabComponentAt(i)
            if (c is JComponent) {
              c.border = if (i == idx) SELECTED_BORDER else DEFAULT_BORDER
            }
          }
        }
    }
  }

  override fun insertTab(title: String, icon: Icon, component: Component, tip: String, index: Int) {
    super.insertTab(title, icon, component, tip, index)
    setTabComponentAt(index, JLabel(title, icon, CENTER))
  }

  companion object {
    private val DEFAULT_BORDER = BorderFactory.createMatteBorder(0, 0, 3, 0, Color(0x0, true))
    private val SELECTED_BORDER = BorderFactory.createMatteBorder(0, 0, 3, 0, Color(0x00AAFF))
  }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
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
