package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree = object : JTree() {
    private var rollover = false
    private var rolloverHandler: MouseAdapter? = null

    override fun updateUI() {
      removeMouseListener(rolloverHandler)
      super.updateUI()
      rolloverHandler = object : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent) {
          rollover = true
          repaint()
        }

        override fun mouseExited(e: MouseEvent) {
          rollover = false
          repaint()
        }
      }
      addMouseListener(rolloverHandler)
      setUI(object : BasicTreeUI() {
        override fun shouldPaintExpandControl(
          path: TreePath,
          row: Int,
          isExpanded: Boolean,
          hasBeenExpanded: Boolean,
          isLeaf: Boolean,
        ) = rollover && super.shouldPaintExpandControl(
          path,
          row,
          isExpanded,
          hasBeenExpanded,
          isLeaf,
        )

        override fun paintHorizontalLine(
          g: Graphics,
          c: JComponent,
          y: Int,
          left: Int,
          right: Int,
        ) {
          if (rollover) {
            super.paintHorizontalLine(g, c, y, left, right)
          }
        }

        override fun paintVerticalLine(
          g: Graphics,
          c: JComponent,
          x: Int,
          top: Int,
          bottom: Int,
        ) {
          if (rollover) {
            super.paintVerticalLine(g, c, x, top, bottom)
          }
        }
      })
    }
  }

  val sp = JSplitPane()
  sp.resizeWeight = .5
  sp.leftComponent = makeTitledPanel(JTree(), "Default")
  val title = "Paint the lines that connect the nodes during rollover"
  sp.rightComponent = makeTitledPanel(tree, title)

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(view: Component, title: String): JScrollPane {
  val scroll = JScrollPane(view)
  scroll.border = BorderFactory.createTitledBorder(title)
  return scroll
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
      defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
