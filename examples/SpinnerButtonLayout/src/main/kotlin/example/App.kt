package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicSpinnerUI

fun makeUI(): Component {
  val model = SpinnerNumberModel(10, 0, 1000, 1)
  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("Default", JSpinner(model)))
  val spinner1 = JSpinner(model)
  spinner1.componentOrientation = ComponentOrientation.RIGHT_TO_LEFT
  box.add(makeTitledPanel("RIGHT_TO_LEFT", spinner1))
  val spinner2 = object : JSpinner(model) {
    override fun updateUI() {
      super.updateUI()
      val tmp = object : BasicSpinnerUI() {
        override fun createLayout() = SpinnerLayout()
      }
      setUI(tmp)
    }
  }
  box.add(makeTitledPanel("L(Prev), R(Next)", spinner2))
  val spinner3 = object : JSpinner(model) {
    override fun setLayout(mgr: LayoutManager) {
      super.setLayout(SpinnerLayout())
    }
  }
  box.add(makeTitledPanel("L(Prev), R(Next)", spinner3))
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class SpinnerLayout : BorderLayout() {
  private val layoutMap = mapOf(
    "Editor" to "Center",
    "Next" to "East",
    "Previous" to "West",
  )

  override fun addLayoutComponent(comp: Component, constraints: Any) {
    val cons = layoutMap[constraints] ?: constraints
    super.addLayoutComponent(comp, cons)
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
