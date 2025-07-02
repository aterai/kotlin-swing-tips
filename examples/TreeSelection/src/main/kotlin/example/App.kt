package example

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.tree.TreeSelectionModel

fun makeUI(): Component {
  val tree = JTree()

  val r0 = JRadioButton("DISCONTIGUOUS_TREE_SELECTION", true)
  r0.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val sm = tree.selectionModel
      sm.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    }
  }

  val r1 = JRadioButton("SINGLE_TREE_SELECTION")
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val sm = tree.selectionModel
      sm.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    }
  }

  val r2 = JRadioButton("CONTIGUOUS_TREE_SELECTION")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val sm = tree.selectionModel
      sm.selectionMode = TreeSelectionModel.CONTIGUOUS_TREE_SELECTION
    }
  }

  val p = Box.createVerticalBox()
  val bg = ButtonGroup()
  listOf(r0, r1, r2).forEach {
    bg.add(it)
    p.add(it)
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(tree))
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
