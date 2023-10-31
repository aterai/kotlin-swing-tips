package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicTabbedPaneUI

fun makeUI(): Component {
  val tabbedPane = object : JTabbedPane() {
    override fun getToolTipTextAt(index: Int) = getTitleAt(index)

    override fun insertTab(
      title: String?,
      icon: Icon?,
      c: Component?,
      tip: String?,
      idx: Int,
    ) {
      super.insertTab(title, icon, c, title, idx)
    }

    override fun updateUI() {
      super.updateUI()
      val tmp = if (ui is WindowsTabbedPaneUI) {
        WindowsClippedTitleTabbedPaneUI()
      } else {
        BasicClippedTitleTabbedPaneUI()
      }
      setUI(tmp)
    }
  }
  val list = listOf(makeTabbedPane(JTabbedPane()), makeTabbedPane(tabbedPane))
  val p = JPanel(GridLayout(2, 1))
  list.forEach { p.add(it) }

  val check = JCheckBox("LEFT")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    val tabPlacement = if (b) SwingConstants.LEFT else SwingConstants.TOP
    list.forEach { it.tabPlacement = tabPlacement }
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabbedPane(tabbedPane: JTabbedPane): JTabbedPane {
  tabbedPane.addTab("1111111111111111111111111111", JLabel("1"))
  tabbedPane.addTab("2", JLabel("2"))
  tabbedPane.addTab("33333333333333333333333333333333333333333333", JLabel("3"))
  tabbedPane.addTab("444444444444", JLabel("4"))
  return tabbedPane
}

private class BasicClippedTitleTabbedPaneUI : BasicTabbedPaneUI() {
  override fun calculateTabWidth(
    tabPlacement: Int,
    tabIndex: Int,
    metrics: FontMetrics,
  ): Int {
    val ins = tabPane.insets
    val width = tabPane.width - tabAreaInsets.left - tabAreaInsets.right - ins.left - ins.right
    return if (tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT) {
      width / 4
    } else { // TOP || BOTTOM
      width / tabPane.tabCount
    }
  }

  override fun paintText(
    g: Graphics,
    tabPlacement: Int,
    font: Font,
    metrics: FontMetrics,
    tabIndex: Int,
    title: String,
    textRect: Rectangle,
    isSelected: Boolean,
  ) {
    val tabRect = rects[tabIndex]
    val x = tabRect.x + tabInsets.left
    val y = textRect.y
    val w = tabRect.width - tabInsets.left - tabInsets.right
    val h = textRect.height
    val viewR = Rectangle(x, y, w, h)
    val iconR = Rectangle()
    val textR = Rectangle()
    val clippedText = SwingUtilities.layoutCompoundLabel(
      metrics, title, null,
      CENTER, CENTER, CENTER, TRAILING,
      viewR, iconR, textR, 0,
    )
    if (title == clippedText) {
      super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected)
    } else {
      textR.x = textRect.x + tabInsets.left
      super.paintText(g, tabPlacement, font, metrics, tabIndex, clippedText, textR, isSelected)
    }
  }
}

private class WindowsClippedTitleTabbedPaneUI : WindowsTabbedPaneUI() {
  override fun calculateTabWidth(
    tabPlacement: Int,
    tabIndex: Int,
    metrics: FontMetrics,
  ): Int {
    val ins = tabPane.insets
    val width = tabPane.width - tabAreaInsets.left - tabAreaInsets.right - ins.left - ins.right
    return if (tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT) {
      width / 4
    } else { // TOP || BOTTOM
      width / tabPane.tabCount
    }
  }

  override fun paintText(
    g: Graphics,
    tabPlacement: Int,
    font: Font,
    metrics: FontMetrics,
    tabIndex: Int,
    title: String,
    textRect: Rectangle,
    isSelected: Boolean,
  ) {
    val tabRect = rects[tabIndex]
    val x = tabRect.x + tabInsets.left
    val y = textRect.y
    val w = tabRect.width - tabInsets.left - tabInsets.right
    val h = textRect.height
    val viewR = Rectangle(x, y, w, h)
    val iconR = Rectangle()
    val textR = Rectangle()
    val clippedText = SwingUtilities.layoutCompoundLabel(
      metrics, title, null,
      CENTER, CENTER, CENTER, TRAILING,
      viewR, iconR, textR, 0,
    )
    if (title == clippedText) {
      super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected)
    } else {
      textR.x = textRect.x + tabInsets.left
      super.paintText(g, tabPlacement, font, metrics, tabIndex, clippedText, textR, isSelected)
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
