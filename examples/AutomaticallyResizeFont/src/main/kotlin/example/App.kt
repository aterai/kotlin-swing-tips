package example

import java.awt.*
import javax.swing.*
import kotlin.math.abs

private const val TEST = "1234567890\nabc def ghi jkl mno"

fun makeUI(): Component {
  val font = Font(Font.MONOSPACED, Font.PLAIN, 12)
  val editor1 = JTextPane()
  editor1.font = font
  editor1.text = "Default\n$TEST"

  val editor2 = object : JTextPane() {
    private val rect = Rectangle()
    private var fontSize = 0f

    override fun doLayout() {
      val f = .08f * SwingUtilities.calculateInnerArea(this, rect).width
      val fontSizeShouldChange = abs(fontSize - f) > 1.0e-1
      if (fontSizeShouldChange) {
        setFont(font.deriveFont(f))
        fontSize = f
      }
      super.doLayout()
    }
  }
  editor2.font = font
  editor2.text = "doLayout + deriveFont\n$TEST"

  return JSplitPane(JSplitPane.VERTICAL_SPLIT, editor1, editor2).also {
    it.topComponent = editor1
    it.bottomComponent = editor2
    it.resizeWeight = .5
    it.preferredSize = Dimension(320, 240)
    EventQueue.invokeLater { it.setDividerLocation(.5) }
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
