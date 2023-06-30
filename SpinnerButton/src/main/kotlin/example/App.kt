package example

import com.sun.java.swing.plaf.windows.WindowsSpinnerUI
import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicSpinnerUI

fun makeUI(): Component {
  val spinner1 = JSpinner(SpinnerNumberModel(10, 0, 1000, 1))
  spinner1.ui = MySpinnerUI()

  val spinner2 = JSpinner(SpinnerNumberModel(10, 0, 1000, 1))
  searchSpinnerButtons(spinner2)

  val spinner3 = JSpinner(SpinnerNumberModel(10, 0, 1000, 1))
  if (spinner3.ui is WindowsSpinnerUI) {
    spinner3.ui = MyWinSpinnerUI()
  } else {
    searchSpinnerButtons(spinner3)
  }

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("BasicSpinnerUI", spinner1))
  box.add(makeTitledPanel("getName()", spinner2))
  box.add(makeTitledPanel("WindowsSpinnerUI", spinner3))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun searchSpinnerButtons(comp: Container) {
  for (c in comp.components) {
    // println(c.name)
    when {
      "Spinner.nextButton" == c.name -> (c as? JButton)?.toolTipText = "getName: next next"
      "Spinner.previousButton" == c.name -> (c as? JButton)?.toolTipText = "getName: prev prev"
      c is Container -> searchSpinnerButtons(c)
    }
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private class MySpinnerUI : BasicSpinnerUI() {
  override fun createNextButton() = (super.createNextButton() as? JComponent)?.also {
    it.toolTipText = "SpinnerUI: next next"
  }

  override fun createPreviousButton() = (super.createPreviousButton() as? JComponent)?.also {
    it.toolTipText = "SpinnerUI: prev prev"
  }
}

private class MyWinSpinnerUI : WindowsSpinnerUI() {
  override fun createNextButton() = (super.createNextButton() as? JComponent)?.also {
    it.toolTipText = "SpinnerUI: next next"
  }

  override fun createPreviousButton() = (super.createPreviousButton() as? JComponent)?.also {
    it.toolTipText = "SpinnerUI: prev prev"
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
