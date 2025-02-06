package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  UIManager.put("CheckBox.disabledText", Color.RED)
  UIManager.put("ComboBox.disabledForeground", Color.GREEN)
  UIManager.put("Button.disabledText", Color.YELLOW)
  UIManager.put("Label.disabledForeground", Color.ORANGE)
  val cbx1 = JCheckBox("default", true)
  val cbx2 = JCheckBox("<html>html <span style='color:red'>tag", true)
  val label = JLabel("label disabledForeground")
  val button = JButton("button disabledText")

  val combo1 = JComboBox(arrayOf("disabledForeground", "bb"))
  val combo2 = object : JComboBox<String>(arrayOf("<html>html</html>", "renderer")) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      val renderer = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        renderer
          .getListCellRendererComponent(
            list,
            value,
            index,
            isSelected,
            cellHasFocus,
          ).also {
            if (index < 0 && !isEnabled && it is JLabel) {
              it.text = "<html><font color='red'>" + it.text
              it.isOpaque = false
            }
          }
      }
    }
  }
  val combo3 = JComboBox(arrayOf("setEditable(true)", "setDisabledTextColor"))

  val cmpList = listOf<JComponent>(cbx1, cbx2, combo1, combo2, combo3, label, button)
  val cbx = JCheckBox("setEnabled")
  cbx.addActionListener { e ->
    val flg = (e.source as? JCheckBox)?.isSelected == true
    cmpList.forEach { it.isEnabled = flg }
  }
  combo3.isEditable = true
  (combo3.editor.editorComponent as? JTextField)?.disabledTextColor = Color.PINK

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 15, 5, 5)
  cmpList.forEach { c ->
    c.isEnabled = false
    c.alignmentX = Component.LEFT_ALIGNMENT
    val h = c.preferredSize.height
    c.maximumSize = Dimension(Int.MAX_VALUE, h)
    box.add(c)
    box.add(Box.createVerticalStrut(5))
  }
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(cbx, BorderLayout.NORTH)
    it.add(box)
    it.preferredSize = Dimension(320, 240)
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
