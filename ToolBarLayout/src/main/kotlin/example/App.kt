package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.net.URL
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    var url1 = javaClass.getResource("/toolbarButtonGraphics/general/Copy24.gif")
    var url2 = javaClass.getResource("/toolbarButtonGraphics/general/Cut24.gif")
    var url3 = javaClass.getResource("/toolbarButtonGraphics/general/Help24.gif")
    if (url1 == null) {
      url1 = javaClass.getResource("Copy24.gif")
      url2 = javaClass.getResource("Cut24.gif")
      url3 = javaClass.getResource("Help24.gif")
    }

    val toolbar1 = JToolBar("ToolBarButton")
    toolbar1.add(JButton(ImageIcon(url1)))
    toolbar1.add(JButton(ImageIcon(url2)))
    toolbar1.add(Box.createGlue())
    toolbar1.add(JButton(ImageIcon(url3)))

    val toolbar2 = JToolBar("JButton")
    toolbar2.add(createToolBarButton(url1))
    toolbar2.add(createToolBarButton(url2))
    toolbar2.add(Box.createGlue())
    toolbar2.add(createToolBarButton(url3))

    add(toolbar1, BorderLayout.NORTH)
    add(JScrollPane(JTextArea()))
    add(toolbar2, BorderLayout.SOUTH)
    preferredSize = Dimension(320, 240)
  }

  private fun createToolBarButton(url: URL?): JButton {
    val b = JButton(ImageIcon(url))
    b.isRequestFocusEnabled = false
    return b
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
