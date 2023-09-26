package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel

fun makeUI(): Component {
  val tree = object : JTree(makeModel()) {
    private val rolloverRowColor = Color(0xDC_F0_FF)
    private var rollOverRowIndex = -1
    private var listener: MouseMotionListener? = null

    override fun updateUI() {
      removeMouseMotionListener(listener)
      setCellRenderer(null)
      super.updateUI()
      val r = DefaultTreeCellRenderer()
      setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
        r.getTreeCellRendererComponent(
          tree,
          value,
          selected,
          expanded,
          leaf,
          row,
          hasFocus,
        ).also {
          val isRollOver = row == rollOverRowIndex
          if (isRollOver) {
            it.background = rolloverRowColor
            if (selected) {
              it.foreground = r.textNonSelectionColor
            }
          }
          (it as? JComponent)?.isOpaque = isRollOver
        }
      }
      listener = object : MouseAdapter() {
        override fun mouseMoved(e: MouseEvent) {
          val row = getRowForLocation(e.x, e.y)
          if (row != rollOverRowIndex) {
            rollOverRowIndex = row
            repaint()
          }
        }
      }
      addMouseMotionListener(listener)
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TreeModel {
  val set1 = DefaultMutableTreeNode("Set 001").also {
    it.add(DefaultMutableTreeNode("111111111"))
    it.add(DefaultMutableTreeNode("22222222222"))
    it.add(DefaultMutableTreeNode("33333"))
  }

  val set2 = DefaultMutableTreeNode("Set 002").also {
    it.add(DefaultMutableTreeNode("444444444"))
    it.add(DefaultMutableTreeNode("5555"))
  }

  val set3 = DefaultMutableTreeNode("Set 003").also {
    it.add(DefaultMutableTreeNode("666666666666"))
    it.add(DefaultMutableTreeNode("777777777"))
    it.add(DefaultMutableTreeNode("88888888888888"))
  }

  val root = DefaultMutableTreeNode("Root").also {
    it.add(set1)
    it.add(set2)
  }
  set2.add(set3)
  return DefaultTreeModel(root)
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
