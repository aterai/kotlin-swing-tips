package example

import java.awt.*
import javax.swing.*

// java - Laying out a keyboard in Swing - Stack Overflow
// https://stackoverflow.com/questions/24622279/laying-out-a-keyboard-in-swing
private val keys = arrayOf(
  arrayOf("`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BS"),
  arrayOf("Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\", ""),
  arrayOf("Ctrl", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "Enter", ""),
  arrayOf("Shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "", "↑"),
  arrayOf("Fn", "Alt", "                                 ", "Alt", "←", "↓", "→"),
)

fun createUI(): Component {
  val box = JPanel(FlowLayout(FlowLayout.CENTER))
  box.add(createKeyboardPanel())

  return JPanel(BorderLayout(2, 2)).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea()))
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createKeyboardPanel() = JPanel(GridBagLayout()).also {
  val c = GridBagConstraints()
  c.fill = GridBagConstraints.BOTH
  var columns = 0
  for (row in keys.indices) {
    c.gridx = 0
    c.gridy = row
    for (key in keys[row]) {
      c.gridwidth = getGridWidth(key)
      val b = if (key.isEmpty()) {
        Box.createHorizontalStrut(KeyButton.SIZE)
      } else {
        KeyButton(key, key.length <= 2)
      }
      it.add(b, c)
      c.gridx += c.gridwidth
    }
    columns = maxOf(columns, c.gridx)
  }

  // Guide row: zero-height struts that fix the width of every grid column
  c.gridy = keys.size
  c.gridwidth = 1
  for (i in 0..<columns) {
    c.gridx = i
    it.add(Box.createHorizontalStrut(KeyButton.SIZE), c)
  }
}

private fun getGridWidth(key: String) = when (key.length) {
  // empty string: invisible spacer
  0 -> 1

  // default keys
  1 -> 2

  // Tab, Ctrl, Alt, BS, Fn
  in 2..4 -> 3

  // Shift, Enter
  in 5..10 -> 4

  // space bar
  else -> 14
}

private class KeyButton(
  text: String,
  private val square: Boolean,
) : JButton(text) {
  override fun updateUI() {
    // The size variant must be set before the UI delegate is installed,
    // otherwise it is not applied until the next updateUI() call
    putClientProperty("JComponent.sizeVariant", "mini")
    super.updateUI()
    isFocusable = false
    border = BorderFactory.createEmptyBorder()
  }

  override fun getPreferredSize(): Dimension = if (square) {
    Dimension(SIZE * 2, SIZE * 2)
  } else {
    super.getPreferredSize()
  }

  companion object {
    const val SIZE = 10
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
