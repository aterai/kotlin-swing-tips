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
      arrayOf("Fn", "Alt", "                         ", "Alt", "←", "↓", "→"))

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
      for (col in 0 until keys[row].size) {
        val key = keys[row][col]
        val len = key.length
        c.gridwidth = getKeyButtonColumnWidth(len)
        it.add(if (key.isEmpty()) Box.createHorizontalStrut(KeyButton.SIZE) else KeyButton(key, len <= 2), c)
        c.gridx += c.gridwidth
      }
    }
  }

  private fun getKeyButtonColumnWidth(length: Int) = when {
    length > 10 -> 14
    length > 4 -> 4
    length > 1 -> 3
    length == 1 -> 2
    else -> 1
  }
}

internal class KeyButton(str: String, private val square: Boolean = true) : JButton(str) {
  init {
    // val font = getFont()
    // setFont(font.deriveFont(6f))
    setFocusable(false)
    putClientProperty("JComponent.sizeVariant", "mini")
    setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0))
  }

  override fun getPreferredSize() = if (square) Dimension(SIZE * 2, SIZE * 2) else super.getPreferredSize()

  companion object {
    const val SIZE = 10
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
