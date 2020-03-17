package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout(2, 2)) {
  // java - Laying out a keyboard in Swing - Stack Overflow
  // https://stackoverflow.com/questions/24622279/laying-out-a-keyboard-in-swing
  private val keys = arrayOf(
    arrayOf("`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "BS"),
    arrayOf("Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\", ""),
    arrayOf("Ctrl", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "Enter", ""),
    arrayOf("Shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "", "↑"),
    arrayOf("Fn", "Alt", "                                 ", "Alt", "←", "↓", "→"))

  init {
    val keyboard = makeKeyboardPanel()
    EventQueue.invokeLater { SwingUtilities.updateComponentTreeUI(keyboard) }

    val box = JPanel(FlowLayout(FlowLayout.CENTER))
    box.add(keyboard)

    add(box, BorderLayout.NORTH)
    add(JScrollPane(JTextArea()))
    setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeKeyboardPanel() = JPanel(GridBagLayout()).also {
    val c = GridBagConstraints()
    c.fill = GridBagConstraints.BOTH
    c.gridy = 50
    for (i in 0 until keys[0].size * 2) {
      c.gridx = i
      it.add(Box.createHorizontalStrut(KeyButton.SIZE))
    }
    for (row in keys.indices) {
      c.gridx = 0
      c.gridy = row
      for (key in keys[row]) {
        val len = key.length
        c.gridwidth = getKeyButtonColumnGridWidth(len)
        it.add(if (key.isEmpty()) Box.createHorizontalStrut(KeyButton.SIZE) else KeyButton(key, len <= 2), c)
        c.gridx += c.gridwidth
      }
    }
  }

  private fun getKeyButtonColumnGridWidth(length: Int) = when (length) {
    in 5..8 -> 4 // Shift, Enter
    in 2..4 -> 3 // Alt, Ctrl, Esc, Tab, ...
    1 -> 2 // A, B, C, ..., Z
    0 -> 1 // HorizontalStrut
    else -> 14 // Space
  }
}

class KeyButton(str: String, private val square: Boolean = true) : JButton(str) {
  init {
    // val font = getFont()
    // setFont(font.deriveFont(6f))
    setFocusable(false)
    putClientProperty("JComponent.sizeVariant", "mini")
    setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0))
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    if (square) {
      it.setSize(SIZE * 2, SIZE * 2)
    }
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
