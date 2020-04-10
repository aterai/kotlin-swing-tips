package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

fun makeUI(): Component {
  val lbl1 = object : JLabel("11111111") {
    override fun getPreferredSize() = Dimension(1200, 600)
  }
  val lbl2 = object : JLabel("22222222") {
    override fun getPreferredSize() = Dimension(600, 1200)
  }
  val sp1 = JScrollPane(lbl1)
  val sp2 = JScrollPane(lbl2)
  val cl = object : ChangeListener {
    private var adjusting = false
    override fun stateChanged(e: ChangeEvent) {
      var src: JViewport? = null
      var tgt: JViewport? = null
      if (e.source === sp1.viewport) {
        src = sp1.viewport
        tgt = sp2.viewport
      } else if (e.source === sp2.viewport) {
        src = sp2.viewport
        tgt = sp1.viewport
      }
      if (adjusting || tgt == null || src == null) {
        return
      }
      adjusting = true
      val dim1 = src.viewSize
      val siz1 = src.size
      val pnt1 = src.viewPosition
      val dim2 = tgt.viewSize
      val siz2 = tgt.size
      val d1 = pnt1.getY() / (dim1.height - siz1.height) * (dim2.height - siz2.height)
      pnt1.y = d1.toInt()
      val d2 = pnt1.getX() / (dim1.width - siz1.width) * (dim2.width - siz2.width)
      pnt1.x = d2.toInt()
      tgt.viewPosition = pnt1
      adjusting = false
    }
  }
  sp1.viewport.addChangeListener(cl)
  sp2.viewport.addChangeListener(cl)
  return JPanel(GridLayout(2, 1)).also {
    it.add(sp1)
    it.add(sp2)
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
