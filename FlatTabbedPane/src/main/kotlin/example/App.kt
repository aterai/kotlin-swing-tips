package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicTabbedPaneUI

private val SELECTED_BG = Color(255, 150, 0)
private val UNSELECTED_BG = Color(255, 50, 0)

fun makeUI(): Component {
  UIManager.put("TabbedPane.tabInsets", Insets(5, 10, 5, 10))
  // UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(2, 3, 2, 2));
  UIManager.put("TabbedPane.contentBorderInsets", Insets(5, 5, 5, 5))
  UIManager.put("TabbedPane.tabAreaInsets", Insets(0, 0, 0, 0))

  UIManager.put("TabbedPane.selectedLabelShift", 0)
  UIManager.put("TabbedPane.labelShift", 0)

  // UIManager.put("TabbedPane.foreground", Color.WHITE);
  // UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
  // UIManager.put("TabbedPane.unselectedBackground", UNSELECTED_BG);
  // UIManager.put("TabbedPane.tabAreaBackground", UNSELECTED_BG);

  val tabs = object : JTabbedPane() {
    override fun updateUI() {
      super.updateUI()
      setUI(object : BasicTabbedPaneUI() {
        protected override fun paintFocusIndicator(
          g: Graphics,
          tabPlacement: Int,
          rects: Array<Rectangle>,
          tabIndex: Int,
          iconRect: Rectangle?,
          textRect: Rectangle?,
          isSelected: Boolean
        ) { /* Do not paint anything */ }

        protected override fun paintTabBorder(
          g: Graphics,
          tabPlacement: Int,
          tabIndex: Int,
          x: Int,
          y: Int,
          w: Int,
          h: Int,
          isSelected: Boolean
        ) { /* Do not paint anything */ }

        protected override fun paintTabBackground(
          g: Graphics,
          tabPlacement: Int,
          tabIndex: Int,
          x: Int,
          y: Int,
          w: Int,
          h: Int,
          isSelected: Boolean
        ) {
          g.setColor(if (isSelected) SELECTED_BG else UNSELECTED_BG)
          g.fillRect(x, y, w, h)
        }

        protected override fun paintContentBorderTopEdge(
          g: Graphics,
          tabPlacement: Int,
          selectedIndex: Int,
          x: Int,
          y: Int,
          w: Int,
          h: Int
        ) {
          g.setColor(SELECTED_BG)
          g.fillRect(x, y, w, h)
        }

        protected override fun paintContentBorderRightEdge(
          g: Graphics,
          tabPlacement: Int,
          selectedIndex: Int,
          x: Int,
          y: Int,
          w: Int,
          h: Int
        ) {
          g.setColor(SELECTED_BG)
          g.fillRect(x, y, w, h)
        }

        protected override fun paintContentBorderBottomEdge(
          g: Graphics,
          tabPlacement: Int,
          selectedIndex: Int,
          x: Int,
          y: Int,
          w: Int,
          h: Int
        ) {
          g.setColor(SELECTED_BG)
          g.fillRect(x, y, w, h)
        }

        protected override fun paintContentBorderLeftEdge(
          g: Graphics,
          tabPlacement: Int,
          selectedIndex: Int,
          x: Int,
          y: Int,
          w: Int,
          h: Int
        ) {
          g.setColor(SELECTED_BG)
          g.fillRect(x, y, w, h)
        }
      })
      setOpaque(true)
      setForeground(Color.WHITE)
      setBackground(UNSELECTED_BG)
      setTabPlacement(SwingConstants.LEFT)
      setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
    }
  }

  // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
  tabs.addTab("A", ImageIcon(tabs.javaClass.getResource("wi0009-32.png")), JScrollPane(JTree()))
  tabs.addTab("B", ImageIcon(tabs.javaClass.getResource("wi0054-32.png")), JScrollPane(JTextArea()))
  tabs.addTab("C", ImageIcon(tabs.javaClass.getResource("wi0062-32.png")), JScrollPane(JTree()))
  tabs.addTab("D", ImageIcon(tabs.javaClass.getResource("wi0063-32.png")), JScrollPane(JTextArea()))

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
