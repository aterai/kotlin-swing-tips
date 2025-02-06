package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*

fun makeUI(): Component {
  val p = JPanel(GridLayout(4, 1, 5, 5))
  p.add(JLabel("Default JComboBox"))
  p.add(JComboBox(arrayOf("Google", "Yahoo!", "Bing")))
  p.add(JLabel("SearchBar JComboBox"))

  val m = SearchEngineComboBoxModel<example.SearchEngine>()
  m.addElement(SearchEngine("Google", "https://www.google.com/", makeIcon("google")))
  m.addElement(SearchEngine("Yahoo!", "https://www.yahoo.com/", makeIcon("yahoo")))
  m.addElement(SearchEngine("Bing", "https://www.bing.com/", makeIcon("bing")))

  val combo = SearchBarComboBox(m)
  combo.getEditor().setItem("java swing")

  p.add(combo)
  // p.add(SearchBarComboBox(makeModel()))
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 20, 5, 20)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeIcon(name: String): ImageIcon {
  val path = "example/$name.png"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  return if (url == null) ImageIcon(makeMissingImage()) else ImageIcon(url)
}

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val iw = missingIcon.iconWidth
  val ih = missingIcon.iconHeight
  val bi = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, (16 - iw) / 2, (16 - ih) / 2)
  g2.dispose()
  return bi
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
