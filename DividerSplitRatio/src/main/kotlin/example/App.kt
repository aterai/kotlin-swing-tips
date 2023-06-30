package example

import java.awt.*
import javax.swing.*
import kotlin.math.roundToInt

fun makeUI(): Component {
  val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  splitPane.topComponent = JScrollPane(JTextArea())
  splitPane.bottomComponent = JScrollPane(JTree())
  EventQueue.invokeLater { splitPane.setDividerLocation(.5) }

  val spw = SplitPaneWrapper()
  spw.add(splitPane)

  val check = JCheckBox("MAXIMIZED_BOTH: keep the same splitting ratio", true)
  check.addActionListener { spw.setTestFlag(check.isSelected) }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(spw)
    it.preferredSize = Dimension(320, 240)
  }
}

private class SplitPaneWrapper : JPanel(BorderLayout()) {
  private var flag = true
  private var prevState = Frame.NORMAL

  fun setTestFlag(f: Boolean) {
    flag = f
  }

  override fun doLayout() {
    val splitPane = getComponent(0)
    if (flag && splitPane is JSplitPane) {
      val size = getOrientedSize(splitPane)
      val proportionalLoc = splitPane.dividerLocation / size.toFloat()
      super.doLayout()
      val state = (topLevelAncestor as? Frame)?.extendedState ?: Frame.NORMAL
      if (splitPane.isShowing && state != prevState) {
        EventQueue.invokeLater {
          val s = getOrientedSize(splitPane)
          val iv = (s * proportionalLoc).roundToInt()
          splitPane.dividerLocation = iv
        }
        prevState = state
      }
    } else {
      super.doLayout()
    }
  }

  private fun getOrientedSize(sp: JSplitPane): Int {
    return if (sp.orientation == JSplitPane.VERTICAL_SPLIT) {
      sp.height - sp.dividerSize
    } else {
      sp.width - sp.dividerSize
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
