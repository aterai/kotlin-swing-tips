package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.math.BigDecimal
import java.math.RoundingMode
import javax.swing.*

fun makeUI(): Component {
  val split1 = makeSplitPane("Default(ResizeWeight:0.5)")
  val split2 = makeSplitPane("SplitPaneWrapper(Keep ratio)")
  val list = listOf(split1, split2)
  val menu = JMenu("JSplitPane")
  menu.add("resetToPreferredSizes").addActionListener {
    list.forEach { it.resetToPreferredSizes() }
  }
  val mb = JMenuBar()
  mb.add(menu)
  EventQueue.invokeLater {
    split1.rootPane.jMenuBar = mb
    list.forEach { s -> s.setDividerLocation(.5) }
  }
  return JPanel(GridLayout(0, 1, 0, 0)).also {
    it.add(split1)
    it.add(SplitPaneWrapper(split2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSplitPane(title: String): JSplitPane {
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  split.border = BorderFactory.createTitledBorder(title)
  split.leftComponent = makeLabel(Color.CYAN)
  split.rightComponent = makeLabel(Color.ORANGE)
  split.resizeWeight = .5
  return split
}

private fun makeLabel(color: Color): JLabel {
  val label = JLabel(" ", SwingConstants.CENTER)
  label.isOpaque = true
  label.background = color
  label.addComponentListener(object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      label.text = "%04dpx".format(label.width)
    }
  })
  return label
}

private class SplitPaneWrapper(
  private val split: JSplitPane,
) : JPanel(BorderLayout()) {
  init {
    add(split)
  }

  override fun doLayout() {
    val size = getOrientedSize(split)
    val loc = split.dividerLocation
    val ratio = BigDecimal
      .valueOf(loc / size.toDouble())
      .setScale(2, RoundingMode.HALF_UP)
    super.doLayout()
    if (split.isShowing) {
      EventQueue.invokeLater {
        val sz = getOrientedSize(split)
        val iv = (.5 + sz * ratio.toDouble()).toInt()
        split.dividerLocation = iv
      }
    }
  }

  private fun getOrientedSize(sp: JSplitPane): Int {
    val b = sp.orientation == JSplitPane.VERTICAL_SPLIT
    return if (b) {
      sp.height - sp.dividerSize
    } else {
      sp.width - sp.dividerSize
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
