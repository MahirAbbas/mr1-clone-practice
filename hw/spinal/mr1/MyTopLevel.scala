package mr1

import spinal.core._

// Hardware definition
case class MyTopLevel() extends Component {


}
object MyTopLevelVerilog extends App {
  Config.spinal.generateVerilog(MyTopLevel())
}
