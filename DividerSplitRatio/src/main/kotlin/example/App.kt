package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.roundToInt

fun makeUI(): Component {
  val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  splitPane.topComponent = JScrollPane(JTextArea())
  splitPane.bottomComponent = JScrollPane(JTree())

  val spw = SplitPaneWrapper(splitPane)
  val check = JCheckBox("MAXIMIZED_BOTH: keep the same splitting ratio", true)
  check.addActionListener { spw.setTestFlag(check.isSelected) }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(spw)
    it.preferredSize = Dimension(320, 240)
  }
}

private class SplitPaneWrapper(private val splitPane: JSplitPane) : JPanel(BorderLayout()) {
  private var flag = true
  private var prevState = Frame.NORMAL
  fun setTestFlag(f: Boolean) {
    flag = f
  }

  override fun doLayout() {
    if (flag) {
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

  companion object {
    private fun getOrientedSize(sp: JSplitPane) =
      if (sp.orientation == JSplitPane.VERTICAL_SPLIT) sp.height - sp.dividerSize else sp.width - sp.dividerSize
  }

  init {
    add(splitPane)
    EventQueue.invokeLater { splitPane.setDividerLocation(.5) }
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
