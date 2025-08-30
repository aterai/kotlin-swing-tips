package example

import com.sun.java.swing.plaf.windows.WindowsScrollBarUI
import java.awt.*
import javax.swing.*
import javax.swing.plaf.metal.MetalScrollBarUI
import kotlin.math.roundToInt

fun makeUI(): Component {
  val scroll = object : JScrollPane(JTable(20, 3)) {
    override fun updateUI() {
      super.updateUI()
      val vsb = verticalScrollBar
      val ui2 = if (vsb.ui is WindowsScrollBarUI) {
        WindowsCustomScrollBarUI()
      } else {
        MetalCustomScrollBarUI()
      }
      vsb.setUI(ui2)
      vsb.unitIncrement = 10
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class WindowsCustomScrollBarUI : WindowsScrollBarUI() {
  override fun layoutVScrollbar(sb: JScrollBar) {
    val sbSize = sb.size
    val sbInsets = sb.insets
    var decrButtonH = decrButton.preferredSize.height
    var incrButtonH = incrButton.preferredSize.height
    var incrButtonY = sbSize.height - sbInsets.bottom - incrButtonH
    val sbInsetsH = sbInsets.top + sbInsets.bottom
    val sbButtonsH = decrButtonH + incrButtonH
    val gaps = decrGap + incrGap
    val trackH = sbSize.height - sbInsetsH - sbButtonsH - gaps
    val min = sb.minimum.toFloat()
    val extent = sb.visibleAmount.toFloat()
    val range = sb.maximum - min
    val value = sb.value.toFloat()
    val maxHeight = getMaximumThumbSize().height
    var thumbH = if (range <= 0) {
      maxHeight
    } else {
      (trackH * (extent / range)).toInt()
    }
    val minHeight = getMinimumThumbSize().height
    thumbH = thumbH.coerceIn(minHeight, maxHeight)
    var thumbY = incrButtonY - incrGap - thumbH
    if (value < sb.maximum - sb.visibleAmount) {
      val thumbRange = trackH - thumbH
      thumbY = (thumbRange * ((value - min) / (range - extent))).roundToInt()
    }

    val sbAvailButtonH = sbSize.height - sbInsetsH
    if (sbAvailButtonH < sbButtonsH) {
      incrButtonH = sbAvailButtonH / 2
      decrButtonH = sbAvailButtonH / 2
      incrButtonY = sbSize.height - sbInsets.bottom - incrButtonH
    }

    val itemW = sbSize.width - sbInsets.left - sbInsets.right
    val itemX = sbInsets.left
    val decrButtonY = sbSize.height - sbInsets.bottom - incrButtonH - decrButtonH
    decrButton.setBounds(itemX, decrButtonY, itemW, decrButtonH)
    incrButton.setBounds(itemX, incrButtonY, itemW, incrButtonH)

    val itrackY = 0
    val itrackH = decrButtonY - itrackY
    trackRect.setBounds(itemX, itrackY, itemW, itrackH)

    if (thumbH >= trackH) {
      setThumbBounds(0, 0, 0, 0)
    } else {
      thumbY = thumbY.coerceIn(0, decrButtonY - decrGap - thumbH)
      setThumbBounds(itemX, thumbY, itemW, thumbH)
    }
  }
}

private class MetalCustomScrollBarUI : MetalScrollBarUI() {
  override fun layoutVScrollbar(sb: JScrollBar) {
    val sbSize = sb.size
    val sbInsets = sb.insets
    var decrButtonH = decrButton.preferredSize.height
    var incrButtonH = incrButton.preferredSize.height
    var incrButtonY = sbSize.height - sbInsets.bottom - incrButtonH
    val sbInsetsH = sbInsets.top + sbInsets.bottom
    val sbButtonsH = decrButtonH + incrButtonH
    val gaps = decrGap + incrGap
    val trackH = sbSize.height - sbInsetsH - sbButtonsH - gaps
    val min = sb.minimum.toFloat()
    val extent = sb.visibleAmount.toFloat()
    val range = sb.maximum - min
    val value = sb.value.toFloat()
    val maxHeight = getMaximumThumbSize().height
    var thumbH = if (range <= 0) {
      maxHeight
    } else {
      (trackH * (extent / range)).toInt()
    }
    val minHeight = getMinimumThumbSize().height
    thumbH = thumbH.coerceIn(minHeight, maxHeight)
    var thumbY = incrButtonY - incrGap - thumbH
    if (value < sb.maximum - sb.visibleAmount) {
      val thumbRange = trackH - thumbH
      thumbY = (thumbRange * ((value - min) / (range - extent))).roundToInt()
    }

    val sbAvailButtonH = sbSize.height - sbInsetsH
    if (sbAvailButtonH < sbButtonsH) {
      incrButtonH = sbAvailButtonH / 2
      decrButtonH = sbAvailButtonH / 2
      incrButtonY = sbSize.height - sbInsets.bottom - incrButtonH
    }

    val itemW = sbSize.width - sbInsets.left - sbInsets.right
    val itemX = sbInsets.left
    val decrButtonY = sbSize.height - sbInsets.bottom - incrButtonH - decrButtonH
    decrButton.setBounds(itemX, decrButtonY, itemW, decrButtonH)
    incrButton.setBounds(itemX, incrButtonY, itemW, incrButtonH)

    val itrackY = 0
    val itrackH = decrButtonY - itrackY
    trackRect.setBounds(itemX, itrackY, itemW, itrackH)

    if (thumbH >= trackH) {
      setThumbBounds(0, 0, 0, 0)
    } else {
      // if (thumbY + thumbH > decrButtonY - decrGap) {
      //   thumbY = decrButtonY - decrGap - thumbH
      // }
      // if (thumbY < 0) {
      //   thumbY = 1
      // }
      thumbY = thumbY.coerceIn(0, decrButtonY - decrGap - thumbH)
      setThumbBounds(itemX, thumbY, itemW, thumbH)
    }
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
