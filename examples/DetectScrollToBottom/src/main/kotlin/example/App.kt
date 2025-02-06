package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  UIManager.put("ScrollBar.minimumThumbSize", Dimension(32, 32))

  val textArea = JTextArea()
  val sb = StringBuilder()
  for (i in 0..2000) {
    sb.append("%04d%n".format(i))
  }
  textArea.text = sb.toString()
  textArea.caretPosition = 0

  return JPanel(GridLayout(1, 2)).also {
    it.add(makePanel(textArea))
    it.add(makePanel(JTable(100, 3)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePanel(c: Component): Component {
  val check = JCheckBox("JCheckBox")
  check.isEnabled = false
  val p = JPanel(BorderLayout())
  val scroll = JScrollPane(c)
  scroll.verticalScrollBar.model.addChangeListener { e ->
    (e.source as? BoundedRangeModel)?.also {
      val extent = it.extent
      val maximum = it.maximum
      val value = it.value
      if (value + extent >= maximum) {
        check.isEnabled = true
      }
    }
  }
  p.add(scroll)
  p.add(check, BorderLayout.SOUTH)
  return p
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
