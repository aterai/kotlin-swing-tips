package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.AlphaComposite
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cardLayout = CardLayout(50, 5)

  val cards = object : JPanel(cardLayout) {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      components.first { it.isVisible }?.also {
        paintSideComponents(g, getComponentZOrder(it))
      }
    }

    private fun paintSideComponents(g: Graphics, idx: Int) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.composite = AlphaComposite.SrcOver.derive(.5f)
      val insets = insets
      val hgap = cardLayout.hgap
      val vgap = cardLayout.vgap
      val nc = componentCount
      val cw = width - (hgap * 2 + insets.left + insets.right)
      // val ch = getHeight() - (vgap * 2 + insets.top + insets.bottom)
      val gap = 10
      val prev = getComponent(if (idx > 0) idx - 1 else nc - 1)
      g2.translate(hgap + insets.left - cw - gap, vgap + insets.top)
      prev.print(g2)
      val next = getComponent((idx + 1) % nc)
      g2.translate((cw + gap) * 2, 0)
      next.print(g2)
      g2.dispose()
    }
  }
  cards.add(JScrollPane(JTree()), "JTree")
  cards.add(JSplitPane(), "JSplitPane")
  cards.add(JScrollPane(JTable(9, 3)), "JTable")
  cards.add(JButton("JButton"), "JButton")

  val prevButton = JButton("Previous")
  prevButton.addActionListener {
    cardLayout.previous(cards)
    cards.repaint()
  }
  val nextButton = JButton("Next")
  nextButton.addActionListener {
    cardLayout.next(cards)
    cards.repaint()
  }
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(prevButton)
  box.add(Box.createHorizontalGlue())
  box.add(nextButton)

  val p = JPanel(BorderLayout())
  p.add(cards)
  p.add(box, BorderLayout.SOUTH)
  p.preferredSize = Dimension(320, 240)
  return p
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
