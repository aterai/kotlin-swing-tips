package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val c = GridBagConstraints()
  c.insets = Insets(1, 1, 1, 1)
  c.fill = GridBagConstraints.BOTH
  c.gridx = 0
  c.gridy = 0
  val panel = JPanel(GridBagLayout())
  panel.add(makeButton("<html>Num<br>Lock"), c)
  c.gridx = GridBagConstraints.RELATIVE
  panel.add(makeButton("/"), c)
  panel.add(makeButton("*"), c)
  panel.add(makeButton("-"), c)
  c.gridx = 0
  c.gridy++
  panel.add(makeButton("7", "Home"), c)
  c.gridx = GridBagConstraints.RELATIVE
  panel.add(makeButton("8", "ª"), c)
  panel.add(makeButton("9", "PgUp"), c)
  c.gridheight = 2
  panel.add(makeButton("+", "+"), c)
  c.gridx = 0
  c.gridy++
  c.gridheight = 1
  panel.add(makeButton("4", "©"), c)
  c.gridx = GridBagConstraints.RELATIVE
  panel.add(makeButton("5"), c)
  panel.add(makeButton("6", "¨"), c)
  c.gridx = 0
  c.gridy++
  panel.add(makeButton("1", "End"), c)
  c.gridx = GridBagConstraints.RELATIVE
  panel.add(makeButton("2", "«"), c)
  panel.add(makeButton("3", "PgDn"), c)
  c.gridheight = 2
  val enter = makeButton("Enter", "Enter")
  panel.add(enter, c)
  EventQueue.invokeLater {
    panel.rootPane.defaultButton = enter
    enter.requestFocusInWindow()
  }
  c.gridx = 0
  c.gridy++
  c.gridwidth = 2
  c.gridheight = 1
  panel.add(makeButton("0", "Insert"), c)
  c.gridx = GridBagConstraints.RELATIVE
  c.gridwidth = 1
  panel.add(makeButton(".", "Delete"), c)
  panel.border = BorderFactory.createLineBorder(Color.WHITE)
  panel.preferredSize = Dimension(320, 240)
  return panel
}

private const val SIZE = 46

private fun makeButton(vararg s: String): JButton {
  val key = s[0]
  val sub = if (s.size > 1) s[1] else " "
  val gap = 2
  val border = BorderFactory.createEmptyBorder(gap, gap, gap, gap)
  val l1 = JLabel(key)
  l1.font = l1.font.deriveFont(12f)
  l1.border = border
  val l2 = JLabel(sub)
  l2.font = l2.font.deriveFont(9.5f)
  l2.border = border
  val button: JButton = object : JButton() {
    override fun updateUI() {
      super.updateUI()
      layout = BorderLayout()
      margin = border.getBorderInsets(this)
    }

    override fun getPreferredSize(): Dimension {
      val sz = SIZE - gap * 2
      return Dimension(sz, sz)
    }
  }
  if (key == sub) {
    button.add(l1)
  } else {
    button.add(l1, BorderLayout.NORTH)
    button.add(l2, BorderLayout.SOUTH)
  }
  return button
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
