package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.net.URL
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  var url1 = cl.getResource("toolBarButtonGraphics/general/Copy24.gif")
  var url2 = cl.getResource("toolBarButtonGraphics/general/Cut24.gif")
  var url3 = cl.getResource("toolBarButtonGraphics/general/Help24.gif")
  if (url1 == null) {
    url1 = cl.getResource("example/Copy24.gif")
    url2 = cl.getResource("example/Cut24.gif")
    url3 = cl.getResource("example/Help24.gif")
  }

  val toolBar1 = JToolBar("ToolBarButton")
  toolBar1.add(JButton(ImageIcon(url1)))
  toolBar1.add(JButton(ImageIcon(url2)))
  toolBar1.add(Box.createGlue())
  toolBar1.add(JButton(ImageIcon(url3)))

  val toolBar2 = JToolBar("JButton")
  toolBar2.add(createToolBarButton(url1))
  toolBar2.add(createToolBarButton(url2))
  toolBar2.add(Box.createGlue())
  toolBar2.add(createToolBarButton(url3))

  val p = JPanel(BorderLayout())
  p.add(toolBar1, BorderLayout.NORTH)
  p.add(JScrollPane(JTextArea()))
  p.add(toolBar2, BorderLayout.SOUTH)
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
