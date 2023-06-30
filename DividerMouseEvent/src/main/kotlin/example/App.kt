package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicSplitPaneUI

fun makeUI(): Component {
  val splitPane = JSplitPane().also {
    it.isOneTouchExpandable = true
    it.isContinuousLayout = true
    it.border = BorderFactory.createMatteBorder(8, 8, 8, 8, Color.WHITE)
    it.leftComponent = JScrollPane(JTree())
    it.rightComponent = JScrollPane(JTable(2, 3))
  }
  EventQueue.invokeLater { splitPane.setDividerLocation(.3) }

  val check = JCheckBox("Show JPopupMenu only on Divider", true)
  val popup = object : JPopupMenu() {
    override fun show(invoker: Component, x: Int, y: Int) {
      if (check.isSelected && invoker is JSplitPane) {
        val ui = invoker.ui as? BasicSplitPaneUI ?: return
        if (ui.divider.bounds.contains(x, y)) {
          super.show(invoker, x, y)
        }
      } else {
        super.show(invoker, x, y)
      }
    }
  }
  popup.add("center").addActionListener { splitPane.setDividerLocation(.5) }
  popup.add("selectMin").addActionListener { selectMinMax(splitPane, "selectMin") }
  popup.add("selectMax").addActionListener { selectMinMax(splitPane, "selectMax") }
  splitPane.componentPopupMenu = popup

  val ui = splitPane.ui as? BasicSplitPaneUI
  ui?.divider?.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      if (SwingUtilities.isLeftMouseButton(e) && e.clickCount >= 2) {
        splitPane.setDividerLocation(.5)
      }
    }
  })

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(splitPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun selectMinMax(splitPane: JSplitPane, cmd: String) {
  splitPane.requestFocusInWindow()
  object : SwingWorker<Void?, Void?>() {
    override fun doInBackground() = null

    override fun done() {
      super.done()
      val a = splitPane.actionMap[cmd]
      a.actionPerformed(ActionEvent(splitPane, ActionEvent.ACTION_PERFORMED, cmd))
      KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner()
    }
  }.execute()
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
      defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
