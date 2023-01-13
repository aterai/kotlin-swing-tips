package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

// private const val PAD = "<html><table><td height='32'>"
// private const val PAD = "<html><table cellpadding='0'>"
private const val PAD = "<html><table><td style='padding:1'>"

fun makeUI(): Component {
  val log = JTextArea()
  log.font = log.font.deriveFont(10f)
  log.append(info("Button.dashedRectGapX"))
  log.append(info("Button.dashedRectGapY"))
  log.append(info("Button.dashedRectGapHeight"))
  log.append(info("Button.dashedRectGapWidth"))

  UIManager.put("Button.dashedRectGapX", 5)
  UIManager.put("Button.dashedRectGapY", 5)
  UIManager.put("Button.dashedRectGapHeight", 10)
  UIManager.put("Button.dashedRectGapWidth", 10)
  UIManager.put("Button.margin", Insets(8, 8, 8, 8))
  UIManager.put("ToggleButton.margin", Insets(8, 8, 8, 8))
  UIManager.put("RadioButton.margin", Insets(8, 8, 8, 8))
  UIManager.put("CheckBox.margin", Insets(8, 8, 8, 8))

  val p = JPanel()
  p.add(JButton("JButton"))
  p.add(Box.createHorizontalStrut(32))
  p.add(JToggleButton("JToggleButton"))
  p.add(Box.createHorizontalStrut(32))
  p.add(JCheckBox("JCheckBox"))

  val check = object : JCheckBox("JCheckBox+BorderPainted") {
    override fun updateUI() {
      super.updateUI()
      isBorderPainted = true
    }
  }
  p.add(check)
  p.add(JCheckBox(PAD + "JCheckBox+td.padding"))

  p.add(JRadioButton("JRadioButton"))
  p.add(JRadioButton(PAD + "JRadioButton+td.padding"))

  return JPanel(BorderLayout(5, 5)).also {
    it.add(p)
    it.add(JScrollPane(log), BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun info(key: String) = "%s: %d%n".format(key, UIManager.getInt(key))

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
