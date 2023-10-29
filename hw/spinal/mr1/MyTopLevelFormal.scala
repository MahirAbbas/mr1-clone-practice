package mr1

import spinal.core._
import spinal.core.formal._

// You need SymbiYosys to be installed.
// See https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Formal%20verification/index.html#installing-requirements
object MyTopLevelFormal extends App {
  FormalConfig
    .withBMC(10)
    .doVerify(new Component {
      val dut = FormalDut(MyTopLevel())

      // Ensure the formal test start with a reset
      assumeInitial(clockDomain.isResetActive)

      // Provide some stimulus

      // Check the state initial value and increment
    })
}
