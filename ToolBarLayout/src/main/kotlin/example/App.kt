package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.net.URL
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  var url1 = cl.getResource("toolbarButtonGraphics/general/Copy24.gif")
  var url2 = cl.getResource("toolbarButtonGraphics/general/Cut24.gif")
  var url3 = cl.getResource("toolbarButtonGraphics/general/Help24.gif")
  if (url1 == null) {
    url1 = cl.getResource("example/Copy24.gif")
    url2 = cl.getResource("example/Cut24.gif")
    url3 = cl.getResource("example/Help24.gif")
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

  val p = JPanel(BorderLayout())
  p.add(toolbar1, BorderLayout.NORTH)
  p.add(JScrollPane(JTextArea()))
  p.add(toolbar2, BorderLayout.SOUTH)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun createToolBarButton(url: URL?): JButton {
  val b = JButton(ImageIcon(url))
  b.isRequestFocusEnabled = false
  return b
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
