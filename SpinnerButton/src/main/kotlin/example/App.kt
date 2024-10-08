package example

import com.sun.java.swing.plaf.windows.WindowsSpinnerUI
import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicSpinnerUI

fun makeUI(): Component {
  val box = Box.createVerticalBox()

  val spinner1 = object : JSpinner(SpinnerNumberModel(10, 0, 1000, 1)) {
    override fun updateUI() {
      super.updateUI()
      setUI(ToolTipSpinnerUI())
    }
  }
  box.add(makeTitledPanel("BasicSpinnerUI", spinner1))

  val spinner2 = object : JSpinner(SpinnerNumberModel(10, 0, 1000, 1)) {
    override fun updateUI() {
      super.updateUI()
      searchSpinnerButtons(this)
    }
  }
  box.add(makeTitledPanel("getName()", spinner2))

  val spinner3 = object : JSpinner(SpinnerNumberModel(10, 0, 1000, 1)) {
    override fun updateUI() {
      super.updateUI()
      if (ui is WindowsSpinnerUI) {
        setUI(ToolTipWindowsSpinnerUI())
      } else {
        searchSpinnerButtons(this)
      }
    }
  }
  box.add(makeTitledPanel("WindowsSpinnerUI", spinner3))

  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun searchSpinnerButtons(comp: Container) {
  for (c in comp.components) {
    when {
      "Spinner.nextButton" == c.name ->
        (c as? JButton)?.toolTipText = "getName: next next"
      "Spinner.previousButton" == c.name ->
        (c as? JButton)?.toolTipText = "getName: prev prev"
      c is Container ->
        searchSpinnerButtons(c)
    }
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

private class ToolTipSpinnerUI : BasicSpinnerUI() {
  override fun createNextButton(): Component {
    val c = super.createNextButton()
    (c as? JComponent)?.toolTipText = "SpinnerUI: next next"
    return c
  }

  override fun createPreviousButton(): Component {
    val c = super.createPreviousButton()
    (c as? JComponent)?.toolTipText = "SpinnerUI: prev prev"
    return c
  }
}

private class ToolTipWindowsSpinnerUI : WindowsSpinnerUI() {
  override fun createNextButton(): Component {
    val c = super.createNextButton()
    (c as? JComponent)?.toolTipText = "SpinnerUI: next next"
    return c
  }

  override fun createPreviousButton(): Component {
    val c = super.createPreviousButton()
    (c as? JComponent)?.toolTipText = "SpinnerUI: prev prev"
    return c
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
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
