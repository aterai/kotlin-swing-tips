package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI

private fun makePanel(): Component {
  val p = JPanel(BorderLayout(5, 5))
  p.add(JComboBox(arrayOf("1111", "2222222", "3333333")))
  val items = arrayOf("JComboBox 11111:", "JComboBox 222:", "JComboBox 33:")
  val comboBox = object : JComboBox<String>(items) {
    override fun updateUI() {
      super.updateUI()
      UIManager.put("ComboBox.squareButton", false)
      UIManager.put("ComboBox.background", p.background)
      val tmp = object : BasicComboBoxUI() {
        override fun createArrowButton() = JButton().also {
          it.border = BorderFactory.createEmptyBorder()
          it.isVisible = false
        }
      }
      setUI(tmp)
      val r = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
          (it as? JLabel)?.horizontalAlignment = SwingConstants.RIGHT
          if (isSelected) {
            it.foreground = list.selectionForeground
            it.background = list.selectionBackground
          } else {
            it.foreground = list.foreground
            it.background = list.background
          }
        }
      }
      border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
      isOpaque = false
      isFocusable = false
    }
  }
  p.add(comboBox, BorderLayout.WEST)
  p.border = BorderFactory.createTitledBorder("JComboBox + JComboBox")
  return p
}

fun makeUI(): Component {
  val p = JPanel(BorderLayout(5, 5))
  p.add(JLabel("JLabel:"), BorderLayout.WEST)
  p.add(JTextField("JTextField"))
  p.border = BorderFactory.createTitledBorder("JLabel + JTextField")

  val panel = JPanel(BorderLayout(25, 25))
  panel.add(makePanel(), BorderLayout.NORTH)
  panel.add(p, BorderLayout.SOUTH)
  panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  return JPanel(BorderLayout()).also {
    it.add(panel, BorderLayout.NORTH)
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
