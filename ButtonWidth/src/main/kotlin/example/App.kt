package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val b1 = Box.createHorizontalBox().also {
      it.add(Box.createHorizontalGlue())
      it.add(JButton("default"))
      it.add(Box.createHorizontalStrut(5))
      it.add(JButton("a"))
      it.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5))
    }
    val b2 = createRightAlignBox2(listOf(JButton("getPreferredSize"), JButton("xxx")), 120, 5)
    val b3 = createRightAlignBox3(listOf(JButton("Spring+Box"), JButton("Layout")), 100, 5)
    val b4 = createRightAlignBox4(listOf(JButton("SpringLayout"), JButton("gap:2")), 120, 2)
    val b5 = createRightAlignBox5(listOf(JButton("GridLayout+Box"), JButton("gap:2")), 2)
    val b6 = createRightAlignBox6(listOf(JButton("GridBugLayout"), JButton("gap:2")), 120, 2)

    val box = Box.createVerticalBox()
    listOf(b6, b5, b4, b3, b2, b1).forEach {
      box.add(JSeparator())
      box.add(it)
    }
    add(box, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun createRightAlignBox6(list: List<Component>, width: Int, gap: Int): Component {
    val p = JPanel(GridBagLayout())
    val c = GridBagConstraints()
    c.insets = Insets(0, gap, 0, 0)
    list.forEach {
      c.ipadx = width - it.getPreferredSize().width
      p.add(it, c)
    }
    p.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap))
    val pp = JPanel(BorderLayout())
    pp.add(p, BorderLayout.EAST)
    return pp
  }

  private fun createRightAlignBox5(list: List<Component>, gap: Int): Component {
    val p = object : JPanel(GridLayout(1, list.size, gap, gap)) {
      override fun getMaximumSize() = super.getPreferredSize()
    }
    list.forEach { p.add(it) }
    return Box.createHorizontalBox().also {
      it.add(Box.createHorizontalGlue())
      it.add(p)
      it.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap))
    }
  }

  private fun createRightAlignBox4(list: List<Component>, width: Int, gap: Int): Component {
    val layout = SpringLayout()
    val p = object : JPanel(layout) {
      override fun getPreferredSize(): Dimension {
        val maxHeight = list.map { it.getPreferredSize().height }.max() ?: 0
        return Dimension(width * list.size + gap + gap, maxHeight + gap + gap)
      }
    }
    var x = layout.getConstraint(SpringLayout.WIDTH, p)
    val y = Spring.constant(gap)
    val g = Spring.minus(Spring.constant(gap))
    val w = Spring.constant(width)
    for (b in list) {
      val constraints = layout.getConstraints(b)
      x = Spring.sum(x, g)
      constraints.setConstraint(SpringLayout.EAST, x)
      constraints.setY(y)
      constraints.setWidth(w)
      p.add(b)
      x = Spring.sum(x, Spring.minus(w))
    }
    return p
  }

  private fun createRightAlignBox3(list: List<Component>, width: Int, gap: Int): Component {
    val layout = SpringLayout()
    val p = object : JPanel(layout) {
      override fun getPreferredSize(): Dimension {
        val maxHeight = list.map { it.getPreferredSize().height }.max() ?: 0
        return Dimension(width * list.size + gap + gap, maxHeight + gap + gap)
      }
    }
    val cons = layout.getConstraints(p)
    // cons.setConstraint(SpringLayout.SOUTH, Spring.constant(p.getPreferredSize().height))
    cons.setConstraint(SpringLayout.EAST, Spring.constant((width + gap) * list.size))

    var x = Spring.constant(0)
    val y = Spring.constant(gap)
    val g = Spring.constant(gap)
    val w = Spring.constant(width)
    for (b in list) {
      val constraints = layout.getConstraints(b)
      constraints.setX(x)
      constraints.setY(y)
      constraints.setWidth(w)
      p.add(b)
      x = Spring.sum(x, w)
      x = Spring.sum(x, g)
    }

    return Box.createHorizontalBox().also {
      it.add(Box.createHorizontalGlue())
      it.add(p)
    }
  }

  private fun createRightAlignBox2(list: List<Component>, width: Int, gap: Int): Component {
    val box = object : JPanel() {
      override fun updateUI() {
        list.forEach { it.setPreferredSize(null) }
        super.updateUI()
        EventQueue.invokeLater {
          val maxHeight = list.map { it.getPreferredSize().height }.max() ?: 0
          val d = Dimension(width, maxHeight)
          list.forEach { it.setPreferredSize(d) }
          revalidate()
        }
      }
    }
    box.setLayout(BoxLayout(box, BoxLayout.X_AXIS))
    box.add(Box.createHorizontalGlue())
    list.forEach {
      box.add(it)
      box.add(Box.createHorizontalStrut(gap))
    }
    box.setBorder(BorderFactory.createEmptyBorder(gap, 0, gap, 0))
    return box
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
