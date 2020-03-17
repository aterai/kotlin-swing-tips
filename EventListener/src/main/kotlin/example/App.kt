package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.EventListener
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  private fun addFontChangeListener(l: FontChangeListener?) {
    listenerList.add(FontChangeListener::class.java, l)
  }

  // private fun removeFontChangeListener(l: FontChangeListener?) {
  //   listenerList.remove(FontChangeListener::class.java, l)
  // }

  private fun fireFontChangeEvent(font: Font) {
    val listeners = listenerList.listenerList
    val e = FontChangeEvent(this, font)
    var i = listeners.size - 2
    while (i >= 0) {
      if (listeners[i] === FontChangeListener::class.java) {
        (listeners[i + 1] as FontChangeListener).fontStateChanged(e)
      }
      i -= 2
    }
  }

  init {
    val button = MyButton("dummy")
    button.font = FONT12
    addFontChangeListener(button)

    val label = MyLabel("test")
    label.font = FONT12
    addFontChangeListener(label)

    val combo = MyComboBox()
    combo.model = DefaultComboBoxModel(arrayOf("item1", "item2"))
    combo.font = FONT12
    addFontChangeListener(combo)

    val menu = JMenu("Font")
    menu.toolTipText = "Select font size"
    menu.add("32pt").addActionListener {
      fireFontChangeEvent(FONT32)
    }
    menu.add("12pt").addActionListener {
      fireFontChangeEvent(FONT12)
    }

    val menuBar = JMenuBar()
    menuBar.add(menu)

    val panel = JPanel()
    panel.add(label)
    panel.add(combo)
    panel.add(button)
    add(menuBar, BorderLayout.NORTH)
    add(panel)
    preferredSize = Dimension(320, 240)
  }

  companion object {
    private val FONT12 = Font(Font.SANS_SERIF, Font.PLAIN, 12)
    private val FONT32 = Font(Font.SANS_SERIF, Font.PLAIN, 32)
  }
}

interface FontChangeListener : EventListener {
  fun fontStateChanged(e: FontChangeEvent)
}

class FontChangeEvent(source: Any?, val font: Font?) : EventObject(source) {
  companion object {
    private const val serialVersionUID = 1L
  }
}

private class MyComboBox : JComboBox<String>(), FontChangeListener {
  override fun fontStateChanged(e: FontChangeEvent) {
    font = e.font
  }
}

private class MyLabel(str: String) : JLabel(str), FontChangeListener {
  override fun fontStateChanged(e: FontChangeEvent) {
    font = e.font
  }
}

private class MyButton(str: String) : JButton(str), FontChangeListener {
  override fun fontStateChanged(e: FontChangeEvent) {
    font = e.font
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
