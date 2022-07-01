package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.plaf.basic.BasicButtonUI

fun makeUI(): Component {
  val tabbedPane = JTabbedPane()
  for (i in 0 until 3) {
    val title = "Tab $i"
    tabbedPane.addTab(title, JLabel(title))
    tabbedPane.setTabComponentAt(i, ButtonTabComponent(tabbedPane))
  }
  tabbedPane.componentPopupMenu = TabTitleRenamePopupMenu()
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ButtonTabComponent(
  val tabbedPane: JTabbedPane
) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {
  private inner class TabButtonHandler : MouseAdapter(), ActionListener {
    override fun actionPerformed(e: ActionEvent) {
      val i = tabbedPane.indexOfTabComponent(this@ButtonTabComponent)
      if (i != -1) {
        tabbedPane.remove(i)
      }
    }

    override fun mouseEntered(e: MouseEvent) {
      (e.component as? AbstractButton)?.isBorderPainted = true
    }

    override fun mouseExited(e: MouseEvent) {
      (e.component as? AbstractButton)?.isBorderPainted = false
    }
  }

  init {
    isOpaque = false
    val label = object : JLabel() {
      override fun getText(): String? {
        val i = tabbedPane.indexOfTabComponent(this@ButtonTabComponent)
        return if (i != -1) {
          tabbedPane.getTitleAt(i)
        } else null
      }

      override fun getIcon(): Icon? {
        val i = tabbedPane.indexOfTabComponent(this@ButtonTabComponent)
        return if (i != -1) {
          tabbedPane.getIconAt(i)
        } else null
      }
    }
    add(label)
    label.border = BorderFactory.createEmptyBorder(0, 0, 0, 5)
    val button = TabButton()
    val handler = TabButtonHandler()
    button.addActionListener(handler)
    button.addMouseListener(handler)
    add(button)
    border = BorderFactory.createEmptyBorder(2, 0, 0, 0)
  }
}

private class TabButton : JButton() {
  init {
    setUI(BasicButtonUI())
    toolTipText = "close this tab"
    isContentAreaFilled = false
    isFocusable = false
    border = BorderFactory.createEtchedBorder()
    isBorderPainted = false
    isRolloverEnabled = true
  }

  override fun getPreferredSize() = Dimension(SIZE, SIZE)

  override fun updateUI() {
    // we don't want to update UI for this button
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.stroke = BasicStroke(2f)
    g2.paint = Color.BLACK
    val model = getModel()
    if (model.isRollover) {
      g2.paint = Color.ORANGE
    }
    if (model.isPressed) {
      g2.paint = Color.BLUE
    }
    g2.drawLine(DELTA, DELTA, width - DELTA - 1, height - DELTA - 1)
    g2.drawLine(width - DELTA - 1, DELTA, DELTA, height - DELTA - 1)
    g2.dispose()
  }

  companion object {
    private const val SIZE = 17
    private const val DELTA = 6
  }
}

private class TabTitleRenamePopupMenu : JPopupMenu() {
  private val rename: JMenuItem

  init {
    val textField = JTextField(10)
    textField.addAncestorListener(FocusAncestorListener())
    rename = add("rename")
    rename.addActionListener {
      (invoker as? JTabbedPane)?.also {
        val idx = it.selectedIndex
        val title = it.getTitleAt(idx)
        textField.text = title
        val ret = JOptionPane.showConfirmDialog(
          it,
          textField,
          "Rename",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE
        )
        if (ret == JOptionPane.OK_OPTION) {
          val str = textField.text.trim()
          val c = it.getTabComponentAt(idx)
          if (str.isNotEmpty() && c != null) {
            it.setTitleAt(idx, str)
            c.revalidate()
          }
        }
      }
    }
    addSeparator()
    add("new tab").addActionListener {
      (invoker as? JTabbedPane)?.also {
        val count = it.tabCount
        val title = "Tab $count"
        it.addTab(title, JLabel(title))
        it.setTabComponentAt(count, ButtonTabComponent(it))
      }
    }
    add("close all").addActionListener { (invoker as? JTabbedPane)?.removeAll() }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTabbedPane) {
      rename.isEnabled = c.indexAtLocation(x, y) >= 0
      super.show(c, x, y)
    }
  }
}

private class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.component.requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent) {
    /* not needed */
  }

  override fun ancestorRemoved(e: AncestorEvent) {
    /* not needed */
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
