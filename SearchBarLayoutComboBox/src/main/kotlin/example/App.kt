package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(GridLayout(4, 1, 5, 5))
  p.add(JLabel("Default JComboBox"))
  p.add(JComboBox(arrayOf("Google", "Yahoo!", "Bing")))
  p.add(JLabel("SearchBar JComboBox"))

  val m = SearchEngineComboBoxModel<example.SearchEngine>()
  m.addElement(SearchEngine("Google", "http://www.google.com/", makeIcon("example/google.png")))
  m.addElement(SearchEngine("Yahoo!", "http://www.yahoo.com/", makeIcon("example/yahoo.png")))
  m.addElement(SearchEngine("Bing", "http://www.bing.com/", makeIcon("example/bing.png")))

  val combo = JSearchBar(m)
  combo.getEditor().setItem("java swing")

  p.add(combo)
  // p.add(new SearchBarComboBox(makeModel()));
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 20, 5, 20)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeIcon(path: String): ImageIcon {
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  return ImageIcon(img)
}

private fun makeMissingImage(): BufferedImage {
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
