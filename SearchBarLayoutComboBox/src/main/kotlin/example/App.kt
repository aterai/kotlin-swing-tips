package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val p = JPanel(GridLayout(4, 1, 5, 5))
    setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20))
    p.add(JLabel("Default JComboBox"))
    p.add(JComboBox<String>(arrayOf("Google", "Yahoo!", "Bing")))
    p.add(JLabel("SearchBar JComboBox"))

    val m = SearchEngineComboBoxModel<example.SearchEngine>()
    m.addElement(SearchEngine("Google", "http://www.google.com/", ImageIcon(javaClass.getResource("google.png"))))
    m.addElement(SearchEngine("Yahoo!", "http://www.yahoo.com/", ImageIcon(javaClass.getResource("yahoo.png"))))
    m.addElement(SearchEngine("Bing", "http://www.bing.com/", ImageIcon(javaClass.getResource("bing.png"))))

    val combo = JSearchBar(m)
    combo.getEditor().setItem("java swing")

    p.add(combo)
    // p.add(new SearchBarComboBox(makeModel()));
    add(p, BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
