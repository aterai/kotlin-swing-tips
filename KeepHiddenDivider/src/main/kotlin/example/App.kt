package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicSplitPaneUI

fun makeUI(): Component {
  val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  splitPane.topComponent = object : JScrollPane(JTable(5, 3)) {
    override fun getMinimumSize() = Dimension(0, 100)
  }
  splitPane.bottomComponent = object : JScrollPane(JTree()) {
    override fun getMinimumSize() = Dimension(0, 100)
  }
  splitPane.isOneTouchExpandable = true

  val north = JPanel(GridLayout(0, 2, 5, 5))
  north.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val minDividerAction = object : AbstractAction("Min:DividerLocation") {
    override fun actionPerformed(e: ActionEvent) {
      splitPane.dividerLocation = 0
    }
  }
  north.add(JButton(minDividerAction))

  val maxDividerAction = object : AbstractAction("Max:DividerLocation") {
    override fun actionPerformed(e: ActionEvent) {
      val ins = splitPane.insets
      if (splitPane.orientation == JSplitPane.VERTICAL_SPLIT) {
        splitPane.dividerLocation = splitPane.height - (ins?.bottom ?: 0)
      } else {
        splitPane.dividerLocation = splitPane.width - (ins?.right ?: 0)
      }
    }
  }
  north.add(JButton(maxDividerAction))

  val minAction = object : AbstractAction("selectMin") {
    override fun actionPerformed(e: ActionEvent) {
      splitPane.requestFocusInWindow()
      EventQueue.invokeLater {
        val cmd = e.actionCommand
        splitPane.actionMap[cmd]?.actionPerformed(ActionEvent(splitPane, e.id, cmd))
      }
    }
  }
  north.add(JButton(minAction))

  val maxAction = object : AbstractAction("selectMax") {
    override fun actionPerformed(e: ActionEvent) {
      splitPane.requestFocusInWindow()
      EventQueue.invokeLater {
        val cmd = e.actionCommand
        splitPane.actionMap[cmd]?.actionPerformed(ActionEvent(splitPane, e.id, cmd))
      }
    }
  }
  north.add(JButton(maxAction))

  val minButton = JButton("Min:keepHidden")
  val maxButton = JButton("Max:keepHidden")
  (splitPane.ui as? BasicSplitPaneUI)?.divider?.also {
    initDividerButtonModel(it, minButton, maxButton)
  }
  north.add(minButton)
  north.add(maxButton)

  val p = JPanel(BorderLayout())
  p.add(north, BorderLayout.NORTH)
  p.add(splitPane)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun initDividerButtonModel(divider: Container, minButton: JButton, maxButton: JButton) {
  var selectMinModel: ButtonModel? = null
  var selectMaxModel: ButtonModel? = null
  for (c in divider.components) {
    if (c is JButton) {
      val m = c.model
      if (selectMinModel == null) {
        selectMinModel = m
      } else if (selectMaxModel == null) {
        selectMaxModel = m
      }
    }
  }
  minButton.model = selectMinModel
  maxButton.model = selectMaxModel
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
