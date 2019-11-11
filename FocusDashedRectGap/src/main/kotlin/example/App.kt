package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel() {
  companion object {
    // private static final String PAD = "<html><table><td height='32'>";
    // private static final String PAD = "<html><table cellpadding='0'>";
    private const val PAD = "<html><table><td style='padding:1'>"
  }

  init {
    println(UIManager.getInt("Button.dashedRectGapX"))
    println(UIManager.getInt("Button.dashedRectGapY"))
    println(UIManager.getInt("Button.dashedRectGapHeight"))
    println(UIManager.getInt("Button.dashedRectGapWidth"))
    UIManager.put("Button.dashedRectGapX", 5)
    UIManager.put("Button.dashedRectGapY", 5)
    UIManager.put("Button.dashedRectGapHeight", 10)
    UIManager.put("Button.dashedRectGapWidth", 10)
    UIManager.put("Button.margin", Insets(8, 8, 8, 8))
    UIManager.put("ToggleButton.margin", Insets(8, 8, 8, 8))
    UIManager.put("RadioButton.margin", Insets(8, 8, 8, 8))
    UIManager.put("CheckBox.margin", Insets(8, 8, 8, 8))
    add(JButton("JButton"))
    add(Box.createHorizontalStrut(32))
    add(JToggleButton("JToggleButton"))
    add(Box.createHorizontalStrut(32))
    add(JCheckBox("JCheckBox"))
    add(object : JCheckBox("JCheckBox+BorderPainted") {
      override fun updateUI() {
        super.updateUI()
        setBorderPainted(true)
      }
    })
    add(JCheckBox(PAD + "JCheckBox+td.padding"))

    add(JRadioButton("JRadioButton"))
    add(JRadioButton(PAD + "JRadioButton+td.padding"))
    setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
