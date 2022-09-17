package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val spinner0 = JSpinner()
  spinner0.isEnabled = false

  val spinner1 = JSpinner()
  spinner1.isEnabled = false
  (spinner1.editor as? JSpinner.DefaultEditor)?.also {
    it.isOpaque = false
    it.textField.isOpaque = false
  }

  // JSpinner s2 = new JSpinner();
  // Color bgc2 = UIManager.getColor("FormattedTextField.inactiveBackground");
  // s2.setBorder(BorderFactory.createCompoundBorder(
  //   BorderFactory.createLineBorder(new Color(127, 157, 185)),
  //   BorderFactory.createLineBorder(bgc2, 2)));

  val info = JTextArea()
  info.append("TextField.shadow: ${UIManager.getColor("TextField.shadow")}\n")
  info.append("TextField.darkShadow: ${UIManager.getColor("TextField.darkShadow")}\n")
  info.append("TextField.light: ${UIManager.getColor("TextField.light")}\n")
  info.append("TextField.highlight: ${UIManager.getColor("TextField.highlight")}\n")
  info.append("Spinner.border: ${UIManager.getBorder("Spinner.border")}\n")
  info.append("editorBorderPainted: ${UIManager.getBoolean("TextField.editorBorderPainted")}\n")

  val spinner2 = object : JSpinner() {
    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder()
      val borderColor = UIManager.getColor("TextField.shadow")
      (editor as? DefaultEditor)?.also {
        it.border = BorderFactory.createMatteBorder(1, 1, 1, 0, borderColor)
        it.textField.border = BorderFactory.createEmptyBorder(2, 2, 2, 0)
      }
    }
  }
  spinner2.isEnabled = false

  val spinner3 = SimpleBorderSpinner()
  spinner3.isEnabled = false

  val box = Box.createVerticalBox()
  addTestSpinner(box, spinner0, "Default")
  addTestSpinner(box, spinner1, "setOpaque(false)")
  addTestSpinner(box, spinner2, "setBorder(...)")
  addTestSpinner(box, spinner3, "paintComponent, paintChildren")

  val list = listOf(spinner0, spinner1, spinner2, spinner3)
  val check = JCheckBox("setEnabled")
  check.addActionListener { e ->
    val flg = (e.source as? JCheckBox)?.isSelected == true
    list.forEach { it.isEnabled = flg }
  }

  return JPanel(BorderLayout(5, 5)).also {
    it.border = BorderFactory.createEmptyBorder(2, 20, 2, 20)
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(info))
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addTestSpinner(box: Box, spinner: JSpinner, title: String) {
  val p = JPanel(BorderLayout())
  p.add(spinner)
  p.border = BorderFactory.createTitledBorder(title)
  box.add(p)
  box.add(Box.createVerticalStrut(2))
}

private class SimpleBorderSpinner : JSpinner() {
  private var isWindowsLnF = false

  override fun updateUI() {
    super.updateUI()
    isWindowsLnF = getUI().javaClass.name.contains("WindowsSpinnerUI")
  }

  override fun paintComponent(g: Graphics) {
    if (isWindowsLnF) {
      val g2 = g.create() as? Graphics2D ?: return
      val key = if (isEnabled) "background" else "inactiveBackground"
      g2.paint = UIManager.getColor("FormattedTextField.$key")
      g2.fillRect(0, 0, width, height)
      g2.dispose()
    } else {
      super.paintComponent(g)
    }
  }

  override fun paintChildren(g: Graphics) {
    super.paintChildren(g)
    if (!isEnabled && isWindowsLnF) {
      val r = getComponent(0).bounds
      r.add(getComponent(1).bounds)
      r.width--
      r.height--
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = UIManager.getColor("FormattedTextField.inactiveBackground")
      g2.draw(r)
      g2.dispose()
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
