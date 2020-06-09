package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(GridLayout(4, 1, 5, 5))
  p.add(JLabel("Default JComboBox"))
  p.add(JComboBox(arrayOf("Google", "Yahoo!", "Bing")))
  p.add(JLabel("SearchBar JComboBox"))

  val cl = Thread.currentThread().contextClassLoader
  val m = SearchEngineComboBoxModel<example.SearchEngine>()
  m.addElement(SearchEngine("Google", "http://www.google.com/", ImageIcon(cl.getResource("example/google.png"))))
  m.addElement(SearchEngine("Yahoo!", "http://www.yahoo.com/", ImageIcon(cl.getResource("example/yahoo.png"))))
  m.addElement(SearchEngine("Bing", "http://www.bing.com/", ImageIcon(cl.getResource("example/bing.png"))))

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
